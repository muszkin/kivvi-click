package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.web.dto.WidgetEditorResponse;
import click.kivvi.web.dto.WidgetsResponse;
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
 * Real-HTTP-layer (full Spring context, real {@code DataSource}) coverage for the popup index and
 * widget editor — mirrors {@code CampaignsApiIT}/{@code FeedsApiIT}: the {@code @WebMvcTest}-sliced
 * {@code WidgetControllerTest} never boots the full application context, which is what the verifier
 * contract's unit/integration split actually requires B01/B29 to be checked against.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class WidgetApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName(
      "B01 GET /pl/popups renders 200 through the real HTTP layer, marker text \"Popupy i"
          + " widgety\"")
  void popupsPageRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/popups", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
    assertThat(document.getBody()).contains("<html");
  }

  @Test
  @DisplayName(
      "B01 GET /pl/popups/p1 (the popup editor) renders 200 through the real HTTP layer, marker"
          + " text \"Exit intent — 10% rabatu\"")
  void popupEditorDocumentRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/popups/p1", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName("B01 GET /pl/popups/new renders 200 through the real HTTP layer")
  void newPopupDocumentRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/popups/new", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName(
      "B29 GET /api/v1/pl/popups: 5 cards, selected p1/modal — through the real HTTP layer")
  void popupsPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<WidgetsResponse> response =
        restTemplate.getForEntity("/api/v1/pl/popups", WidgetsResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    WidgetsResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.cards()).hasSize(5);
    assertThat(body.selected().id()).isEqualTo("p1");
    assertThat(body.selected().type()).isEqualTo("modal");
  }

  @Test
  @DisplayName("B29 GET /api/v1/pl/popups?preview=p2 selects the banner widget")
  void previewQuerySelectsTheBannerWidgetThroughTheRealHttpLayer() {
    ResponseEntity<WidgetsResponse> response =
        restTemplate.getForEntity("/api/v1/pl/popups?preview=p2", WidgetsResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().selected().type()).isEqualTo("banner");
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/popups is not found, through the real HTTP layer")
  void unsupportedLocalePopupsIsNotFoundThroughTheRealHttpLayer() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/de/popups", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName(
      "B29 GET /api/v1/pl/popups/p1: known widget, desktop viewport — through the real HTTP"
          + " layer")
  void knownPopupPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<WidgetEditorResponse> response =
        restTemplate.getForEntity("/api/v1/pl/popups/p1", WidgetEditorResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    WidgetEditorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.widget().name()).isEqualTo("Exit intent — 10% rabatu");
    assertThat(body.device()).isEqualTo("desktop");
    assertThat(body.viewport()).isEqualTo("1440 × 900");
  }

  @Test
  @DisplayName(
      "B29 GET /api/v1/pl/popups/p1?type=banner&device=mobile overrides content and viewport —"
          + " through the real HTTP layer")
  void typeAndDeviceQueryOverrideThroughTheRealHttpLayer() {
    ResponseEntity<WidgetEditorResponse> response =
        restTemplate.getForEntity(
            "/api/v1/pl/popups/p1?type=banner&device=mobile", WidgetEditorResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    WidgetEditorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.widget().type()).isEqualTo("banner");
    assertThat(body.device()).isEqualTo("mobile");
    assertThat(body.viewport()).isEqualTo("390 × 844");
  }

  @Test
  @DisplayName(
      "B29 GET /api/v1/pl/popups/new: the blank-draft widget — through the real HTTP layer")
  void newPopupPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<WidgetEditorResponse> response =
        restTemplate.getForEntity("/api/v1/pl/popups/new", WidgetEditorResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    WidgetEditorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.widget().name()).isEqualTo("Nowy widget");
    assertThat(body.widget().type()).isEqualTo("modal");
  }

  @Test
  @DisplayName(
      "B29/DEV-7 GET /api/v1/pl/popups/{id} 404s for an id shape outside \"new\"/\"p\\d+\", through"
          + " the real HTTP layer")
  void unmatchedPopupIdIsNotFoundThroughTheRealHttpLayer() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/pl/popups/bogus", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }
}
