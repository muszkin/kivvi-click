package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/events} response — the wire shape {@link
 * click.kivvi.web.EventsController} builds from {@link click.kivvi.application.EventsViewService}.
 */
public record EventsResponse(
    List<Filter> typeFilters,
    List<Filter> siteFilters,
    List<Range> ranges,
    List<Row> events,
    String total,
    int shown,
    String mercureTopic) {

  public record Filter(
      String label, String icon, String dotColor, boolean active, String action, String payload) {}

  public record Range(String value, String label, boolean active) {}

  /** One sample-feed row; every field is always present (the generator never leaves one blank). */
  public record Row(
      String time,
      String typeIcon,
      String tone,
      String type,
      String detail,
      String customerName,
      String customerId,
      String siteName,
      String siteColor) {}
}
