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

  /**
   * PIO-129 translates the panel one page at a time. This page's copy is still Polish only, so its
   * figures stay Polish too — a Polish label above a euro amount would be worse than either
   * language on its own. The slice that translates this page replaces the marker with the real
   * locale; {@code PanelTranslationCoverageTest} holds the remaining markers to a declared list, so
   * the last page cannot be forgotten silently.
   */
  private static final SupportedLocale UNTRANSLATED = SupportedLocale.PL;

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

  public Payload build() {
    List<Feed> feeds = FeedsFixtures.rawFeeds().stream().map(FeedsViewService::toFeed).toList();
    return new Payload(
        FeedsFixtures.kpis(),
        FeedsFixtures.sources(),
        feeds,
        FeedsFixtures.coverage(),
        FeedsFixtures.fallbackRules(),
        FeedsFixtures.mismatchedCount());
  }

  private static Feed toFeed(FeedsFixtures.RawFeed raw) {
    return new Feed(
        raw.name(),
        raw.url(),
        raw.source(),
        raw.status(),
        raw.error(),
        groupWithSpace(raw.products()),
        groupWithSpace(raw.mapped()),
        mappedPercent(raw.mapped(), raw.products()),
        raw.mismatched(),
        lastSync(raw.lastSyncMinutes()),
        raw.schedule());
  }

  /**
   * Mirrors feed-card.html.twig's own {@code lastSyncMinutes < 60 ? … 'min temu' : intdiv(…, 60) …
   * 'godz. temu'} branch, computed here instead — exactly like {@code ProductFeedCatalog::feeds()}
   * already did on the old stack.
   */
  private static String lastSync(int minutes) {
    return minutes < 60 ? minutes + " min temu" : (minutes / 60) + " godz. temu";
  }

  /**
   * feed-card.html.twig's {@code {{ products|number_format(0, ',', ' ') }}}: zero decimals, grouped
   * with a plain ASCII space — a different separator from {@link Format#number}, which the KPI
   * strip uses (U+202F narrow no-break space). Both conventions exist verbatim in the old stack and
   * are reproduced verbatim here, not reconciled into one.
   */
  private static String groupWithSpace(int value) {
    String digits = Integer.toString(value);
    StringBuilder grouped = new StringBuilder();
    for (int i = 0; i < digits.length(); i++) {
      if (i > 0 && (digits.length() - i) % 3 == 0) {
        grouped.append(' ');
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
  private static String mappedPercent(int mapped, int products) {
    if (products == 0) {
      return null;
    }
    return Format.percent(mapped * 100.0 / products, 1, UNTRANSLATED);
  }
}
