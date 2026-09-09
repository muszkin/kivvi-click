package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.web.dto.CampaignEmailResponse;
import click.kivvi.web.dto.CampaignsResponse;
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
 * Real-HTTP-layer (full Spring context, real {@code DataSource}) coverage for the campaigns index
 * and e-mail editor — mirrors {@code FeedsApiIT}/{@code CustomersApiIT}: the {@code @WebMvcTest}-
 * sliced {@code CampaignsControllerTest} never boots the full application context, which is what
 * the verifier contract's unit/integration split actually requires B27/B28 to be checked against.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CampaignsApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName("B01 GET /pl/campaigns renders 200 through the real HTTP layer")
  void campaignsPageRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/campaigns", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
    assertThat(document.getBody()).contains("<html");
  }

  @Test
  @DisplayName(
      "B27 GET /api/v1/pl/campaigns: 4 KPIs, 4 filters, 5 rows, last row \"—\" — through the real HTTP layer")
  void campaignsPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<CampaignsResponse> response =
        restTemplate.getForEntity("/api/v1/pl/campaigns", CampaignsResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    CampaignsResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.kpis()).hasSize(4);
    assertThat(body.filters()).hasSize(4);
    assertThat(body.rows()).hasSize(5);
    assertThat(body.rows().get(0).typeLabel()).isEqualTo("Wyzwalana");
    assertThat(body.rows().get(4).sent()).isEqualTo("—");
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/campaigns is not found, through the real HTTP layer")
  void unsupportedLocaleCampaignsIsNotFoundThroughTheRealHttpLayer() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/de/campaigns", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName("B01/B28 GET /pl/emails/new renders 200 through the real HTTP layer")
  void newEmailDocumentRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/emails/new", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName("B01/B28 GET /pl/emails/k1 (a known id) renders 200 through the real HTTP layer")
  void knownEmailDocumentRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/emails/k1", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName(
      "B28 GET /pl/emails/k999 (an unseeded but shape-valid id) still renders 200 — unlike the "
          + "customer profile, the editor route never validates the id against the fixture data")
  void unseededButShapeValidEmailDocumentStillRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/emails/k999", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName(
      "B28 GET /api/v1/pl/emails/k1: 10 blocks, 5 variables, 5 sections, selected block "
          + "\"hero_1\" — through the real HTTP layer")
  void knownEmailPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<CampaignEmailResponse> response =
        restTemplate.getForEntity("/api/v1/pl/emails/k1", CampaignEmailResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    CampaignEmailResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.template().name()).isEqualTo("Powrót do koszyka — wariant A");
    assertThat(body.blocks()).hasSize(10);
    assertThat(body.variables()).hasSize(5);
    assertThat(body.sections()).hasSize(5);
    assertThat(body.selectedBlock().blockId()).isEqualTo("hero_1");
  }

  @Test
  @DisplayName(
      "B28 GET /api/v1/pl/emails/new: the blank draft template, through the real HTTP layer")
  void newEmailPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<CampaignEmailResponse> response =
        restTemplate.getForEntity("/api/v1/pl/emails/new", CampaignEmailResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    CampaignEmailResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.template().name()).isEqualTo("Nowy szablon email");
    assertThat(body.template().subject()).isEqualTo("Temat wiadomości");
  }
}
