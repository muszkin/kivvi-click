package click.kivvi.infrastructure.session;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * {@link SessionRequestSerializationFilter} against plain servlet mocks — no Spring context, no
 * HTTP server. Covers the behaviours the registry test cannot: session-less pass-through, the 503
 * on a lock-acquisition timeout, and that the lock is always released (and the registry entry
 * evicted) once a request finishes, success or not. Sessions are simulated with a real {@code
 * SESSION} cookie, never {@code MockHttpServletRequest#setRequestedSessionId} — the filter reads
 * the cookie directly (see its class Javadoc for why), so a test built on the container's own
 * requested-session-id concept would not actually exercise that code path.
 */
class SessionRequestSerializationFilterTest {

  private static final String SESSION_COOKIE_NAME = "SESSION";
  private static final String SESSION_ID = "test-session";

  @Test
  void requestWithoutASessionIdPassesThroughUntouched() throws Exception {
    SessionLockRegistry registry = new SessionLockRegistry();
    SessionRequestSerializationFilter filter = newFilter(registry, 1_000, 0, true);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/collect");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean();
    FilterChain chain = (req, res) -> chainInvoked.set(true);

    filter.doFilterInternal(request, response, chain);

    assertThat(chainInvoked).isTrue();
    assertThat(registry.trackedSessionCount()).isZero();
  }

  @Test
  void successfulRequestReleasesTheLockAndEvictsTheRegistryEntry() throws Exception {
    SessionLockRegistry registry = new SessionLockRegistry();
    SessionRequestSerializationFilter filter = newFilter(registry, 1_000, 0, true);
    MockHttpServletRequest request = requestWithSessionCookie("GET", "/api/v1/pl/shell");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean();
    FilterChain chain = (req, res) -> chainInvoked.set(true);

    filter.doFilterInternal(request, response, chain);

    assertThat(chainInvoked).isTrue();
    assertThat(registry.trackedSessionCount())
        .as("the lock must be released once the request completes")
        .isZero();
  }

  @Test
  void lockDisabledStillLetsTheRequestThroughWithoutSerializing() throws Exception {
    SessionLockRegistry registry = new SessionLockRegistry();
    SessionRequestSerializationFilter filter = newFilter(registry, 1_000, 0, false);
    MockHttpServletRequest request = requestWithSessionCookie("GET", "/api/v1/pl/shell");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean chainInvoked = new AtomicBoolean();
    FilterChain chain = (req, res) -> chainInvoked.set(true);

    filter.doFilterInternal(request, response, chain);

    assertThat(chainInvoked).isTrue();
    assertThat(registry.trackedSessionCount())
        .as("with the lock disabled, no entry should ever be created")
        .isZero();
  }

  @Test
  void exceedingTheLockTimeoutRespondsWith503AndNeverInvokesTheChain() throws Exception {
    SessionLockRegistry registry = new SessionLockRegistry();
    ReentrantLock heldByAnotherRequest = registry.acquire(SESSION_ID);
    heldByAnotherRequest.lock();
    ExecutorService pool = Executors.newSingleThreadExecutor();
    try {
      SessionRequestSerializationFilter filter = newFilter(registry, 100, 0, true);
      MockHttpServletRequest request = requestWithSessionCookie("POST", "/preferences/theme");
      MockHttpServletResponse response = new MockHttpServletResponse();
      AtomicBoolean chainInvoked = new AtomicBoolean();
      FilterChain chain = (req, res) -> chainInvoked.set(true);

      pool.submit(
              () -> {
                filter.doFilterInternal(request, response, chain);
                return null;
              })
          .get(5, TimeUnit.SECONDS);

      assertThat(response.getStatus()).isEqualTo(503);
      assertThat(chainInvoked)
          .as("the timed-out request must never reach the rest of the chain")
          .isFalse();
    } finally {
      heldByAnotherRequest.unlock();
      registry.release(SESSION_ID);
      pool.shutdown();
    }
  }

  @Test
  void testDelayOnlyAppliesToTheThemePostAndOnlyWhenConfigured() throws Exception {
    SessionLockRegistry registry = new SessionLockRegistry();
    SessionRequestSerializationFilter filter = newFilter(registry, 1_000, 200, true);
    MockHttpServletRequest themePost = requestWithSessionCookie("POST", "/preferences/theme");
    MockHttpServletRequest shellGet = requestWithSessionCookie("GET", "/api/v1/pl/shell");
    FilterChain chain = (req, res) -> {};

    long shellElapsedMillis = timeFilterInvocation(filter, shellGet, chain);
    assertThat(shellElapsedMillis)
        .as("only the theme POST is slowed down, never other requests")
        .isLessThan(200);

    long themeElapsedMillis = timeFilterInvocation(filter, themePost, chain);
    assertThat(themeElapsedMillis)
        .as("the configured test delay must actually elapse before the chain runs")
        .isGreaterThanOrEqualTo(200);
  }

  private SessionRequestSerializationFilter newFilter(
      SessionLockRegistry registry,
      long lockTimeoutMillis,
      long testDelayMillis,
      boolean lockEnabled) {
    return new SessionRequestSerializationFilter(
        registry, lockTimeoutMillis, testDelayMillis, lockEnabled, SESSION_COOKIE_NAME);
  }

  private long timeFilterInvocation(
      SessionRequestSerializationFilter filter, MockHttpServletRequest request, FilterChain chain)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    long start = System.nanoTime();
    filter.doFilterInternal(request, response, chain);
    return (System.nanoTime() - start) / 1_000_000;
  }

  private MockHttpServletRequest requestWithSessionCookie(String method, String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setCookies(new Cookie(SESSION_COOKIE_NAME, SESSION_ID));
    return request;
  }
}
