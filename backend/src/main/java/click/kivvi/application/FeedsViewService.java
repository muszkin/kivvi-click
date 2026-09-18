package click.kivvi.application;

import click.kivvi.domain.Format;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.FeedsFixtures;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Assembles the product-feeds page view-model: the KPI strip, available sources, each connected
 * feed's synchronisation state and the price-matching diagnostic — mirrors what {@code
 * ProductFeedController} rendered from {@code ProductFeedCatalog}.
 *
 * <p>A connected feed's product/mapped counts and match rate used to be formatted by {@code
 * feed-card.html.twig} itself (Twig's own {@code number_format} filter); that decision moves here,
 * since the SPA never formats a number — mirrors {@code ProductFeedCatalog::feeds()} already having
 * moved the {@code lastSync} computation out of the template.
 */
@Service
public class FeedsViewService {

  /** One connected feed with every number already formatted for display. */
  public record Feed(
      String name,
      String url,
      String source,
      String status,
      String error,
      String products,
      String mapped,
      String mappedPercent,
      int mismatched,
      String lastSync,
      String schedule) {}

  public record Payload(
      List<FeedsFixtures.Kpi> kpis,
      List<FeedsFixtures.Source> sources,
      List<Feed> feeds,
      List<FeedsFixtures.CoverageBar> coverage,
      List<FeedsFixtures.FallbackRule> fallbackRules,
      int mismatched) {}

  public Payload build(SupportedLocale locale) {
    List<Feed> feeds =
        FeedsFixtures.rawFeeds(locale).stream().map(raw -> toFeed(locale, raw)).toList();
    return new Payload(
        FeedsFixtures.kpis(locale),
        FeedsFixtures.sources(locale),
        feeds,
        FeedsFixtures.coverage(locale),
        FeedsFixtures.fallbackRules(locale),
        FeedsFixtures.mismatchedCount());
  }

  private static Feed toFeed(SupportedLocale locale, FeedsFixtures.RawFeed raw) {
    return new Feed(
        raw.name(),
        raw.url(),
        raw.source(),
        raw.status(),
        raw.error(),
        grouped(raw.products(), locale),
        grouped(raw.mapped(), locale),
        mappedPercent(raw.mapped(), raw.products(), locale),
        raw.mismatched(),
        lastSync(locale, raw.lastSyncMinutes()),
        raw.schedule());
  }

  /**
   * Mirrors feed-card.html.twig's own {@code lastSyncMinutes < 60 ? … 'min temu' : intdiv(…, 60) …
   * 'godz. temu'} branch, computed here instead — exactly like {@code ProductFeedCatalog::feeds()}
   * already did on the old stack.
   */
  private static String lastSync(SupportedLocale locale, int minutes) {
    boolean withinTheHour = minutes < 60;
    return switch (locale) {
      case PL -> withinTheHour ? minutes + " min temu" : (minutes / 60) + " godz. temu";
      case EN -> withinTheHour ? minutes + " min ago" : (minutes / 60) + " hr ago";
    };
  }

  /**
   * feed-card.html.twig's {@code {{ products|number_format(0, ',', ' ') }}}: zero decimals, grouped
   * with a plain ASCII space — a different separator from {@link Format#number}, which the KPI
   * strip uses (U+202F narrow no-break space). Both conventions exist verbatim in the old stack and
   * are reproduced verbatim here, not reconciled into one.
   */
  private static String grouped(int value, SupportedLocale locale) {
    char separator =
        switch (locale) {
          case PL -> ' ';
          case EN -> ',';
        };
    String digits = Integer.toString(value);
    StringBuilder grouped = new StringBuilder();
    for (int i = 0; i < digits.length(); i++) {
      if (i > 0 && (digits.length() - i) % 3 == 0) {
        grouped.append(separator);
      }
      grouped.append(digits.charAt(i));
    }
    return grouped.toString();
  }

  /**
   * feed-card.html.twig's {@code {% if products %}…(mapped / products * 100)|number_format(1, ',',
   * ' ')…{% endif %}} — {@code null} (and so omitted from the JSON, and never rendered) when there
   * are no products to compute a rate from. The result never reaches 1000, so {@link
   * Format#percent}'s narrow-no-break-space grouping never actually triggers — safe to reuse rather
   * than reimplementing Twig's plain-space grouping a second time for a case that cannot occur.
   */
  private static String mappedPercent(int mapped, int products, SupportedLocale locale) {
    if (products == 0) {
      return null;
    }
    return Format.percent(mapped * 100.0 / products, 1, locale);
  }
}
