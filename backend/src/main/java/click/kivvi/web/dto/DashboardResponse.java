package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/dashboard} response: the KPI strip, cardiogram legend, the live
 * feed's first {@code LIVE_ROWS} rows, the Mercure topic to subscribe to, recently seen customers
 * and the best-performing automations — the wire shape {@link click.kivvi.web.DashboardController}
 * builds from {@link click.kivvi.application.DashboardViewService}'s computed view-model.
 */
public record DashboardResponse(
    List<Kpi> kpis,
    List<Legend> legend,
    List<Row> events,
    String mercureTopic,
    List<Customer> recentCustomers,
    List<Automation> topAutomations) {

  public record Kpi(String label, String value, String delta, String dir, List<Double> series) {}

  public record Legend(String label, String value) {}

  /** One live-stream row; same shape {@code EventsResponse.Row} carries (DEV-3). */
  public record Row(
      String time,
      String typeIcon,
      String tone,
      String type,
      String detail,
      String customerName,
      String customerId,
      String siteName,
      String siteColor) {}

  /**
   * One "recently seen" row; {@code orders} and {@code lastSeen} are already formatted for display.
   */
  public record Customer(String id, String name, String email, int orders, String lastSeen) {}

  /**
   * One best-performing automation; {@code runs}, {@code conversion} and {@code revenue} are
   * already formatted for display.
   */
  public record Automation(
      String id,
      String name,
      List<String> channels,
      String runs,
      String conversion,
      String revenue) {}
}
