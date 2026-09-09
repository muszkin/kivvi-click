package click.kivvi.infrastructure.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * PHP-session-lock parity: PHP's native session handler locks the session file for the duration of
 * a request, so a reload's {@code GET} blocks until an in-flight {@code POST} to the same session
 * has finished writing it. Spring Session JDBC has no equivalent — two concurrent requests each
 * load their own copy of the session from Postgres, so a {@code GET} that lands while a {@code
 * POST} is still being handled can read the session as it was *before* that {@code POST} committed.
 * This filter reproduces the PHP behaviour by serializing requests that carry the same session id,
 * one at a time, for as long as the rest of the filter chain (including Spring Session's own
 * commit) takes to run.
 *
 * <p>Registered (see {@code SessionRequestSerializationConfig}) at an order lower than {@code
 * SessionRepositoryFilter}'s ({@code Ordered.HIGHEST_PRECEDENCE + 50}, from Spring Boot's {@code
 * SessionProperties.Servlet#filterOrder}), i.e. <em>outside</em> it: on the way in, this filter's
 * {@link #doFilterInternal} runs first and acquires the lock before calling {@code
 * chain.doFilter(...)}, which is what actually invokes {@code SessionRepositoryFilter}; on the way
 * out, {@code SessionRepositoryFilter}'s own {@code finally} — where it saves the session back to
 * {@code spring_session} — therefore always completes before control returns here, so this filter's
 * own {@code finally} (which releases the lock) never runs until that commit is done. A request
 * without a session cookie (e.g. {@code /collect}, static assets — this server has no SSE endpoint)
 * passes straight through untouched.
 *
 * <p>The session id is read by looking up the {@link #sessionCookieName} cookie directly on {@link
 * HttpServletRequest#getCookies()} — <strong>not</strong> via {@link
 * HttpServletRequest#getRequestedSessionId()} or {@link HttpServletRequest#getSession(boolean)}.
 * Both of those are answered by the servlet container's own (Tomcat) session support, which knows
 * only its own session cookie name ({@code JSESSIONID} by default) — a name this application never
 * uses, since {@code server.servlet.session.cookie.name} is set to {@code SESSION}. At this
 * filter's position, ahead of Spring Session's own filter, the request has not yet been wrapped by
 * Spring Session's {@code SessionRepositoryRequestWrapper} either, so there is no wrapper to defer
 * to. {@link HttpServletRequest#getCookies()} is plain, framework-agnostic cookie parsing done by
 * the container regardless of session mechanism, so it is the one API that actually sees the cookie
 * Spring Session issues. The raw cookie value (Spring Session's base64-encoded id, never decoded
 * here) is used verbatim as the lock key — it only needs to be a stable, unique-per-session string,
 * not the decoded {@code spring_session.session_id} value itself.
 *
 * <p>Lock acquisition is bounded by {@link #lockTimeoutMillis} so a stuck request can never wedge
 * every other request to the same session forever; exceeding it fails the waiting request with
 * {@code 503 Service Unavailable} rather than deadlocking.
 */
public class SessionRequestSerializationFilter extends OncePerRequestFilter {

  /** Named per the repair packet: "Timeout the lock acquisition (named constant, e.g. 30 s)". */
  static final long DEFAULT_LOCK_TIMEOUT_MS = 30_000L;

  /**
   * Test-only knob: the one write endpoint this repair's IT slows down to force the race window
   * open deterministically. Never matched when {@link #testDelayMillis} is 0 (the production
   * default), so this has no effect outside a test that explicitly sets {@code
   * kivvi.testing.preferences-theme-post-delay-ms}.
   */
  private static final String TEST_DELAY_TARGET_METHOD = "POST";

  private static final String TEST_DELAY_TARGET_PATH = "/preferences/theme";

  private final SessionLockRegistry lockRegistry;
  private final long lockTimeoutMillis;
  private final long testDelayMillis;
  private final boolean lockEnabled;
  private final String sessionCookieName;

  public SessionRequestSerializationFilter(
      SessionLockRegistry lockRegistry,
      long lockTimeoutMillis,
      long testDelayMillis,
      boolean lockEnabled,
      String sessionCookieName) {
    this.lockRegistry = lockRegistry;
    this.lockTimeoutMillis = lockTimeoutMillis;
    this.testDelayMillis = testDelayMillis;
    this.lockEnabled = lockEnabled;
    this.sessionCookieName = sessionCookieName;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String sessionId = resolveSessionId(request);
    if (sessionId == null) {
      chain.doFilter(request, response);
      return;
    }

    if (!lockEnabled) {
      // No lock held here (the whole point of this branch is proving the race exists without
      // one), so the test delay is applied directly — same relative timing as the locked path
      // below, just with nothing serializing a concurrent request against it.
      delayIfConfiguredForTest(request);
      chain.doFilter(request, response);
      return;
    }

    ReentrantLock lock = lockRegistry.acquire(sessionId);
    try {
      boolean acquired;
      try {
        acquired = lock.tryLock(lockTimeoutMillis, TimeUnit.MILLISECONDS);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        acquired = false;
      }
      if (!acquired) {
        response.sendError(
            HttpServletResponse.SC_SERVICE_UNAVAILABLE,
            "Timed out waiting for another request on this session to finish");
        return;
      }
      try {
        // Deliberately inside the locked section: this is what actually widens the race
        // window for SessionRequestSerializationIT — a delay outside the lock would just slow
        // the request down without ever making a concurrent request on the same session wait.
        delayIfConfiguredForTest(request);
        chain.doFilter(request, response);
      } finally {
        lock.unlock();
      }
    } finally {
      lockRegistry.release(sessionId);
    }
  }

  private String resolveSessionId(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (sessionCookieName.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }

  private void delayIfConfiguredForTest(HttpServletRequest request) {
    if (testDelayMillis <= 0) {
      return;
    }
    boolean isTargetRequest =
        TEST_DELAY_TARGET_METHOD.equals(request.getMethod())
            && TEST_DELAY_TARGET_PATH.equals(request.getRequestURI());
    if (!isTargetRequest) {
      return;
    }
    try {
      Thread.sleep(testDelayMillis);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
