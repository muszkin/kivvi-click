package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.application.LandingView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real-HTTP-layer (full Spring context, real {@code DataSource}) coverage for the public marketing
 * page's view-model endpoint and its demo shortcut — mirrors {@code FeedsApiIT}: the
 * {@code @WebMvcTest}-sliced {@code LandingControllerTest} never boots the full application
 * context, which is what the verifier contract's unit/integration split actually requires B01/B22
 * to be checked against.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class LandingApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName(
      "B01 GET /api/v1/pl/landing: the marketing page's copy arrays, through the real HTTP "
          + "layer")
  void landingPayloadMatchesTheOracleThroughTheRealHttpLayer() {
    ResponseEntity<LandingView> response =
        restTemplate.getForEntity("/api/v1/pl/landing", LandingView.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    LandingView body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.features()).hasSize(6);
    assertThat(body.steps()).hasSize(4);
    assertThat(body.trustPoints())
        .containsExactly("Licencja MIT", "Postawisz u siebie", "Skrypt 2 KB");
    assertThat(body.previewTiles()).hasSize(4);
    assertThat(body.previewTiles())
        .extracting(LandingView.PreviewTileView::value)
        .contains("8 410", "94 200");
    assertThat(body.previewSeries()).isNotEmpty();
  }

  /**
   * Deliberately the inverse of the assertion it replaced. B22 pinned the parity quirk: the old
   * stack's {@code LandingContent} never ran these arrays through the translator, so the oracle's
   * English capture ({@code journeys/landing/steps/2/texts.json}) shows Polish feature and step
   * copy on {@code /en}. PIO-117 retired that quirk when PIO-125 made English the default — the
   * default page could not be half Polish.
   */
  @Test
  @DisplayName(
      "PIO-117 GET /api/v1/en/landing returns English copy, not the Polish arrays the old stack"
          + " served to both locales — through the real HTTP layer")
  void englishRouteReturnsEnglishCopyThroughTheRealHttpLayer() {
    ResponseEntity<LandingView> response =
        restTemplate.getForEntity("/api/v1/en/landing", LandingView.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    LandingView body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.features()).hasSize(6);
    assertThat(body.features().get(0).title()).isEqualTo("Live event stream");
    assertThat(body.steps()).hasSize(4);
    assertThat(body.steps().get(0).title()).isEqualTo("Run it yourself");
    assertThat(body.trustPoints())
        .containsExactly("MIT licence", "Host it yourself", "2 KB script");
    assertThat(body.previewTiles())
        .extracting(LandingView.PreviewTileView::label)
        .containsExactly("Events / min", "Active sessions", "Emails (24h)", "Revenue (24h)");
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/landing is not found, through the real HTTP layer")
  void unsupportedLocaleIsNotFoundThroughTheRealHttpLayer() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/de/landing", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName("B22 GET /pl/demo redirects to /pl/dashboard, through the real HTTP layer")
  void demoRedirectsToDashboardThroughTheRealHttpLayer() {
    TestRestTemplate noRedirects = restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW);

    ResponseEntity<Void> response = noRedirects.getForEntity("/pl/demo", Void.class);

    assertThat(response.getStatusCode().value()).isEqualTo(302);
    assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("/pl/dashboard");
  }
}
