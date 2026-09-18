package click.kivvi.application;

import click.kivvi.domain.Format;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.WidgetFixtures;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Assembles the popup index and widget-editor view-models — mirrors what {@code WidgetController}
 * rendered from {@code WidgetCatalog}. Every count/percent value is formatted here, exactly like
 * {@code WidgetCatalog::cards()} already did on the old stack — the SPA never formats a number
 * itself.
 *
 * <p>{@link #list} returns the render() parameters {@code WidgetController::index} passed to {@code
 * pages/popups.html.twig} unmerged — {@code cards()}'s {@code action}/{@code payload} pair mirrors
 * {@code WidgetCatalog::cards()} verbatim ({@code go-popup}/id); the template-level override to a
 * {@code ?preview=} navigation happened inside the Twig template itself, and now happens inside
 * {@code PopupsView.vue} the same way (see that view's own comment) — the same layering {@code
 * AutomationsView.vue}/{@code useIntents.ts}'s own "go-automation" precedent already established
 * for {@code list-card.html.twig}'s row-click override.
 */
@Service
public class WidgetViewService {

  /**
   * PIO-129 translates the panel one page at a time. This page's copy is still Polish only, so its
   * figures stay Polish too — a Polish label above a euro amount would be worse than either
   * language on its own. The slice that translates this page replaces the marker with the real
   * locale; {@code PanelTranslationCoverageTest} holds the remaining markers to a declared list, so
   * the last page cannot be forgotten silently.
   */
  private static final SupportedLocale UNTRANSLATED = SupportedLocale.PL;

  private static final String NEW_WIDGET_NAME = "Nowy widget";
  private static final String NEW_WIDGET_META = "Szkic · nieopublikowany";
  private static final String NEW_WIDGET_TYPE = "modal";
  private static final String DEVICE_MOBILE = "mobile";
  private static final String DEVICE_DESKTOP = "desktop";
  private static final String VIEWPORT_DESKTOP = "1440 × 900";
  private static final String VIEWPORT_MOBILE = "390 × 844";

  /** One list-card chip; {@code tone} is {@code null} for a plain neutral chip. */
  public record Chip(String label, String tone) {}

  /** One list-card metric; {@code color} is {@code null} when the default colour applies. */
  public record Metric(String value, String label, int width, String color) {}

  /** One popup-index row, every number already formatted for display. */
  public record Card(
      String title, List<Chip> chips, List<Metric> metrics, String action, String payload) {}

  /** The previewed widget: only the fields {@code popups.html.twig} actually reads. */
  public record Selected(String id, String name, String type, WidgetFixtures.Content content) {}

  public record ListPayload(
      List<Card> cards, Selected selected, List<WidgetFixtures.TypeOption> types) {}

  /** The edited widget: {@code WidgetController::editor}'s own {@code widget} array shape. */
  public record EditorWidget(
      String id, String name, String meta, String type, WidgetFixtures.Content content) {}

  public record EditorPayload(
      EditorWidget widget,
      String device,
      List<WidgetFixtures.TypeOption> types,
      List<WidgetFixtures.Block> blocks,
      List<String> variables,
      List<WidgetFixtures.Trigger> triggers,
      List<WidgetFixtures.AudienceRule> audience,
      List<String> accentColors,
      String viewport) {}

  /** Resolved widget identity shared by both endpoints — {@code WidgetCatalog::selected}. */
  private record Resolved(String id, String name, String type, String meta) {}

  /**
   * @param previewId the {@code ?preview=} value, or {@code null} to fall back to the first seeded
   *     widget — mirrors {@code $request->query->get('preview', $widgets->firstId())}.
   */
  public ListPayload list(String previewId) {
    Resolved resolved = resolve(previewId != null ? previewId : firstId());
    Selected selected =
        new Selected(
            resolved.id(),
            resolved.name(),
            resolved.type(),
            WidgetFixtures.content(resolved.type()));
    return new ListPayload(cards(), selected, WidgetFixtures.types());
  }

  /**
   * @param id "new" or a {@code p\d+} id; an id with no seeded widget (including "new" itself)
   *     falls back to the same blank-draft shape {@code WidgetCatalog::selected} returns for any
   *     unmatched id.
   * @param typeParam the {@code ?type=} value, or {@code null} to fall back to the widget's own
   *     type — mirrors {@code $request->query->get('type', $widget['type'])}; not validated against
   *     the known type list, exactly like the old stack's own query read.
   * @param deviceParam the {@code ?device=} value; only the literal {@code "mobile"} selects the
   *     mobile viewport, everything else (including {@code null}) is desktop.
   */
  public EditorPayload editor(String id, String typeParam, String deviceParam) {
    Resolved resolved = resolve(id);
    String type = typeParam != null ? typeParam : resolved.type();
    String device = DEVICE_MOBILE.equals(deviceParam) ? DEVICE_MOBILE : DEVICE_DESKTOP;
    EditorWidget widget =
        new EditorWidget(
            resolved.id(), resolved.name(), resolved.meta(), type, WidgetFixtures.content(type));
    String viewport = DEVICE_DESKTOP.equals(device) ? VIEWPORT_DESKTOP : VIEWPORT_MOBILE;
    return new EditorPayload(
        widget,
        device,
        WidgetFixtures.types(),
        WidgetFixtures.blocks(),
        WidgetFixtures.variables(),
        WidgetFixtures.triggers(),
        WidgetFixtures.audience(),
        WidgetFixtures.accentColors(),
        viewport);
  }

  private static String firstId() {
    return WidgetFixtures.all().get(0).id();
  }

  private static Resolved resolve(String id) {
    return WidgetFixtures.all().stream()
        .filter(widget -> widget.id().equals(id))
        .findFirst()
        .map(
            widget ->
                new Resolved(
                    widget.id(),
                    widget.name(),
                    widget.type(),
                    "%s na aureashop.pl · %s wyświetleń · %s konwersji"
                        .formatted(
                            WidgetFixtures.statusChip(widget.status()).label(),
                            Format.number(widget.impressions(), UNTRANSLATED),
                            Format.percent(widget.conversion(), UNTRANSLATED))))
        .orElseGet(() -> new Resolved(id, NEW_WIDGET_NAME, NEW_WIDGET_TYPE, NEW_WIDGET_META));
  }

  private static List<Card> cards() {
    return WidgetFixtures.all().stream().map(WidgetViewService::toCard).toList();
  }

  private static Card toCard(WidgetFixtures.Widget widget) {
    WidgetFixtures.StatusChip statusChip = WidgetFixtures.statusChip(widget.status());
    List<Chip> chips =
        List.of(
            new Chip(statusChip.label(), statusChip.tone()),
            new Chip(WidgetFixtures.typeLabel(widget.type()), "brown"),
            new Chip("na wszystkich stronach", null),
            new Chip("exit intent", null));
    boolean hasImpressions = widget.impressions() > 0;
    boolean hasConversion = widget.conversion() > 0;
    List<Metric> metrics =
        List.of(
            new Metric(
                hasImpressions ? Format.number(widget.impressions(), UNTRANSLATED) : "—",
                "wyświetleń",
                90,
                null),
            new Metric(
                hasConversion ? Format.percent(widget.conversion(), UNTRANSLATED) : "—",
                "konwersja",
                80,
                hasConversion ? "var(--good)" : "var(--fg-muted)"));
    return new Card(widget.name(), chips, metrics, "go-popup", widget.id());
  }
}
