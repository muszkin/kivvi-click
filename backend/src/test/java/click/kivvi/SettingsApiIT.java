package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.web.dto.SettingsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * Real-HTTP-layer (full Spring context, real {@code DataSource}) coverage for the settings tabs,
 * including the routing precedence {@link click.kivvi.web.SettingsController}'s document route
 * depends on: with {@link click.kivvi.web.SpaDocumentController} ALSO registered in this full
 * context, {@code GET /pl/settings/nonexistent} must still 404 rather than fall through to the
 * wildcard route table, which would otherwise render it 200 (the tab "looks like" a settings tab to
 * the route table's regex, but is not a known one) — a {@code @WebMvcTest} slice of {@code
 * SettingsController} alone cannot prove this, since it never registers the competing wildcard
 * mapping.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SettingsApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  /**
   * B01 ("every panel/public URL renders 200 and shows its own headline/marker text") for each of
   * the eight settings rows — mirrors the old stack's {@code PanelPagesTest::pages()} data
   * provider, one case per row, each asserting a {@code .card-title} selector's text (e.g.
   * "settings account" → {@code /pl/settings/account}, {@code .card-title}, "Dane konta"). The new
   * stack's document route serves the SPA shell only (card titles are rendered client-side after JS
   * executes, which a {@code TestRestTemplate} never does — that half is proven by the Playwright
   * e2e suite instead, matching {@code EventsApiIT}'s B01 comment on the same limitation), so the
   * "marker" half is proven here through {@code tabSubtitle} instead: a backend-owned, per-tab
   * string the SPA renders verbatim under the page's own {@code <h1>} (see {@code
   * SettingsFixtures}'s {@code SUBTITLES} map, byte-identical to the old {@code
   * SettingsCatalog::SUBTITLES} entry for the same tab) — as real and row-specific a marker as the
   * old test's card title, just carried by the API response instead of the HTML body.
   */
  @Test
  @DisplayName(
      "B01 GET /pl/settings/account renders the SPA document 200 and its own tabSubtitle marker")
  void accountRowRendersWithItsOwnMarker() {
    ResponseEntity<String> document =
        restTemplate.getForEntity("/pl/settings/account", String.class);
    assertThat(document.getStatusCode().value()).isEqualTo(200);
    assertThat(document.getBody()).contains("<html");

    ResponseEntity<SettingsResponse> api =
        restTemplate.getForEntity("/api/v1/pl/settings/account", SettingsResponse.class);
    assertThat(api.getStatusCode().value()).isEqualTo(200);
    SettingsResponse body = api.getBody();
    assertThat(body).isNotNull();
    assertThat(body.tab()).isEqualTo("account");
    assertThat(body.tabSubtitle()).isEqualTo("Dane firmy i preferencje właściciela konta.");
  }

  @Test
  @DisplayName(
      "B01 GET /pl/settings/sites renders the SPA document 200 and its own tabSubtitle marker")
  void sitesRowRendersWithItsOwnMarker() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/settings/sites", String.class);
    assertThat(document.getStatusCode().value()).isEqualTo(200);
    assertThat(document.getBody()).contains("<html");

    ResponseEntity<SettingsResponse> api =
        restTemplate.getForEntity("/api/v1/pl/settings/sites", SettingsResponse.class);
    assertThat(api.getStatusCode().value()).isEqualTo(200);
    SettingsResponse body = api.getBody();
    assertThat(body).isNotNull();
    assertThat(body.tab()).isEqualTo("sites");
    assertThat(body.tabSubtitle()).isEqualTo("Domeny objęte trackingiem oraz instalacja skryptu.");
  }

  @Test
  @DisplayName(
      "B01 GET /pl/settings/team renders the SPA document 200 and its own tabSubtitle marker")
  void teamRowRendersWithItsOwnMarker() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/settings/team", String.class);
    assertThat(document.getStatusCode().value()).isEqualTo(200);
    assertThat(document.getBody()).contains("<html");

    ResponseEntity<SettingsResponse> api =
        restTemplate.getForEntity("/api/v1/pl/settings/team", SettingsResponse.class);
    assertThat(api.getStatusCode().value()).isEqualTo(200);
    SettingsResponse body = api.getBody();
    assertThat(body).isNotNull();
    assertThat(body.tab()).isEqualTo("team");
    assertThat(body.tabSubtitle()).isEqualTo("Osoby z dostępem do panelu, ich role i zaproszenia.");
  }

  @Test
  @DisplayName(
      "B01 GET /pl/settings/providers renders the SPA document 200 and its own tabSubtitle marker")
  void providersRowRendersWithItsOwnMarker() {
    ResponseEntity<String> document =
        restTemplate.getForEntity("/pl/settings/providers", String.class);
    assertThat(document.getStatusCode().value()).isEqualTo(200);
    assertThat(document.getBody()).contains("<html");

    ResponseEntity<SettingsResponse> api =
        restTemplate.getForEntity("/api/v1/pl/settings/providers", SettingsResponse.class);
    assertThat(api.getStatusCode().value()).isEqualTo(200);
    SettingsResponse body = api.getBody();
    assertThat(body).isNotNull();
    assertThat(body.tab()).isEqualTo("providers");
    assertThat(body.tabSubtitle())
        .isEqualTo("Skąd wychodzą Twoje e-maile i jak radzą sobie z dostarczalnością.");
  }

  @Test
  @DisplayName(
      "B01 GET /pl/settings/api renders the SPA document 200 and its own tabSubtitle marker")
  void apiRowRendersWithItsOwnMarker() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/settings/api", String.class);
    assertThat(document.getStatusCode().value()).isEqualTo(200);
    assertThat(document.getBody()).contains("<html");

    ResponseEntity<SettingsResponse> api =
        restTemplate.getForEntity("/api/v1/pl/settings/api", SettingsResponse.class);
    assertThat(api.getStatusCode().value()).isEqualTo(200);
    SettingsResponse body = api.getBody();
    assertThat(body).isNotNull();
    assertThat(body.tab()).isEqualTo("api");
    assertThat(body.tabSubtitle()).isEqualTo("Klucze API, webhooks i logi wywołań.");
  }

  @Test
  @DisplayName(
      "B01 GET /pl/settings/notifications renders the SPA document 200 and its own tabSubtitle"
          + " marker")
  void notificationsRowRendersWithItsOwnMarker() {
    ResponseEntity<String> document =
        restTemplate.getForEntity("/pl/settings/notifications", String.class);
    assertThat(document.getStatusCode().value()).isEqualTo(200);
    assertThat(document.getBody()).contains("<html");

    ResponseEntity<SettingsResponse> api =
        restTemplate.getForEntity("/api/v1/pl/settings/notifications", SettingsResponse.class);
    assertThat(api.getStatusCode().value()).isEqualTo(200);
    SettingsResponse body = api.getBody();
    assertThat(body).isNotNull();
    assertThat(body.tab()).isEqualTo("notifications");
    assertThat(body.tabSubtitle()).isEqualTo("Kiedy Kivvi ma Cię powiadomić i którym kanałem.");
  }

  @Test
  @DisplayName(
      "B01 GET /pl/settings/gdpr renders the SPA document 200 and its own tabSubtitle marker")
  void gdprRowRendersWithItsOwnMarker() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/settings/gdpr", String.class);
    assertThat(document.getStatusCode().value()).isEqualTo(200);
    assertThat(document.getBody()).contains("<html");

    ResponseEntity<SettingsResponse> api =
        restTemplate.getForEntity("/api/v1/pl/settings/gdpr", SettingsResponse.class);
    assertThat(api.getStatusCode().value()).isEqualTo(200);
    SettingsResponse body = api.getBody();
    assertThat(body).isNotNull();
    assertThat(body.tab()).isEqualTo("gdpr");
    assertThat(body.tabSubtitle())
        .isEqualTo("Retencja danych, umowa powierzenia i obsługa żądań podmiotów.");
  }

  @Test
  @DisplayName(
      "B32 GET /api/v1/pl/settings/account: 7 tabs, 3 tracked sites, 5 team rows, through the "
          + "real HTTP layer")
  void settingsPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<SettingsResponse> response =
        restTemplate.getForEntity("/api/v1/pl/settings/account", SettingsResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    SettingsResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.tabs()).hasSize(7);
    assertThat(body.settings().trackedSites()).hasSize(3);
    assertThat(body.settings().team()).hasSize(5);
  }

  @Test
  @DisplayName("B06 GET /api/v1/pl/settings/nonexistent is not found, through the real HTTP layer")
  void unknownTabApiIsNotFoundThroughTheRealHttpLayer() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/pl/settings/nonexistent", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName(
      "B06/DEV-12 GET /pl/settings/nonexistent is a 404 document even though the tab shape "
          + "matches the route table's settings pattern — SettingsController's exact mapping "
          + "wins over SpaDocumentController's wildcard")
  void unknownTabDocumentIsNotFoundThroughTheRealHttpLayer() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/pl/settings/nonexistent", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  /**
   * PIO-123 retired the billing tab. {@code billing} is now an unknown tab id, so both its document
   * and its API route 404 — the same behaviour every other unknown tab has always had. It is
   * asserted here, in the full context, for the same reason the {@code nonexistent} case is: {@code
   * /pl/settings/billing} still matches {@link click.kivvi.domain.RouteTable}'s {@code
   * settings(?:/[a-z0-9-]+)?} pattern, so without {@link click.kivvi.web.SettingsController}'s
   * exact mapping winning over {@link click.kivvi.web.SpaDocumentController}'s wildcard the retired
   * tab would quietly render a 200 shell with an empty body. The url also used to be a live page,
   * which is exactly why the 404 needs to be written down rather than inferred.
   */
  @Test
  @DisplayName(
      "PIO-123 GET /pl/settings/billing is a 404 document through the real HTTP layer now that "
          + "the tab is retired")
  void retiredBillingTabDocumentIsNotFoundThroughTheRealHttpLayer() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/pl/settings/billing", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName(
      "PIO-123 GET /api/v1/pl/settings/billing is not found through the real HTTP layer, and no "
          + "tab in the payload points back at it")
  void retiredBillingTabApiIsNotFoundThroughTheRealHttpLayer() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/pl/settings/billing", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);

    ResponseEntity<SettingsResponse> account =
        restTemplate.getForEntity("/api/v1/pl/settings/account", SettingsResponse.class);
    SettingsResponse body = account.getBody();
    assertThat(body).isNotNull();
    assertThat(body.tabs()).extracting(SettingsResponse.Tab::id).doesNotContain("billing");
  }

  @Test
  @DisplayName(
      "B32 GET /pl/settings (no tab) still renders the SPA document — the default tab is applied "
          + "client-side, not a 404")
  void bareSettingsDocumentStillRendersTheSpaShell() {
    ResponseEntity<String> response = restTemplate.getForEntity("/pl/settings", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).contains("<html");
  }
}
