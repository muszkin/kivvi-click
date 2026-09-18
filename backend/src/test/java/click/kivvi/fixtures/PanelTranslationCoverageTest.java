package click.kivvi.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.SupportedLocale;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PIO-129's guard. The panel is being translated one page at a time, and two things can go wrong
 * quietly between slices: a page can be left out and nobody notices, or a page can be declared
 * translated while half its English copy is still the Polish original.
 *
 * <p>PIO-125's key-parity test catches neither. It asks whether a key exists in both catalogues,
 * not whether the English value is English — every one of the panel's Polish strings passes it.
 *
 * <p>So this test asks two different questions:
 *
 * <ol>
 *   <li><b>What is left?</b> Each page still serving Polish only marks itself with an {@code
 *       UNTRANSLATED} constant. The set of files carrying that marker must equal {@link
 *       #STILL_POLISH_ONLY} exactly — so finishing a page without removing its marker fails, and so
 *       does removing a marker without updating this list. When the list is empty the panel is
 *       done, and the marker can go away with it.
 *   <li><b>Is the finished work actually English?</b> For every page already translated, each
 *       English string must carry no Polish diacritic and must differ from its Polish counterpart.
 *       Values that are genuinely the same in both languages — a domain name, a unit, a person's
 *       name — are listed one by one in {@link #IDENTICAL_IN_BOTH_LANGUAGES} rather than waved
 *       through by a pattern.
 * </ol>
 *
 * <p>The second question is the one a diacritic check alone answers badly: {@code Kampania}, {@code
 * Typ}, {@code Masowa} and {@code Status} have no diacritics at all. Comparing the two languages
 * catches those, because an untranslated string is by definition identical to its original.
 */
class PanelTranslationCoverageTest {

  private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

  private static final String MARKER = "UNTRANSLATED = SupportedLocale.PL";

  /**
   * The pages PIO-129 has not reached yet, in the order its slices take them. Delete a line when
   * that page's copy exists in both languages — and delete its {@code UNTRANSLATED} constant in the
   * same change, or this test fails.
   */
  private static final Set<String> STILL_POLISH_ONLY =
      new TreeSet<>(
          Set.of(
              // Translated itself, but it also shows two lists borrowed from pages that are not:
              // the best-performing automations and the recently-seen customers. It leaves the list
              // when both of those do.
              "click/kivvi/application/DashboardViewService.java",
              "click/kivvi/application/AutomationsViewService.java",
              "click/kivvi/application/CampaignsViewService.java",
              "click/kivvi/application/CustomersViewService.java",
              "click/kivvi/application/FeedsViewService.java",
              "click/kivvi/application/ImportViewService.java",
              "click/kivvi/application/WidgetViewService.java",
              "click/kivvi/fixtures/FeedsFixtures.java",
              "click/kivvi/fixtures/SettingsFixtures.java"));

  /**
   * Strings that read the same in Polish and in English on purpose: the shop's domain and its
   * two-letter mark, two counts that are bare digits, and a peak reading written in units and a
   * clock time. Nothing here is a word, which is the point — a word that survives translation
   * unchanged is almost always a word nobody translated.
   */
  private static final Set<String> IDENTICAL_IN_BOTH_LANGUAGES =
      Set.of("aureashop.pl", "AS", "847", "312", "28 ev/s · 12:42:18");

  private static final String POLISH_DIACRITICS = "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ";

  @Test
  @DisplayName("PIO-129 exactly the pages listed here still carry the untranslated marker")
  void theRemainingPagesAreTheOnesDeclared() throws IOException {
    assertThat(filesCarryingTheMarker()).isEqualTo(STILL_POLISH_ONLY);
  }

  @Test
  @DisplayName("PIO-129 no English string on a translated page is still its Polish original")
  void translatedPagesCarryNoPolishLeftovers() {
    for (Pair pair : translatedPairs()) {
      if (IDENTICAL_IN_BOTH_LANGUAGES.contains(pair.polish())) {
        continue;
      }
      assertThat(pair.english())
          .as("%s — English value equal to the Polish one", pair.where())
          .isNotEqualTo(pair.polish());
    }
  }

  @Test
  @DisplayName("PIO-129 no English string on a translated page carries a Polish diacritic")
  void translatedPagesCarryNoPolishLetters() {
    for (Pair pair : translatedPairs()) {
      assertThat(hasPolishLetters(pair.english()))
          .as("%s — English value \"%s\" carries Polish letters", pair.where(), pair.english())
          .isFalse();
    }
  }

  /** One string in both languages, with enough context to name it when it fails. */
  private record Pair(String where, String polish, String english) {}

  private static List<Pair> translatedPairs() {
    List<Pair> pairs = new ArrayList<>();

    zip(pairs, "ShellFixtures.workspace.meta", locale -> ShellFixtures.workspace(locale).meta());
    zip(pairs, "ShellFixtures.workspace.name", locale -> ShellFixtures.workspace(locale).name());
    zip(pairs, "ShellFixtures.workspace.mark", locale -> ShellFixtures.workspace(locale).mark());

    zipList(
        pairs,
        "DashboardFixtures.kpis.label",
        locale -> map(DashboardFixtures.kpis(locale), DashboardFixtures.KpiSeed::label));
    zipList(
        pairs,
        "DashboardFixtures.kpis.value",
        locale -> map(DashboardFixtures.kpis(locale), DashboardFixtures.KpiSeed::value));
    zipList(
        pairs,
        "DashboardFixtures.kpis.delta",
        locale -> map(DashboardFixtures.kpis(locale), DashboardFixtures.KpiSeed::delta));
    zipList(
        pairs,
        "DashboardFixtures.legend.label",
        locale -> map(DashboardFixtures.legend(locale), DashboardFixtures.Legend::label));
    zipList(
        pairs,
        "DashboardFixtures.legend.value",
        locale -> map(DashboardFixtures.legend(locale), DashboardFixtures.Legend::value));

    zipList(
        pairs,
        "EventsFixtures.customers.name",
        locale -> map(EventsFixtures.customers(locale), EventsFixtures.SampleCustomer::name));
    zipList(pairs, "EventsFixtures.productPaths", EventsFixtures::productPaths);
    zipList(pairs, "EventsFixtures.productNames", EventsFixtures::productNames);
    zipList(pairs, "EventsFixtures.searchPhrases", EventsFixtures::searchPhrases);
    zipList(pairs, "EventsFixtures.abandonDelays", EventsFixtures::abandonDelays);
    zipList(pairs, "EventsFixtures.basketSizes", EventsFixtures::basketSizes);

    return pairs;
  }

  private static <T> List<String> map(List<T> values, Function<T, String> field) {
    return values.stream().map(field).toList();
  }

  private static void zip(List<Pair> pairs, String where, Function<SupportedLocale, String> value) {
    pairs.add(new Pair(where, value.apply(SupportedLocale.PL), value.apply(SupportedLocale.EN)));
  }

  private static void zipList(
      List<Pair> pairs, String where, Function<SupportedLocale, List<String>> values) {
    List<String> polish = values.apply(SupportedLocale.PL);
    List<String> english = values.apply(SupportedLocale.EN);

    assertThat(english)
        .as("%s — the two languages carry a different number of entries", where)
        .hasSameSizeAs(polish);

    for (int i = 0; i < polish.size(); i++) {
      pairs.add(new Pair(where + "[" + i + "]", polish.get(i), english.get(i)));
    }
  }

  private static boolean hasPolishLetters(String value) {
    return value.chars().anyMatch(codePoint -> POLISH_DIACRITICS.indexOf(codePoint) >= 0);
  }

  private static Set<String> filesCarryingTheMarker() throws IOException {
    assertThat(SOURCE_ROOT)
        .as(
            "the test runs from the backend module, so %s is reachable",
            SOURCE_ROOT.toAbsolutePath())
        .isDirectory();

    try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
      return files
          .filter(path -> path.toString().endsWith(".java"))
          .filter(PanelTranslationCoverageTest::carriesTheMarker)
          .map(path -> SOURCE_ROOT.relativize(path).toString().replace('\\', '/'))
          .collect(Collectors.toCollection(TreeSet::new));
    }
  }

  private static boolean carriesTheMarker(Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8).contains(MARKER);
    } catch (IOException cause) {
      throw new IllegalStateException("Cannot read " + path, cause);
    }
  }
}
