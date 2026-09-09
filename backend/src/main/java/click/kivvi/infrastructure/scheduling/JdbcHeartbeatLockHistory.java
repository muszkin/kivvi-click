package click.kivvi.infrastructure.scheduling;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reads {@code shedlock.locked_at} for {@value HeartbeatJob#LOCK_NAME} — the same row ShedLock's
 * {@code JdbcTemplateLockProvider} writes on every lock acquisition (B20: a Postgres-backed round
 * trip, not an in-memory guess about whether a run is due).
 */
@Component
class JdbcHeartbeatLockHistory implements HeartbeatLockHistory {

  private static final String SELECT_LOCKED_AT = "select locked_at from shedlock where name = ?";

  private final JdbcTemplate jdbcTemplate;

  JdbcHeartbeatLockHistory(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public Optional<Instant> lastLockedAt() {
    try {
      Timestamp lockedAt =
          jdbcTemplate.queryForObject(SELECT_LOCKED_AT, Timestamp.class, HeartbeatJob.LOCK_NAME);
      return Optional.ofNullable(lockedAt).map(Timestamp::toInstant);
    } catch (EmptyResultDataAccessException noLockRowYet) {
      return Optional.empty();
    }
  }
}
