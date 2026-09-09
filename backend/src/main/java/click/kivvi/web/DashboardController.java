package click.kivvi.web;

import click.kivvi.application.DashboardViewService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.web.dto.DashboardResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The dashboard's data: KPIs, cardiogram legend, the live feed's first page, recently seen
 * customers and the best-performing automations — mirrors {@code DashboardController}.
 */
@RestController
public class DashboardController {

  private final DashboardViewService dashboardViewService;

  public DashboardController(DashboardViewService dashboardViewService) {
    this.dashboardViewService = dashboardViewService;
  }

  @GetMapping("/api/v1/{locale:pl|en}/dashboard")
  public DashboardResponse dashboard(@PathVariable String locale) {
    SupportedLocale resolvedLocale = SupportedLocale.fromCode(locale).orElseThrow();
    DashboardViewService.Payload payload =
        dashboardViewService.build(resolvedLocale, Instant.now());
    return new DashboardResponse(
        payload.kpis().stream().map(DashboardController::toKpi).toList(),
        payload.legend().stream().map(DashboardController::toLegend).toList(),
        payload.events().stream().map(DashboardController::toRow).toList(),
        payload.mercureTopic(),
        payload.recentCustomers().stream().map(DashboardController::toCustomer).toList(),
        payload.topAutomations().stream().map(DashboardController::toAutomation).toList());
  }

  private static DashboardResponse.Kpi toKpi(DashboardViewService.KpiView kpi) {
    return new DashboardResponse.Kpi(
        kpi.label(), kpi.value(), kpi.delta(), kpi.dir(), kpi.series());
  }

  private static DashboardResponse.Legend toLegend(DashboardViewService.LegendView legend) {
    return new DashboardResponse.Legend(legend.label(), legend.value());
  }

  private static DashboardResponse.Row toRow(DashboardViewService.RowView row) {
    return new DashboardResponse.Row(
        row.time(),
        row.typeIcon(),
        row.tone(),
        row.type(),
        row.detail(),
        row.customerName(),
        row.customerId(),
        row.siteName(),
        row.siteColor());
  }

  private static DashboardResponse.Customer toCustomer(DashboardViewService.CustomerView customer) {
    return new DashboardResponse.Customer(
        customer.id(), customer.name(), customer.email(), customer.orders(), customer.lastSeen());
  }

  private static DashboardResponse.Automation toAutomation(
      DashboardViewService.AutomationView automation) {
    return new DashboardResponse.Automation(
        automation.id(),
        automation.name(),
        automation.channels(),
        automation.runs(),
        automation.conversion(),
        automation.revenue());
  }
}
