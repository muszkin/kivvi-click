package click.kivvi.infrastructure.scheduling;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

/**
 * Reproduces {@code src/Schedule.php}'s {@code stateful($cache)->processOnlyLastMissedRun(true)}
 * semantics on top of the {@code shedlock} table (contract.md "Missed-run semantics"): after a
 * process restart, the next tick is due exactly {@link #interval} after the <em>last recorded
 * lock</em> — not immediately and not one tick per hour missed. A routine restart minutes after the
 * previous tick waits out the remainder of the hour; a restart after downtime longer than {@link
 * #interval} fires once, immediately, then resumes the normal cadence.
 *
 * <p>That lookup only happens once per process: {@link TriggerContext#lastCompletion()} is {@code
 * null} only for the very first scheduling decision after a JVM start (Spring has no earlier
 * completion to report), so every later decision in the same process anchors off it directly and
 * needs no further database read.
 */
public final class HeartbeatTrigger implements Trigger {

  private final HeartbeatLockHistory lockHistory;
  private final Duration interval;

  public HeartbeatTrigger(HeartbeatLockHistory lockHistory, Duration interval) {
    this.lockHistory = lockHistory;
    this.interval = interval;
  }

  @Override
  public Instant nextExecution(TriggerContext triggerContext) {
    Instant lastCompletion = triggerContext.lastCompletion();
    if (lastCompletion != null) {
      return lastCompletion.plus(interval);
    }
    return firstExecutionAfterProcessStart(triggerContext.getClock());
  }

  private Instant firstExecutionAfterProcessStart(Clock clock) {
    Instant now = clock.instant();
    return lockHistory
        .lastLockedAt()
        .map(lastLockedAt -> lastLockedAt.plus(interval))
        .filter(dueAt -> dueAt.isAfter(now))
        .orElse(now);
  }
}
