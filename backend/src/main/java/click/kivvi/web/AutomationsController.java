package click.kivvi.web;

import click.kivvi.application.AutomationsViewService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.AutomationsFixtures;
import click.kivvi.web.dto.AutomationEditorResponse;
import click.kivvi.web.dto.AutomationListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Automation index and the rule editor — mirrors {@code AutomationController}. The document routes
 * themselves are served by {@link SpaDocumentController} from {@code RouteTable} (no data-dependent
 * 404 here, unlike customers: an id shaped like {@code a\d+} but not seeded still renders the
 * editor, with a generic draft header — see {@code AutomationsFixtures#header}, ported from {@code
 * AutomationCatalog::header}'s own fallback). The {@code {id:new|a\d+}} constraint below exists
 * only so the API 404s for an id shape the route table could never have matched either, rather than
 * falling through to a made-up header for garbage input.
 */
@RestController
public class AutomationsController {

  private final AutomationsViewService automationsViewService;

  public AutomationsController(AutomationsViewService automationsViewService) {
    this.automationsViewService = automationsViewService;
  }

  @GetMapping("/api/v1/{locale:pl|en}/automations")
  public AutomationListResponse list(
      @PathVariable String locale, @RequestParam(defaultValue = "all") String status) {
    SupportedLocale.fromCode(locale).orElseThrow();
    AutomationsViewService.ListPayload payload = automationsViewService.list(status);
    return new AutomationListResponse(
        payload.filters().stream().map(AutomationsController::toFilter).toList(),
        payload.automations().stream().map(AutomationsController::toCard).toList());
  }

  @GetMapping("/api/v1/{locale:pl|en}/automations/{id:new|a\\d+}")
  public AutomationEditorResponse editor(
      @PathVariable String locale,
      @PathVariable String id,
      @RequestParam(required = false) String view) {
    SupportedLocale.fromCode(locale).orElseThrow();
    AutomationsViewService.EditorPayload payload = automationsViewService.editor(id, view);
    return new AutomationEditorResponse(
        toAutomation(payload.automation()),
        payload.view(),
        payload.tabs().stream().map(AutomationsController::toTab).toList(),
        payload.steps().stream().map(AutomationsController::toStep).toList(),
        payload.nodes().stream().map(AutomationsController::toNode).toList(),
        payload.edges().stream().map(AutomationsController::toEdge).toList(),
        payload.simulation().stream().map(AutomationsController::toSimulationItem).toList());
  }

  private static AutomationListResponse.Filter toFilter(AutomationsViewService.Filter filter) {
    return new AutomationListResponse.Filter(
        filter.label(),
        filter.icon(),
        filter.count(),
        filter.active(),
        filter.action(),
        filter.payload());
  }

  private static AutomationListResponse.AutomationCard toCard(
      AutomationsViewService.AutomationCard card) {
    return new AutomationListResponse.AutomationCard(
        card.title(),
        card.chips().stream()
            .map(chip -> new AutomationListResponse.Chip(chip.label(), chip.tone()))
            .toList(),
        card.metrics().stream()
            .map(
                metric ->
                    new AutomationListResponse.Metric(
                        metric.value(), metric.label(), metric.width(), metric.color()))
            .toList(),
        card.action(),
        card.payload());
  }

  private static AutomationEditorResponse.Automation toAutomation(
      AutomationsFixtures.Header header) {
    return new AutomationEditorResponse.Automation(
        header.id(), header.name(), header.status(), header.statusLabel());
  }

  private static AutomationEditorResponse.Tab toTab(AutomationsFixtures.EditorTab tab) {
    return new AutomationEditorResponse.Tab(tab.label(), tab.active());
  }

  private static AutomationEditorResponse.PipelineStep toStep(
      AutomationsFixtures.PipelineStep step) {
    return new AutomationEditorResponse.PipelineStep(
        step.n(),
        step.kicker(),
        step.title(),
        step.addLabel(),
        step.blocks().stream()
            .map(
                block ->
                    new AutomationEditorResponse.PipelineBlock(
                        block.icon(), block.title(), block.body()))
            .toList());
  }

  private static AutomationEditorResponse.FlowNode toNode(AutomationsFixtures.FlowNode node) {
    return new AutomationEditorResponse.FlowNode(
        node.x(), node.y(), node.kind(), node.kicker(), node.title(), node.sub(), node.icon());
  }

  private static AutomationEditorResponse.Edge toEdge(AutomationsFixtures.Edge edge) {
    return new AutomationEditorResponse.Edge(edge.from(), edge.to());
  }

  private static AutomationEditorResponse.SimulationItem toSimulationItem(
      AutomationsFixtures.SimulationItem item) {
    return new AutomationEditorResponse.SimulationItem(
        item.label(), item.value(), item.note(), item.color());
  }
}
