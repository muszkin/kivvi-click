package click.kivvi.application;

import click.kivvi.domain.Format;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.AutomationsFixtures;
import click.kivvi.fixtures.CustomersFixtures;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Assembles the customers index and 360-profile view-models — mirrors what {@code
 * CustomerController} rendered from {@code CustomerDirectory} and {@code AutomationCatalog}. Every
 * "ago" string is computed against the request clock, exactly like {@code
 * Format::timeAgo($now->modify(...), $now)} did — the SPA never formats a number or a relative time
 * itself.
 */
@Service
public class CustomersViewService {

  private static final int PAGES = 192;
  private static final int ANONYMOUS_SESSIONS = 7_632;
  private static final String PROFILE_SINCE_PL = "14 stycznia 2024";
  private static final String PROFILE_SINCE_EN = "14 January 2024";

  /** One segment-rail tile. {@code icon} is {@code null} for every tile but "Wszyscy". */
  public record SegmentTile(
      String label, String icon, String count, boolean active, String action, String payload) {}

  /** One customers-index row, every number already formatted for display. */
  public record CustomerRow(
      String id,
      String name,
      String initials,
      String email,
      CustomersFixtures.Segment segment,
      String orders,
      String revenue,
      String lastSeen) {}

  public record ListPayload(
      String subtitle, List<SegmentTile> segments, List<CustomerRow> rows, int page, int pages) {}

  public record Tag(String label, String tone) {}

  /** The 360 profile's header identity: the same fields the index row carries, plus its tags. */
  public record CustomerDetail(
      String id, String name, String initials, String email, List<Tag> tags) {}

  public record Fact(String label, String value, boolean small) {}

  public record Tab(String label, boolean active) {}

  public record Score(
      String label, String value, String unit, String delta, String dir, String deltaIcon) {}

  public record TimelineEntry(String time, String title, String detail, String icon) {}

  public record DetailPayload(
      CustomerDetail customer,
      String profileSub,
      List<Fact> facts,
      List<AutomationsFixtures.ActiveAutomation> automations,
      List<Tab> tabs,
      List<Score> scores,
      List<TimelineEntry> timeline) {}

  public ListPayload list(SupportedLocale locale, int page, Instant now) {
    int safePage = Math.max(1, page);
    List<CustomerRow> rows =
        CustomersFixtures.all(locale).stream().map(c -> toRow(locale, c, now)).toList();
    return new ListPayload(subtitle(locale), segments(locale), rows, safePage, PAGES);
  }

  /**
   * @throws java.util.NoSuchElementException when {@code id} is not a seeded customer — mirrors
   *     {@code CustomerController::show}'s catch of {@code CustomerDirectory::byId}.
   */
  public DetailPayload detail(SupportedLocale locale, String id, Instant now) {
    CustomersFixtures.Customer customer = CustomersFixtures.byId(locale, id);
    String lastSeen = lastSeen(locale, customer, now);
    String profileSub =
        switch (locale) {
          case PL ->
              customer.email()
                  + " · klient od "
                  + PROFILE_SINCE_PL
                  + " · ostatnia aktywność "
                  + lastSeen;
          case EN ->
              customer.email()
                  + " · customer since "
                  + PROFILE_SINCE_EN
                  + " · last seen "
                  + lastSeen;
        };
    return new DetailPayload(
        toDetail(locale, customer),
        profileSub,
        facts(locale, customer),
        AutomationsFixtures.activeForCustomer(locale),
        tabs(locale, customer),
        scores(locale),
        timeline(locale, customer.email()));
  }

  private static CustomerRow toRow(
      SupportedLocale locale, CustomersFixtures.Customer customer, Instant now) {
    return new CustomerRow(
        customer.id(),
        customer.name(),
        customer.initials(),
        customer.email(),
        customer.segment(),
        String.valueOf(customer.orders()),
        Format.money(customer.revenue(), locale),
        lastSeen(locale, customer, now));
  }

  private static String lastSeen(
      SupportedLocale locale, CustomersFixtures.Customer customer, Instant now) {
    Instant moment = now.minusSeconds(customer.lastSeenMinutes() * 60L);
    return Format.timeAgo(moment, now, locale);
  }

