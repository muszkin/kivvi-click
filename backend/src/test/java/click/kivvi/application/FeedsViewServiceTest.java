package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.FeedsFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedsViewServiceTest {

  private final FeedsViewService feedsViewService = new FeedsViewService();

  @Test
  @DisplayName("B30 the four KPIs match the oracle, narrow-space-grouped and money-formatted")
  void kpisMatchTheOracle() {
    FeedsViewService.Payload payload = feedsViewService.build(SupportedLocale.PL);

    assertThat(payload.kpis())
        .extracting(FeedsFixtures.Kpi::label, FeedsFixtures.Kpi::value, FeedsFixtures.Kpi::unit)
        .containsExactly(
            tuple("Aktywne feedy", "3", "/4"),
            tuple("Produktów w katalogu", "2 648", null),
            tuple("Dopasowanie zdarzeń → produkty", "94,8", "%"),
            tuple("Wartość koszyków (24h)", "382 140 zł", null));
  }

  @Test
  @DisplayName("B30 four sources are offered, Google Merchant recommended")
  void sourcesMatchTheOracle() {
    FeedsViewService.Payload payload = feedsViewService.build(SupportedLocale.PL);

    assertThat(payload.sources())
        .extracting(
            FeedsFixtures.Source::id,
            FeedsFixtures.Source::letter,
            FeedsFixtures.Source::recommended)
        .containsExactly(
            tuple("google", "G", true),
            tuple("facebook", "f", false),
            tuple("xml", "×", false),
            tuple("csv", "↧", false));
  }

  @Test
  @DisplayName(
      "B30 four feeds are connected, one failing with HTTP 503, product/mapped counts "
          + "plain-space-grouped and the match rate computed here (never in the SPA)")
  void feedsMatchTheOracle() {
    FeedsViewService.Payload payload = feedsViewService.build(SupportedLocale.PL);

    assertThat(payload.feeds())
        .extracting(
            FeedsViewService.Feed::name,
            FeedsViewService.Feed::status,
            FeedsViewService.Feed::error,
            FeedsViewService.Feed::products,
            FeedsViewService.Feed::mapped,
            FeedsViewService.Feed::mappedPercent,
            FeedsViewService.Feed::mismatched,
            FeedsViewService.Feed::lastSync)
        .containsExactly(
            tuple(
                "aureashop.pl — Google Merchant",
                "synced",
                null,
                "1 284",
                "1 284",
                "100,0%",
                0,
                "12 min temu"),
            tuple(
                "aureashop.pl — Facebook Catalog",
                "syncing",
                null,
                "1 280",
                "1 278",
                "99,8%",
                2,
                "1 min temu"),
            tuple(
                "mlot-narzedzia.pl — Google Merchant",
                "error",
                "HTTP 503 — Service Unavailable",
                "0",
                "0",
                null,
                0,
                "3 godz. temu"),
            tuple(
                "polna-bistro.pl — XML własny",
                "synced",
                null,
                "84",
                "84",
                "100,0%",
                0,
                "28 min temu"));
  }

  @Test
  @DisplayName("B30 the matching diagnostic: 4 coverage bars, 3 fallback rules, 142 mismatched")
  void matchingDiagnosticMatchesTheOracle() {
    FeedsViewService.Payload payload = feedsViewService.build(SupportedLocale.PL);

    assertThat(payload.coverage())
        .extracting(FeedsFixtures.CoverageBar::label, FeedsFixtures.CoverageBar::value)
        .containsExactly(
            tuple("Po id", "78%"),
            tuple("Po sku (fallback)", "12%"),
            tuple("Po URL", "4,8%"),
            tuple("Niedopasowane", "5,2%"));
    assertThat(payload.fallbackRules())
        .extracting(FeedsFixtures.FallbackRule::text, FeedsFixtures.FallbackRule::field)
        .containsExactly(
            tuple("Jeśli brak id → spróbuj", "sku"),
            tuple("Jeśli brak sku → spróbuj", "gtin"),
            tuple("Jeśli nadal brak → użyj", "URL produktu"));
    assertThat(payload.mismatched()).isEqualTo(142);
  }
}
