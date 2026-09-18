package click.kivvi.application;

import click.kivvi.domain.Format;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.CampaignsFixtures;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Assembles the campaigns index and e-mail editor view-models — mirrors what {@code
 * CampaignController} rendered from {@code CampaignCatalog}. Every count/money/percent value is
 * formatted here, exactly like {@code CampaignCatalog::rows()}/{@code ::kpis()} already did on the
 * old stack — the SPA never formats a number itself.
 *
 * <p>Neither endpoint used to vary its content by locale: {@code CampaignCatalog} never took a
 * locale parameter, and its fixture text — campaign names, status and type labels, the block
 * library, the sample e-mail's own copy — was hard-coded Polish whatever {@code _locale} said.
 * PIO-129 changed that: both endpoints now answer in the language of the route prefix, including
 * the demonstration e-mail itself, which is written in euro for English readers.
 */
@Service
public class CampaignsViewService {

  private static final int SENT_LAST_30_DAYS = 142_410;
  private static final int REVENUE_FROM_CAMPAIGNS = 184_230;

  /** The demonstration shop's sending address, in the language of the shop being shown. */
  private static final String SENDER_PL = "sklep@aureashop.pl";

  private static final String SENDER_EN = "shop@aureashop.pl";

  private static final String KNOWN_TEMPLATE_META_PL =
      "Szablon wyzwalany · 3 produkty placeholders · 412 wysyłek (7d)";
  private static final String KNOWN_TEMPLATE_META_EN =
      "Triggered template · 3 product placeholders · 412 sends (7d)";
  private static final String KNOWN_TEMPLATE_SUBJECT_PL =
      "Hania, Twój koszyk czeka — wróć i odbierz −10%";
  private static final String KNOWN_TEMPLATE_SUBJECT_EN =
      "Hannah, your basket is waiting — come back for −10%";
  private static final String NEW_TEMPLATE_NAME_PL = "Nowy szablon email";
  private static final String NEW_TEMPLATE_NAME_EN = "New email template";
  private static final String NEW_TEMPLATE_META_PL = "Szkic · nigdy nie wysłany";
  private static final String NEW_TEMPLATE_META_EN = "Draft · never sent";
  private static final String NEW_TEMPLATE_SUBJECT_PL = "Temat wiadomości";
  private static final String NEW_TEMPLATE_SUBJECT_EN = "Message subject";

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

  public ListPayload list(SupportedLocale locale, String filter) {
    return new ListPayload(kpis(locale), filters(locale, filter), columns(locale), rows(locale));
  }

  /**
   * @param id "new" or a {@code k\d+} id; an id not present in the seeded campaigns (including
   *     "new" itself) falls back to the same blank-template shape {@code CampaignCatalog::template}
   *     returns for any unmatched id — not just literally "new".
   */
  public EditorPayload editor(SupportedLocale locale, String id) {
    return new EditorPayload(
        template(locale, id),
        CampaignsFixtures.blocks(locale),
        CampaignsFixtures.variables(locale),
        CampaignsFixtures.sections(locale),
        CampaignsFixtures.selectedBlock(locale));
  }

  private static List<Kpi> kpis(SupportedLocale locale) {
    String sent = Format.number(SENT_LAST_30_DAYS, locale);
    String revenue = Format.money(REVENUE_FROM_CAMPAIGNS, locale);
    return switch (locale) {
      case PL ->
          List.of(
              new Kpi("Wysłane (30 dni)", sent, null, "+18% vs poprzedni okres", "up"),
              new Kpi("Średni open rate", "38,4", "%", "+2,1pp", "up"),
              new Kpi("Średni CTR", "7,8", "%", "−0,4pp", "down"),
              new Kpi("Przychód z kampanii", revenue, null, "+24%", "up"));
      case EN ->
          List.of(
              new Kpi("Sent (30 days)", sent, null, "+18% vs the previous period", "up"),
              new Kpi("Average open rate", "38.4", "%", "+2.1pp", "up"),
              new Kpi("Average CTR", "7.8", "%", "−0.4pp", "down"),
              new Kpi("Campaign revenue", revenue, null, "+24%", "up"));
    };
  }

