package click.kivvi.infrastructure.mercure;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publishes to the Mercure hub over plain HTTP inside the compose network — ported from the old
 * stack's {@code symfony/mercure-bundle} publish call (POSTs {@code topic}/{@code data} as {@code
 * application/x-www-form-urlencoded} to {@code /.well-known/mercure} with a publisher JWT), which
 * itself published over plain HTTP to {@code http://php/.well-known/mercure} (compose.yaml).
 *
 * <p>That worked on the old stack because {@code php}'s own Caddy site list included a second,
 * explicit-port address ({@code php:80}) alongside the public auto-HTTPS one — Caddy only enables
 * automatic HTTPS (and its HTTP→HTTPS redirect) for a site address with no explicit port. {@code
 * compose.yaml} reproduces the same trick for the {@code mercure} service's {@code SERVER_NAME}
 * (adding {@code mercure:80}), so {@link #hub} here is a plain {@code http://} address the hub
 * serves without any TLS involved — no certificate to trust, no SNI to reason about, no dependency
 * on this container's TLS configuration at all.
 *
 * <p>A failed publish (hub unreachable, non-2xx, timeout) is never swallowed: it propagates as a
 * {@link MercurePublishException} (unchecked), which {@link click.kivvi.web.CollectController} does
 * not catch — matching the old stack exactly, where {@code HubInterface::publish} throwing turns an
 * accepted {@code /collect} call into an uncaught exception (a 500), because {@code
 * EventIngestionController} never caught anything but {@code InvalidEventPayload} either.
 */
@Component
public class HttpMercurePublisher implements MercurePublisher {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

  private final URI hub;
  private final MercureJwt jwt;
  private final HttpClient httpClient;

  public HttpMercurePublisher(
      @Value("${kivvi.mercure.url}") String hubUrl,
      @Value("${kivvi.mercure.jwt-secret}") String jwtSecret) {
    this.hub = URI.create(hubUrl);
    this.jwt = new MercureJwt(jwtSecret);
    // HTTP_1_1, not the default HTTP_2: this call never benefits from HTTP/2 (one POST, no
    // multiplexing, no server push), and pinning the protocol removes HttpClient's h2c
    // (cleartext upgrade) negotiation from the picture entirely — one less variable when
    // something about this hop needs debugging later.
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .version(HttpClient.Version.HTTP_1_1)
            .build();
  }

  @Override
  public void publish(String topic, String data) {
    HttpRequest request = buildRequest(topic, data);
    try {
      HttpResponse<Void> response =
          httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() != 200) {
        throw new MercurePublishException(
            "Mercure hub responded with HTTP " + response.statusCode() + " for topic " + topic,
            null);
      }
    } catch (IOException exception) {
      throw new MercurePublishException(
          "Failed to publish to Mercure hub topic " + topic, exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new MercurePublishException(
          "Interrupted while publishing to Mercure hub topic " + topic, exception);
    }
  }

  /**
   * Package-private so {@code HttpMercurePublisherTest} can assert on the request shape (URI,
   * method, headers, form body) without ever sending it — the JWT signature, topic/data
   * form-encoding and Authorization header, covered with no network involved.
   */
  HttpRequest buildRequest(String topic, String data) {
    return HttpRequest.newBuilder(hub)
        .timeout(REQUEST_TIMEOUT)
        .header("Authorization", "Bearer " + jwt.token())
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(formBody(topic, data), StandardCharsets.UTF_8))
        .build();
  }

  private static String formBody(String topic, String data) {
    return "topic=" + urlEncode(topic) + "&data=" + urlEncode(data);
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
