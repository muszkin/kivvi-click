package click.kivvi.infrastructure.mercure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link HttpMercurePublisher} against a real local HTTP server standing in for the Mercure hub —
 * verifies the JWT signature, the topic/data form encoding and the published payload shape,
 * mirroring what the old stack's own test verified against {@code MockHub}.
 */
class HttpMercurePublisherTest {

  private static final String SECRET = "test-mercure-secret";
  private static final String TOPIC = "/accounts/1/events";
  private static final String DATA = "{\"event\":{\"type\":\"purchase\"}}";

  private final BlockingQueue<Map<String, String>> receivedRequests = new LinkedBlockingQueue<>();
  private HttpServer server;
  private int responseStatus = 200;

  @BeforeEach
  void startStubHub() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/.well-known/mercure",
        exchange -> {
          Map<String, String> received = new HashMap<>();
          received.put("method", exchange.getRequestMethod());
          received.put("authorization", exchange.getRequestHeaders().getFirst("Authorization"));
          received.put("contentType", exchange.getRequestHeaders().getFirst("Content-Type"));
          received.put(
              "body", new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          receivedRequests.add(received);

          byte[] responseBody = "urn:uuid:test-event-id".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(responseStatus, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stopStubHub() {
    server.stop(0);
  }

  @Test
  @DisplayName("publishes topic/data form-encoded, with a valid HS256 publisher JWT")
  void publishesFormEncodedTopicAndDataWithASignedPublisherJwt() throws InterruptedException {
    HttpMercurePublisher publisher = publisherFor(SECRET);

    publisher.publish(TOPIC, DATA);

    Map<String, String> received = receivedRequests.poll(5, TimeUnit.SECONDS);
    assertThat(received).isNotNull();
    assertThat(received.get("method")).isEqualTo("POST");
    assertThat(received.get("contentType")).isEqualTo("application/x-www-form-urlencoded");
    assertThat(received.get("body"))
        .isEqualTo(
            "topic="
                + URLEncoder.encode(TOPIC, StandardCharsets.UTF_8)
                + "&data="
                + URLEncoder.encode(DATA, StandardCharsets.UTF_8));

    String token = received.get("authorization").replaceFirst("^Bearer ", "");
    assertThat(isValidHs256Jwt(token, SECRET)).isTrue();
    assertThat(decodeClaims(token)).isEqualTo("{\"mercure\":{\"publish\":[\"*\"]}}");
  }

  @Test
  @DisplayName("raises when the hub refuses the publish with a non-200 status")
  void raisesWhenTheHubRespondsWithANonTwoHundredStatus() {
    responseStatus = 500;
    HttpMercurePublisher publisher = publisherFor(SECRET);

    assertThatThrownBy(() -> publisher.publish(TOPIC, DATA))
        .isInstanceOf(MercurePublishException.class);
  }

  @Test
  @DisplayName("raises when the hub cannot be reached at all")
  void raisesWhenTheHubIsUnreachable() {
    HttpMercurePublisher publisher =
        new HttpMercurePublisher("http://127.0.0.1:1/.well-known/mercure", SECRET);

    assertThatThrownBy(() -> publisher.publish(TOPIC, DATA))
        .isInstanceOf(MercurePublishException.class);
  }

  // --- Request-building, no network: HttpMercurePublisher#buildRequest is package-private
  // exactly so these can inspect the request HttpClient would send without ever sending it. ---

  @Test
  @DisplayName("buildRequest: POSTs the hub URI with the JWT bearer and form content type")
  void buildRequestTargetsTheHubWithTheExpectedMethodAndHeaders() {
    HttpMercurePublisher publisher =
        new HttpMercurePublisher("http://mercure/.well-known/mercure", SECRET);

    HttpRequest request = publisher.buildRequest(TOPIC, DATA);

    assertThat(request.uri()).isEqualTo(URI.create("http://mercure/.well-known/mercure"));
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.headers().firstValue("Content-Type"))
        .contains("application/x-www-form-urlencoded");
    String token =
        request.headers().firstValue("Authorization").orElseThrow().replaceFirst("^Bearer ", "");
    assertThat(isValidHs256Jwt(token, SECRET)).isTrue();
    assertThat(decodeClaims(token)).isEqualTo("{\"mercure\":{\"publish\":[\"*\"]}}");
  }

  @Test
  @DisplayName("buildRequest: form-encodes topic and data as the request body")
  void buildRequestFormEncodesTopicAndData() throws InterruptedException {
    HttpMercurePublisher publisher =
        new HttpMercurePublisher("http://mercure/.well-known/mercure", SECRET);

    HttpRequest request = publisher.buildRequest(TOPIC, DATA);

    assertThat(bodyOf(request))
        .isEqualTo(
            "topic="
                + URLEncoder.encode(TOPIC, StandardCharsets.UTF_8)
                + "&data="
                + URLEncoder.encode(DATA, StandardCharsets.UTF_8));
  }

  /** Drains an {@link HttpRequest.BodyPublisher} without ever opening a connection. */
  private static String bodyOf(HttpRequest request) throws InterruptedException {
    HttpRequest.BodyPublisher bodyPublisher = request.bodyPublisher().orElseThrow();
    ByteArrayOutputStream collected = new ByteArrayOutputStream();
    CountDownLatch done = new CountDownLatch(1);
    bodyPublisher.subscribe(
        new Flow.Subscriber<>() {
          @Override
          public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
          }

          @Override
          public void onNext(ByteBuffer item) {
            byte[] chunk = new byte[item.remaining()];
            item.get(chunk);
            collected.writeBytes(chunk);
          }

          @Override
          public void onError(Throwable throwable) {
            done.countDown();
          }

          @Override
          public void onComplete() {
            done.countDown();
          }
        });
    done.await(5, TimeUnit.SECONDS);
    return collected.toString(StandardCharsets.UTF_8);
  }

  private HttpMercurePublisher publisherFor(String secret) {
    int port = server.getAddress().getPort();
    return new HttpMercurePublisher("http://127.0.0.1:" + port + "/.well-known/mercure", secret);
  }

  private static boolean isValidHs256Jwt(String token, String secret) {
    String[] parts = token.split("\\.", 3);
    if (parts.length != 3) {
      return false;
    }
    String signingInput = parts[0] + "." + parts[1];
    byte[] expectedSignature = hmacSha256(secret, signingInput);
    byte[] actualSignature = Base64.getUrlDecoder().decode(parts[2]);
    return java.util.Arrays.equals(expectedSignature, actualSignature);
  }

  private static String decodeClaims(String token) {
    String[] parts = token.split("\\.", 3);
    return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
  }

  private static byte[] hmacSha256(String secret, String signingInput) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
