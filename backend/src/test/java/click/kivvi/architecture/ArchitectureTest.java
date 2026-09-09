package click.kivvi.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Layering rules for the wave-0 rows of {@code architecture/rules-translated.md}: {@code web} may
 * not be reached from {@code domain}, {@code infrastructure} may only be used from {@code
 * application}, and the four top-level packages form no cycle. Also the wave-2 rows: "Mercure topic
 * built only server-side" and, transitively, "Row markup in one place" on the backend side (the
 * frontend half of both — {@code no-restricted-syntax} on {@code /accounts/} literals, {@code
 * vue/no-restricted-class} on {@code event-row} — is enforced by {@code
 * frontend/eslint.config.js}).
 */
class ArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("click.kivvi");
  }

  @Test
  @DisplayName("domain never depends on web")
  void domainDoesNotDependOnWeb() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..web..");
    rule.check(classes);
  }

  @Test
  @DisplayName("infrastructure is only used from application")
  void infrastructureIsOnlyUsedFromApplication() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..infrastructure..")
            .should()
            .onlyBeAccessed()
            .byAnyPackage("..infrastructure..", "..application..");
    rule.check(classes);
  }

  @Test
  @DisplayName("web, application, domain and infrastructure form no package cycle")
  void topLevelPackagesFormNoCycle() {
    ArchRule rule =
        SlicesRuleDefinition.slices().matching("click.kivvi.(*)..").should().beFreeOfCycles();
    rule.check(classes);
  }

  @Test
  @DisplayName(
      "only application.tracking and infrastructure.mercure may depend on the Mercure publisher"
          + " types (wave-2: \"Mercure topic built only server-side\")")
  void onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher() {
    ArchRule rule =
        classes()
            .that()
            .haveSimpleName("MercurePublisher")
            .or()
            .haveSimpleName("HttpMercurePublisher")
            .should()
            .onlyBeAccessed()
            .byAnyPackage(
                "click.kivvi.application.tracking..", "click.kivvi.infrastructure.mercure..");
    rule.check(classes);
  }

  @Test
  @DisplayName(
      "the /accounts/ Mercure topic literal exists only in domain.tracking.EventStreamTopic —"
          + " a plain source scan, not an ArchUnit condition: ArchUnit reasons about compiled"
          + " bytecode class/member dependencies, and a string constant folded into every call"
          + " site by the compiler is not reliably distinguishable there from one held in a"
          + " single shared field, so this checks the .java sources directly instead.")
  void accountsTopicLiteralExistsOnlyInEventStreamTopic() throws IOException {
    Path sourceRoot = Path.of("src/main/java");
    List<Path> filesContainingLiteral;
    try (Stream<Path> files = Files.walk(sourceRoot)) {
      filesContainingLiteral =
          files
              .filter(path -> path.toString().endsWith(".java"))
              .filter(ArchitectureTest::containsAccountsTopicLiteral)
              .toList();
    }

    assertThat(filesContainingLiteral)
        .extracting(Path::toString)
        .containsExactly("src/main/java/click/kivvi/domain/tracking/EventStreamTopic.java");
  }

  private static boolean containsAccountsTopicLiteral(Path javaFile) {
    try {
      // Quote-anchored so a Javadoc mention like "{@code /accounts/1/events}" (no leading
      // quote) never counts as a real string literal composing the topic.
      return Files.readString(javaFile).contains("\"/accounts/");
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
