package click.kivvi.application;

import click.kivvi.domain.Format;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.tracking.EventStreamTopic;
import click.kivvi.domain.tracking.EventType;
import click.kivvi.domain.tracking.TrackedSite;
import click.kivvi.fixtures.EventsFixtures;
import click.kivvi.fixtures.EventsFixtures.SampleCustomer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Assembles the event stream page's view-model — ported from {@code EventStreamController} and
 * {@code EventFeed}. The type/site/range query parameters only ever mark a filter chip active:
 * {@code EventFeed::rows()} generates the same 30 sample rows regardless of them (confirmed against
 * the oracle: journey step 7, filtered by {@code type=purchase&site=aurea&range=24h}, renders
 * byte-identical rows to step 1's unfiltered view — the old stack's tracking script feeds this feed
 * for real in production, so the sample generator itself was never wired to filter).
 */
@Service
public class EventsViewService {

  private static final int ROWS_PER_PAGE = 30;
  private static final int EVENTS_IN_WINDOW = 9_360;
  private static final int SECONDS_BETWEEN_EVENTS = 12;
  private static final DateTimeFormatter TIME_OF_DAY = DateTimeFormatter.ofPattern("HH:mm:ss");

  public record FilterView(
      String label, String icon, String dotColor, boolean active, String action, String payload) {}

  public record RangeView(String value, String label, boolean active) {}

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

  public record Payload(
      List<FilterView> typeFilters,
      List<FilterView> siteFilters,
      List<RangeView> ranges,
      List<RowView> events,
      String total,
      int shown,
      String mercureTopic) {}

  public Payload build(
      SupportedLocale locale, String typeParam, String siteParam, String rangeParam) {
    String activeType = orDefault(typeParam, "all");
    String activeSite = orDefault(siteParam, "all");
    String activeRange = orDefault(rangeParam, "1h");

    return new Payload(
        typeFilters(locale, activeType),
        siteFilters(locale, activeSite),
        ranges(locale, activeRange),
        rows(locale, ROWS_PER_PAGE, Instant.now()),
        Format.number(EVENTS_IN_WINDOW, locale),
        ROWS_PER_PAGE,
        EventStreamTopic.forCurrentAccount());
  }

  private List<FilterView> typeFilters(SupportedLocale locale, String active) {
    List<FilterView> filters = new ArrayList<>();
    filters.add(
        new FilterView(
            allLabel(locale), null, null, "all".equals(active), "set-event-type", "all"));
    for (EventType type : EventType.FEED_TYPES) {
      filters.add(
          new FilterView(
              type.label(locale),
              type.icon(),
              null,
              type.code().equals(active),
              "set-event-type",
              type.code()));
    }
    return filters;
  }

  private List<FilterView> siteFilters(SupportedLocale locale, String active) {
    List<FilterView> filters = new ArrayList<>();
    filters.add(
        new FilterView(
            allLabel(locale), "globe", null, "all".equals(active), "set-event-site", "all"));
    for (TrackedSite site : TrackedSite.all()) {
      filters.add(
          new FilterView(
              site.siteName(),
              null,
              site.color(),
              site.id().equals(active),
              "set-event-site",
              site.id()));
    }
    return filters;
  }

  private List<RangeView> ranges(SupportedLocale locale, String active) {
    return switch (locale) {
      case PL ->
          List.of(
              new RangeView("5m", "5 min", "5m".equals(active)),
              new RangeView("1h", "1 godz.", "1h".equals(active)),
              new RangeView("24h", "24 godz.", "24h".equals(active)),
              new RangeView("7d", "7 dni", "7d".equals(active)));
      case EN ->
          List.of(
              new RangeView("5m", "5 min", "5m".equals(active)),
              new RangeView("1h", "1 hr", "1h".equals(active)),
              new RangeView("24h", "24 hr", "24h".equals(active)),
              new RangeView("7d", "7 days", "7d".equals(active)));
    };
  }

  private List<RowView> rows(SupportedLocale locale, int count, Instant now) {
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
  private String detail(SupportedLocale locale, EventType type, int seed, String email) {
    return switch (type) {
      case PAGEVIEW -> EventDetail.pagePath(locale, seed);
      case ADD_TO_CART, WISHLIST -> EventDetail.productName(locale, seed);
      case PURCHASE -> EventDetail.purchase(locale, seed);
      case SEARCH -> EventDetail.searchPhrase(locale, seed);
      case CART_ABANDON -> EventDetail.abandonDelay(locale, seed);
      default -> email;
    };
  }

  private String allLabel(SupportedLocale locale) {
    return locale == SupportedLocale.EN ? "All" : "Wszystkie";
  }

  private static String orDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
