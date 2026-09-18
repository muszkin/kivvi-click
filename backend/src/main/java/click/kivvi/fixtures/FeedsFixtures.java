package click.kivvi.fixtures;

import click.kivvi.domain.Format;
import click.kivvi.domain.SupportedLocale;
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

  private static final int CATALOGUE_PRODUCTS = 2_648;
  private static final int BASKET_VALUE_24H = 382_140;

  private static final List<Kpi> KPIS_PL =
      List.of(
          new Kpi("Aktywne feedy", "3", "/4", "1 z błędem", "down", null),
          new Kpi(
              "Produktów w katalogu",
              Format.number(CATALOGUE_PRODUCTS, SupportedLocale.PL),
              null,
              "+42 w tym tyg.",
              "up",
              null),
          new Kpi("Dopasowanie zdarzeń → produkty", "94,8", "%", "ostatnia doba", "up", "spark"),
          new Kpi(
              "Wartość koszyków (24h)",
              Format.money(BASKET_VALUE_24H, SupportedLocale.PL),
              null,
              "z dopasowanymi cenami",
              "up",
              null));

  private static final List<Source> SOURCES_PL =
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

  private static final List<RawFeed> RAW_FEEDS_PL =
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

  private static final List<CoverageBar> COVERAGE_PL =
      List.of(
          new CoverageBar("Po id", 78.0, "78%", "accent"),
          new CoverageBar("Po sku (fallback)", 12.0, "12%", "brown"),
          new CoverageBar("Po URL", 4.8, "4,8%", "brown"),
          new CoverageBar("Niedopasowane", 5.2, "5,2%", "bad"));

  private static final List<FallbackRule> FALLBACK_RULES_PL =
      List.of(
          new FallbackRule("Jeśli brak id → spróbuj", "sku"),
          new FallbackRule("Jeśli brak sku → spróbuj", "gtin"),
          new FallbackRule("Jeśli nadal brak → użyj", "URL produktu"));

  private static final List<Kpi> KPIS_EN =
      List.of(
          new Kpi("Active feeds", "3", "/4", "1 failing", "down", null),
          new Kpi(
              "Products in the catalogue",
              Format.number(CATALOGUE_PRODUCTS, SupportedLocale.EN),
              null,
              "+42 this week",
              "up",
              null),
          new Kpi("Events matched → products", "94.8", "%", "last 24 hours", "up", "spark"),
          new Kpi(
              "Basket value (24h)",
              Format.money(BASKET_VALUE_24H, SupportedLocale.EN),
              null,
              "with matched prices",
              "up",
              null));

  private static final List<Source> SOURCES_EN =
      List.of(
          new Source(
              "google",
              "G",
              "oklch(0.62 0.16 28)",
              "oklch(0.93 0.04 28)",
              "Google Merchant Center",
              "OAuth · synchronises automatically every 6h",
              true),
          new Source(
              "facebook",
              "f",
              "oklch(0.55 0.13 250)",
              "oklch(0.91 0.04 250)",
              "Facebook Catalog (Meta)",
              "Catalog API · synchronises every 1h",
              false),
          new Source(
              "xml",
              "×",
              "oklch(0.45 0.07 60)",
              "oklch(0.91 0.04 60)",
              "Your own XML / RSS",
              "Any URL — PrestaShop, Shoper, WooCommerce and the like",
              false),
          new Source(
              "csv",
              "↧",
              "oklch(0.42 0.06 150)",
              "oklch(0.91 0.04 150)",
              "CSV file / Google sheet",
              "Upload a single file or link to a sheet",
              false));

  private static final List<RawFeed> RAW_FEEDS_EN =
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
              "every 6 hours"),
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
              "every hour"),
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
              "every 6 hours"),
          new RawFeed(
              "polna-bistro.pl — own XML",
              "https://polna-bistro.pl/produkty.xml",
              "xml",
              "synced",
              null,
              84,
              84,
              0,
              28,
              "daily"));

  private static final List<CoverageBar> COVERAGE_EN =
      List.of(
          new CoverageBar("By id", 78.0, "78%", "accent"),
          new CoverageBar("By sku (fallback)", 12.0, "12%", "brown"),
          new CoverageBar("By URL", 4.8, "4.8%", "brown"),
          new CoverageBar("Unmatched", 5.2, "5.2%", "bad"));

  private static final List<FallbackRule> FALLBACK_RULES_EN =
      List.of(
          new FallbackRule("When there is no id, try", "sku"),
          new FallbackRule("When there is no sku, try", "gtin"),
          new FallbackRule("When there is still none, use", "the product URL"));

  private static final int MISMATCHED_COUNT = 142;

  private FeedsFixtures() {}

  public static List<Kpi> kpis(SupportedLocale locale) {
    return switch (locale) {
      case PL -> KPIS_PL;
      case EN -> KPIS_EN;
    };
  }

  public static List<Source> sources(SupportedLocale locale) {
    return switch (locale) {
      case PL -> SOURCES_PL;
      case EN -> SOURCES_EN;
    };
  }

  public static List<RawFeed> rawFeeds(SupportedLocale locale) {
    return switch (locale) {
      case PL -> RAW_FEEDS_PL;
      case EN -> RAW_FEEDS_EN;
    };
  }

  public static List<CoverageBar> coverage(SupportedLocale locale) {
    return switch (locale) {
      case PL -> COVERAGE_PL;
      case EN -> COVERAGE_EN;
    };
  }

  public static List<FallbackRule> fallbackRules(SupportedLocale locale) {
    return switch (locale) {
      case PL -> FALLBACK_RULES_PL;
      case EN -> FALLBACK_RULES_EN;
    };
  }

  public static int mismatchedCount() {
    return MISMATCHED_COUNT;
  }
}
