package click.kivvi.domain.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TrackedSiteTest {

  @Test
  void threeSitesInWorkspaceOrder() {
    assertThat(TrackedSite.all())
        .containsExactly(TrackedSite.AUREA, TrackedSite.MLOT, TrackedSite.POL);
  }

  @Test
  void byIndexRoundRobinsThroughTheThreeSites() {
    assertThat(TrackedSite.byIndex(0)).isEqualTo(TrackedSite.AUREA);
    assertThat(TrackedSite.byIndex(1)).isEqualTo(TrackedSite.MLOT);
    assertThat(TrackedSite.byIndex(2)).isEqualTo(TrackedSite.POL);
    assertThat(TrackedSite.byIndex(3)).isEqualTo(TrackedSite.AUREA);
    assertThat(TrackedSite.byIndex(12)).isEqualTo(TrackedSite.AUREA);
  }

  @Test
  void colorForSiteNameResolvesATrackedSite() {
    assertThat(TrackedSite.colorForSiteName("aureashop.pl")).isEqualTo("#7a8763");
    assertThat(TrackedSite.colorForSiteName("mlot-narzedzia.pl")).isEqualTo("#a3825b");
  }

  @Test
  void colorForSiteNameFallsBackToTheMutedTokenForAnUntrackedName() {
    assertThat(TrackedSite.colorForSiteName("unknown-shop.pl")).isEqualTo("var(--fg-muted)");
  }

  @Test
  void byIdResolvesTheFilterPayload() {
    assertThat(TrackedSite.byId("aurea")).contains(TrackedSite.AUREA);
    assertThat(TrackedSite.byId("nope")).isEmpty();
  }
}
