package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/automations/{id}} response: the same rule model rendered two
 * interchangeable ways (list pipeline / flow graph) plus its simulation — the wire shape {@link
 * click.kivvi.web.AutomationsController} builds from {@link
 * click.kivvi.application.AutomationsViewService}'s computed view-model. Field names mirror {@code
 * AutomationController::editor}'s own render parameters exactly.
 */
public record AutomationEditorResponse(
    Automation automation,
    String view,
    List<Tab> tabs,
    List<PipelineStep> steps,
    List<FlowNode> nodes,
    List<Edge> edges,
    List<SimulationItem> simulation) {

  /** The editor header identity. */
  public record Automation(String id, String name, String status, String statusLabel) {}

  /** One tab of the editor's tab strip. */
  public record Tab(String label, boolean active) {}

  /** One block inside a rule-pipeline step; {@code body} carries inline HTML (pills). */
  public record PipelineBlock(String icon, String title, String body) {}

  /** One KIEDY/JEŚLI/WTEDY step of the linear rule pipeline. */
  public record PipelineStep(
      int n, String kicker, String title, String addLabel, List<PipelineBlock> blocks) {}

  /** One node of the rule flow graph, positioned in canvas pixels. */
  public record FlowNode(
      int x, int y, String kind, String kicker, String title, String sub, String icon) {}

  /** One directed edge of the flow graph, by node index. */
  public record Edge(int from, int to) {}

  /**
   * One read-out of the rule simulation; {@code value} may carry inline HTML. {@code color} is
   * {@code null} when unstyled.
   */
  public record SimulationItem(String label, String value, String note, String color) {}
}