  private static String subtitle(SupportedLocale locale) {
    String identified = Format.number(CustomersFixtures.total(), locale);
    String anonymous = Format.number(ANONYMOUS_SESSIONS, locale);
    return switch (locale) {
      case PL -> identified + " zidentyfikowanych klientów · " + anonymous + " anonimowych sesji";
      case EN -> identified + " identified customers · " + anonymous + " anonymous sessions";
    };
  }

  private static List<SegmentTile> segments(SupportedLocale locale) {
    String[] labels =
        switch (locale) {
          case PL ->
              new String[] {
                "Wszyscy", "VIP", "Nowi (7 dni)", "Porzucone koszyki", "Subskrybenci", "Reaktywować"
              };
          case EN ->
              new String[] {
                "Everyone", "VIP", "New (7 days)", "Abandoned baskets", "Subscribers", "Win back"
              };
        };
    return List.of(
        new SegmentTile(
            labels[0],
            "users",
            grouped(CustomersFixtures.total(), locale),
            true,
            "set-segment",
            "all"),
        new SegmentTile(labels[1], null, grouped(142, locale), false, "set-segment", "vip"),
        new SegmentTile(labels[2], null, grouped(412, locale), false, "set-segment", "new"),
        new SegmentTile(labels[3], null, grouped(287, locale), false, "set-segment", "abandoned"),
        new SegmentTile(
            labels[4], null, grouped(1_829, locale), false, "set-segment", "subscribers"),
        new SegmentTile(labels[5], null, grouped(612, locale), false, "set-segment", "winback"));
  }

  private static CustomerDetail toDetail(
      SupportedLocale locale, CustomersFixtures.Customer customer) {
    // The three profile tags are hard-coded in CustomerController::show, independent of the
    // customer's own list-page segment — reproduced verbatim, not derived from customer.segment().
    String subscriber =
        switch (locale) {
          case PL -> "subskrybent";
          case EN -> "subscriber";
        };
    List<Tag> tags =
        List.of(new Tag("VIP", "accent"), new Tag(subscriber, "brown"), new Tag("PL", null));
    return new CustomerDetail(
        customer.id(), customer.name(), customer.initials(), customer.email(), tags);
  }

  private static List<Fact> facts(SupportedLocale locale, CustomersFixtures.Customer customer) {
    String[] labels =
        switch (locale) {
          case PL ->
              new String[] {
                "Zamówienia",
                "Wartość życiowa",
                "Średnia wartość koszyka",
                "Pierwsze zdarzenie",
                "14 sty 2024",
                "Liczba sesji",
                "Liczba zdarzeń"
              };
          case EN ->
              new String[] {
                "Orders",
                "Lifetime value",
                "Average basket value",
                "First event",
                "14 Jan 2024",
                "Sessions",
                "Events"
              };
        };
    return List.of(
        new Fact(labels[0], String.valueOf(customer.orders()), false),
        new Fact(labels[1], Format.money(customer.revenue() * 4.0, locale), false),
        new Fact(labels[2], Format.money(customer.revenue(), locale), false),
        new Fact(labels[3], labels[4], false),
        new Fact(labels[5], "28", false),
        new Fact(labels[6], "412", false),
        new Fact("customer_id", customer.id(), true));
  }

  private static List<Tab> tabs(SupportedLocale locale, CustomersFixtures.Customer customer) {
    return switch (locale) {
      case PL ->
          List.of(
              new Tab("Aktywność", true),
              new Tab("Zamówienia (" + customer.orders() + ")", false),
              new Tab("Wysłane maile (12)", false),
              new Tab("Otrzymane kupony (3)", false),
              new Tab("Atrybuty", false));
      case EN ->
          List.of(
              new Tab("Activity", true),
              new Tab("Orders (" + customer.orders() + ")", false),
              new Tab("Emails sent (12)", false),
              new Tab("Coupons received (3)", false),
              new Tab("Attributes", false));
    };
  }

