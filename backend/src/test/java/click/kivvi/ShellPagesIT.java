package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.application.ShellView;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration-level (real HTTP layer, real Spring context) equivalent of the old stack's {@code
 * tests/Controller/PanelPagesTest.php}: every one of its URLs still renders through the real HTTP
 * layer, and — for the panel pages that carry the shell (all but landing/login, which use {@code
 * PublicLayout}/{@code AuthLayout} and never render a sidebar or breadcrumb) — {@code GET
 * /api/v1/{locale}/shell?route=<name>} resolves the same {@code currentSection}/{@code crumb} the
 * old page's server-rendered breadcrumb carried (B01, B02).
 *
 * <p>The old test's per-page body markers (a customer's first name, an automation's title, a
 * settings tab's card heading, …) are page-body content owned by each page's own journey
 * (customers, automations, campaigns-email-editor, popups-widget-editor, feeds, import-wizard,
 * settings) — see {@code CustomersApiIT}, {@code CampaignsApiIT}, {@code WidgetApiIT}, {@code
 * FeedsApiIT}, {@code ImportApiIT} and {@code SettingsApiIT} for that coverage, and this journey's
 * {@code frontend/test/integration/ShellNavigation.spec.ts} for the client-rendered half of B02
 * (the SPA document below never contains rendered {@code .sidebar}/{@code .topbar}/{@code
 * .main-scroll}/{@code aria-current} markup — those only exist after Vue mounts and the shell
 * payload this class asserts resolves correctly).
 *
 * <p>{@code testDetailRoutesKeepTheirSectionActive} (B03, owned by customers), {@code
 * testUnknownCustomerIsNotFound} (B05, owned by customers) and {@code
 * testUnknownSettingsTabIsNotFound} (B06, owned by settings) are the old file's remaining
 * non-parameterized methods; each already has its own named integration coverage in its owning
 * journey's {@code *ApiIT} — not duplicated here. {@code testUnsupportedLocaleIsNotFound} (B07,
 * this journey's own) and {@code testEnglishLocaleTranslatesTheNavigation} (B04, this journey's
 * own) are already covered by {@code ShellApiIT}; reproduced here too since they are part of {@code
 * PanelPagesTest.php}'s own coverage this class otherwise mirrors in full.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ShellPagesIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  /**
   * One row of {@code PanelPagesTest::pages()}. {@code routeName} is {@code null} for the two
   * entries with no shell chrome (landing, login) — {@code section}/{@code crumb} are then never
   * read.
   */
  private record Page(String url, String routeName, String section, String crumb) {
    @Override
    public String toString() {
      return url;
    }
  }

  static Stream<Page> pages() {
    return Stream.of(
        // landing, login: PublicLayout/AuthLayout — no sidebar, no breadcrumb to resolve.
        new Page("/pl", null, null, null),
        new Page("/pl/login", null, null, null),
        new Page("/pl/dashboard", "dashboard", "dashboard", "Pulpit"),
        new Page("/pl/events", "events", "events", "Strumień zdarzeń"),
        new Page("/pl/customers", "customers", "customers", "Klienci"),
        new Page("/pl/customers/c_1001", "customer_show", "customers", "Klienci"),
        new Page("/pl/automations", "automations", "automations", "Reguły"),
        new Page("/pl/automations/a1", "automation_edit", "automations", "Reguły"),
        new Page("/pl/campaigns", "campaigns", "campaigns", "Kampanie email"),
        new Page("/pl/emails/k1", "email_edit", "campaigns", "Kampanie email"),
        new Page("/pl/popups", "popups", "popups", "Popupy i widgety"),
        new Page("/pl/popups/p1", "popup_edit", "popups", "Popupy i widgety"),
        new Page("/pl/feeds", "feeds", "feeds", "Feedy produktów"),
        new Page("/pl/import/1", "import", "import", "Import klientów"),
        new Page("/pl/import/2", "import", "import", "Import klientów"),
        new Page("/pl/import/3", "import", "import", "Import klientów"),
        new Page("/pl/import/4", "import", "import", "Import klientów"),
        new Page("/pl/settings/account", "settings", "settings", "Ustawienia"),
        new Page("/pl/settings/sites", "settings", "settings", "Ustawienia"),
        new Page("/pl/settings/team", "settings", "settings", "Ustawienia"),
        new Page("/pl/settings/providers", "settings", "settings", "Ustawienia"),
        new Page("/pl/settings/api", "settings", "settings", "Ustawienia"),
        new Page("/pl/settings/notifications", "settings", "settings", "Ustawienia"),
        // PIO-123 retired the billing tab; /pl/settings/billing is a 404 now and is covered as
        // such by SettingsApiIT and SettingsControllerTest rather than as a rendering page here.
        new Page("/pl/settings/gdpr", "settings", "settings", "Ustawienia"));
  }

  @ParameterizedTest
  @MethodSource("pages")
  @DisplayName("B01 every one of PanelPagesTest.php's still-live URLs renders the SPA document 200")
  void everyPageDocumentRenders200(Page page) {
    ResponseEntity<String> response = restTemplate.getForEntity(page.url(), String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).contains("<html");
  }

  @ParameterizedTest
  @MethodSource("pages")
  @DisplayName(
      "B02 every panel page's shell payload resolves the currentSection/crumb its old "
          + "server-rendered breadcrumb carried")
  void everyPanelPageResolvesItsShellSection(Page page) {
    if (page.routeName() == null) {
      return; // landing/login carry no shell chrome — nothing to resolve.
    }

    ResponseEntity<ShellView> response =
        restTemplate.getForEntity("/api/v1/pl/shell?route=" + page.routeName(), ShellView.class);

    ShellView shell = response.getBody();
    assertThat(shell).isNotNull();
    assertThat(shell.currentSection()).isEqualTo(page.section());
    assertThat(shell.crumb()).isEqualTo(page.crumb());
  }

  @Test
  @DisplayName(
      "B04 GET /en/dashboard renders 200 and its shell payload carries the English navigation, "
          + "through the real HTTP layer")
  void englishLocaleDocumentAndShellBothTranslate() {
    ResponseEntity<String> document = restTemplate.getForEntity("/en/dashboard", String.class);
    assertThat(document.getStatusCode().value()).isEqualTo(200);

    ResponseEntity<ShellView> shell =
        restTemplate.getForEntity("/api/v1/en/shell?route=dashboard", ShellView.class);
    assertThat(shell.getBody()).isNotNull();
    assertThat(shell.getBody().locale()).isEqualTo("en");
    assertThat(shell.getBody().crumb()).isEqualTo("Dashboard");
  }

  @Test
  @DisplayName("B07 GET /de/dashboard is not found, through the real HTTP layer")
  void unsupportedLocaleIsNotFound() {
    ResponseEntity<String> response = restTemplate.getForEntity("/de/dashboard", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }
}
