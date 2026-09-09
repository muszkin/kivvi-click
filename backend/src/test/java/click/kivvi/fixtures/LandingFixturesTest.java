package click.kivvi.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.fixtures.LandingFixtures.Plan;
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
  @DisplayName("B22 the onboarding path has three numbered steps")
  void onboardingPathHasThreeSteps() {
    assertThat(LandingFixtures.steps()).hasSize(3);
    assertThat(LandingFixtures.steps()).extracting("number").containsExactly("01", "02", "03");
  }

  @Test
  @DisplayName("B22 the pricing grid is Free then featured Pro, carrying the 'popularne' badge")
  void pricingGridIsFreeThenFeaturedPro() {
    assertThat(LandingFixtures.plans()).hasSize(2);

    Plan free = LandingFixtures.plans().get(0);
    assertThat(free.tier()).isEqualTo("Free");
    assertThat(free.featured()).isFalse();
    assertThat(free.badge()).isNull();

    Plan pro = LandingFixtures.plans().get(1);
    assertThat(pro.tier()).isEqualTo("Pro");
    assertThat(pro.featured()).isTrue();
    assertThat(pro.badge()).isEqualTo("popularne");
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
