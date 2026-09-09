package click.kivvi.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.SimpleTriggerContext;

/**
 * Unit coverage (a mutable {@link Clock}, no Spring context, no database) for the missed-run
 * semantics contract.md describes: {@code src/Schedule.php}'s {@code
 * stateful()->processOnlyLastMissedRun(true)} reproduced on top of the {@code shedlock} table's
 * {@code locked_at} column.
 */
class HeartbeatTriggerTest {

  private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
  private static final Duration ONE_HOUR = Duration.ofHours(1);

  @Test
  @DisplayName("B20/B33 with no prior lock row, the first-ever tick fires immediately")
  void firstEverTickFiresImmediately() {
    HeartbeatTrigger trigger = new HeartbeatTrigger(stubLockHistory(Optional.empty()), ONE_HOUR);

    Instant next = trigger.nextExecution(freshProcessContext());

    assertThat(next).isEqualTo(NOW);
  }

  @Test
  @DisplayName(
      "B33 a routine restart minutes after the last tick waits out the remainder of the "
          + "interval — no extra catch-up run")
  void restartShortlyAfterLastTickWaitsUntilDue() {
    Instant lastLockedAt = NOW.minus(Duration.ofMinutes(10));
    HeartbeatTrigger trigger =
        new HeartbeatTrigger(stubLockHistory(Optional.of(lastLockedAt)), ONE_HOUR);

    Instant next = trigger.nextExecution(freshProcessContext());

    assertThat(next).isEqualTo(lastLockedAt.plus(ONE_HOUR)).isAfter(NOW);
  }

  @Test
  @DisplayName(
      "B33 a restart after downtime longer than the interval fires exactly one catch-up run, "
          + "immediately")
  void restartAfterDowntimeCatchesUpOnce() {
    Instant lastLockedAt = NOW.minus(Duration.ofHours(3));
    HeartbeatTrigger trigger =
        new HeartbeatTrigger(stubLockHistory(Optional.of(lastLockedAt)), ONE_HOUR);

    Instant next = trigger.nextExecution(freshProcessContext());

    assertThat(next).isEqualTo(NOW);
  }

  @Test
  @DisplayName(
      "B33 once the process has completed a tick, the next one is due one interval after that "
          + "completion — the lock table is not consulted again")
  void subsequentTicksAnchorOnTheProcesssOwnLastCompletion() {
    HeartbeatLockHistory neverConsulted =
        () -> {
          throw new AssertionError(
              "lastLockedAt() must not be called once lastCompletion() is set");
        };
    HeartbeatTrigger trigger = new HeartbeatTrigger(neverConsulted, ONE_HOUR);
    Instant lastCompletion = NOW.minus(Duration.ofMinutes(45));
    SimpleTriggerContext context = new SimpleTriggerContext(Clock.fixed(NOW, ZoneOffset.UTC));
    context.update(lastCompletion, lastCompletion, lastCompletion);

    Instant next = trigger.nextExecution(context);

    assertThat(next).isEqualTo(lastCompletion.plus(ONE_HOUR));
  }

  private static SimpleTriggerContext freshProcessContext() {
    return new SimpleTriggerContext(Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static HeartbeatLockHistory stubLockHistory(Optional<Instant> lastLockedAt) {
    return () -> lastLockedAt;
  }
}
