package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import click.kivvi.web.dto.DashboardResponse;
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
 * Real-HTTP-layer coverage for the dashboard page and its data endpoint — mirrors {@code
 * EventsApiIT}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class DashboardApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName(
      "B01 GET /pl/dashboard renders 200 (the page's own markup is verified by the Playwright e2e"
          + " suite; this only proves the document route resolves through the real HTTP layer)")
  void dashboardPageRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/dashboard", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName(
      "B23 GET /api/v1/pl/dashboard: 4 KPIs, 10 events, 6 recent customers with lastSeen strings,"
          + " 4 top automations, through the real HTTP layer")
  void dashboardPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<DashboardResponse> response =
        restTemplate.getForEntity("/api/v1/pl/dashboard", DashboardResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    DashboardResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.kpis()).hasSize(4);
    assertThat(body.legend()).hasSize(3);
    assertThat(body.events()).hasSize(10);
    assertThat(body.mercureTopic()).isEqualTo("/accounts/1/events");

    assertThat(body.recentCustomers()).hasSize(6);
    assertThat(body.recentCustomers())
        .extracting(DashboardResponse.Customer::id, DashboardResponse.Customer::lastSeen)
        .containsExactly(
            tuple("c_1000", "teraz"),
            tuple("c_1001", "1 min temu"),
            tuple("c_1002", "2 min temu"),
            tuple("c_1003", "3 min temu"),
            tuple("c_1004", "4 min temu"),
            tuple("c_1005", "5 min temu"));

    assertThat(body.topAutomations()).hasSize(4);
    assertThat(body.topAutomations().get(0).id()).isEqualTo("a3");
    assertThat(body.topAutomations().get(0).revenue()).isEqualTo("41 200 zł");
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/dashboard is not found, through the real HTTP layer")
  void unsupportedLocaleIsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/de/dashboard", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }
}
