package click.kivvi.fixtures;

import click.kivvi.domain.Format;
import java.util.List;
import java.util.Map;

/**
 * The seven settings tabs and the data each one renders — ported from {@code SettingsCatalog}.
 *
 * <p>Every tab is a URL ({@code /settings/{tab}}), so a support link can point straight at "e-mail
 * providers" and the browser's back button behaves. None of this data is translated: the old
 * templates never ran the catalogue's own return values through {@code |trans} either (only the
 * surrounding card titles/labels did, which is why those live in the {@code settings.*} i18n
 * catalogue instead of here) — reproduced as hard-coded Polish fixture text, exactly like {@code
 * CustomersFixtures} does for customer rows.
 */
public final class SettingsFixtures {

  public static final String DEFAULT_TAB = "account";

  public record Tab(String id, String icon, String label) {}

  public record TrackedSite(String name, String color, String events, String key) {}

  public record TeamMember(
      String name,
      String email,
      String role,
      String roleTone,
      String last,
      boolean invited,
      boolean mfa) {}

  public record Role(String name, String description, int count) {}

  public record EmailProvider(
      String name,
      String region,
      String statusTone,
      String statusLabel,
      String sent,
      String bounce,
      boolean bounceWarn,
      String complaint,
      boolean verified) {}

  public record DnsRecord(String record, String value, boolean ok) {}

  public record ApiKey(
      String name, String prefix, String created, String last, List<String> scopes) {}

  public record Webhook(String url, List<String> events, int code, String last) {}

  public record Bar(String label, double pct, String value, String tone) {}

  public record NotificationRow(String label, boolean email, boolean slack, boolean sms) {}

  public record DataSubjectRequest(
      String id, String person, String type, String status, boolean done, String due) {}

  public record RetentionPolicy(String label, List<String> options, String selected) {}

  /**
   * The verbatim source of the tracker snippet, escaped and highlighted client-side by {@code
   * highlight.ts} — never here: highlighting is a presentation concern the SPA owns.
   */
  public static final String TRACKER_SNIPPET =
      """
      <!-- Kivvi-click tracker · ~2KB, no deps -->
      <script async
        src="https://cdn.kivvi-click.io/k.js"
        data-site="aureashop.pl"
        data-key="pk_live_8a4f2c..."></script>

      // then anywhere in your shop code:
      window.kvi('purchase', {
        order_id: 'AR-1287',
        value:     412.00,
        currency:  'PLN',
        items: [
          { sku: 'TEA-SENCHA-100', qty: 1, price: 38.90 },
          { sku: 'CUP-NORA',       qty: 2, price: 50.00 },
        ],
      });""";

  private static final List<Tab> TABS =
      List.of(
          new Tab("account", "user", "Konto"),
          new Tab("sites", "globe", "Śledzone strony"),
          new Tab("team", "users", "Zespół"),
          new Tab("providers", "mail", "Dostawcy email"),
          new Tab("api", "code", "Webhooks i API"),
          new Tab("notifications", "bell", "Powiadomienia"),
          new Tab("gdpr", "info", "RODO / DPA"));

  private static final Map<String, String> SUBTITLES =
      Map.ofEntries(
          Map.entry("account", "Dane firmy i preferencje właściciela konta."),
          Map.entry("sites", "Domeny objęte trackingiem oraz instalacja skryptu."),
          Map.entry("team", "Osoby z dostępem do panelu, ich role i zaproszenia."),
          Map.entry(
              "providers", "Skąd wychodzą Twoje e-maile i jak radzą sobie z dostarczalnością."),
          Map.entry("api", "Klucze API, webhooks i logi wywołań."),
          Map.entry("notifications", "Kiedy Kivvi ma Cię powiadomić i którym kanałem."),
          Map.entry("gdpr", "Retencja danych, umowa powierzenia i obsługa żądań podmiotów."));