  private static List<Filter> filters(SupportedLocale locale, String active) {
    String safeActive = active == null ? "all" : active;
    String[] labels =
        switch (locale) {
          case PL -> new String[] {"Wszystkie", "Wyzwalane", "Jednorazowe", "Wstrzymane"};
          case EN -> new String[] {"All", "Triggered", "One-off", "Paused"};
        };
    return List.of(
        new Filter(labels[0], "18", "all".equals(safeActive), "set-campaign-filter", "all"),
        new Filter(labels[1], "12", "trigger".equals(safeActive), "set-campaign-filter", "trigger"),
        new Filter(
            labels[2], "6", "broadcast".equals(safeActive), "set-campaign-filter", "broadcast"),
        new Filter(labels[3], "2", "paused".equals(safeActive), "set-campaign-filter", "paused"));
  }

  private static List<Column> columns(SupportedLocale locale) {
    String[] headings =
        switch (locale) {
          case PL ->
              new String[] {
                "Kampania", "Typ", "Status", "Wysłane", "Otwarcia", "Kliknięcia", "Przychód"
              };
          case EN ->
              new String[] {"Campaign", "Type", "Status", "Sent", "Opens", "Clicks", "Revenue"};
        };
    return List.of(
        new Column(headings[0], null),
        new Column(headings[1], null),
        new Column(headings[2], null),
        new Column(headings[3], "right"),
        new Column(headings[4], "right"),
        new Column(headings[5], "right"),
        new Column(headings[6], "right"),
        new Column("", null));
  }

  private static List<Row> rows(SupportedLocale locale) {
    return CampaignsFixtures.all(locale).stream().map(campaign -> toRow(locale, campaign)).toList();
  }

  private static Row toRow(SupportedLocale locale, CampaignsFixtures.Campaign campaign) {
    CampaignsFixtures.StatusChip chip = CampaignsFixtures.statusChip(locale, campaign.status());
    boolean trigger = "trigger".equals(campaign.type());
    String[] typeLabels =
        switch (locale) {
          case PL -> new String[] {"Wyzwalana", "Masowa"};
          case EN -> new String[] {"Triggered", "Broadcast"};
        };
    return new Row(
        campaign.id(),
        campaign.name(),
        chip.tone(),
        chip.label(),
        trigger ? typeLabels[0] : typeLabels[1],
        trigger ? "accent" : "brown",
        campaign.sent() > 0 ? Format.number(campaign.sent(), locale) : "—",
        campaign.open() > 0 ? Format.percent(campaign.open(), locale) : "—",
        campaign.click() > 0 ? Format.percent(campaign.click(), locale) : "—",
        campaign.revenue() > 0 ? Format.money(campaign.revenue(), locale) : "—");
  }

  private static Template template(SupportedLocale locale, String id) {
    String sender = pick(locale, SENDER_PL, SENDER_EN);
    return CampaignsFixtures.all(locale).stream()
        .filter(campaign -> campaign.id().equals(id))
        .findFirst()
        .map(
            campaign ->
                new Template(
                    campaign.id(),
                    campaign.name(),
                    pick(locale, KNOWN_TEMPLATE_META_PL, KNOWN_TEMPLATE_META_EN),
                    pick(locale, KNOWN_TEMPLATE_SUBJECT_PL, KNOWN_TEMPLATE_SUBJECT_EN),
                    sender))
        .orElseGet(
            () ->
                new Template(
                    id,
                    pick(locale, NEW_TEMPLATE_NAME_PL, NEW_TEMPLATE_NAME_EN),
                    pick(locale, NEW_TEMPLATE_META_PL, NEW_TEMPLATE_META_EN),
                    pick(locale, NEW_TEMPLATE_SUBJECT_PL, NEW_TEMPLATE_SUBJECT_EN),
                    sender));
  }

  private static String pick(SupportedLocale locale, String polish, String english) {
    return switch (locale) {
      case PL -> polish;
      case EN -> english;
    };
  }
}
