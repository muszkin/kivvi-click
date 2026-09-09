package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/feeds} response: the KPI strip, available sources, each connected
 * feed and the price-matching diagnostic — the wire shape {@link click.kivvi.web.FeedsController}
 * builds from {@link click.kivvi.application.FeedsViewService}'s computed view-model.
 */
public record FeedsResponse(
    List<Kpi> kpis,
    List<Source> sources,
    List<Feed> feeds,
    List<CoverageBar> coverage,
    List<FallbackRule> fallbackRules,
    int mismatched) {

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
   * {@code products}, {@code mapped} and {@code mappedPercent} arrive pre-formatted strings: the
   * SPA never formats a number. {@code mappedPercent} is {@code null} when there were no products
   * to compute a rate from.
   */
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

  public record CoverageBar(String label, double pct, String value, String tone) {}

  public record FallbackRule(String text, String field) {}
}