  private static final List<TrackedSite> TRACKED_SITES =
      List.of(
          new TrackedSite("aureashop.pl", "#7a8763", Format.number(28410), "pk_live_8a4f2c…"),
          new TrackedSite("mlot-narzedzia.pl", "#a3825b", Format.number(14820), "pk_live_3c91b7…"),
          new TrackedSite("polna-bistro.pl", "#8b6f53", Format.number(12240), "pk_live_be22a0…"));

  private static final List<String> AUTOMATIC_EVENTS =
      List.of(
          "pageview",
          "login",
          "signup",
          "search",
          "add_to_cart",
          "remove_from_cart",
          "wishlist",
          "cart_abandon");

  private static final List<TeamMember> TEAM =
      List.of(
          new TeamMember(
              "Maciej Kowalczyk",
              "maciej@aureashop.pl",
              "Właściciel",
              "accent",
              "teraz",
              false,
              true),
          new TeamMember(
              "Anna Bartosz",
              "anna@aureashop.pl",
              "Administrator",
              "brown",
              "2 godz. temu",
              false,
              true),
          new TeamMember(
              "Piotr Sobczak",
              "piotr@aureashop.pl",
              "Marketer",
              "neutral",
              "wczoraj",
              false,
              false),
          new TeamMember(
              "Iga Nowak", "iga@agencja-lumo.pl", "Marketer", "neutral", "4 dni temu", false, true),
          new TeamMember("—", "kamil@aureashop.pl", "Analityk", "neutral", "—", true, false));

  private static final List<Role> ROLES =
      List.of(
          new Role("Właściciel", "Pełny dostęp, rozliczenia, usuwanie konta.", 1),
          new Role("Administrator", "Wszystko poza rozliczeniami i usuwaniem konta.", 1),
          new Role("Marketer", "Automatyzacje, kampanie, popupy, segmenty. Bez ustawień.", 2),
          new Role("Analityk", "Tylko podglądanie danych i eksport raportów.", 1));

  private static final List<EmailProvider> EMAIL_PROVIDERS =
      List.of(
          new EmailProvider(
              "Amazon SES",
              "eu-central-1",
              "good",
              "Główny",
              Format.number(118420),
              "0,24%",
              false,
              "0,01%",
              true),
          new EmailProvider(
              "SendGrid",
              "EU",
              "info",
              "Zapasowy",
              Format.number(23990),
              "0,41%",
              true,
              "0,03%",
              true),
          new EmailProvider(
              "SMTP własny (poczta.aureashop.pl)",
              "—",
              "neutral",
              "Wyłączony",
              "0",
              "0%",
              false,
              "0%",
              false));

  private static final List<DnsRecord> DNS_RECORDS =
      List.of(
          new DnsRecord("SPF", "v=spf1 include:amazonses.com ~all", true),
          new DnsRecord("DKIM", "kivvi1._domainkey.aureashop.pl", true),
          new DnsRecord("DMARC", "v=DMARC1; p=quarantine; rua=mailto:dmarc@aureashop.pl", true),
          new DnsRecord("BIMI", "nie skonfigurowane — logo marki nie pojawi się w Gmailu", false));

  private static final List<ApiKey> API_KEYS =
      List.of(
          new ApiKey(
              "Produkcja — backend",
              "sk_live_8a4f2c",
              "14 sty 2024",
              "3 min temu",
              List.of("events:write", "customers:read")),
          new ApiKey(
              "Tracker publiczny",
              "pk_live_8a4f2c",
              "14 sty 2024",
              "teraz",
              List.of("events:write")),
          new ApiKey("Staging", "sk_test_1b09de", "02 mar 2026", "wczoraj", List.of("*")));

  private static final List<Webhook> WEBHOOKS =
      List.of(
          new Webhook(
              "https://aureashop.pl/hooks/kivvi",
              List.of("purchase", "cart_abandon"),
              200,
              "2 min temu"),
          new Webhook(
              "https://erp.aureashop.pl/api/kivvi",
              List.of("customer.created"),
              200,
              "11 min temu"),
          new Webhook(
              "https://hooks.slack.com/services/T0…",
              List.of("automation.failed"),
              410,
              "3 godz. temu"));

