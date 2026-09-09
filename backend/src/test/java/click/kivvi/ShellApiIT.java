package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import click.kivvi.application.ShellView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration-level (real HTTP layer, real Spring context — real {@code DataSource}, real {@code
 * MessageSource}, real Flyway-backed session store) coverage for the shell API, 404 routing, and
 * the login form's rejection path. Repair-2 (R2-B): the existing {@code @WebMvcTest}-sliced
 * coverage of these same behaviours (SpaDocumentControllerTest, ShellViewServiceTest) never boots
 * the full application context, which is what the verifier contract's unit/integration split
 * actually requires B04 and B07 to be checked against. Repair-3 (R3-A): the same gap existed for
 * B12/B13 — {@code LoginControllerTest} covers the rejection messages at the {@code @WebMvcTest}
 * (unit) slice, but nothing exercised {@code POST /{locale}/login} through a real HTTP layer with
 * an empty or malformed address.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ShellApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName("B04 GET /api/v1/en/shell returns English navigation labels and locale \"en\"")
  void englishShellReturnsEnglishLabels() {
    ResponseEntity<ShellView> response =
        restTemplate.getForEntity("/api/v1/en/shell", ShellView.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    ShellView shell = response.getBody();
    assertThat(shell).isNotNull();
    assertThat(shell.locale()).isEqualTo("en");
    assertThat(shell.navGroups())
        .flatExtracting(ShellView.NavGroupView::items)
        .extracting(ShellView.NavItemView::label)
        .contains(
            "Dashboard",
            "Event stream",
            "Customers",
            "Rules",
            "Email campaigns",
            "Popups & widgets",
            "Product feeds",
            "Customer import",
            "Settings");
  }

  @Test
  @DisplayName("B04 GET /api/v1/pl/shell returns Polish navigation labels and locale \"pl\"")
  void polishShellReturnsPolishLabels() {
    ResponseEntity<ShellView> response =
        restTemplate.getForEntity("/api/v1/pl/shell", ShellView.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    ShellView shell = response.getBody();
    assertThat(shell).isNotNull();
    assertThat(shell.locale()).isEqualTo("pl");
    assertThat(shell.navGroups())
        .flatExtracting(ShellView.NavGroupView::items)
        .extracting(ShellView.NavItemView::label)
        .contains(
            "Pulpit",
            "Strumień zdarzeń",
            "Klienci",
            "Reguły",
            "Kampanie email",
            "Popupy i widgety",
            "Feedy produktów",
            "Import klientów",
            "Ustawienia");
  }

  @Test
  @DisplayName("B07 GET /de/dashboard is not found, through the real HTTP layer")
  void unsupportedLocaleIsNotFound() {
    ResponseEntity<String> response = restTemplate.getForEntity("/de/dashboard", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName("B07 GET /pl/nonexistent is not found, through the real HTTP layer")
  void unknownPathIsNotFound() {
    ResponseEntity<String> response = restTemplate.getForEntity("/pl/nonexistent", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName(
      "ShellController: the dashboard route's full payload matches the oracle "
          + "(journeys/login/steps/8 texts/a11y) — nav labels/hrefs/badge, currentSection, "
          + "crumb, workspace, default identity")
  void dashboardShellPayloadMatchesTheOracle() {
    ResponseEntity<ShellView> response =
        restTemplate.getForEntity("/api/v1/pl/shell?route=dashboard", ShellView.class);

    ShellView shell = response.getBody();
    assertThat(shell).isNotNull();
    assertThat(shell.locale()).isEqualTo("pl");
    assertThat(shell.theme()).isEqualTo("light");
    assertThat(shell.sidebar()).isEqualTo("expanded");
    assertThat(shell.currentSection()).isEqualTo("dashboard");
    // Oracle a11y: navigation "Ścieżka": aureashop.pl / Pulpit
    assertThat(shell.crumb()).isEqualTo("Pulpit");

    // Oracle a11y: button "AS aureashop.pl Plan Pro · 3 strony"
    assertThat(shell.workspace().name()).isEqualTo("aureashop.pl");
    assertThat(shell.workspace().meta()).isEqualTo("Plan Pro · 3 strony");
    assertThat(shell.workspace().mark()).isEqualTo("AS");

    // Oracle a11y (step 8, after logout): text: MK Maciej Kowalczyk maciej@aureashop.pl
    assertThat(shell.user().name()).isEqualTo("Maciej Kowalczyk");
    assertThat(shell.user().email()).isEqualTo("maciej@aureashop.pl");

    assertThat(shell.navGroups()).hasSize(4);

    ShellView.NavGroupView main = shell.navGroups().get(0);
    assertThat(main.label()).isEqualTo("Główne");
    assertThat(main.items())
        .extracting(
            ShellView.NavItemView::label,
            ShellView.NavItemView::route,
            ShellView.NavItemView::href,
            ShellView.NavItemView::badge)
        .containsExactly(
            tuple("Pulpit", "dashboard", "/pl/dashboard", null),
            tuple("Strumień zdarzeń", "events", "/pl/events", "·"),
            tuple("Klienci", "customers", "/pl/customers", null));

    ShellView.NavGroupView automate = shell.navGroups().get(1);
    assertThat(automate.label()).isEqualTo("Automatyzacja");
    assertThat(automate.items())
        .extracting(ShellView.NavItemView::label, ShellView.NavItemView::href)
        .containsExactly(
            tuple("Reguły", "/pl/automations"),
            tuple("Kampanie email", "/pl/campaigns"),
            tuple("Popupy i widgety", "/pl/popups"));

    ShellView.NavGroupView data = shell.navGroups().get(2);
    assertThat(data.label()).isEqualTo("Dane");
    assertThat(data.items())
        .extracting(ShellView.NavItemView::label, ShellView.NavItemView::href)
        .containsExactly(
            tuple("Feedy produktów", "/pl/feeds"), tuple("Import klientów", "/pl/import"));

    ShellView.NavGroupView config = shell.navGroups().get(3);
    assertThat(config.label()).isEqualTo("Konfiguracja");
    assertThat(config.items())
        .extracting(ShellView.NavItemView::label, ShellView.NavItemView::href)
        .containsExactly(tuple("Ustawienia", "/pl/settings"));
  }

  @Test
  @DisplayName(
      "B12 POST /pl/login with an empty address is rejected with \"Podaj adres e-mail.\", "
          + "through the real HTTP layer, and establishes no session identity")
  void emptyEmailIsRejectedThroughTheRealHttpLayer() {
    ResponseEntity<String> login =
        restTemplate.postForEntity(
            "/pl/login", new HttpEntity<>("_username=", formHeaders()), String.class);

    assertThat(login.getStatusCode().value()).isEqualTo(200);
    assertThat(login.getBody())
        .contains("data-login-error=\"Podaj adres e-mail.\"")
        // LoginController redisplays the sample address for a blank submission (mirrors
        // Twig's default() filter falling back on an empty string, not only an absent one).
        .contains("data-last-username=\"maciej@aureashop.pl\"");

    assertDefaultIdentityAfterwards(login);
  }

  @Test
  @DisplayName(
      "B13 POST /pl/login with a malformed address is rejected with \"To nie wygląda na "
          + "poprawny adres e-mail.\", through the real HTTP layer, and establishes no session "
          + "identity")
  void malformedEmailIsRejectedThroughTheRealHttpLayer() {
    ResponseEntity<String> login =
        restTemplate.postForEntity(
            "/pl/login", new HttpEntity<>("_username=not-an-email", formHeaders()), String.class);

    assertThat(login.getStatusCode().value()).isEqualTo(200);
    assertThat(login.getBody())
        .contains("data-login-error=\"To nie wygląda na poprawny adres e-mail.\"")
        .contains("data-last-username=\"not-an-email\"");

    assertDefaultIdentityAfterwards(login);
  }

  /**
   * Neither B12 nor B13's rejected POST ever calls {@code request.getSession(true)}
   * (LoginController only signs in on a validation success), so there is usually no {@code
   * Set-Cookie} at all to carry forward — the absence of one is itself part of what is being proved
   * here, not an oversight. Whatever cookie state exists is carried forward regardless, and the
   * shell must still report the default identity either way.
   */
  private void assertDefaultIdentityAfterwards(ResponseEntity<String> rejectedLogin) {
    String sessionCookie = rejectedLogin.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    HttpHeaders cookieHeader = new HttpHeaders();
    if (sessionCookie != null) {
      cookieHeader.add(HttpHeaders.COOKIE, sessionCookie);
    }

    ResponseEntity<ShellView> shell =
        restTemplate.exchange(
            "/api/v1/pl/shell", HttpMethod.GET, new HttpEntity<>(cookieHeader), ShellView.class);

    assertThat(shell.getBody()).isNotNull();
    assertThat(shell.getBody().user().email())
        .as("a rejected login must never establish a session identity")
        .isEqualTo("maciej@aureashop.pl");
  }

  private HttpHeaders formHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    return headers;
  }
}
