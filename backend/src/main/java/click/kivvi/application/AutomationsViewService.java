package click.kivvi.application;

import click.kivvi.domain.Format;
import click.kivvi.fixtures.AutomationsFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Assembles the automations index and rule-editor view-models — mirrors what {@code
 * AutomationController} rendered from {@code AutomationCatalog}. Every number reaching the SPA is
 * already formatted (see {@link Format}): the index's counts and per-row metrics here, the editor's
 * rule-builder content is fixture text pre-baked the same way {@code AutomationsFixtures} always
 * has been.
 *
 * <p>The status filter never actually filters the index list — {@code AutomationController::index}
 * always returned every automation and only used the query parameter to mark a filter-chip active,
 * a quirk of the old page reproduced here on purpose (see {@code common-journey-rules.md}'s
 * query-string-state rule: "Query-string state … is read by the backend view service and reflected
 * in the payload, exactly as the Symfony controller did.").
 */
@Service
public class AutomationsViewService {

  private static final String VIEW_FLOW = "flow";
  private static final String VIEW_LIST = "list";
  private static final String STATUS_ALL = "all";

  /** One status filter-chip on the index. */
  public record Filter(
      String label, String icon, String count, boolean active, String action, String payload) {}

  /** One metadata/status chip on an index card. */
  public record Chip(String label, String tone) {}

  /** One right-aligned metric on an index card. */
  public record Metric(String value, String label, int width, String color) {}

  /** One automations-index card. */
  public record AutomationCard(
      String title, List<Chip> chips, List<Metric> metrics, String action, String payload) {}

  public record ListPayload(List<Filter> filters, List<AutomationCard> automations) {}

  public record EditorPayload(
      AutomationsFixtures.Header automation,
      String view,
      List<AutomationsFixtures.EditorTab> tabs,
      List<AutomationsFixtures.PipelineStep> steps,
      List<AutomationsFixtures.FlowNode> nodes,
      List<AutomationsFixtures.Edge> edges,
      List<AutomationsFixtures.SimulationItem> simulation) {}

  public ListPayload list(String status) {
    return new ListPayload(statusFilters(status), cards());
  }

  public EditorPayload editor(String id, String view) {
    String resolvedView = VIEW_FLOW.equals(view) ? VIEW_FLOW : VIEW_LIST;
    return new EditorPayload(
        AutomationsFixtures.header(id),
        resolvedView,
        AutomationsFixtures.editorTabs(),
        AutomationsFixtures.pipelineSteps(),
        AutomationsFixtures.flowNodes(),
        AutomationsFixtures.flowEdges(),
        AutomationsFixtures.simulation());
  }

  private static List<Filter> statusFilters(String active) {
    Map<String, Long> counts =
        AutomationsFixtures.all().stream()
            .collect(
                Collectors.groupingBy(
                    AutomationsFixtures.Automation::status, Collectors.counting()));
    return List.of(
        new Filter(
            "Wszystkie",
            "grid",
            Format.number(AutomationsFixtures.all().size()),
            STATUS_ALL.equals(active),
            "set-automation-status",
            STATUS_ALL),
        new Filter(
            "Aktywne",
            null,
            Format.number(counts.getOrDefault("active", 0L)),
            "active".equals(active),
            "set-automation-status",
            "active"),
        new Filter(
            "Wstrzymane",
            null,
            Format.number(counts.getOrDefault("paused", 0L)),
            "paused".equals(active),
            "set-automation-status",
            "paused"),
        new Filter(
            "Szkice",
            null,
            Format.number(counts.getOrDefault("draft", 0L)),
            "draft".equals(active),
            "set-automation-status",
            "draft"));
  }

  private static List<AutomationCard> cards() {
    return AutomationsFixtures.all().stream().map(AutomationsViewService::toCard).toList();
  }

  private static AutomationCard toCard(AutomationsFixtures.Automation automation) {
    AutomationsFixtures.StatusChip statusChip = AutomationsFixtures.statusChip(automation.status());

    List<Chip> chips = new ArrayList<>();
    chips.add(new Chip(statusChip.label(), statusChip.tone()));
    chips.add(new Chip(AutomationsFixtures.triggerLabel(automation.trigger()), "accent"));
    for (String channel : automation.channels()) {
      chips.add(new Chip(channel, "brown"));
    }

    boolean hasConversion = automation.conversion() > 0;
    boolean hasRevenue = automation.revenue() > 0;
    List<Metric> metrics =
        List.of(
            new Metric(Format.number(automation.runs()), "uruchomień (7d)", 90, null),
            new Metric(
                hasConversion ? Format.percent(automation.conversion()) : "—",
                "konwersja",
                80,
                hasConversion ? "var(--good)" : "var(--fg-muted)"),
            new Metric(
                hasRevenue ? Format.money(automation.revenue()) : "—", "przychód (7d)", 110, null));

    return new AutomationCard(automation.name(), chips, metrics, "go-automation", automation.id());
  }
}