  private static final List<Bar> API_LIMITS =
      List.of(
          new Bar("Ingest zdarzeń (na sek.)", 42.0, "42 / 100", "accent"),
          new Bar("REST API (na min.)", 18.0, "216 / 1200", "accent"),
          new Bar("Eksporty (na godz.)", 60.0, "6 / 10", "brown"));

  private static final List<NotificationRow> NOTIFICATION_MATRIX =
      List.of(
          new NotificationRow("Automatyzacja przestała działać", true, true, true),
          new NotificationRow("Feed produktów zwrócił błąd", true, true, false),
          new NotificationRow("Bounce rate przekroczył próg", true, true, true),
          new NotificationRow("Limit wysyłek na wyczerpaniu (80%)", true, false, false),
          new NotificationRow("Import klientów zakończony", true, false, false),
          new NotificationRow("Nowa osoba dołączyła do zespołu", true, false, false),
          new NotificationRow("Tygodniowe podsumowanie wyników", true, false, false),
          new NotificationRow("Tracker przestał odbierać zdarzenia", true, true, true));

  private static final List<DataSubjectRequest> DATA_SUBJECT_REQUESTS =
      List.of(
          new DataSubjectRequest(
              "DSR-0142",
              "hania.k@aurea.pl",
              "Dostęp do danych",
              "zakończone",
              true,
              "22 sie 2026"),
          new DataSubjectRequest(
              "DSR-0141",
              "marek.p@example.com",
              "Usunięcie danych",
              "w toku",
              false,
              "25 sie 2026"),
          new DataSubjectRequest(
              "DSR-0139",
              "iga.n@example.pl",
              "Sprzeciw wobec profilowania",
              "zakończone",
              true,
              "18 sie 2026"));

  private static final List<RetentionPolicy> RETENTION_POLICIES =
      List.of(
          new RetentionPolicy(
              "Surowe zdarzenia", List.of("90 dni", "13 miesięcy", "24 miesiące"), "13 miesięcy"),
          new RetentionPolicy(
              "Profile klientów bez aktywności",
              List.of("24 miesiące", "36 miesięcy", "bez limitu"),
              "24 miesiące"),
          new RetentionPolicy(
              "Logi wysyłek e-mail", List.of("12 miesięcy", "24 miesiące"), "12 miesięcy"),
          new RetentionPolicy("Logi webhooków", List.of("30 dni", "90 dni"), "30 dni"));

  private SettingsFixtures() {}

  public static List<Tab> tabs() {
    return TABS;
  }

  public static boolean isKnownTab(String tab) {
    return SUBTITLES.containsKey(tab);
  }

  public static String subtitle(String tab) {
    return SUBTITLES.getOrDefault(tab, "");
  }

  public static List<TrackedSite> trackedSites() {
    return TRACKED_SITES;
  }

  public static List<String> automaticEvents() {
    return AUTOMATIC_EVENTS;
  }

  public static List<TeamMember> team() {
    return TEAM;
  }

  public static List<Role> roles() {
    return ROLES;
  }

  public static List<EmailProvider> emailProviders() {
    return EMAIL_PROVIDERS;
  }

  public static List<DnsRecord> dnsRecords() {
    return DNS_RECORDS;
  }

  public static List<ApiKey> apiKeys() {
    return API_KEYS;
  }

  public static List<Webhook> webhooks() {
    return WEBHOOKS;
  }

  public static List<Bar> apiLimits() {
    return API_LIMITS;
  }

  public static List<NotificationRow> notificationMatrix() {
    return NOTIFICATION_MATRIX;
  }

  public static List<DataSubjectRequest> dataSubjectRequests() {
    return DATA_SUBJECT_REQUESTS;
  }

  public static List<RetentionPolicy> retentionPolicies() {
    return RETENTION_POLICIES;
  }
}
