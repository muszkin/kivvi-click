package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.web.dto.FeedsResponse;
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
 * Real-HTTP-layer (full Spring context, real {@code DataSource}) coverage for the product-feeds
 * page and its data endpoint — mirrors {@code ShellApiIT}: the {@code @WebMvcTest}-sliced {@code
 * FeedsControllerTest} never boots the full application context, which is what the verifier
 * contract's unit/integration split actually requires B01/B30 to be checked against.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class FeedsApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName(
      "B01 GET /pl/feeds renders 200 (the page's own headline/marker text is verified by the "
          + "Playwright e2e suite, which renders the client-side SPA; this only proves the "
          + "document route itself resolves through the real HTTP layer)")
  void feedsPageRenders200() {
    ResponseEntity<String> document = restTemplate.getForEntity("/pl/feeds", String.class);

    assertThat(document.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @DisplayName(
      "B30 GET /api/v1/pl/feeds: 4 sources, 4 feeds (one failing with HTTP 503), matching "
          + "diagnostic with 142 mismatched — through the real HTTP layer")
  void feedsPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<FeedsResponse> response =
        restTemplate.getForEntity("/api/v1/pl/feeds", FeedsResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    FeedsResponse body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.kpis()).hasSize(4);
    assertThat(body.sources()).hasSize(4);
    assertThat(body.feeds()).hasSize(4);
    assertThat(body.feeds().get(2).status()).isEqualTo("error");
    assertThat(body.feeds().get(2).error()).isEqualTo("HTTP 503 — Service Unavailable");
    assertThat(body.feeds().get(2).mappedPercent()).isNull();
    assertThat(body.coverage()).hasSize(4);
    assertThat(body.fallbackRules()).hasSize(3);
    assertThat(body.mismatched()).isEqualTo(142);
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/feeds is not found, through the real HTTP layer")
  void unsupportedLocaleIsNotFound() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/de/feeds", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }
}
