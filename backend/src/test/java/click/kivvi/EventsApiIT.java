package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.web.dto.EventsResponse;
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
 * Real-HTTP-layer coverage for the event stream page and its data endpoint — mirrors {@code
 * FeedsApiIT}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class EventsApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName(
      "B01 GET /pl/events renders 200 (the page's own markup is verified by the Playwright e2e "
          + "suite; this only proves the document route resolves through the real HTTP layer)")
  void eventsPageRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/events", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName("B24 GET /api/v1/pl/events: 30 rows, 9 type filters, 4 site filters, 4 ranges")
  void eventsPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<EventsResponse> response =
        restTemplate.getForEntity("/api/v1/pl/events", EventsResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    EventsResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.events()).hasSize(30);
    assertThat(body.typeFilters()).hasSize(9);
    assertThat(body.siteFilters()).hasSize(4);
    assertThat(body.ranges()).hasSize(4);
    assertThat(body.total()).isEqualTo("9 360");
    assertThat(body.shown()).isEqualTo(30);
    assertThat(body.mercureTopic()).isEqualTo("/accounts/1/events");
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/events is not found, through the real HTTP layer")
  void unsupportedLocaleIsNotFound() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/de/events", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }
}