  private static List<Score> scores(SupportedLocale locale) {
    return switch (locale) {
      case PL ->
          List.of(
              new Score("Wskaźnik zaangażowania", "82", "/100", "+12 vs miesiąc temu", "up", null),
              new Score("Prawd. zakupu (30d)", "68", "%", "silny sygnał", "up", "spark"),
              new Score("Open rate (90d)", "74", "%", "+6,2pp", "up", null));
      case EN ->
          List.of(
              new Score("Engagement score", "82", "/100", "+12 vs last month", "up", null),
              new Score("Purchase likelihood (30d)", "68", "%", "a strong signal", "up", "spark"),
              new Score("Open rate (90d)", "74", "%", "+6.2pp", "up", null));
    };
  }

  private static List<TimelineEntry> timeline(SupportedLocale locale, String email) {
    return switch (locale) {
      case PL -> polishTimeline(email);
      case EN -> englishTimeline(email);
    };
  }

  private static List<TimelineEntry> polishTimeline(String email) {
    return List.of(
        new TimelineEntry(
            "Dziś · 14:42", "Wyświetlenie produktu", "/produkt/zielona-herbata-sencha", "eye"),
        new TimelineEntry(
            "Dziś · 14:39",
            "Dodanie do koszyka",
            "Zielona herbata Sencha 100g · 38,90 PLN",
            "cart"),
        new TimelineEntry(
            "Dziś · 14:38", "Wyświetlenie produktu", "/produkt/swieca-soja-figa", "eye"),
        new TimelineEntry("Dziś · 14:32", "Wyszukiwanie", "„herbata zielona organiczna”", "search"),
        new TimelineEntry("Dziś · 14:30", "Zalogowanie", email, "user"),
        new TimelineEntry(
            "Wczoraj · 18:14",
            "Otrzymanie emaila",
            "Newsletter „Tydzień smaków #18” — otwarty po 12 min",
            "mail"),
        new TimelineEntry(
            "Wczoraj · 17:09",
            "Porzucenie koszyka",
            "2 produkty · 88,90 PLN · uruchomiona reguła „Powrót do koszyka”",
            "cart"),
        new TimelineEntry("Wczoraj · 16:42", "Wyświetlenie strony", "/kolekcja/zima-2025", "eye"),
        new TimelineEntry(
            "14 mar · 11:08", "Zakup", "4 produkty · 412,00 PLN · zamówienie #AR-1287", "money"));
  }

  private static List<TimelineEntry> englishTimeline(String email) {
    return List.of(
        new TimelineEntry("Today · 14:42", "Product viewed", "/product/sencha-green-tea", "eye"),
        new TimelineEntry(
            "Today · 14:39", "Added to basket", "Sencha green tea 100g · €38.90", "cart"),
        new TimelineEntry("Today · 14:38", "Product viewed", "/product/soy-candle-fig", "eye"),
        new TimelineEntry("Today · 14:32", "Search", "“organic green tea”", "search"),
        new TimelineEntry("Today · 14:30", "Signed in", email, "user"),
        new TimelineEntry(
            "Yesterday · 18:14",
            "Email received",
            "Newsletter “Taste Week #18” — opened after 12 min",
            "mail"),
        new TimelineEntry(
            "Yesterday · 17:09",
            "Basket abandoned",
            "2 items · €88.90 · the “Back to the basket” rule fired",
            "cart"),
        new TimelineEntry("Yesterday · 16:42", "Page viewed", "/collection/winter-2025", "eye"),
        new TimelineEntry(
            "14 Mar · 11:08", "Purchase", "4 items · €412.00 · order #AR-1287", "money"));
  }

  /**
   * filter-chip.html.twig's own {@code {{ count|number_format(0, ',', ' ') }}}: zero decimals,
   * grouped with a plain ASCII space — a different separator from {@link Format#number} (U+202F
   * narrow no-break space), which the subtitle above uses. Both conventions exist verbatim in the
   * old stack and are reproduced verbatim here, not reconciled into one — the same duplication
   * {@code FeedsViewService.groupWithSpace} documents for the same reason.
   */
  private static String grouped(int value, SupportedLocale locale) {
    char separator =
        switch (locale) {
          case PL -> ' ';
          case EN -> ',';
        };
    String digits = Integer.toString(value);
    StringBuilder grouped = new StringBuilder();
    for (int i = 0; i < digits.length(); i++) {
      if (i > 0 && (digits.length() - i) % 3 == 0) {
        grouped.append(separator);
      }
      grouped.append(digits.charAt(i));
    }
    return grouped.toString();
  }
}
