package click.kivvi.infrastructure.waitlist;

import java.time.Duration;

/**
 * The rate-limiting seam {@link click.kivvi.application.waitlist.WaitlistSignupService} depends on.
 * The only production implementation is {@link JdbcSignupThrottle} (Postgres-backed); unit tests
 * substitute an in-memory fake, exactly as {@code EventDedupLedger} keeps {@code
 * EventIngestionService} testable without a database.
 */
public interface SignupThrottle {

  /**
   * Every waitlist bucket slides over the same one-hour window; only the allowance differs per
   * bucket. Declared here rather than in the caller so the hourly sweep that drops lapsed buckets
   * cannot drift away from the window the limiter actually enforces.
   */
  Duration WINDOW = Duration.ofHours(1);

  /**
   * Records one attempt against {@code bucketKey} and reports whether it still fits inside the
   * allowance.
   *
   * @return {@code true} when this attempt is within {@code limit} for the current {@link #WINDOW};
   *     {@code false} when the bucket is already exhausted.
   */
  boolean tryAcquire(String bucketKey, int limit);
}
