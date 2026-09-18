package click.kivvi.application;

import click.kivvi.domain.Format;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.tracking.EventStreamTopic;
import click.kivvi.domain.tracking.EventType;
import click.kivvi.domain.tracking.TrackedSite;
import click.kivvi.fixtures.AutomationsFixtures;
import click.kivvi.fixtures.CustomersFixtures;
import click.kivvi.fixtures.DashboardFixtures;
import click.kivvi.fixtures.EventsFixtures;
import click.kivvi.fixtures.EventsFixtures.SampleCustomer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Assembles the dashboard's view-model — ported from {@code DashboardController} and {@code
 * DashboardMetrics}. Reuses {@link DashboardFixtures} for the KPI/legend/range copy, {@link
 * EventsFixtures} for the live-feed sample data, {@link CustomersFixtures} for "recently seen" and
 * {@link AutomationsFixtures} for the best-performing automations — the same fixtures {@code
 * EventFeed}, {@code CustomerDirectory} and {@code AutomationCatalog} were on the old stack.
 *
 * <p>{@link #liveRows} intentionally re-implements {@code EventFeed::rows()}'s exact loop (also
 * ported once already, into {@link EventsViewService#build}) rather than depending on that service:
 * mirrors {@link EventsFixtures}'s own class comment on why it reproduces {@code
 * CustomerDirectory::all()} instead of importing a shared directory — every {@code *ViewService} in
 * this codebase reads only fixtures, never a sibling view service, so a page's data never depends
 * on another page's assembly staying stable. {@code DashboardController} on the old stack took the
 * same shape: {@code EventFeed} was injected directly into it, not reached through {@code
 * EventStreamController}.
 */
@Service
public class DashboardViewService {

  /**
   * PIO-129: the dashboard also shows two lists whose words belong to other pages — the
   * best-performing automations and the recently-seen customers. Those pages are still Polish-only,
   * so their figures stay Polish too rather than pairing a Polish automation name with a euro
   * amount. Each slice that translates a page replaces its own marker with the real locale; the
   * guard test in {@code PanelTranslationCoverageTest} holds the remaining markers to a declared
   * list, so the last one cannot be forgotten silently.
   */
  private static final SupportedLocale UNTRANSLATED = SupportedLocale.PL;

  private static final int LIVE_ROWS = 10;
  private static final int RECENT_CUSTOMERS = 6;
  private static final int TOP_AUTOMATIONS = 4;
  private static final int SECONDS_BETWEEN_EVENTS = 12;
  private static final int SERIES_LENGTH = 40;
  private static final DateTimeFormatter TIME_OF_DAY = DateTimeFormatter.ofPattern("HH:mm:ss");

  public record KpiView(
      String label, String value, String delta, String dir, List<Double> series) {}

  public record LegendView(String label, String value) {}

  /** One live-stream row — same shape {@code EventsViewService.RowView} carries (DEV-3). */
  public record RowView(
      String time,
      String typeIcon,
      String tone,
      String type,
      String detail,
      String customerName,
      String customerId,
      String siteName,
      String siteColor) {}

  public record CustomerView(String id, String name, String email, int orders, String lastSeen) {}

  public record AutomationView(
      String id,
      String name,
      List<String> channels,
      String runs,
      String conversion,
      String revenue) {}

  public record Payload(
      List<KpiView> kpis,
      List<LegendView> legend,
      List<RowView> events,
      String mercureTopic,
      List<CustomerView> recentCustomers,
      List<AutomationView> topAutomations) {}

  public Payload build(SupportedLocale locale, Instant now) {
    return new Payload(
        DashboardFixtures.kpis(locale).stream().map(DashboardViewService::toKpiView).toList(),
        DashboardFixtures.legend(locale).stream()
            .map(seed -> new LegendView(seed.label(), seed.value()))
            .toList(),
        liveRows(locale, LIVE_ROWS, now),
        EventStreamTopic.forCurrentAccount(),
        recentCustomers(now),
        topAutomations());
  }

  private static KpiView toKpiView(DashboardFixtures.KpiSeed seed) {
    return new KpiView(
        seed.label(),
        seed.value(),
        seed.delta(),
        seed.dir(),
        series(seed.seriesBase(), seed.seriesVariance()));
  }

  /**
   * {@code DashboardMetrics::series()}, ported call for call: a deterministic wave so the sparkline
   * never changes shape between renders.
   */
  private static List<Double> series(double base, double variance) {
    List<Double> values = new ArrayList<>(SERIES_LENGTH);
    for (int i = 0; i < SERIES_LENGTH; i++) {
      double wave = Math.sin(i / 3.1 + base) + 0.5 * Math.sin(i / 1.7 + variance * 10);
      values.add(round2(base * (1 + variance * wave / 2)));
    }
    return values;
  }

  private static double round2(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  private List<CustomerView> recentCustomers(Instant now) {
    return CustomersFixtures.all().stream()
        .limit(RECENT_CUSTOMERS)
        .map(customer -> toCustomerView(customer, now))
        .toList();
  }

  private static CustomerView toCustomerView(CustomersFixtures.Customer customer, Instant now) {
    Instant lastSeenAt = now.minusSeconds(customer.lastSeenMinutes() * 60L);
    return new CustomerView(
        customer.id(),
        customer.name(),
        customer.email(),
        customer.orders(),
        Format.timeAgo(lastSeenAt, now, UNTRANSLATED));
  }

  /** {@code AutomationCatalog::topEarning()}: active automations only, highest revenue first. */
  private List<AutomationView> topAutomations() {
    return AutomationsFixtures.all().stream()
        .filter(automation -> "active".equals(automation.status()))
        .sorted(Comparator.comparingDouble(AutomationsFixtures.Automation::revenue).reversed())
        .limit(TOP_AUTOMATIONS)
        .map(DashboardViewService::toAutomationView)
        .toList();
  }

  private static AutomationView toAutomationView(AutomationsFixtures.Automation automation) {
    return new AutomationView(
        automation.id(),
        automation.name(),
        automation.channels(),
        Format.number(automation.runs(), UNTRANSLATED),
        Format.percent(automation.conversion(), UNTRANSLATED),
        Format.money(automation.revenue(), UNTRANSLATED));
  }

  /** {@code EventFeed::rows()}, ported call for call, limited to {@link #LIVE_ROWS} rows. */
  private List<RowView> liveRows(SupportedLocale locale, int count, Instant now) {
    List<SampleCustomer> customers = EventsFixtures.customers(locale);
    List<RowView> rows = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      EventType type = EventType.FEED_TYPES.get((i * 3 + 1) % EventType.FEED_TYPES.size());
      SampleCustomer customer = customers.get((i * 5) % customers.size());
      TrackedSite site = TrackedSite.byIndex(i * 2);
      Instant moment = now.minusSeconds((long) i * SECONDS_BETWEEN_EVENTS);

      rows.add(
          new RowView(
              TIME_OF_DAY.format(moment.atZone(ZoneId.systemDefault())),
              type.icon(),
              type.tone(),
              type.label(locale),
              detail(locale, type, i, customer.email()),
              customer.name(),
              customer.id(),
              site.siteName(),
              site.color()));
    }
    return rows;
  }

  /** {@code EventFeed::detail()}, ported case for case, in the language the feed is read in. */
  private static String detail(SupportedLocale locale, EventType type, int seed, String email) {
    return switch (type) {
      case PAGEVIEW -> EventDetail.pagePath(locale, seed);
      case ADD_TO_CART, WISHLIST -> EventDetail.productName(locale, seed);
      case PURCHASE -> EventDetail.purchase(locale, seed);
      case SEARCH -> EventDetail.searchPhrase(locale, seed);
      case CART_ABANDON -> EventDetail.abandonDelay(locale, seed);
      default -> email;
    };
  }
}
