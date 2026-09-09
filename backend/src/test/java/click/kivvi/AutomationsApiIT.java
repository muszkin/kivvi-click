package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.application.ShellView;
import click.kivvi.web.dto.AutomationEditorResponse;
import click.kivvi.web.dto.AutomationListResponse;
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
 * Real-HTTP-layer (full Spring context, real {@code DataSource}) coverage for the automations index
 * and rule editor, including the SPA document route ({@code RouteTable} already lists {@code
 * automations}/{@code automation_new}/{@code automation_edit}, so — unlike customers — {@link
 * click.kivvi.web.SpaDocumentController}'s generic route table alone renders every one of them,
 * even {@code /pl/automations/a99}, a shape-valid but unseeded id).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AutomationsApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName("B01/B26 GET /pl/automations renders 200 through the real HTTP layer")
  void automationsIndexPageRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/automations", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
    assertThat(document.getBody()).contains("<html");
  }

  @Test
  @DisplayName("B01/B26 GET /pl/automations/new renders 200 through the real HTTP layer")
  void automationNewPageRenders200() {
    ResponseEntity<String> document =
        restTemplate.getForEntity("/pl/automations/new", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName(
      "B01/B26 GET /pl/automations/a99 (shape-valid, unseeded) still renders the SPA shell")
  void unseededShapeValidAutomationDocumentStillRenders200() {
    ResponseEntity<String> document =
        restTemplate.getForEntity("/pl/automations/a99", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName("B26 GET /api/v1/pl/automations: 6 cards, 4 filters — through the real HTTP layer")
  void automationsListPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<AutomationListResponse> response =
        restTemplate.getForEntity("/api/v1/pl/automations", AutomationListResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    AutomationListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.automations()).hasSize(6);
    assertThat(body.filters()).hasSize(4);
  }

  @Test
  @DisplayName(
      "B26 GET /api/v1/pl/automations/a1: 3 steps, 6 nodes, 5 edges, 3 simulation read-outs — through the real HTTP layer")
  void automationEditorPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<AutomationEditorResponse> response =
        restTemplate.getForEntity("/api/v1/pl/automations/a1", AutomationEditorResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    AutomationEditorResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.automation().name()).isEqualTo("Powrót do porzuconego koszyka");
    assertThat(body.steps()).hasSize(3);
    assertThat(body.nodes()).hasSize(6);
    assertThat(body.edges()).hasSize(5);
    assertThat(body.simulation()).hasSize(3);
  }

  @Test
  @DisplayName("B26 GET /api/v1/pl/automations/xyz is not found, through the real HTTP layer")
  void unknownIdShapeApiIsNotFoundThroughTheRealHttpLayer() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/pl/automations/xyz", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  /**
   * B03's server side for this journey's detail route, the same chain proved for customers by
   * {@code CustomersApiIT}: the SPA sends the matched route name to the shell API, which must keep
   * the automations section active/crumbed for the editor route, not just the index route.
   */
  @Test
  @DisplayName(
      "B03 GET /api/v1/pl/shell?route=automation_edit reports currentSection \"automations\" and "
          + "crumb \"Reguły\", through the real HTTP layer")
  void shellForAutomationEditRouteKeepsAutomationsSectionActiveThroughTheRealHttpLayer() {
    ResponseEntity<ShellView> response =
        restTemplate.getForEntity("/api/v1/pl/shell?route=automation_edit", ShellView.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    ShellView shell = response.getBody();
    assertThat(shell).isNotNull();
    assertThat(shell.currentSection()).isEqualTo("automations");
    assertThat(shell.crumb()).isEqualTo("Reguły");
  }
}
