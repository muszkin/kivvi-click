package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/customers} response: the segment rail and the 24-row index page — the
 * wire shape {@link click.kivvi.web.CustomersController} builds from {@link
 * click.kivvi.application.CustomersViewService}'s computed view-model.
 */
public record CustomerListResponse(
    String subtitle, List<Segment> segments, List<Row> customersRows, int page, int pages) {

  /** One segment-rail tile. {@code icon} is {@code null} for every tile but "Wszyscy". */
  public record Segment(
      String label, String icon, String count, boolean active, String action, String payload) {}

  /** One customers-index row, every number and relative time already formatted for display. */
  public record Row(
      String id,
      String name,
      String initials,
      String email,
      RowSegment segment,
      String orders,
      String revenue,
      String lastSeen) {}

  public record RowSegment(String label, String tone) {}
}
