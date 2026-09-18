package click.kivvi.fixtures;

import click.kivvi.domain.Format;
import click.kivvi.domain.SupportedLocale;
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

  private static final int EVENTS_AUREASHOP = 28_410;
  private static final int EVENTS_MLOT = 14_820;
  private static final int EVENTS_POLNA = 12_240;
  private static final int SES_SENT = 118_420;
  private static final int SENDGRID_SENT = 23_990;

  private static final List<Tab> TABS_PL =
      List.of(
          new Tab("account", "user", "Konto"),
          new Tab("sites", "globe", "Śledzone strony"),
          new Tab("team", "users", "Zespół"),
          new Tab("providers", "mail", "Dostawcy email"),
          new Tab("api", "code", "Webhooks i API"),
          new Tab("notifications", "bell", "Powiadomienia"),
          new Tab("gdpr", "info", "RODO / DPA"));

  private static final Map<String, String> SUBTITLES_PL =
      Map.ofEntries(
          Map.entry("account", "Dane firmy i preferencje właściciela konta."),
          Map.entry("sites", "Domeny objęte trackingiem oraz instalacja skryptu."),
          Map.entry("team", "Osoby z dostępem do panelu, ich role i zaproszenia."),
          Map.entry(
              "providers", "Skąd wychodzą Twoje e-maile i jak radzą sobie z dostarczalnością."),
          Map.entry("api", "Klucze API, webhooks i logi wywołań."),
          Map.entry("notifications", "Kiedy Kivvi ma Cię powiadomić i którym kanałem."),
          Map.entry("gdpr", "Retencja danych, umowa powierzenia i obsługa żądań podmiotów."));

  private static final List<TrackedSite> TRACKED_SITES_PL =
      List.of(
          new TrackedSite(
              "aureashop.pl",
              "#7a8763",
              Format.number(EVENTS_AUREASHOP, SupportedLocale.PL),
              "pk_live_8a4f2c…"),
          new TrackedSite(
              "mlot-narzedzia.pl",
              "#a3825b",
              Format.number(EVENTS_MLOT, SupportedLocale.PL),
              "pk_live_3c91b7…"),
          new TrackedSite(
              "polna-bistro.pl",
              "#8b6f53",
              Format.number(EVENTS_POLNA, SupportedLocale.PL),
              "pk_live_be22a0…"));

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

  private static final List<TeamMember> TEAM_PL =
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

  private static final List<Role> ROLES_PL =
      List.of(
          new Role("Właściciel", "Pełny dostęp do wszystkiego, łącznie z usunięciem konta.", 1),
          new Role("Administrator", "Wszystko poza usunięciem konta.", 1),
          new Role("Marketer", "Automatyzacje, kampanie, popupy, segmenty. Bez ustawień.", 2),
          new Role("Analityk", "Tylko podglądanie danych i eksport raportów.", 1));

  private static final List<EmailProvider> EMAIL_PROVIDERS_PL =
      List.of(
          new EmailProvider(
              "Amazon SES",
              "eu-central-1",
              "good",
              "Główny",
              Format.number(SES_SENT, SupportedLocale.PL),
              "0,24%",
              false,
              "0,01%",
              true),
          new EmailProvider(
              "SendGrid",
              "EU",
              "info",
              "Zapasowy",
              Format.number(SENDGRID_SENT, SupportedLocale.PL),
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

  private static final List<DnsRecord> DNS_RECORDS_PL =
      List.of(
          new DnsRecord("SPF", "v=spf1 include:amazonses.com ~all", true),
          new DnsRecord("DKIM", "kivvi1._domainkey.aureashop.pl", true),
          new DnsRecord("DMARC", "v=DMARC1; p=quarantine; rua=mailto:dmarc@aureashop.pl", true),
          new DnsRecord("BIMI", "nie skonfigurowane — logo marki nie pojawi się w Gmailu", false));

  private static final List<ApiKey> API_KEYS_PL =
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

  private static final List<Webhook> WEBHOOKS_PL =
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

  private static final List<Bar> API_LIMITS_PL =
      List.of(
          new Bar("Ingest zdarzeń (na sek.)", 42.0, "42 / 100", "accent"),
          new Bar("REST API (na min.)", 18.0, "216 / 1200", "accent"),
          new Bar("Eksporty (na godz.)", 60.0, "6 / 10", "brown"));

  private static final List<NotificationRow> NOTIFICATION_MATRIX_PL =
      List.of(
          new NotificationRow("Automatyzacja przestała działać", true, true, true),
          new NotificationRow("Feed produktów zwrócił błąd", true, true, false),
          new NotificationRow("Bounce rate przekroczył próg", true, true, true),
          new NotificationRow("Limit wysyłek na wyczerpaniu (80%)", true, false, false),
          new NotificationRow("Import klientów zakończony", true, false, false),
          new NotificationRow("Nowa osoba dołączyła do zespołu", true, false, false),
          new NotificationRow("Tygodniowe podsumowanie wyników", true, false, false),
          new NotificationRow("Tracker przestał odbierać zdarzenia", true, true, true));

  private static final List<DataSubjectRequest> DATA_SUBJECT_REQUESTS_PL =
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

  private static final List<RetentionPolicy> RETENTION_POLICIES_PL =
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

  private static final List<Tab> TABS_EN =
      List.of(
          new Tab("account", "user", "Account"),
          new Tab("sites", "globe", "Tracked sites"),
          new Tab("team", "users", "Team"),
          new Tab("providers", "mail", "Email providers"),
          new Tab("api", "code", "Webhooks and API"),
          new Tab("notifications", "bell", "Notifications"),
          new Tab("gdpr", "info", "GDPR / DPA"));

  private static final Map<String, String> SUBTITLES_EN =
      Map.ofEntries(
          Map.entry("account", "Your company details and the account owner's preferences."),
          Map.entry("sites", "The domains being tracked, and how to install the script."),
          Map.entry("team", "Who can reach the panel, their roles and pending invitations."),
          Map.entry("providers", "Where your e-mails go out from, and how they are delivered."),
          Map.entry("api", "API keys, webhooks and the call log."),
          Map.entry(
              "notifications", "When Kivvi should tell you something, and through which channel."),
          Map.entry("gdpr", "Data retention, the processing agreement and data-subject requests."));

  private static final List<TrackedSite> TRACKED_SITES_EN =
      List.of(
          new TrackedSite(
              "aureashop.pl",
              "#7a8763",
              Format.number(EVENTS_AUREASHOP, SupportedLocale.EN),
              "pk_live_8a4f2c…"),
          new TrackedSite(
              "mlot-narzedzia.pl",
              "#a3825b",
              Format.number(EVENTS_MLOT, SupportedLocale.EN),
              "pk_live_3c91b7…"),
          new TrackedSite(
              "polna-bistro.pl",
              "#8b6f53",
              Format.number(EVENTS_POLNA, SupportedLocale.EN),
              "pk_live_be22a0…"));

  private static final List<TeamMember> TEAM_EN =
      List.of(
          new TeamMember(
              "Maciej Kowalczyk",
              "maciej@aureashop.pl",
              "Owner",
              "accent",
              "just now",
              false,
              true),
          new TeamMember(
              "Anna Bartosz",
              "anna@aureashop.pl",
              "Administrator",
              "brown",
              "2 hr ago",
              false,
              true),
          new TeamMember(
              "Piotr Sobczak",
              "piotr@aureashop.pl",
              "Marketer",
              "neutral",
              "yesterday",
              false,
              false),
          new TeamMember(
              "Iga Nowak", "iga@agencja-lumo.pl", "Marketer", "neutral", "4 days ago", false, true),
          new TeamMember("—", "kamil@aureashop.pl", "Analyst", "neutral", "—", true, false));

  private static final List<Role> ROLES_EN =
      List.of(
          new Role("Owner", "Full access to everything, deleting the account included.", 1),
          new Role("Administrator", "Everything except deleting the account.", 1),
          new Role("Marketer", "Automations, campaigns, popups, segments. No settings.", 2),
          new Role("Analyst", "Reading the data and exporting reports, nothing else.", 1));

  private static final List<EmailProvider> EMAIL_PROVIDERS_EN =
      List.of(
          new EmailProvider(
              "Amazon SES",
              "eu-central-1",
              "good",
              "Primary",
              Format.number(SES_SENT, SupportedLocale.EN),
              "0.24%",
              false,
              "0.01%",
              true),
          new EmailProvider(
              "SendGrid",
              "EU",
              "info",
              "Backup",
              Format.number(SENDGRID_SENT, SupportedLocale.EN),
              "0.41%",
              true,
              "0.03%",
              true),
          new EmailProvider(
              "Your own SMTP (poczta.aureashop.pl)",
              "—",
              "neutral",
              "Disabled",
              "0",
              "0%",
              false,
              "0%",
              false));

  private static final List<DnsRecord> DNS_RECORDS_EN =
      List.of(
          new DnsRecord("SPF", "v=spf1 include:amazonses.com ~all", true),
          new DnsRecord("DKIM", "kivvi1._domainkey.aureashop.pl", true),
          new DnsRecord("DMARC", "v=DMARC1; p=quarantine; rua=mailto:dmarc@aureashop.pl", true),
          new DnsRecord("BIMI", "not configured — your logo will not show up in Gmail", false));

  private static final List<ApiKey> API_KEYS_EN =
      List.of(
          new ApiKey(
              "Production — backend",
              "sk_live_8a4f2c",
              "14 Jan 2024",
              "3 min ago",
              List.of("events:write", "customers:read")),
          new ApiKey(
              "Public tracker",
              "pk_live_8a4f2c",
              "14 Jan 2024",
              "just now",
              List.of("events:write")),
          new ApiKey("Staging", "sk_test_1b09de", "02 Mar 2026", "yesterday", List.of("*")));

  private static final List<Webhook> WEBHOOKS_EN =
      List.of(
          new Webhook(
              "https://aureashop.pl/hooks/kivvi",
              List.of("purchase", "cart_abandon"),
              200,
              "2 min ago"),
          new Webhook(
              "https://erp.aureashop.pl/api/kivvi", List.of("customer.created"), 200, "11 min ago"),
          new Webhook(
              "https://hooks.slack.com/services/T0…",
              List.of("automation.failed"),
              410,
              "3 hr ago"));

  private static final List<Bar> API_LIMITS_EN =
      List.of(
          new Bar("Event ingest (per second)", 42.0, "42 / 100", "accent"),
          new Bar("REST API (per minute)", 18.0, "216 / 1200", "accent"),
          new Bar("Exports (per hour)", 60.0, "6 / 10", "brown"));

  private static final List<NotificationRow> NOTIFICATION_MATRIX_EN =
      List.of(
          new NotificationRow("An automation stopped working", true, true, true),
          new NotificationRow("A product feed returned an error", true, true, false),
          new NotificationRow("Bounce rate crossed the threshold", true, true, true),
          new NotificationRow("The sending allowance is running out (80%)", true, false, false),
          new NotificationRow("A customer import finished", true, false, false),
          new NotificationRow("Someone new joined the team", true, false, false),
          new NotificationRow("The weekly performance summary", true, false, false),
          new NotificationRow("The tracker stopped receiving events", true, true, true));

  private static final List<DataSubjectRequest> DATA_SUBJECT_REQUESTS_EN =
      List.of(
          new DataSubjectRequest(
              "DSR-0142",
              "hannah.k@aurea.pl",
              "Access to the data",
              "completed",
              true,
              "22 Aug 2026"),
          new DataSubjectRequest(
              "DSR-0141",
              "mark.p@example.com",
              "Erasure of the data",
              "in progress",
              false,
              "25 Aug 2026"),
          new DataSubjectRequest(
              "DSR-0139",
              "ivy.n@example.com",
              "Objection to profiling",
              "completed",
              true,
              "18 Aug 2026"));

  private static final List<RetentionPolicy> RETENTION_POLICIES_EN =
      List.of(
          new RetentionPolicy(
              "Raw events", List.of("90 days", "13 months", "24 months"), "13 months"),
          new RetentionPolicy(
              "Customer profiles with no activity",
              List.of("24 months", "36 months", "no limit"),
              "24 months"),
          new RetentionPolicy(
              "E-mail delivery logs", List.of("12 months", "24 months"), "12 months"),
          new RetentionPolicy("Webhook logs", List.of("30 days", "90 days"), "30 days"));

  private SettingsFixtures() {}

  public static List<Tab> tabs(SupportedLocale locale) {
    return switch (locale) {
      case PL -> TABS_PL;
      case EN -> TABS_EN;
    };
  }

  public static boolean isKnownTab(String tab) {
    return TABS_PL.stream().anyMatch(known -> known.id().equals(tab));
  }

  public static String subtitle(SupportedLocale locale, String tab) {
    return switch (locale) {
      case PL -> SUBTITLES_PL.get(tab);
      case EN -> SUBTITLES_EN.get(tab);
    };
  }

  public static List<TrackedSite> trackedSites(SupportedLocale locale) {
    return switch (locale) {
      case PL -> TRACKED_SITES_PL;
      case EN -> TRACKED_SITES_EN;
    };
  }

  /** Event names on the wire — the same tokens whatever the page is read in. */
  public static List<String> automaticEvents() {
    return AUTOMATIC_EVENTS;
  }

  public static List<TeamMember> team(SupportedLocale locale) {
    return switch (locale) {
      case PL -> TEAM_PL;
      case EN -> TEAM_EN;
    };
  }

  public static List<Role> roles(SupportedLocale locale) {
    return switch (locale) {
      case PL -> ROLES_PL;
      case EN -> ROLES_EN;
    };
  }

  public static List<EmailProvider> emailProviders(SupportedLocale locale) {
    return switch (locale) {
      case PL -> EMAIL_PROVIDERS_PL;
      case EN -> EMAIL_PROVIDERS_EN;
    };
  }

  public static List<DnsRecord> dnsRecords(SupportedLocale locale) {
    return switch (locale) {
      case PL -> DNS_RECORDS_PL;
      case EN -> DNS_RECORDS_EN;
    };
  }

  public static List<ApiKey> apiKeys(SupportedLocale locale) {
    return switch (locale) {
      case PL -> API_KEYS_PL;
      case EN -> API_KEYS_EN;
    };
  }

  public static List<Webhook> webhooks(SupportedLocale locale) {
    return switch (locale) {
      case PL -> WEBHOOKS_PL;
      case EN -> WEBHOOKS_EN;
    };
  }

  public static List<Bar> apiLimits(SupportedLocale locale) {
    return switch (locale) {
      case PL -> API_LIMITS_PL;
      case EN -> API_LIMITS_EN;
    };
  }

  public static List<NotificationRow> notificationMatrix(SupportedLocale locale) {
    return switch (locale) {
      case PL -> NOTIFICATION_MATRIX_PL;
      case EN -> NOTIFICATION_MATRIX_EN;
    };
  }

  public static List<DataSubjectRequest> dataSubjectRequests(SupportedLocale locale) {
    return switch (locale) {
      case PL -> DATA_SUBJECT_REQUESTS_PL;
      case EN -> DATA_SUBJECT_REQUESTS_EN;
    };
  }

  public static List<RetentionPolicy> retentionPolicies(SupportedLocale locale) {
    return switch (locale) {
      case PL -> RETENTION_POLICIES_PL;
      case EN -> RETENTION_POLICIES_EN;
    };
  }
}
