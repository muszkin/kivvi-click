package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/automations} response: the status filter rail and the index cards —
 * the wire shape {@link click.kivvi.web.AutomationsController} builds from {@link
 * click.kivvi.application.AutomationsViewService}'s computed view-model.
 */
public record AutomationListResponse(List<Filter> filters, List<AutomationCard> automations) {

  /** One status filter-chip. {@code icon} is {@code null} for every chip but "Wszystkie". */
  public record Filter(
      String label, String icon, String count, boolean active, String action, String payload) {}

  /** One index card's metadata/status chip. */
  public record Chip(String label, String tone) {}

  /** One index card's right-aligned metric. {@code color} is {@code null} when unstyled. */
  public record Metric(String value, String label, int width, String color) {}

  /** One automations-index card, every number already formatted for display. */
  public record AutomationCard(
      String title, List<Chip> chips, List<Metric> metrics, String action, String payload) {}
}
