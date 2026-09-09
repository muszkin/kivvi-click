package click.kivvi.web;

import click.kivvi.application.WidgetViewService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.WidgetFixtures;
import click.kivvi.web.dto.WidgetEditorResponse;
import click.kivvi.web.dto.WidgetsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * On-site widgets: the index with a live preview, and the widget composer's data — mirrors {@code
 * WidgetController}. Both document routes ({@code /popups}, {@code /popups/new}, {@code
 * /popups/p\d+}) are already plain entries in {@link click.kivvi.domain.RouteTable}, served
 * generically by {@code SpaDocumentController}: an id shape outside {@code new|p\d+} 404s at the
 * routing layer for both the document and this API, exactly like the old stack's own route
 * requirement ({@code id: 'p\d+'}) never matched anything else either — this controller never needs
 * its own not-found branch.
 */
@RestController
public class WidgetController {

  private final WidgetViewService widgetViewService;

  public WidgetController(WidgetViewService widgetViewService) {
    this.widgetViewService = widgetViewService;
  }

  @GetMapping("/api/v1/{locale:pl|en}/popups")
  public WidgetsResponse popups(
      @PathVariable String locale, @RequestParam(required = false) String preview) {
    SupportedLocale.fromCode(locale).orElseThrow();
    WidgetViewService.ListPayload payload = widgetViewService.list(preview);
    return new WidgetsResponse(
        payload.cards().stream().map(WidgetController::toCard).toList(),
        toSelected(payload.selected()),
        payload.types().stream().map(WidgetController::toTypeOption).toList());
  }

  @GetMapping("/api/v1/{locale:pl|en}/popups/{id:new|p\\d+}")
  public WidgetEditorResponse popup(
      @PathVariable String locale,
      @PathVariable String id,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String device) {
    SupportedLocale.fromCode(locale).orElseThrow();
    WidgetViewService.EditorPayload payload = widgetViewService.editor(id, type, device);
    return new WidgetEditorResponse(
        toWidget(payload.widget()),
        payload.device(),
        payload.types().stream().map(WidgetController::toEditorTypeOption).toList(),
        payload.blocks().stream().map(WidgetController::toBlock).toList(),
        payload.variables(),
        payload.triggers().stream().map(WidgetController::toTrigger).toList(),
        payload.audience().stream().map(WidgetController::toAudienceRule).toList(),
        payload.accentColors(),
        payload.viewport());
  }

  private static WidgetsResponse.Card toCard(WidgetViewService.Card card) {
    return new WidgetsResponse.Card(
        card.title(),
        card.chips().stream()
            .map(chip -> new WidgetsResponse.Chip(chip.label(), chip.tone()))
            .toList(),
        card.metrics().stream()
            .map(
                metric ->
                    new WidgetsResponse.Metric(
                        metric.value(), metric.label(), metric.width(), metric.color()))
            .toList(),
        card.action(),
        card.payload());
  }

  private static WidgetsResponse.Selected toSelected(WidgetViewService.Selected selected) {
    return new WidgetsResponse.Selected(
        selected.id(), selected.name(), selected.type(), toListContent(selected.content()));
  }

  private static WidgetsResponse.Content toListContent(WidgetFixtures.Content content) {
    return new WidgetsResponse.Content(
        content.type(),
        content.kicker(),
        content.title(),
        content.body(),
        content.placeholder(),
        content.cta(),
        content.fine());
  }

  private static WidgetsResponse.TypeOption toTypeOption(WidgetFixtures.TypeOption type) {
    return new WidgetsResponse.TypeOption(type.id(), type.label(), type.icon());
  }

  private static WidgetEditorResponse.TypeOption toEditorTypeOption(
      WidgetFixtures.TypeOption type) {
    return new WidgetEditorResponse.TypeOption(type.id(), type.label(), type.icon());
  }

  private static WidgetEditorResponse.Widget toWidget(WidgetViewService.EditorWidget widget) {
    return new WidgetEditorResponse.Widget(
        widget.id(),
        widget.name(),
        widget.meta(),
        widget.type(),
        toEditorContent(widget.content()));
  }

  private static WidgetEditorResponse.Content toEditorContent(WidgetFixtures.Content content) {
    return new WidgetEditorResponse.Content(
        content.type(),
        content.kicker(),
        content.title(),
        content.body(),
        content.placeholder(),
        content.cta(),
        content.fine());
  }

  private static WidgetEditorResponse.Block toBlock(WidgetFixtures.Block block) {
    return new WidgetEditorResponse.Block(block.icon(), block.label(), block.type());
  }

  private static WidgetEditorResponse.Trigger toTrigger(WidgetFixtures.Trigger trigger) {
    return new WidgetEditorResponse.Trigger(
        trigger.label(), trigger.tone(), trigger.note(), trigger.value(), trigger.unit());
  }

  private static WidgetEditorResponse.AudienceRule toAudienceRule(
      WidgetFixtures.AudienceRule rule) {
    return new WidgetEditorResponse.AudienceRule(rule.label(), rule.checked());
  }
}
