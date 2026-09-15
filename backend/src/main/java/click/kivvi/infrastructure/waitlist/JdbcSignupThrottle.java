package click.kivvi.infrastructure.waitlist;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Sliding-window request counter on {@code waitlist_throttle}. Postgres backs everything in this
 * stack, so the limiter is a table rather than a Redis counter or a new library (CODE_REVIEW.md,
 * "Architecture") — and being a table, it survives a restart of the single {@code api} process
 * instead of handing an abuser a fresh quota.
 *
 * <p>One statement does the whole job. The {@code CASE} pairs decide, inside the database, whether
 * the stored window has lapsed — restarting the count at 1 and stamping a new window — or is still
 * live, in which case the hit count grows by one; {@code RETURNING hits} then hands back the number
 * this very attempt was assigned. There is deliberately no read before the write: a
 * check-then-increment pair lets two simultaneous requests both observe "one below the limit" and
 * both proceed, which is precisely the burst a rate limiter exists to stop.
 */
@Component
public class JdbcSignupThrottle implements SignupThrottle {

  private static final String CONSUME_SQL =
      """
      INSERT INTO waitlist_throttle (bucket_key, window_started_at, hits)
      VALUES (?, ?, 1)
      ON CONFLICT (bucket_key) DO UPDATE
        SET hits = CASE
              WHEN waitlist_throttle.window_started_at <= ? THEN 1
              ELSE waitlist_throttle.hits + 1
            END,
            window_started_at = CASE
              WHEN waitlist_throttle.window_started_at <= ? THEN excluded.window_started_at
              ELSE waitlist_throttle.window_started_at
            END
      RETURNING hits
      """;

  private static final String DELETE_LAPSED_SQL =
      "DELETE FROM waitlist_throttle WHERE window_started_at <= ?";

  private final JdbcTemplate jdbcTemplate;

  public JdbcSignupThrottle(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public boolean tryAcquire(String bucketKey, int limit) {
    Instant now = Instant.now();
    Timestamp windowCutoff = Timestamp.from(now.minus(WINDOW));
    Integer hits =
        jdbcTemplate.queryForObject(
            CONSUME_SQL, Integer.class, bucketKey, Timestamp.from(now), windowCutoff, windowCutoff);
    return hits != null && hits <= limit;
  }

  /**
   * The hourly cleanup hook: drops buckets whose window has lapsed, so a table keyed by client IP
   * and address cannot grow without bound. Removing a lapsed row changes no decision — {@link
   * #tryAcquire} already restarts the count for a lapsed window on its own.
   */
  public int deleteLapsedWindows() {
    return jdbcTemplate.update(DELETE_LAPSED_SQL, Timestamp.from(Instant.now().minus(WINDOW)));
  }
}
