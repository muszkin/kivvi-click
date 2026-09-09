package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.web.dto.EventCollectResponse;
import click.kivvi.web.dto.EventErrorResponse;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real-HTTP-layer coverage (full Spring context, real {@code DataSource}, real Mercure JWT signing)
 * for {@code POST /collect}'s 202 → 200 → 400 sequence — B19, mirroring {@code
 * EventIngestionTest::testEndpointAnswersAcceptedThenDuplicate}. The hub itself is a local stub
 * HTTP server (started once at class load, wired in via {@code kivvi.mercure.url} through
 * {@code @DynamicPropertySource}): this project has no mocking-library dependency to swap in a fake
 * hub client the way the old test's {@code MockHub} did, so it swaps the network endpoint instead.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class CollectApiIT {

  private static final List<String> PUBLISHED_BODIES =
      Collections.synchronizedList(new ArrayList<>());
  private static final HttpServer STUB_HUB = startStubHub();

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  @DynamicPropertySource
  static void mercureProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "kivvi.mercure.url",
        () -> "http://127.0.0.1:" + STUB_HUB.getAddress().getPort() + "/.well-known/mercure");
  }

  @AfterAll
  static void stopStubHub() {
    STUB_HUB.stop(0);
  }

  @Test
  @DisplayName("B19 POST /collect: 202 accepted, then 200 duplicate, then 400 on invalid payload")
  void acceptedThenDuplicateThenBadRequest() {
    Map<String, Object> payload =
        Map.of(
            "idempotency_id", "evt-it-" + System.nanoTime(),
            "type", "purchase",
            "detail", "412,00 PLN · 4 produkty",
            "customer_id", "c_1001",
            "customer_name", "Hania Kowalska",
            "site", "aureashop.pl");

    ResponseEntity<EventCollectResponse> first =
        restTemplate.postForEntity("/collect", payload, EventCollectResponse.class);
    assertThat(first.getStatusCode().value()).isEqualTo(202);
    assertThat(first.getBody().status()).isEqualTo("accepted");

    ResponseEntity<EventCollectResponse> replay =
        restTemplate.postForEntity("/collect", payload, EventCollectResponse.class);
    assertThat(replay.getStatusCode().value()).isEqualTo(200);
    assertThat(replay.getBody().status()).isEqualTo("duplicate");

    ResponseEntity<EventErrorResponse> invalid =
        restTemplate.postForEntity(
            "/collect", Map.of("type", "purchase"), EventErrorResponse.class);
    assertThat(invalid.getStatusCode().value()).isEqualTo(400);
    assertThat(invalid.getBody().error()).isEqualTo("Pole „idempotency_id” jest wymagane.");
  }

  @Test
  @DisplayName(
      "B16 an unknown event type answers 400 with the exact Polish message, through the real HTTP layer")
  void unknownEventTypeAnswers400() {
    Map<String, Object> payload =
        Map.of("idempotency_id", "evt-it-" + System.nanoTime(), "type", "teleport");

    ResponseEntity<EventErrorResponse> response =
        restTemplate.postForEntity("/collect", payload, EventErrorResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody().error()).isEqualTo("Nieznany typ zdarzenia „teleport”.");
  }

  @Test
  @DisplayName(
      "B15 a missing idempotency_id answers 400 with the exact Polish message, through the real HTTP layer")
  void missingIdempotencyIdAnswers400() {
    Map<String, Object> payload = Map.of("type", "purchase");

    ResponseEntity<EventErrorResponse> response =
        restTemplate.postForEntity("/collect", payload, EventErrorResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody().error()).isEqualTo("Pole „idempotency_id” jest wymagane.");
  }

  @Test
  @DisplayName(
      "a non-JSON body answers 400 with 'Oczekiwano obiektu JSON.', through the real HTTP layer")
  void nonJsonBodyAnswers400() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>("\"just a string\"", headers);

    ResponseEntity<EventErrorResponse> response =
        restTemplate.postForEntity("/collect", request, EventErrorResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody().error()).isEqualTo("Oczekiwano obiektu JSON.");
  }

  @Test
  @DisplayName("B18 a live duplicate is answered 200 and the hub receives no second publish for it")
  void duplicateIdempotencyIdIsNeverPublishedTwice() {
    Map<String, Object> payload =
        Map.of(
            "idempotency_id", "evt-it-b18-" + System.nanoTime(),
            "type", "purchase",
            "detail", "308,00 PLN · 4 produkty",
            "customer_id", "c_1001",
            "customer_name", "Hania Kowalska",
            "site", "aureashop.pl");

    ResponseEntity<EventCollectResponse> first =
        restTemplate.postForEntity("/collect", payload, EventCollectResponse.class);
    assertThat(first.getStatusCode().value()).isEqualTo(202);
    int publishedAfterFirst = PUBLISHED_BODIES.size();

    ResponseEntity<EventCollectResponse> second =
        restTemplate.postForEntity("/collect", payload, EventCollectResponse.class);
    assertThat(second.getStatusCode().value()).isEqualTo(200);
    assertThat(second.getBody().status()).isEqualTo("duplicate");

    assertThat(PUBLISHED_BODIES.size())
        .as("the stub hub must not receive a second publish for the same idempotency id")
        .isEqualTo(publishedAfterFirst);
  }

  @Test
  @DisplayName("DEV-3 the accepted event is published to the hub as JSON, not server-rendered HTML")
  void acceptedEventIsPublishedAsJson() {
    String idempotencyId = "evt-it-payload-" + System.nanoTime();
    Map<String, Object> payload =
        Map.of(
            "idempotency_id", idempotencyId,
            "type", "purchase",
            "detail", "412,00 PLN · 4 produkty",
            "customer_id", "c_1001",
            "customer_name", "Hania Kowalska",
            "site", "aureashop.pl");
    int publishedBefore = PUBLISHED_BODIES.size();

    ResponseEntity<EventCollectResponse> response =
        restTemplate.postForEntity("/collect", payload, EventCollectResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(202);
    List<String> published = List.copyOf(PUBLISHED_BODIES);
    assertThat(published.size()).isGreaterThan(publishedBefore);
    Map<String, String> form = decodeFormBody(published.get(published.size() - 1));
    assertThat(form.get("topic")).isEqualTo("/accounts/1/events");
    String data = form.get("data");
    assertThat(data).doesNotContain("event-row").doesNotContain("<div");
    assertThat(data).contains("\"type\":\"Zakup\"");
    assertThat(data).contains("\"siteColor\":\"#7a8763\"");
    assertThat(data).contains("\"customerName\":\"Hania Kowalska\"");
    assertThat(data).contains("\"detail\":\"412,00 PLN · 4 produkty\"");
  }

  private static Map<String, String> decodeFormBody(String body) {
    Map<String, String> values = new java.util.HashMap<>();
    for (String pair : body.split("&")) {
      String[] parts = pair.split("=", 2);
      values.put(
          java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
          java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
    }
    return values;
  }

  private static HttpServer startStubHub() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/.well-known/mercure",
          exchange -> {
            String body =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            PUBLISHED_BODIES.add(body);
            byte[] responseBody = "urn:uuid:it-event-id".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
