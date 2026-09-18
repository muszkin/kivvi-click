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

  /**
   * PIO-129 translates the panel one page at a time. This page's copy is still Polish only, so its
   * figures stay Polish too — a Polish label above a euro amount would be worse than either
   * language on its own. The slice that translates this page replaces the marker with the real
   * locale; {@code PanelTranslationCoverageTest} holds the remaining markers to a declared list, so
   * the last page cannot be forgotten silently.
   */
  private static final SupportedLocale UNTRANSLATED = SupportedLocale.PL;

  private static final int PAGES = 192;
  private static final int ANONYMOUS_SESSIONS = 7_632;
  private static final String PROFILE_SINCE = "14 stycznia 2024";

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

  public ListPayload list(int page, Instant now) {
    int safePage = Math.max(1, page);
    List<CustomerRow> rows = CustomersFixtures.all().stream().map(c -> toRow(c, now)).toList();
    return new ListPayload(subtitle(), segments(), rows, safePage, PAGES);
  }

  /**
   * @throws java.util.NoSuchElementException when {@code id} is not a seeded customer — mirrors
   *     {@code CustomerController::show}'s catch of {@code CustomerDirectory::byId}.
   */
  public DetailPayload detail(String id, Instant now) {
    CustomersFixtures.Customer customer = CustomersFixtures.byId(id);
    String lastSeen = lastSeen(customer, now);
    String profileSub =
        customer.email() + " · klient od " + PROFILE_SINCE + " · ostatnia aktywność " + lastSeen;
    return new DetailPayload(
        toDetail(customer),
        profileSub,
        facts(customer),
        AutomationsFixtures.activeForCustomer(UNTRANSLATED),
        tabs(customer),
        scores(),
        timeline(customer.email()));
  }

  private static CustomerRow toRow(CustomersFixtures.Customer customer, Instant now) {
    return new CustomerRow(
        customer.id(),
        customer.name(),
        customer.initials(),
        customer.email(),
        customer.segment(),
        String.valueOf(customer.orders()),
        Format.money(customer.revenue(), UNTRANSLATED),
        lastSeen(customer, now));
  }

  private static String lastSeen(CustomersFixtures.Customer customer, Instant now) {
    Instant moment = now.minusSeconds(customer.lastSeenMinutes() * 60L);
    return Format.timeAgo(moment, now, UNTRANSLATED);
  }

  private static String subtitle() {
    return Format.number(CustomersFixtures.total(), UNTRANSLATED)
        + " zidentyfikowanych klientów · "
        + Format.number(ANONYMOUS_SESSIONS, UNTRANSLATED)
        + " anonimowych sesji";
  }

  private static List<SegmentTile> segments() {
    return List.of(
        new SegmentTile(
            "Wszyscy",
            "users",
            groupWithSpace(CustomersFixtures.total()),
            true,
            "set-segment",
            "all"),
        new SegmentTile("VIP", null, groupWithSpace(142), false, "set-segment", "vip"),
        new SegmentTile("Nowi (7 dni)", null, groupWithSpace(412), false, "set-segment", "new"),
        new SegmentTile(
            "Porzucone koszyki", null, groupWithSpace(287), false, "set-segment", "abandoned"),
        new SegmentTile(
            "Subskrybenci", null, groupWithSpace(1_829), false, "set-segment", "subscribers"),
        new SegmentTile("Reaktywować", null, groupWithSpace(612), false, "set-segment", "winback"));
  }

  private static CustomerDetail toDetail(CustomersFixtures.Customer customer) {
    // The three profile tags are hard-coded in CustomerController::show, independent of the
    // customer's own list-page segment — reproduced verbatim, not derived from customer.segment().
    List<Tag> tags =
        List.of(new Tag("VIP", "accent"), new Tag("subskrybent", "brown"), new Tag("PL", null));
    return new CustomerDetail(
        customer.id(), customer.name(), customer.initials(), customer.email(), tags);
  }

  private static List<Fact> facts(CustomersFixtures.Customer customer) {
    return List.of(
        new Fact("Zamówienia", String.valueOf(customer.orders()), false),
        new Fact("Wartość życiowa", Format.money(customer.revenue() * 4.0, UNTRANSLATED), false),
        new Fact("Średnia wartość koszyka", Format.money(customer.revenue(), UNTRANSLATED), false),
        new Fact("Pierwsze zdarzenie", "14 sty 2024", false),
        new Fact("Liczba sesji", "28", false),
        new Fact("Liczba zdarzeń", "412", false),
        new Fact("customer_id", customer.id(), true));
  }

  private static List<Tab> tabs(CustomersFixtures.Customer customer) {
    return List.of(
        new Tab("Aktywność", true),
        new Tab("Zamówienia (" + customer.orders() + ")", false),
        new Tab("Wysłane maile (12)", false),
        new Tab("Otrzymane kupony (3)", false),
        new Tab("Atrybuty", false));
  }

  private static List<Score> scores() {
    return List.of(
        new Score("Wskaźnik zaangażowania", "82", "/100", "+12 vs miesiąc temu", "up", null),
        new Score("Prawd. zakupu (30d)", "68", "%", "silny sygnał", "up", "spark"),
        new Score("Open rate (90d)", "74", "%", "+6,2pp", "up", null));
  }

  private static List<TimelineEntry> timeline(String email) {
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

  /**
   * filter-chip.html.twig's own {@code {{ count|number_format(0, ',', ' ') }}}: zero decimals,
   * grouped with a plain ASCII space — a different separator from {@link Format#number} (U+202F
   * narrow no-break space), which the subtitle above uses. Both conventions exist verbatim in the
   * old stack and are reproduced verbatim here, not reconciled into one — the same duplication
   * {@code FeedsViewService.groupWithSpace} documents for the same reason.
   */
  private static String groupWithSpace(int value) {
    String digits = Integer.toString(value);
    StringBuilder grouped = new StringBuilder();
    for (int i = 0; i < digits.length(); i++) {
      if (i > 0 && (digits.length() - i) % 3 == 0) {
        grouped.append(' ');
      }
      grouped.append(digits.charAt(i));
    }
    return grouped.toString();
  }
}
