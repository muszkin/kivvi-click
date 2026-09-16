package click.kivvi.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LandingFixturesTest {

  @Test
  @DisplayName("B22 the feature grid has six entries with icon, title and body")
  void featureGridHasSixEntries() {
    assertThat(LandingFixtures.features()).hasSize(6);
    assertThat(LandingFixtures.features().get(0).icon()).isEqualTo("activity");
    assertThat(LandingFixtures.features().get(0).title()).isEqualTo("Strumień zdarzeń na żywo");
  }

  @Test
  @DisplayName("PIO-121 the onboarding path starts with deploying, in four numbered steps")
  void onboardingPathStartsWithDeploying() {
    assertThat(LandingFixtures.steps()).hasSize(4);
    assertThat(LandingFixtures.steps())
        .extracting("number")
        .containsExactly("01", "02", "03", "04");
    // The first step is the whole point of the new narrative: you get your own instance before
    // you paste anything. B22's three steps began at the snippet, which only makes sense for a
    // hosted product the visitor never installs.
    assertThat(LandingFixtures.steps().getFirst().title()).isEqualTo("Postaw u siebie");
  }

  @Test
  @DisplayName("PIO-121 the trust points promise the licence and self-hosting, not a trial")
  void trustPointsPromiseTheLicenceNotATrial() {
    assertThat(LandingFixtures.trustPoints())
        .containsExactly("Licencja MIT", "Postawisz u siebie", "Skrypt 2 KB");
  }

  @Test
  @DisplayName("PIO-121 no feature card gates a capability behind a paid plan")
  void noFeatureCardGatesACapabilityBehindAPaidPlan() {
    assertThat(LandingFixtures.features())
        .extracting("body")
        .noneMatch(body -> ((String) body).contains("Plan Pro"));
  }

  @Test
  @DisplayName(
      "B22 the preview KPI tiles are pre-formatted with the narrow no-break thousands separator")
  void previewTilesArePreFormatted() {
    assertThat(LandingFixtures.previewTiles()).hasSize(4);
    assertThat(LandingFixtures.previewTiles().get(2).value()).isEqualTo("8 410");
    assertThat(LandingFixtures.previewTiles().get(3).value()).isEqualTo("94 200");
    assertThat(LandingFixtures.previewTiles().get(3).unit()).isEqualTo("zł");
  }

  @Test
  @DisplayName("B22 the traffic sparkline has 60 points, none of them negative")
  void previewSeriesHasSixtyNonNegativePoints() {
    assertThat(LandingFixtures.previewSeries()).hasSize(60);
    assertThat(LandingFixtures.previewSeries()).allMatch(value -> value >= 0);
  }
}
