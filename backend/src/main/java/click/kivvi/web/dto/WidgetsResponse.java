package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/popups} response: the widget cards, the currently previewed widget
 * and the five widget types — the wire shape {@link click.kivvi.web.WidgetController} builds from
 * {@link click.kivvi.application.WidgetViewService}'s computed view-model.
 */
public record WidgetsResponse(List<Card> cards, Selected selected, List<TypeOption> types) {

  /** {@code color}/{@code tone} are {@code null} when the default styling applies. */
  public record Chip(String label, String tone) {}

  public record Metric(String value, String label, int width, String color) {}

  /** One popup-index row, every number already formatted for display. */
  public record Card(
      String title, List<Chip> chips, List<Metric> metrics, String action, String payload) {}

  public record TypeOption(String id, String label, String icon) {}

  /**
   * The previewed widget's shape. Fields a given widget type does not use ({@code kicker}, {@code
   * body}, {@code placeholder}, {@code fine}) are dropped by Jackson's {@code non_null} inclusion
   * (application.yml).
   */
  public record Content(
      String type,
      String kicker,
      String title,
      String body,
      String placeholder,
      String cta,
      String fine) {}

  public record Selected(String id, String name, String type, Content content) {}
}
