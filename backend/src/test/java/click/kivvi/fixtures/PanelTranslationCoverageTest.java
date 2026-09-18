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
   * Empty, and it has to stay empty: every page of the panel now exists in both languages. The
   * check below is what stops a new one arriving in Polish only — a page that reaches for an {@code
   * UNTRANSLATED} constant must declare itself here first, which turns the omission into a decision
   * someone wrote down rather than something nobody noticed.
   */
  private static final Set<String> STILL_POLISH_ONLY = new TreeSet<>(Set.of());

  /**
   * Strings that read the same in Polish and in English on purpose: the shop's domain and its
   * two-letter mark, two counts that are bare digits, and a peak reading written in units and a
   * clock time. Nothing here is a word, which is the point — a word that survives translation
   * unchanged is almost always a word nobody translated.
   */
  private static final Set<String> IDENTICAL_IN_BOTH_LANGUAGES =
      Set.of(
          "aureashop.pl",
          "AS",
          "847",
          "312",
          "28 ev/s · 12:42:18",
          // A campaign named after the shopping weekend it runs on; the name is the same in both.
          "Black weekend — VIP",
          // The customer segment, and the initialism is the same word in both languages.
          "VIP",
          // A role name and two provider names that are the same word in both.
          "Administrator",
          "Marketer",
          // Sections carry only the fields their own kind uses; the rest are absent in both.
          "");

  /** The three shapes {@code WidgetFixtures.content} answers with. */
  private static final List<String> WIDGET_CONTENT_TYPES = List.of("banner", "toast", "modal");

  private static final String POLISH_DIACRITICS = "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ";

  @Test
  @DisplayName("PIO-129 no page serves Polish only, and none may start to without saying so")
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

    zipList(
        pairs,
        "AutomationsFixtures.all.name",
        locale -> map(AutomationsFixtures.all(locale), AutomationsFixtures.Automation::name));
    zipList(
        pairs,
        "AutomationsFixtures.activeForCustomer.name",
        locale ->
            map(
                AutomationsFixtures.activeForCustomer(locale),
                AutomationsFixtures.ActiveAutomation::name));
    zipList(
        pairs,
        "AutomationsFixtures.editorTabs.label",
        locale ->
            map(AutomationsFixtures.editorTabs(locale), AutomationsFixtures.EditorTab::label));
    zipList(
        pairs,
        "AutomationsFixtures.pipelineSteps.title",
        locale ->
            map(
                AutomationsFixtures.pipelineSteps(locale),
                AutomationsFixtures.PipelineStep::title));
    zipList(
        pairs,
        "AutomationsFixtures.pipelineSteps.kicker",
        locale ->
            map(
                AutomationsFixtures.pipelineSteps(locale),
                AutomationsFixtures.PipelineStep::kicker));
    zipList(
        pairs,
        "AutomationsFixtures.pipelineSteps.addLabel",
        locale ->
            map(
                AutomationsFixtures.pipelineSteps(locale),
                AutomationsFixtures.PipelineStep::addLabel));
    zipList(
        pairs,
        "AutomationsFixtures.pipelineSteps.blocks.title",
        PanelTranslationCoverageTest::pipelineBlockTitles);
    zipList(
        pairs,
        "AutomationsFixtures.pipelineSteps.blocks.body",
        PanelTranslationCoverageTest::pipelineBlockBodies);
    zipList(
        pairs,
        "AutomationsFixtures.flowNodes.title",
        locale -> map(AutomationsFixtures.flowNodes(locale), AutomationsFixtures.FlowNode::title));
    zipList(
        pairs,
        "AutomationsFixtures.flowNodes.kicker",
        locale -> map(AutomationsFixtures.flowNodes(locale), AutomationsFixtures.FlowNode::kicker));
    zipList(
        pairs,
        "AutomationsFixtures.simulation.label",
        locale ->
            map(AutomationsFixtures.simulation(locale), AutomationsFixtures.SimulationItem::label));
    zipList(
        pairs,
        "CampaignsFixtures.all.name",
        locale -> map(CampaignsFixtures.all(locale), CampaignsFixtures.Campaign::name));
    zipList(
        pairs,
        "CampaignsFixtures.blocks.label",
        locale -> map(CampaignsFixtures.blocks(locale), CampaignsFixtures.Block::label));
    zipList(
        pairs,
        "CampaignsFixtures.variables.label",
        locale ->
            map(CampaignsFixtures.variables(locale), CampaignsFixtures.Variable::description));
    zipList(
        pairs,
        "CampaignsFixtures.sections.title",
        locale -> nullSafe(CampaignsFixtures.sections(locale), CampaignsFixtures.Section::title));
    zipList(
        pairs,
        "CampaignsFixtures.sections.kicker",
        locale -> nullSafe(CampaignsFixtures.sections(locale), CampaignsFixtures.Section::kicker));
    zipList(
        pairs,
        "CampaignsFixtures.sections.body",
        locale -> nullSafe(CampaignsFixtures.sections(locale), CampaignsFixtures.Section::body));
    zip(
        pairs,
        "CampaignsFixtures.selectedBlock.blockName",
        locale -> CampaignsFixtures.selectedBlock(locale).blockName());
    zip(
        pairs,
        "CampaignsFixtures.selectedBlock.title",
        locale -> CampaignsFixtures.selectedBlock(locale).title());

    zipList(
        pairs,
        "WidgetFixtures.all.name",
        locale -> map(WidgetFixtures.all(locale), WidgetFixtures.Widget::name));
    zipList(
        pairs,
        "WidgetFixtures.blocks.label",
        locale -> map(WidgetFixtures.blocks(locale), WidgetFixtures.Block::label));
    zipList(
        pairs,
        "WidgetFixtures.audience.label",
        locale -> map(WidgetFixtures.audience(locale), WidgetFixtures.AudienceRule::label));
    zipList(
        pairs,
        "WidgetFixtures.content.title",
        locale ->
            WIDGET_CONTENT_TYPES.stream()
                .map(type -> WidgetFixtures.content(locale, type).title())
                .toList());
    zipList(
        pairs,
        "WidgetFixtures.content.cta",
        locale ->
            WIDGET_CONTENT_TYPES.stream()
                .map(type -> WidgetFixtures.content(locale, type).cta())
                .toList());

    zipList(
        pairs,
        "CustomersFixtures.all.name",
        locale -> map(CustomersFixtures.all(locale), CustomersFixtures.Customer::name));
    zipList(
        pairs,
        "CustomersFixtures.all.segment",
        locale ->
            CustomersFixtures.all(locale).stream()
                .map(customer -> customer.segment().label())
                .toList());

    zipList(
        pairs,
        "FeedsFixtures.kpis.label",
        locale -> map(FeedsFixtures.kpis(locale), FeedsFixtures.Kpi::label));
    zipList(
        pairs,
        "FeedsFixtures.sources.sub",
        locale -> map(FeedsFixtures.sources(locale), FeedsFixtures.Source::sub));
    zipList(
        pairs,
        "FeedsFixtures.rawFeeds.schedule",
        locale -> map(FeedsFixtures.rawFeeds(locale), FeedsFixtures.RawFeed::schedule));
    zipList(
        pairs,
        "FeedsFixtures.coverage.label",
        locale -> map(FeedsFixtures.coverage(locale), FeedsFixtures.CoverageBar::label));
    zipList(
        pairs,
        "FeedsFixtures.fallbackRules.text",
        locale -> map(FeedsFixtures.fallbackRules(locale), FeedsFixtures.FallbackRule::text));

    zipList(
        pairs,
        "ImportFixtures.steps.label",
        locale -> map(ImportFixtures.steps(locale), ImportFixtures.StepDef::label));
    zipList(
        pairs,
        "ImportFixtures.targets.label",
        locale -> map(ImportFixtures.targets(locale), ImportFixtures.Target::label));
    zipList(
        pairs,
        "ImportFixtures.rawColumns.name",
        locale -> map(ImportFixtures.rawColumns(locale), ImportFixtures.RawColumn::name));
    zipList(
        pairs,
        "ImportFixtures.validations.label",
        locale -> map(ImportFixtures.validations(locale), ImportFixtures.ValidationCheck::label));
    zipList(
        pairs,
        "ImportFixtures.dedupStrategies.label",
        locale -> map(ImportFixtures.dedupStrategies(locale), ImportFixtures.DedupStrategy::label));
    zipList(
        pairs,
        "ImportFixtures.rawPreviewRows.name",
        locale -> map(ImportFixtures.rawPreviewRows(locale), ImportFixtures.RawPreviewRow::name));
    zip(pairs, "ImportFixtures.fileMeta", ImportFixtures::fileMeta);
    zip(pairs, "ImportFixtures.defaultFileName", ImportFixtures::defaultFileName);

    zipList(
        pairs,
        "SettingsFixtures.tabs.label",
        locale -> map(SettingsFixtures.tabs(locale), SettingsFixtures.Tab::label));
    zipList(
        pairs,
        "SettingsFixtures.subtitle",
        locale ->
            SettingsFixtures.tabs(locale).stream()
                .map(tab -> SettingsFixtures.subtitle(locale, tab.id()))
                .toList());
    zipList(
        pairs,
        "SettingsFixtures.roles.name",
        locale -> map(SettingsFixtures.roles(locale), SettingsFixtures.Role::name));
    zipList(
        pairs,
        "SettingsFixtures.roles.description",
        locale -> map(SettingsFixtures.roles(locale), SettingsFixtures.Role::description));
    zipList(
        pairs,
        "SettingsFixtures.team.role",
        locale -> map(SettingsFixtures.team(locale), SettingsFixtures.TeamMember::role));
    zipList(
        pairs,
        "SettingsFixtures.emailProviders.statusLabel",
        locale ->
            map(
                SettingsFixtures.emailProviders(locale),
                SettingsFixtures.EmailProvider::statusLabel));
    zipList(
        pairs,
        "SettingsFixtures.apiLimits.label",
        locale -> map(SettingsFixtures.apiLimits(locale), SettingsFixtures.Bar::label));
    zipList(
        pairs,
        "SettingsFixtures.notificationMatrix.label",
        locale ->
            map(
                SettingsFixtures.notificationMatrix(locale),
                SettingsFixtures.NotificationRow::label));
    zipList(
        pairs,
        "SettingsFixtures.retentionPolicies.label",
        locale ->
            map(
                SettingsFixtures.retentionPolicies(locale),
                SettingsFixtures.RetentionPolicy::label));
    zipList(
        pairs,
        "SettingsFixtures.dataSubjectRequests.type",
        locale ->
            map(
                SettingsFixtures.dataSubjectRequests(locale),
                SettingsFixtures.DataSubjectRequest::type));

    zipList(
        pairs,
        "AutomationsFixtures.simulation.note",
        locale ->
            map(AutomationsFixtures.simulation(locale), AutomationsFixtures.SimulationItem::note));

    return pairs;
  }

  private static List<String> pipelineBlockTitles(SupportedLocale locale) {
    return AutomationsFixtures.pipelineSteps(locale).stream()
        .flatMap(step -> step.blocks().stream())
        .map(AutomationsFixtures.PipelineBlock::title)
        .toList();
  }

  private static List<String> pipelineBlockBodies(SupportedLocale locale) {
    return AutomationsFixtures.pipelineSteps(locale).stream()
        .flatMap(step -> step.blocks().stream())
        .map(AutomationsFixtures.PipelineBlock::body)
        .toList();
  }

  private static <T> List<String> map(List<T> values, Function<T, String> field) {
    return values.stream().map(field).toList();
  }

  /**
   * A section carries only the fields its own kind uses, so most of them are null. Comparing a null
   * against a null proves nothing, and dropping them would silently shrink what is checked — they
   * become the empty string, which is equal in both languages and so allowed by name.
   */
  private static <T> List<String> nullSafe(List<T> values, Function<T, String> field) {
    return values.stream().map(field).map(value -> value == null ? "" : value).toList();
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
