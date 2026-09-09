package click.kivvi.fixtures;

import click.kivvi.domain.Format;
import java.util.List;

/**
 * Sample product-feed catalogue: the KPI strip, available sources, connected feeds and the
 * price-matching diagnostic — ported from {@code ProductFeedCatalog}.
 *
 * <p>A connected feed's raw counters ({@link RawFeed#products()}, {@link RawFeed#mapped()}, minutes
 * since the last sync) are carried unformatted, exactly like {@code ProductFeedCatalog}'s own
 * {@code self::FEEDS} constant: the display strings feed-card.html.twig used to compute for itself
 * are now {@link click.kivvi.application.FeedsViewService}'s job, since the SPA never formats a
 * number.
 */
public final class FeedsFixtures {

  public record Kpi(
      String label, String value, String unit, String delta, String dir, String deltaIcon) {}

  public record Source(
      String id,
      String letter,
      String color,
      String bg,
      String title,
      String sub,
      boolean recommended) {}

  /**
   * One connected feed, before {@link click.kivvi.application.FeedsViewService} derives its display
   * strings.
   */
  public record RawFeed(
      String name,
      String url,
      String source,
      String status,
      String error,
      int products,
      int mapped,
      int mismatched,
      int lastSyncMinutes,
      String schedule) {}

  public record CoverageBar(String label, double pct, String value, String tone) {}

  public record FallbackRule(String text, String field) {}

  private static final List<Kpi> KPIS =
      List.of(
          new Kpi("Aktywne feedy", "3", "/4", "1 z błędem", "down", null),
          new Kpi("Produktów w katalogu", Format.number(2648), null, "+42 w tym tyg.", "up", null),
          new Kpi("Dopasowanie zdarzeń → produkty", "94,8", "%", "ostatnia doba", "up", "spark"),
          new Kpi(
              "Wartość koszyków (24h)",
              Format.money(382140),
              null,
              "z dopasowanymi cenami",
              "up",
              null));

  private static final List<Source> SOURCES =
      List.of(
          new Source(
              "google",
              "G",
              "oklch(0.62 0.16 28)",
              "oklch(0.93 0.04 28)",
              "Google Merchant Center",
              "OAuth · automatyczna synchronizacja co 6h",
              true),
          new Source(
              "facebook",
              "f",
              "oklch(0.55 0.13 250)",
              "oklch(0.91 0.04 250)",
              "Facebook Catalog (Meta)",
              "Catalog API · synchronizacja co 1h",
              false),
          new Source(
              "xml",
              "×",
              "oklch(0.45 0.07 60)",
              "oklch(0.91 0.04 60)",
              "XML / RSS własny",
              "Dowolny URL — np. PrestaShop, Shoper, WooCommerce",
              false),
          new Source(
              "csv",
              "↧",
              "oklch(0.42 0.06 150)",
              "oklch(0.91 0.04 150)",
              "Plik CSV / arkusz Google",
              "Upload pojedynczego pliku lub link do arkusza",
              false));

  private static final List<RawFeed> RAW_FEEDS =
      List.of(
          new RawFeed(
              "aureashop.pl — Google Merchant",
              "https://aureashop.pl/feeds/google.xml",
              "google",
              "synced",
              null,
              1284,
              1284,
              0,
              12,
              "co 6 godzin"),
          new RawFeed(
              "aureashop.pl — Facebook Catalog",
              "https://aureashop.pl/feeds/facebook.xml",
              "facebook",
              "syncing",
              null,
              1280,
              1278,
              2,
              1,
              "co 1 godzinę"),
          new RawFeed(
              "mlot-narzedzia.pl — Google Merchant",
              "https://mlot-narzedzia.pl/feed/google",
              "google",
              "error",
              "HTTP 503 — Service Unavailable",
              0,
              0,
              0,
              180,
              "co 6 godzin"),
          new RawFeed(
              "polna-bistro.pl — XML własny",
              "https://polna-bistro.pl/produkty.xml",
              "xml",
              "synced",
              null,
              84,
              84,
              0,
              28,
              "codziennie"));

  private static final List<CoverageBar> COVERAGE =
      List.of(
          new CoverageBar("Po id", 78.0, "78%", "accent"),
          new CoverageBar("Po sku (fallback)", 12.0, "12%", "brown"),
          new CoverageBar("Po URL", 4.8, "4,8%", "brown"),
          new CoverageBar("Niedopasowane", 5.2, "5,2%", "bad"));

  private static final List<FallbackRule> FALLBACK_RULES =
      List.of(
          new FallbackRule("Jeśli brak id → spróbuj", "sku"),
          new FallbackRule("Jeśli brak sku → spróbuj", "gtin"),
          new FallbackRule("Jeśli nadal brak → użyj", "URL produktu"));

  private static final int MISMATCHED_COUNT = 142;

  private FeedsFixtures() {}

  public static List<Kpi> kpis() {
    return KPIS;
  }

  public static List<Source> sources() {
    return SOURCES;
  }

  public static List<RawFeed> rawFeeds() {
    return RAW_FEEDS;
  }

  public static List<CoverageBar> coverage() {
    return COVERAGE;
  }

  public static List<FallbackRule> fallbackRules() {
    return FALLBACK_RULES;
  }

  public static int mismatchedCount() {
    return MISMATCHED_COUNT;
  }
}
