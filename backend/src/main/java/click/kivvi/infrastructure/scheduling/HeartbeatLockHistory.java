package click.kivvi.infrastructure.scheduling;

import java.time.Instant;
import java.util.Optional;

/**
 * When the {@value HeartbeatJob#LOCK_NAME} ShedLock last acquired its lock — i.e. when {@link
 * HeartbeatJob#tick()} last actually ran — so {@link HeartbeatTrigger} can decide, on the first
 * scheduling decision after a process start, whether a run is already due (a restart after downtime
 * longer than the interval) or should wait out the remainder of the interval (a routine restart).
 * Kept as its own interface, separate from {@link JdbcHeartbeatLockHistory}, so {@link
 * HeartbeatTrigger} can be unit-tested with a stub instead of a real database.
 */
public interface HeartbeatLockHistory {

  Optional<Instant> lastLockedAt();
}
