package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/campaigns} response: the KPI strip, filter rail and the campaign
 * performance table — the wire shape {@link click.kivvi.web.CampaignsController} builds from {@link
 * click.kivvi.application.CampaignsViewService}'s computed view-model.
 */
public record CampaignsResponse(
    List<Kpi> kpis, List<Filter> filters, List<Column> columns, List<Row> rows) {

  public record Kpi(String label, String value, String unit, String delta, String dir) {}

  /** {@code count} arrives pre-formatted: the SPA never formats a number itself. */
  public record Filter(String label, String count, boolean active, String action, String payload) {}

  public record Column(String label, String align) {}

  /** One campaigns-index row, every number already formatted for display. */
  public record Row(
      String id,
      String name,
      String statusTone,
      String statusLabel,
      String typeLabel,
      String typeTone,
      String sent,
      String open,
      String click,
      String revenue) {}
}
