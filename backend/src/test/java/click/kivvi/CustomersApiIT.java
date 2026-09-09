package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.application.ShellView;
import click.kivvi.web.dto.CustomerDetailResponse;
import click.kivvi.web.dto.CustomerListResponse;
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
 * Real-HTTP-layer (full Spring context, real {@code DataSource}) coverage for the customers index
 * and 360 profile, including the routing precedence {@link click.kivvi.web.CustomersController}'s
 * document route depends on: with {@link click.kivvi.web.SpaDocumentController} ALSO registered in
 * this full context, {@code GET /pl/customers/c_9999} must still 404 rather than fall through to
 * the wildcard route table, which would otherwise render it 200 (the id "looks like" a customer to
 * the route table's regex, but is not a seeded one) — a {@code @WebMvcTest} slice of {@code
 * CustomersController} alone cannot prove this, since it never registers the competing wildcard
 * mapping.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CustomersApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName("B25 GET /pl/customers renders 200 through the real HTTP layer")
  void customersIndexPageRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/customers", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName(
      "B25 GET /api/v1/pl/customers: 24 rows, 6 segments, page 1 of 192 — through the real HTTP layer")
  void customersListPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<CustomerListResponse> response =
        restTemplate.getForEntity("/api/v1/pl/customers", CustomerListResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    CustomerListResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.customersRows()).hasSize(24);
    assertThat(body.segments()).hasSize(6);
    assertThat(body.page()).isEqualTo(1);
    assertThat(body.pages()).isEqualTo(192);
  }

  @Test
  @DisplayName(
      "B25 GET /api/v1/pl/customers/c_1000: 7 facts, 3 automations, 5 tabs, 3 scores, 9 timeline entries")
  void customerDetailPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<CustomerDetailResponse> response =
        restTemplate.getForEntity("/api/v1/pl/customers/c_1000", CustomerDetailResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    CustomerDetailResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.customer().name()).isEqualTo("Anna K.");
    assertThat(body.facts()).hasSize(7);
    assertThat(body.automations()).hasSize(3);
    assertThat(body.tabs()).hasSize(5);
    assertThat(body.scores()).hasSize(3);
    assertThat(body.timeline()).hasSize(9);
  }

  @Test
  @DisplayName("B05 GET /api/v1/pl/customers/c_9999 is not found, through the real HTTP layer")
  void unknownCustomerApiIsNotFoundThroughTheRealHttpLayer() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/pl/customers/c_9999", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName(
      "B05/DEV-12 GET /pl/customers/c_9999 is a 404 document even though the id shape matches "
          + "the route table's customer_show pattern — CustomersController's exact mapping wins "
          + "over SpaDocumentController's wildcard")
  void unknownCustomerDocumentIsNotFoundThroughTheRealHttpLayer() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/pl/customers/c_9999", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName("B25 GET /pl/customers/c_1000 (a known id) still renders the SPA document, not JSON")
  void knownCustomerDocumentStillRendersTheSpaShell() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/pl/customers/c_1000", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).contains("<html");
  }

  /**
   * Repair-1 (R1-A): the SPA's AppLayout sends the matched route's name as the shell API's {@code
   * route} query param (see frontend/src/layouts/AppLayout.vue), so a live customer-profile page
   * ({@code customer_show}) resolves through {@link
   * click.kivvi.domain.NavigationCatalog#currentSection(String)}'s detail-route mapping — this is
   * the server side of B03 "detail routes keep their index section active", proved through the real
   * HTTP layer rather than only at the {@code NavigationCatalog}/{@code ShellViewService} unit
   * level (see the sibling {@code CustomerDetailSidebarSection.spec.ts} for the frontend side of
   * the same chain: router → AppLayout → this endpoint → Sidebar's {@code current} prop).
   */
  @Test
  @DisplayName(
      "B03 GET /api/v1/pl/shell?route=customer_show reports currentSection \"customers\" and "
          + "crumb \"Klienci\", through the real HTTP layer")
  void shellForCustomerDetailRouteKeepsCustomersSectionActiveThroughTheRealHttpLayer() {
    ResponseEntity<ShellView> response =
        restTemplate.getForEntity("/api/v1/pl/shell?route=customer_show", ShellView.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    ShellView shell = response.getBody();
    assertThat(shell).isNotNull();
    assertThat(shell.currentSection()).isEqualTo("customers");
    assertThat(shell.crumb()).isEqualTo("Klienci");
  }
}
