package click.kivvi.application;

import click.kivvi.domain.Format;
import click.kivvi.fixtures.CampaignsFixtures;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Assembles the campaigns index and e-mail editor view-models — mirrors what {@code
 * CampaignController} rendered from {@code CampaignCatalog}. Every count/money/percent value is
 * formatted here, exactly like {@code CampaignCatalog::rows()}/{@code ::kpis()} already did on the
 * old stack — the SPA never formats a number itself.
 *
 * <p>Neither endpoint varies its content by locale: {@code CampaignCatalog} never took a locale
 * parameter either (its fixture text — campaign names, status/type labels, block library, document
 * copy — is hard-coded Polish regardless of {@code _locale}), so {@code locale} only gates which of
 * the two supported route prefixes resolves, exactly like {@code FeedsViewService}'s own doc
 * comment describes for the product-feeds page.
 */
@Service
public class CampaignsViewService {

  private static final String SENDER = "sklep@aureashop.pl";
  private static final String KNOWN_TEMPLATE_META =
      "Szablon wyzwalany · 3 produkty placeholders · 412 wysyłek (7d)";
  private static final String KNOWN_TEMPLATE_SUBJECT =
      "Hania, Twój koszyk czeka — wróć i odbierz −10%";
  private static final String NEW_TEMPLATE_NAME = "Nowy szablon email";
  private static final String NEW_TEMPLATE_META = "Szkic · nigdy nie wysłany";
  private static final String NEW_TEMPLATE_SUBJECT = "Temat wiadomości";

  /** One KPI tile, every number/percent already formatted for display. */
  public record Kpi(String label, String value, String unit, String delta, String dir) {}

  /** One filter-rail chip; {@code count} arrives pre-formatted (never reaches four digits here). */
  public record Filter(String label, String count, boolean active, String action, String payload) {}

  public record Column(String label, String align) {}

  /** One campaigns-index row, every number already formatted for display. */
  public record Row(
      String id,
      String name,
      String statusTone,
      String statusLabel,
      String typeLabel,
      String typeTone,
      String sent,
      String open,
      String click,
      String revenue) {}

  public record ListPayload(
      List<Kpi> kpis, List<Filter> filters, List<Column> columns, List<Row> rows) {}

  public record Template(String id, String name, String meta, String subject, String sender) {}

  public record EditorPayload(
      Template template,
      List<CampaignsFixtures.Block> blocks,
      List<CampaignsFixtures.Variable> variables,
      List<CampaignsFixtures.Section> sections,
      CampaignsFixtures.SelectedBlock selectedBlock) {}

  public ListPayload list(String filter) {
    return new ListPayload(kpis(), filters(filter), columns(), rows());
  }

  /**
   * @param id "new" or a {@code k\d+} id; an id not present in the seeded campaigns (including
   *     "new" itself) falls back to the same blank-template shape {@code CampaignCatalog::template}
   *     returns for any unmatched id — not just literally "new".
   */
  public EditorPayload editor(String id) {
    return new EditorPayload(
        template(id),
        CampaignsFixtures.blocks(),
        CampaignsFixtures.variables(),
        CampaignsFixtures.sections(),
        CampaignsFixtures.selectedBlock());
  }

  private static List<Kpi> kpis() {
    return List.of(
        new Kpi("Wysłane (30 dni)", Format.number(142410), null, "+18% vs poprzedni okres", "up"),
        new Kpi("Średni open rate", "38,4", "%", "+2,1pp", "up"),
        new Kpi("Średni CTR", "7,8", "%", "−0,4pp", "down"),
        new Kpi("Przychód z kampanii", Format.money(184230), null, "+24%", "up"));
  }

  private static List<Filter> filters(String active) {
    String safeActive = active == null ? "all" : active;
    return List.of(
        new Filter("Wszystkie", "18", "all".equals(safeActive), "set-campaign-filter", "all"),
        new Filter(
            "Wyzwalane", "12", "trigger".equals(safeActive), "set-campaign-filter", "trigger"),
        new Filter(
            "Jednorazowe", "6", "broadcast".equals(safeActive), "set-campaign-filter", "broadcast"),
        new Filter(
            "Wstrzymane", "2", "paused".equals(safeActive), "set-campaign-filter", "paused"));
  }

  private static List<Column> columns() {
    return List.of(
        new Column("Kampania", null),
        new Column("Typ", null),
        new Column("Status", null),
        new Column("Wysłane", "right"),
        new Column("Otwarcia", "right"),
        new Column("Kliknięcia", "right"),
        new Column("Przychód", "right"),
        new Column("", null));
  }

  private static List<Row> rows() {
    return CampaignsFixtures.all().stream().map(CampaignsViewService::toRow).toList();
  }

  private static Row toRow(CampaignsFixtures.Campaign campaign) {
    CampaignsFixtures.StatusChip chip = CampaignsFixtures.statusChip(campaign.status());
    boolean trigger = "trigger".equals(campaign.type());
    return new Row(
        campaign.id(),
        campaign.name(),
        chip.tone(),
        chip.label(),
        trigger ? "Wyzwalana" : "Masowa",
        trigger ? "accent" : "brown",
        campaign.sent() > 0 ? Format.number(campaign.sent()) : "—",
        campaign.open() > 0 ? Format.percent(campaign.open()) : "—",
        campaign.click() > 0 ? Format.percent(campaign.click()) : "—",
        campaign.revenue() > 0 ? Format.money(campaign.revenue()) : "—");
  }

  private static Template template(String id) {
    return CampaignsFixtures.all().stream()
        .filter(campaign -> campaign.id().equals(id))
        .findFirst()
        .map(
            campaign ->
                new Template(
                    campaign.id(),
                    campaign.name(),
                    KNOWN_TEMPLATE_META,
                    KNOWN_TEMPLATE_SUBJECT,
                    SENDER))
        .orElseGet(
            () ->
                new Template(
                    id, NEW_TEMPLATE_NAME, NEW_TEMPLATE_META, NEW_TEMPLATE_SUBJECT, SENDER));
  }
}
