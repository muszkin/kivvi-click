package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.application.ShellView;
import click.kivvi.infrastructure.session.SessionRequestSerializationFilter;
import click.kivvi.web.dto.ThemeResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.session.autoconfigure.SessionProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Repair-1 (wave-3, shell-preferences): proves the PHP-session-lock parity fix for the "theme
 * toggle survives a reload" flake — {@code navigation.spec.ts} intermittently observed the
 * pre-{@code POST} theme after {@code page.reload()} because Spring Session JDBC lets a concurrent
 * {@code GET} read the session while a {@code POST} to the same session is still being processed.
 * {@code kivvi.testing.preferences-theme-post-delay-ms} (test-only; 0, a no-op, in every non-test
 * profile) widens that race window deterministically instead of relying on raw thread scheduling
 * luck.
 */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "kivvi.testing.preferences-theme-post-delay-ms=400")
@AutoConfigureTestRestTemplate
class SessionRequestSerializationIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private SessionProperties sessionProperties;

  @Autowired
  private FilterRegistrationBean<SessionRequestSerializationFilter>
      sessionRequestSerializationFilterRegistration;

  @Test
  @DisplayName(
      "the session-lock filter is registered at a lower order than SessionRepositoryFilter, so"
          + " it wraps outside it and its own finally runs after Spring Session's commit")
  void sessionLockFilterWrapsOutsideSessionRepositoryFilter() {
    int lockFilterOrder = sessionRequestSerializationFilterRegistration.getOrder();
    int sessionRepositoryFilterOrder = sessionProperties.getServlet().getFilterOrder();

    assertThat(lockFilterOrder)
        .as(
            "session-lock filter order=%d must be lower than SessionRepositoryFilter order=%d",
            lockFilterOrder, sessionRepositoryFilterOrder)
        .isLessThan(sessionRepositoryFilterOrder);
  }

  @Test
  @DisplayName(
      "a reload's GET blocks until an in-flight theme POST on the same session commits, and then"
          + " sees the new theme instead of the stale pre-POST one")
  void concurrentReloadSeesTheCommittedThemeNotTheStaleOne() throws Exception {
    // ShellController's GET never forces a session into existence (request.getSession(false)),
    // so the session — and its cookie — must come from an actual write first, exactly like a
    // real visitor who has already toggled a preference once before reloading.
    ResponseEntity<ThemeResponse> baseline =
        restTemplate.postForEntity(
            "/preferences/theme",
            new HttpEntity<>("{\"theme\":\"light\"}", jsonHeaders()),
            ThemeResponse.class);
    String sessionCookie = baseline.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertThat(sessionCookie).as("Spring Session issues a SESSION cookie").startsWith("SESSION=");
    String newTheme = "dark".equals(baseline.getBody().theme()) ? "light" : "dark";

    HttpHeaders cookieHeader = new HttpHeaders();
    cookieHeader.add(HttpHeaders.COOKIE, sessionCookie);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      CountDownLatch postAboutToFire = new CountDownLatch(1);
      Future<ResponseEntity<ThemeResponse>> postFuture =
          pool.submit(
              () -> {
                HttpHeaders postHeaders = jsonHeaders();
                postHeaders.add(HttpHeaders.COOKIE, sessionCookie);
                postAboutToFire.countDown();
                return restTemplate.postForEntity(
                    "/preferences/theme",
                    new HttpEntity<>("{\"theme\":\"" + newTheme + "\"}", postHeaders),
                    ThemeResponse.class);
              });

      // Mirrors the SPA's own race: page.reload() fires right after the click, while the
      // fire-and-forget POST is still in flight — not before it starts.
      postAboutToFire.await(2, TimeUnit.SECONDS);
      Thread.sleep(20);
      ResponseEntity<ShellView> reload =
          restTemplate.exchange(
              "/api/v1/pl/shell", HttpMethod.GET, new HttpEntity<>(cookieHeader), ShellView.class);

      ResponseEntity<ThemeResponse> post = postFuture.get(5, TimeUnit.SECONDS);
      assertThat(post.getBody().theme()).isEqualTo(newTheme);
      assertThat(reload.getBody())
          .as("the reload must observe the just-committed theme, never the pre-POST value")
          .extracting(ShellView::theme)
          .isEqualTo(newTheme);
    } finally {
      pool.shutdown();
    }
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
