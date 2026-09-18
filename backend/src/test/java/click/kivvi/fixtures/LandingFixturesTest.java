package click.kivvi.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.SupportedLocale;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LandingFixturesTest {

  /** Letters only Polish uses. None of them has any business in an English string on this page. */
  private static final Pattern POLISH_LETTERS = Pattern.compile("[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]");

  @Test
  @DisplayName("B22 the feature grid has six entries with icon, title and body")
  void featureGridHasSixEntries() {
    assertThat(LandingFixtures.features(SupportedLocale.PL)).hasSize(6);
    assertThat(LandingFixtures.features(SupportedLocale.PL).get(0).icon()).isEqualTo("activity");
    assertThat(LandingFixtures.features(SupportedLocale.PL).get(0).title())
        .isEqualTo("Strumień zdarzeń na żywo");
  }

  @Test
  @DisplayName("PIO-117 the English feature grid is written in English")
  void theEnglishFeatureGridIsInEnglish() {
    assertThat(LandingFixtures.features(SupportedLocale.EN))
        .extracting(LandingFixtures.Feature::title)
        .containsExactly(
            "Live event stream",
            "Rules without code",
            "Emails done properly",
            "Popups and web layers",
            "Coupons at the point of conversion",
            "ML recommendations");
  }

  @Test
  @DisplayName("PIO-121 the onboarding path starts with deploying, in four numbered steps")
  void onboardingPathStartsWithDeploying() {
    assertThat(LandingFixtures.steps(SupportedLocale.PL)).hasSize(4);
    assertThat(LandingFixtures.steps(SupportedLocale.PL))
        .extracting("number")
        .containsExactly("01", "02", "03", "04");
    // The first step is the whole point of the new narrative: you get your own instance before
    // you paste anything. B22's three steps began at the snippet, which only makes sense for a
    // hosted product the visitor never installs.
    assertThat(LandingFixtures.steps(SupportedLocale.PL).getFirst().title())
        .isEqualTo("Postaw u siebie");
  }

  @Test
  @DisplayName("PIO-117 the English first step carries the wording agreed when PIO-121 added it")
  void theEnglishFirstStepCarriesTheAgreedWording() {
    LandingFixtures.Step first = LandingFixtures.steps(SupportedLocale.EN).getFirst();

    assertThat(first.title()).isEqualTo("Run it yourself");
    assertThat(first.body())
        .isEqualTo(
            "Clone the repository and bring an instance up with one command. Would rather not"
                + " — we will do it for you.");
  }

  @Test
  @DisplayName("PIO-121 the trust points promise the licence and self-hosting, not a trial")
  void trustPointsPromiseTheLicenceNotATrial() {
    assertThat(LandingFixtures.trustPoints(SupportedLocale.PL))
        .containsExactly("Licencja MIT", "Postawisz u siebie", "Skrypt 2 KB");
    assertThat(LandingFixtures.trustPoints(SupportedLocale.EN))
        .containsExactly("MIT licence", "Host it yourself", "2 KB script");
  }

  @ParameterizedTest
  @EnumSource(SupportedLocale.class)
  @DisplayName("PIO-121 no feature card gates a capability behind a paid plan, in either language")
  void noFeatureCardGatesACapabilityBehindAPaidPlan(SupportedLocale locale) {
    assertThat(LandingFixtures.features(locale))
        .extracting(LandingFixtures.Feature::body)
        .noneMatch(body -> body.contains("Plan Pro") || body.contains("Pro plan"));
  }

  @Test
  @DisplayName(
      "B22 the preview KPI tiles are pre-formatted with the narrow no-break thousands separator")
  void previewTilesArePreFormatted() {
    assertThat(LandingFixtures.previewTiles(SupportedLocale.PL)).hasSize(4);
    assertThat(LandingFixtures.previewTiles(SupportedLocale.PL).get(2).value()).isEqualTo("8 410");
    assertThat(LandingFixtures.previewTiles(SupportedLocale.PL).get(3).value()).isEqualTo("94 200");
    assertThat(LandingFixtures.previewTiles(SupportedLocale.PL).get(3).unit()).isEqualTo("zł");
  }

  @Test
  @DisplayName("PIO-129 the English preview tiles are labelled in English and priced in euro")
  void theEnglishPreviewTilesAreInEnglish() {
    assertThat(LandingFixtures.previewTiles(SupportedLocale.EN))
        .extracting(LandingFixtures.PreviewTile::label)
        .containsExactly("Events / min", "Active sessions", "Emails (24h)", "Revenue (24h)");
    // PIO-129: the panel this tile previews now trades in euro for English readers, so the tile
    // that used to say PLN under an English label says EUR.
    assertThat(LandingFixtures.previewTiles(SupportedLocale.EN).get(3).unit()).isEqualTo("EUR");
  }

  @Test
  @DisplayName(
      "PIO-117 both languages carry the same entries in the same order — only the words differ")
  void bothLanguagesCarryTheSameEntries() {
    assertThat(LandingFixtures.features(SupportedLocale.EN))
        .extracting(LandingFixtures.Feature::icon)
        .containsExactlyElementsOf(
            LandingFixtures.features(SupportedLocale.PL).stream()
                .map(LandingFixtures.Feature::icon)
                .toList());
    assertThat(LandingFixtures.steps(SupportedLocale.EN))
        .extracting(LandingFixtures.Step::number)
        .containsExactlyElementsOf(
            LandingFixtures.steps(SupportedLocale.PL).stream()
                .map(LandingFixtures.Step::number)
                .toList());
    assertThat(LandingFixtures.trustPoints(SupportedLocale.EN))
        .hasSameSizeAs(LandingFixtures.trustPoints(SupportedLocale.PL));
    // PIO-129 gave each language its own thousands separator, so the rendered values are no
    // longer character-identical. The figures behind them still must be: one shop, two
    // conventions. Comparing them with the separators stripped says exactly that, and would fail
    // if a tile's number itself drifted between the two languages.
    assertThat(LandingFixtures.previewTiles(SupportedLocale.EN))
        .extracting(tile -> digitsOf(tile.value()))
        .containsExactlyElementsOf(
            LandingFixtures.previewTiles(SupportedLocale.PL).stream()
                .map(tile -> digitsOf(tile.value()))
                .toList());
  }

  /**
   * The leak PIO-117 was filed for, generalised: an English string that is identical to its Polish
   * counterpart was never translated, and one carrying a Polish letter was translated badly. Either
   * fails here by position, so the message names the string rather than "something".
   */
  @Test
  @DisplayName("PIO-117 every English string differs from its Polish counterpart and has no ą–ż")
  void everyEnglishStringIsTranslated() {
    List<String> polish = copyOf(SupportedLocale.PL);
    List<String> english = copyOf(SupportedLocale.EN);

    assertThat(english).hasSameSizeAs(polish);
    IntStream.range(0, english.size())
        .forEach(
            index -> {
              assertThat(english.get(index))
                  .as("English string #%d", index)
                  .isNotEqualTo(polish.get(index))
                  .doesNotContainPattern(POLISH_LETTERS);
            });
  }

  @Test
  @DisplayName("B22 the traffic sparkline has 60 points, none of them negative")
  void previewSeriesHasSixtyNonNegativePoints() {
    assertThat(LandingFixtures.previewSeries()).hasSize(60);
    assertThat(LandingFixtures.previewSeries()).allMatch(value -> value >= 0);
  }

  /** Every translatable string the page renders for {@code locale}, in a fixed order. */
  private static List<String> copyOf(SupportedLocale locale) {
    List<String> strings = new ArrayList<>();
    LandingFixtures.features(locale)
        .forEach(
            feature -> {
              strings.add(feature.title());
              strings.add(feature.body());
            });
    LandingFixtures.steps(locale)
        .forEach(
            step -> {
              strings.add(step.title());
              strings.add(step.body());
            });
    strings.addAll(LandingFixtures.trustPoints(locale));
    LandingFixtures.previewTiles(locale)
        .forEach(
            tile -> {
              strings.add(tile.label());
              if (tile.unit() != null) {
                strings.add(tile.unit());
              }
            });
    return strings;
  }

  /**
   * A rendered figure with its thousands separators removed — U+202F for Polish, "," for English.
   */
  private static String digitsOf(String formatted) {
    return formatted.replace("\u202F", "").replace(",", "");
  }
}
