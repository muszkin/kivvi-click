package click.kivvi.fixtures;

import java.util.List;

/**
 * E-mail campaign fixture data — ported verbatim from {@code CampaignCatalog}'s private {@code
 * CAMPAIGNS} array, its status-chip map and the composer model behind the e-mail editor (block
 * library, placeholder variables, the 600px document's sections and the inspector's selected
 * block). Raw and unformatted: {@link click.kivvi.application.CampaignsViewService} applies {@link
 * click.kivvi.domain.Format} before this reaches the wire, exactly like this fixture class's
 * siblings ({@code CustomersFixtures}, {@code FeedsFixtures}).
 */
public final class CampaignsFixtures {

  /** One row of the old stack's private {@code CAMPAIGNS} array — status/type are raw enums. */
  public record Campaign(
      String id,
      String name,
      String status,
      String type,
      int sent,
      double open,
      double click,
      int revenue) {}

  /** {@code CampaignCatalog::STATUS_CHIP}'s tone/label pair for one status. */
  public record StatusChip(String tone, String label) {}

  /** One of the ten draggable block sources in {@code block-library.html.twig} — never varies. */
  public record Block(String icon, String label, String type) {}

  /** One placeholder token offered in the "Zmienne" list — never varies. */
  public record Variable(String token, String description) {}

  /** One item in a "products" document section. */
  public record Product(String name, String price) {}

  /**
   * One block of the 600px e-mail document. Flat and nullable-by-type rather than a sealed
   * hierarchy: {@code type} selects which of the other fields are populated, exactly like {@code
   * CampaignCatalog::sections()}'s own untyped array shape — {@code
   * click.kivvi.web.dto.CampaignEmailResponse.Section} mirrors this 1:1 and Jackson's {@code
   * non_null} inclusion (application.yml) drops every field a given section type does not use.
   */
  public record Section(
      String type,
      String kicker,
      String title,
      String body,
      String code,
      String note,
      String cta,
      String href,
      List<Product> items) {}

  /** Inspector state for the block selected in the canvas — never varies (no selection state). */
  public record SelectedBlock(
      String blockName,
      String blockId,
      String title,
      List<String> placeholders,
      List<String> backgrounds,
      String alignment,
      String padding,
      String visibility) {}

  private static final List<Campaign> CAMPAIGNS =
      List.of(
          new Campaign(
              "k1", "Powrót do koszyka — wariant A", "active", "trigger", 1287, 62.4, 18.4, 24800),
          new Campaign("k2", "Witamy w Kivvi", "active", "trigger", 412, 78.1, 41.2, 8930),
          new Campaign(
              "k3", "Newsletter — Tydzień smaków #18", "sent", "broadcast", 8420, 31.8, 6.2, 14200),
          new Campaign("k4", "Black weekend — VIP", "scheduled", "broadcast", 0, 0.0, 0.0, 0),
          new Campaign("k5", "Reaktywacja po 60 dniach", "paused", "trigger", 0, 0.0, 0.0, 0));

  private static final List<Block> BLOCKS =
      List.of(
          new Block("layout", "Nagłówek", "hero"),
          new Block("list", "Tekst", "text"),
          new Block("eye", "Obraz", "image"),
          new Block("cart", "Produkty", "products"),
          new Block("coupon", "Kupon", "coupon"),
          new Block("play", "Przycisk CTA", "cta"),
          new Block("users", "Recenzje", "reviews"),
          new Block("minus", "Separator", "divider"),
          new Block("mail", "Stopka", "footer"),
          new Block("code", "HTML własny", "html"));

  private static final List<Variable> VARIABLES =
      List.of(
          new Variable("{{customer.first_name}}", "Imię klienta"),
          new Variable("{{cart.value}}", "Wartość koszyka"),
          new Variable("{{cart.items}}", "Produkty w koszyku"),
          new Variable("{{coupon.code}}", "Kod kuponu"),
          new Variable("{{site.name}}", "Nazwa sklepu"));

  private static final List<Section> SECTIONS =
      List.of(
          new Section(
              "hero",
              "AUREASHOP · ZIELONE HERBATY",
              "Hania, Twój koszyk czeka.",
              "Zostawiłaś u nas <strong>2 produkty</strong> warte <strong>88,90 zł</strong>. Wróć w"
                  + " 48h, dostajesz <strong>−10%</strong>.",
              null,
              null,
              null,
              null,
              null),
          new Section(
              "coupon",
              null,
              null,
              null,
              "WROCMY-A8F2",
              "Ważny do 14 maja 2026, 23:59",
              "Wróć do koszyka →",
              "#",
              null),
          new Section(
              "products",
              "W TWOIM KOSZYKU",
              null,
              null,
              null,
              null,
              null,
              null,
              List.of(
                  new Product("Zielona herbata Sencha 100g", "38,90 zł"),
                  new Product("Filiżanka porcelanowa Nora", "50,00 zł"))),
          new Section(
              "products",
              "REKOMENDACJE DLA CIEBIE",
              null,
              null,
              null,
              null,
              null,
              null,
              List.of(
                  new Product("Czajnik żeliwny", "189,00 zł"),
                  new Product("Świeca sojowa „Figa”", "49,00 zł"))),
          new Section(
              "footer",
              null,
              null,
              "Dostajesz tę wiadomość, bo subskrybujesz aureashop.pl. <a href=\"#\""
                  + " style=\"color: oklch(0.42 0.06 150);\">Zarządzaj subskrypcjami</a> · <a"
                  + " href=\"#\" style=\"color: oklch(0.42 0.06 150);\">Wypisz się</a>",
              null,
              null,
              null,
              null,
              null));

  private static final SelectedBlock SELECTED_BLOCK =
      new SelectedBlock(
          "Hero — tytuł + opis",
          "hero_1",
          "Hania, Twój koszyk czeka.",
          List.of("{{customer.first_name}}, Twój koszyk czeka.", "Twój koszyk czeka."),
          List.of(
              "oklch(0.94 0.02 85)",
              "oklch(0.90 0.04 150)",
              "oklch(0.88 0.035 60)",
              "oklch(1 0 0)"),
          "center",
          "36px 32px",
          "customer.has_orders > 0");

  private CampaignsFixtures() {}

  public static List<Campaign> all() {
    return CAMPAIGNS;
  }

  public static StatusChip statusChip(String status) {
    return switch (status) {
      case "active" -> new StatusChip("good", "Aktywna");
      case "paused" -> new StatusChip("warn", "Wstrzymana");
      case "sent" -> new StatusChip("neutral", "Wysłana");
      case "scheduled" -> new StatusChip("info", "Zaplanowana");
      default -> new StatusChip("neutral", "Szkic");
    };
  }

  public static List<Block> blocks() {
    return BLOCKS;
  }

  public static List<Variable> variables() {
    return VARIABLES;
  }

  public static List<Section> sections() {
    return SECTIONS;
  }

  public static SelectedBlock selectedBlock() {
    return SELECTED_BLOCK;
  }
}
