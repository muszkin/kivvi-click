package click.kivvi.infrastructure.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * {@link EventDedupStore} against a real Postgres {@code event_dedup} table — insert, conflict and
 * expiry, the three cases {@code EventIngestion}'s PSR-cache-backed dedup used to cover implicitly.
 */
@Testcontainers
@SpringBootTest
class EventDedupStoreIT {

  private static final String COUNT_BY_HASH_SQL =
      "select count(*) from event_dedup where idempotency_hash = ?";

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private EventDedupStore store;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("B18 a fresh idempotency id is claimed and recorded")
  void aFreshIdIsClaimed() {
    String id = "evt-fresh-" + System.nanoTime();

    boolean claimed = store.claim(id);

    assertThat(claimed).isTrue();
    Integer rows = jdbcTemplate.queryForObject(COUNT_BY_HASH_SQL, Integer.class, sha256Hex(id));
    assertThat(rows).isEqualTo(1);
  }

  @Test
  @DisplayName("B18 the same idempotency id is never claimed twice while it is still live")
  void aLiveDuplicateIsRefused() {
    String id = "evt-dup-" + System.nanoTime();

    assertThat(store.claim(id)).isTrue();
    assertThat(store.claim(id)).isFalse();
  }

  @Test
  @DisplayName("an already-expired row is reclaimable, matching a PSR cache item past its TTL")
  void anExpiredRowIsReclaimable() {
    String id = "evt-expired-" + System.nanoTime();
    insertRow(id, Instant.now().minusSeconds(60));

    boolean claimed = store.claim(id);

    assertThat(claimed).isTrue();
  }

  @Test
  @DisplayName("deleteExpired removes only rows whose TTL has lapsed")
  void deleteExpiredRemovesOnlyLapsedRows() {
    String expiredId = "evt-sweep-expired-" + System.nanoTime();
    String liveId = "evt-sweep-live-" + System.nanoTime();
    insertRow(expiredId, Instant.now().minusSeconds(60));
    store.claim(liveId);

    int removed = store.deleteExpired();

    assertThat(removed).isGreaterThanOrEqualTo(1);
    assertThat(jdbcTemplate.queryForObject(COUNT_BY_HASH_SQL, Integer.class, sha256Hex(expiredId)))
        .isZero();
    assertThat(jdbcTemplate.queryForObject(COUNT_BY_HASH_SQL, Integer.class, sha256Hex(liveId)))
        .isEqualTo(1);
  }

  private void insertRow(String idempotencyId, Instant expiresAt) {
    jdbcTemplate.update(
        "insert into event_dedup (idempotency_hash, expires_at) values (?, ?)",
        sha256Hex(idempotencyId),
        java.sql.Timestamp.from(expiresAt));
  }

  private static String sha256Hex(String idempotencyId) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(idempotencyId.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
