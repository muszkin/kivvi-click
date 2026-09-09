package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/popups/{id}} (and {@code .../popups/new}) response: the composer
 * model behind the popup editor (widget shape, type list, block library, placeholder variables,
 * triggers, audience and accent colours) — the wire shape {@link click.kivvi.web.WidgetController}
 * builds from {@link click.kivvi.application.WidgetViewService}'s computed view-model.
 */
public record WidgetEditorResponse(
    Widget widget,
    String device,
    List<TypeOption> types,
    List<Block> blocks,
    List<String> variables,
    List<Trigger> triggers,
    List<AudienceRule> audience,
    List<String> accentColors,
    String viewport) {

  public record TypeOption(String id, String label, String icon) {}

  public record Block(String icon, String label, String type) {}

  /**
   * One trigger row. {@code tone}/{@code note} accompany the qualitative trigger; {@code
   * value}/{@code unit} accompany the two numeric triggers.
   */
  public record Trigger(String label, String tone, String note, String value, String unit) {}

  /** {@code label} may carry a literal {@code <span class="mono">} fragment, bound with v-html. */
  public record AudienceRule(String label, boolean checked) {}

  /**
   * The edited widget's content shape. Fields a given widget type does not use are dropped by
   * Jackson's {@code non_null} inclusion (application.yml).
   */
  public record Content(
      String type,
      String kicker,
      String title,
      String body,
      String placeholder,
      String cta,
      String fine) {}

  public record Widget(String id, String name, String meta, String type, Content content) {}
}
