package click.kivvi.web;

import click.kivvi.application.CampaignsViewService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.CampaignsFixtures;
import click.kivvi.web.dto.CampaignEmailResponse;
import click.kivvi.web.dto.CampaignsResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The campaigns index and its e-mail template editor's data — mirrors {@code CampaignController}.
 * Both document routes ({@code /campaigns}, {@code /emails/new}, {@code /emails/k\d+}) are already
 * plain entries in {@link click.kivvi.domain.RouteTable}, served generically by {@link
 * SpaDocumentController}: unlike the customer profile, every {@code k\d+} id (seeded or not)
 * renders the same 200 SPA shell, exactly like {@code CampaignController::edit}'s route requirement
 * never re-validated the id against {@code CampaignCatalog} either — only this controller's own
 * {@code editor()}/{@code template()} chain decides whether an id is a known campaign.
 */
@RestController
public class CampaignsController {

  private final CampaignsViewService campaignsViewService;

  public CampaignsController(CampaignsViewService campaignsViewService) {
    this.campaignsViewService = campaignsViewService;
  }

  @GetMapping("/api/v1/{locale:pl|en}/campaigns")
  public CampaignsResponse campaigns(
      @PathVariable String locale, @RequestParam(defaultValue = "all") String filter) {
    SupportedLocale.fromCode(locale).orElseThrow();
    CampaignsViewService.ListPayload payload = campaignsViewService.list(filter);
    return new CampaignsResponse(
        payload.kpis().stream().map(CampaignsController::toKpi).toList(),
        payload.filters().stream().map(CampaignsController::toFilter).toList(),
        payload.columns().stream().map(CampaignsController::toColumn).toList(),
        payload.rows().stream().map(CampaignsController::toRow).toList());
  }

  @GetMapping("/api/v1/{locale:pl|en}/emails/{id:new|k\\d+}")
  public CampaignEmailResponse email(@PathVariable String locale, @PathVariable String id) {
    SupportedLocale.fromCode(locale).orElseThrow();
    CampaignsViewService.EditorPayload payload = campaignsViewService.editor(id);
    return new CampaignEmailResponse(
        toTemplate(payload.template()),
        payload.blocks().stream().map(CampaignsController::toBlock).toList(),
        payload.variables().stream().map(CampaignsController::toVariable).toList(),
        payload.sections().stream().map(CampaignsController::toSection).toList(),
        toSelectedBlock(payload.selectedBlock()));
  }

  private static CampaignsResponse.Kpi toKpi(CampaignsViewService.Kpi kpi) {
    return new CampaignsResponse.Kpi(kpi.label(), kpi.value(), kpi.unit(), kpi.delta(), kpi.dir());
  }

  private static CampaignsResponse.Filter toFilter(CampaignsViewService.Filter filter) {
    return new CampaignsResponse.Filter(
        filter.label(), filter.count(), filter.active(), filter.action(), filter.payload());
  }

  private static CampaignsResponse.Column toColumn(CampaignsViewService.Column column) {
    return new CampaignsResponse.Column(column.label(), column.align());
  }

  private static CampaignsResponse.Row toRow(CampaignsViewService.Row row) {
    return new CampaignsResponse.Row(
        row.id(),
        row.name(),
        row.statusTone(),
        row.statusLabel(),
        row.typeLabel(),
        row.typeTone(),
        row.sent(),
        row.open(),
        row.click(),
        row.revenue());
  }

  private static CampaignEmailResponse.Template toTemplate(CampaignsViewService.Template template) {
    return new CampaignEmailResponse.Template(
        template.id(), template.name(), template.meta(), template.subject(), template.sender());
  }

  private static CampaignEmailResponse.Block toBlock(CampaignsFixtures.Block block) {
    return new CampaignEmailResponse.Block(block.icon(), block.label(), block.type());
  }

  private static CampaignEmailResponse.Variable toVariable(CampaignsFixtures.Variable variable) {
    return new CampaignEmailResponse.Variable(variable.token(), variable.description());
  }

  private static CampaignEmailResponse.Section toSection(CampaignsFixtures.Section section) {
    List<CampaignEmailResponse.Product> items =
        section.items() == null
            ? null
            : section.items().stream().map(CampaignsController::toProduct).toList();
    return new CampaignEmailResponse.Section(
        section.type(),
        section.kicker(),
        section.title(),
        section.body(),
        section.code(),
        section.note(),
        section.cta(),
        section.href(),
        items);
  }

  private static CampaignEmailResponse.Product toProduct(CampaignsFixtures.Product product) {
    return new CampaignEmailResponse.Product(product.name(), product.price());
  }

  private static CampaignEmailResponse.SelectedBlock toSelectedBlock(
      CampaignsFixtures.SelectedBlock block) {
    return new CampaignEmailResponse.SelectedBlock(
        block.blockName(),
        block.blockId(),
        block.title(),
        block.placeholders(),
        block.backgrounds(),
        block.alignment(),
        block.padding(),
        block.visibility());
  }
}
