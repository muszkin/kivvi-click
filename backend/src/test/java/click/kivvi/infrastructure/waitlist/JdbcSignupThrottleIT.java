package click.kivvi.infrastructure.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * {@link JdbcSignupThrottle} against a real Postgres {@code waitlist_throttle} table. The limiter
 * is one {@code INSERT ... ON CONFLICT ... RETURNING} whose whole behaviour lives in SQL, so — as
 * with {@code EventDedupStoreIT} — only the database can actually prove it.
 *
 * <p>Window expiry is exercised by back-dating the stored {@code window_started_at} rather than by
 * waiting an hour or injecting a clock: the lapse decision is made by the statement's own {@code
 * CASE} comparison against that column, so a back-dated row reproduces it exactly.
 */
@Testcontainers
@SpringBootTest
// This test asserts on how many rows its own delete removed, and the hourly job sweeps
// the very same table. A @Scheduled fixedRate task fires the moment its context is
// ready, so leaving it on lets the startup sweep land between the setup and the
// assertion and carry the row off first. What the job itself does is covered by
// ExpiredRowsCleanupJobTest.
@TestPropertySource(properties = "kivvi.cleanup.enabled=false")
class JdbcSignupThrottleIT {

  private static final Duration WINDOW = SignupThrottle.WINDOW;
  private static final int LIMIT = 3;

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private JdbcSignupThrottle throttle;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("every attempt up to the limit is allowed and the one after it is refused")
  void theLimitIsEnforcedOnTheAttemptAfterIt() {
    String bucket = uniqueBucket("limit");

    for (int attempt = 1; attempt <= LIMIT; attempt++) {
      assertThat(throttle.tryAcquire(bucket, LIMIT))
          .describedAs("attempt %d of %d", attempt, LIMIT)
          .isTrue();
    }

    assertThat(throttle.tryAcquire(bucket, LIMIT)).isFalse();
  }

  @Test
  @DisplayName("a lapsed window restarts the count from one")
  void aLapsedWindowRestartsTheCount() {
    String bucket = uniqueBucket("lapsed");
    exhaust(bucket);
    backdateWindow(bucket, Instant.now().minus(WINDOW).minusSeconds(60));

    assertThat(throttle.tryAcquire(bucket, LIMIT)).isTrue();
    assertThat(hitsFor(bucket)).isEqualTo(1);
  }

  @Test
  @DisplayName("two buckets count independently: exhausting one leaves the other untouched")
  void bucketsDoNotShareTheirAllowance() {
    String exhausted = uniqueBucket("ip");
    String untouched = uniqueBucket("email");
    exhaust(exhausted);

    assertThat(throttle.tryAcquire(exhausted, LIMIT)).isFalse();
    assertThat(throttle.tryAcquire(untouched, LIMIT)).isTrue();
  }

  @Test
  @DisplayName("deleteLapsedWindows removes only buckets whose window has already lapsed")
  void deleteLapsedWindowsRemovesOnlyLapsedBuckets() {
    String lapsed = uniqueBucket("sweep-lapsed");
    String live = uniqueBucket("sweep-live");
    throttle.tryAcquire(lapsed, LIMIT);
    throttle.tryAcquire(live, LIMIT);
    backdateWindow(lapsed, Instant.now().minus(WINDOW).minusSeconds(60));

    int removed = throttle.deleteLapsedWindows();

    assertThat(removed).isGreaterThanOrEqualTo(1);
    assertThat(countFor(lapsed)).isZero();
    assertThat(countFor(live)).isEqualTo(1);
  }

  private void exhaust(String bucket) {
    for (int attempt = 0; attempt < LIMIT; attempt++) {
      throttle.tryAcquire(bucket, LIMIT);
    }
  }

  private void backdateWindow(String bucket, Instant windowStartedAt) {
    jdbcTemplate.update(
        "update waitlist_throttle set window_started_at = ? where bucket_key = ?",
        Timestamp.from(windowStartedAt),
        bucket);
  }

  private Integer hitsFor(String bucket) {
    return jdbcTemplate.queryForObject(
        "select hits from waitlist_throttle where bucket_key = ?", Integer.class, bucket);
  }

  private Integer countFor(String bucket) {
    return jdbcTemplate.queryForObject(
        "select count(*) from waitlist_throttle where bucket_key = ?", Integer.class, bucket);
  }

  private static String uniqueBucket(String prefix) {
    return prefix + ":" + System.nanoTime();
  }
}
