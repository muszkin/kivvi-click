package click.kivvi.fixtures;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Automations catalogue — ported from {@code AutomationCatalog}. {@link #activeForCustomer()}
 * predates this journey (the 360 profile's only consumer); everything else below is the automations
 * journey's own index/editor data, kept in this same class per the packet's instruction to extend
 * rather than replace it.
 *
 * <p>The rule-builder content ({@link #pipelineSteps()}, {@link #flowNodes()}, {@link
 * #flowEdges()}, {@link #simulation()}, {@link #editorTabs()}) is the same fixed demo rule for
 * every automation id — {@code AutomationCatalog} never varied it by id either, only {@link
 * #header(String)} does.
 */
public final class AutomationsFixtures {

  /** One automation currently running for a customer — just its display name. */
  public record ActiveAutomation(String name) {}

  /** One row of the automations index / seed for the editor header. */
  public record Automation(
      String id,
      String name,
      String status,
      String trigger,
      List<String> channels,
      int runs,
      double conversion,
      double revenue) {}

  /** The tone + label a status renders as, everywhere it appears as a chip. */
  public record StatusChip(String tone, String label) {}

  /** The editor header identity — what {@link #header(String)} returns. */
  public record Header(String id, String name, String status, String statusLabel) {}

  /** One tab of the editor's tab strip. */
  public record EditorTab(String label, boolean active) {}

  /** One block inside a rule-pipeline step; {@code body} is developer-authored HTML (pills). */
  public record PipelineBlock(String icon, String title, String body) {}

  /** One KIEDY/JEŚLI/WTEDY step of the linear rule pipeline. */
  public record PipelineStep(
      int n, String kicker, String title, String addLabel, List<PipelineBlock> blocks) {}

  /** One node of the rule flow graph, positioned in canvas pixels. */
  public record FlowNode(
      int x, int y, String kind, String kicker, String title, String sub, String icon) {}

  /** One directed edge of the flow graph, by node index. */
  public record Edge(int from, int to) {}

  /** One read-out of the rule simulation; {@code value} may carry inline HTML. */
  public record SimulationItem(String label, String value, String note, String color) {}

  private static final List<ActiveAutomation> ACTIVE_FOR_CUSTOMER =
      List.of(
          new ActiveAutomation("Powitanie po rejestracji"),
          new ActiveAutomation("Rekomendacje „podobne produkty”"),
          new ActiveAutomation("Newsletter — Tydzień smaków"));

  private static final List<Automation> AUTOMATIONS =
      List.of(
          new Automation(
              "a1",
              "Powrót do porzuconego koszyka",
              "active",
              "cart_abandon",
              List.of("email", "popup"),
              1287,
              18.4,
              24800),
          new Automation(
              "a2",
              "Powitanie po rejestracji",
              "active",
              "signup",
              List.of("email"),
              412,
              41.2,
              8930),
          new Automation(
              "a3",
              "Rekomendacje „podobne produkty”",
              "active",
              "pageview",
              List.of("widget"),
              18420,
              7.9,
              41200),
          new Automation(
              "a4",
              "Kupon dla VIP po 5 zamówieniach",
              "active",
              "purchase",
              List.of("coupon", "email"),
              89,
              62.1,
              12400),
          new Automation(
              "a5",
              "Win-back po 60 dniach nieaktywności",
              "paused",
              "inactivity",
              List.of("email"),
              0,
              0.0,
              0),
          new Automation(
              "a6",
              "Powiadomienie o powrocie produktu",
              "draft",
              "product_back_in_stock",
              List.of("email"),
              0,
              0.0,
              0));

  private static final Map<String, StatusChip> STATUS_CHIP =
      Map.of(
          "active", new StatusChip("good", "Aktywna"),
          "paused", new StatusChip("warn", "Wstrzymana"),
          "draft", new StatusChip("neutral", "Szkic"));

  private static final Map<String, String> TRIGGER_LABEL =
      Map.of(
          "cart_abandon", "Porzucenie koszyka",
          "signup", "Rejestracja",
          "pageview", "Wyświetlenie produktu",
          "purchase", "Zakup",
          "inactivity", "Brak aktywności",
          "product_back_in_stock", "Powrót produktu");

  private static final String NEW_AUTOMATION_NAME = "Nowa automatyzacja";
  private static final String DRAFT_STATUS_LABEL = "Wersja robocza";
  private static final String STATUS_LABEL_SUFFIX =
      " · Edytuj logikę uruchamiania, warunki i akcje";

  private static final List<EditorTab> EDITOR_TABS =
      List.of(
          new EditorTab("Budowniczy", true),
          new EditorTab("Statystyki", false),
          new EditorTab("Wykonania (1 287)", false),
          new EditorTab("Historia zmian", false));

  private static final List<PipelineStep> PIPELINE_STEPS =
      List.of(
          new PipelineStep(
              1,
              "KIEDY · trigger",
              "Klient porzuca koszyk",
              "Dodaj warunek wyzwalacza",
              List.of(
                  new PipelineBlock(
                      "cart",
                      "Zdarzenie",
                      "<span class=\"pill\">cart_abandon</span> wyzwolone po "
                          + "<span class=\"pill\">8 min</span> nieaktywności"),
                  new PipelineBlock(
                      "globe",
                      "Strony",
                      "Reguła działa na: <span class=\"pill\">aureashop.pl</span> "
                          + "<span class=\"pill\">mlot-narzedzia.pl</span>"),
                  new PipelineBlock(
                      "user",
                      "Segment klienta",
                      "Klient z koszykiem o wartości <span class=\"pill\">≥ 150 PLN</span>, "
                          + "niebędący w segmencie <span class=\"pill\">VIP</span>"))),
          new PipelineStep(
              2,
              "JEŚLI · warunki",
              "Wszystkie muszą się zgadzać",
              "Dodaj warunek",
              List.of(
                  new PipelineBlock(
                      "eye",
                      "Częstotliwość",
                      "Klient <strong>nie otrzymał</strong> tej automatyzacji w ciągu "
                          + "<span class=\"pill\">7 dni</span>"),
                  new PipelineBlock(
                      "book",
                      "Historia zakupów",
                      "Liczba zamówień <span class=\"pill\">≥ 1</span> w ciągu ostatnich "
                          + "<span class=\"pill\">180 dni</span>"),
                  new PipelineBlock(
                      "mail",
                      "Subskrypcja",
                      "Klient ma zgodę marketingową <span class=\"pill\">tak</span> i adres "
                          + "email <span class=\"pill\">zweryfikowany</span>"))),
          new PipelineStep(
              3,
              "WTEDY · akcje",
              "Sekwencja krok po kroku",
              "Dodaj krok",
              List.of(
                  new PipelineBlock(
                      "mail",
                      "Wyślij email — od razu",
                      "Szablon <span class=\"pill\">„Wróć po Twój koszyk”</span> · od "
                          + "<span class=\"pill\">sklep@aureashop.pl</span>"),
                  new PipelineBlock(
                      "coupon",
                      "Po 24h — wyślij kupon",
                      "Kod jednorazowy <span class=\"pill\">WROCMY-{id}</span> · "
                          + "<span class=\"pill\">−10%</span> na cały koszyk · ważny 48h"),
                  new PipelineBlock(
                      "layout",
                      "Pokaż popup przy powrocie",
                      "Slide-in z napisem „Twój koszyk czeka” — tylko jeśli klient wraca w "
                          + "ciągu <span class=\"pill\">72h</span>"))));

  private static final List<FlowNode> FLOW_NODES =
      List.of(
          new FlowNode(
              30, 40, "trigger", "KIEDY", "Porzucenie koszyka", "cart_abandon · po 8 min", "cart"),
          new FlowNode(
              320, 40, "cond", "JEŚLI", "Wartość ≥ 150 PLN", "cart.value >= 150", "filter"),
          new FlowNode(
              320,
              200,
              "cond",
              "JEŚLI",
              "Nie był w segmencie VIP",
              "customer.segment != \"vip\"",
              "filter"),
          new FlowNode(
              610, 40, "action", "WTEDY · 1", "Email — szablon A", "„Wróć po koszyk”", "mail"),
          new FlowNode(610, 200, "action", "WTEDY · 2", "Czekaj 24h", "delay 24h", "pause"),
          new FlowNode(
              610, 360, "action", "WTEDY · 3", "Kupon −10%", "coupon: WROCMY-{id}", "coupon"));

  private static final List<Edge> FLOW_EDGES =
      List.of(new Edge(0, 1), new Edge(1, 2), new Edge(1, 3), new Edge(3, 4), new Edge(4, 5));

  private static final List<SimulationItem> SIMULATION =
      List.of(
          new SimulationItem("Zdarzeń pasujących", "2 412", "w ostatnich 7 dniach", null),
          new SimulationItem(
              "Spełniających warunki",
              "1 287 <span class=\"muted\" style=\"font-size:14px;\">(53,4%)</span>",
              "zostałoby uruchomione",
              null),
          new SimulationItem(
              "Estymowany przychód", "~ 24 800 zł", "przy 18,4% konwersji", "var(--good)"));

  private AutomationsFixtures() {}

  public static List<ActiveAutomation> activeForCustomer() {
    return ACTIVE_FOR_CUSTOMER;
  }

  public static List<Automation> all() {
    return AUTOMATIONS;
  }

  public static Optional<Automation> byId(String id) {
    return AUTOMATIONS.stream().filter(automation -> automation.id().equals(id)).findFirst();
  }

  public static StatusChip statusChip(String status) {
    return STATUS_CHIP.get(status);
  }

  public static String triggerLabel(String trigger) {
    return TRIGGER_LABEL.get(trigger);
  }

  /**
   * The editor header for {@code id}: a seeded automation's own identity, or — mirroring {@code
   * AutomationCatalog::header()}'s fallback — a generic draft header for any id that does not match
   * a seeded row (including {@code "new"}).
   */
  public static Header header(String id) {
    return byId(id)
        .map(
            automation ->
                new Header(
                    automation.id(),
                    automation.name(),
                    automation.status(),
                    statusChip(automation.status()).label() + STATUS_LABEL_SUFFIX))
        .orElseGet(
            () ->
                new Header(
                    id, NEW_AUTOMATION_NAME, "draft", DRAFT_STATUS_LABEL + STATUS_LABEL_SUFFIX));
  }

  public static List<EditorTab> editorTabs() {
    return EDITOR_TABS;
  }

  public static List<PipelineStep> pipelineSteps() {
    return PIPELINE_STEPS;
  }

  public static List<FlowNode> flowNodes() {
    return FLOW_NODES;
  }

  public static List<Edge> flowEdges() {
    return FLOW_EDGES;
  }

  public static List<SimulationItem> simulation() {
    return SIMULATION;
  }
}
