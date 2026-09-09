package click.kivvi.infrastructure.tracking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Idempotency ledger for {@code /collect}, backed by the frozen {@code event_dedup} table — ported
 * from {@code EventIngestion}'s PSR-cache-backed dedup check (DEV-5 mapping of the old {@code
 * cache_items} store).
 *
 * <p>{@code idempotency_hash} is SHA-256 of the tracking script's {@code idempotency_id}: the old
 * stack hashed with xxh128 (a non-JCA algorithm with no JDK-standard implementation), and pulling
 * in a dependency to reproduce that exact algorithm would buy nothing — the hash is an opaque dedup
 * key, never compared across stacks or surfaced to a caller, so any collision-resistant digest is
 * behaviourally identical. Recorded here rather than left implicit: see the worker report for
 * DEV-5.
 *
 * <p>{@link #claim} is one atomic upsert rather than a check-then-insert: a row already present
 * with a still-live {@code expires_at} loses the conflict (0 rows affected — duplicate); a missing
 * or already-expired row wins it (1 row affected — claimed), matching a PSR cache item's {@code
 * isHit()} reporting {@code false} once its TTL has lapsed, not merely once a sweep has removed it.
 */
@Component
public class EventDedupStore implements EventDedupLedger {

  private static final Duration TTL = Duration.ofHours(24);

  private static final String CLAIM_SQL =
      """
      INSERT INTO event_dedup (idempotency_hash, expires_at)
      VALUES (?, ?)
      ON CONFLICT (idempotency_hash) DO UPDATE
        SET expires_at = excluded.expires_at
        WHERE event_dedup.expires_at < ?
      """;

  private static final String DELETE_EXPIRED_SQL = "DELETE FROM event_dedup WHERE expires_at < ?";

  private final JdbcTemplate jdbcTemplate;

  public EventDedupStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * @return {@code true} when {@code idempotencyId} was not already claimed (and is now recorded,
   *     valid for {@link #TTL}); {@code false} when it is a live duplicate.
   */
  @Override
  public boolean claim(String idempotencyId) {
    Instant now = Instant.now();
    int rowsAffected =
        jdbcTemplate.update(
            CLAIM_SQL, hash(idempotencyId), Timestamp.from(now.plus(TTL)), Timestamp.from(now));
    return rowsAffected == 1;
  }

  /** The hourly cleanup hook: removes dedup rows whose TTL has lapsed. */
  public int deleteExpired() {
    return jdbcTemplate.update(DELETE_EXPIRED_SQL, Timestamp.from(Instant.now()));
  }

  private static String hash(String idempotencyId) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(idempotencyId.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable on this JVM.", exception);
    }
  }
}
