package click.kivvi.infrastructure.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Fast, Spring-free coverage of {@link EventDedupCleanupJob#sweep()} — mirrors {@code
 * HeartbeatJobTest}'s pattern; the real scheduler/lock wiring (piggybacked on wave-1's {@code
 * SchedulingConfig}, untouched here) has no dedicated end-to-end test of its own, since this slice
 * introduces no new scheduling infrastructure to prove wired up.
 */
class EventDedupCleanupJobTest {

  private Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void attachAppender() {
    logger = (Logger) LoggerFactory.getLogger(EventDedupCleanupJob.class);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(appender);
    appender.stop();
  }

  @Test
  @DisplayName("sweep() delegates to the dedup store and logs the removed row count")
  void sweepDelegatesAndLogsTheRemovedCount() {
    EventDedupStore store =
        new EventDedupStore(null) {
          @Override
          public int deleteExpired() {
            return 3;
          }
        };

    new EventDedupCleanupJob(store).sweep();

    assertThat(appender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(tuple(Level.INFO, "Event dedup cleanup removed 3 expired row(s)."));
  }
}
