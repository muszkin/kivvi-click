package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.application.ShellView;
import click.kivvi.web.dto.ThemeResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * RED-proof only, kept {@link Disabled} on purpose: this is the exact scenario from {@link
 * SessionRequestSerializationIT#concurrentReloadSeesTheCommittedThemeNotTheStaleOne()}, run against
 * a context with {@code kivvi.session-lock.enabled=false} — i.e. with the repair-1 fix turned off
 * but the same deterministic test-only POST delay still in place. It fails (observes the stale
 * pre-POST theme) exactly as {@code navigation.spec.ts}'s "theme toggle survives a reload" did
 * before this repair, which is the proof this repair's regression test actually detects the bug it
 * claims to fix. It stays {@code @Disabled} so CI never depends on a deliberately-broken
 * configuration; remove the annotation (or run this class directly) to reproduce — captured once as
 * evidence/repair-1/red-proof.log.
 */
@Disabled("RED proof only — see class Javadoc; enable manually to reproduce the pre-fix race")
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "kivvi.testing.preferences-theme-post-delay-ms=400",
      "kivvi.session-lock.enabled=false"
    })
@AutoConfigureTestRestTemplate
class SessionRequestSerializationRedProofIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName(
      "without the lock, a reload racing an in-flight theme POST observes the stale pre-POST"
          + " theme instead of the one just written")
  void withoutTheLockAReloadObservesTheStaleTheme() throws Exception {
    // ShellController's GET never forces a session into existence (request.getSession(false)),
    // so the session — and its cookie — must come from an actual write first.
    HttpHeaders baselineHeaders = new HttpHeaders();
    baselineHeaders.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<ThemeResponse> baseline =
        restTemplate.postForEntity(
            "/preferences/theme",
            new HttpEntity<>("{\"theme\":\"light\"}", baselineHeaders),
            ThemeResponse.class);
    String sessionCookie = baseline.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    String staleTheme = baseline.getBody().theme();
    String newTheme = "dark".equals(staleTheme) ? "light" : "dark";

    HttpHeaders cookieHeader = new HttpHeaders();
    cookieHeader.add(HttpHeaders.COOKIE, sessionCookie);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      CountDownLatch postAboutToFire = new CountDownLatch(1);
      Future<ResponseEntity<ThemeResponse>> postFuture =
          pool.submit(
              () -> {
                HttpHeaders postHeaders = new HttpHeaders();
                postHeaders.setContentType(MediaType.APPLICATION_JSON);
                postHeaders.add(HttpHeaders.COOKIE, sessionCookie);
                postAboutToFire.countDown();
                return restTemplate.postForEntity(
                    "/preferences/theme",
                    new HttpEntity<>("{\"theme\":\"" + newTheme + "\"}", postHeaders),
                    ThemeResponse.class);
              });

      postAboutToFire.await(2, TimeUnit.SECONDS);
      Thread.sleep(20);
      ResponseEntity<ShellView> reload =
          restTemplate.exchange(
              "/api/v1/pl/shell", HttpMethod.GET, new HttpEntity<>(cookieHeader), ShellView.class);

      postFuture.get(5, TimeUnit.SECONDS);

      // With the lock disabled this is expected to observe staleTheme, not newTheme — proving
      // the race exists without repair-1's fix.
      assertThat(reload.getBody().theme()).isEqualTo(newTheme);
    } finally {
      pool.shutdown();
    }
  }
}
