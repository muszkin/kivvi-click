package click.kivvi.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import click.kivvi.infrastructure.tracking.EventDedupStore;
import click.kivvi.infrastructure.waitlist.JdbcSignupThrottle;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Fast, Spring-free coverage of {@link ExpiredRowsCleanupJob#sweep()} — mirrors {@code
 * HeartbeatJobTest}'s pattern. What each sweep actually deletes is proven against a real database
 * by {@code EventDedupStoreIT} and {@code JdbcSignupThrottleIT}; what is checked here is that the
 * one scheduled method drives both of them and reports what it removed.
 */
class ExpiredRowsCleanupJobTest {

  private Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void attachAppender() {
    logger = (Logger) LoggerFactory.getLogger(ExpiredRowsCleanupJob.class);
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
  @DisplayName("sweep() clears both tables in one pass and logs what each removed")
  void sweepClearsBothTablesAndLogsTheCounts() {
    new ExpiredRowsCleanupJob(dedupStoreRemoving(3), throttleRemoving(7)).sweep();

    assertThat(appender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(
            tuple(
                Level.INFO,
                "Cleanup removed 3 expired dedup row(s) and 7 lapsed throttle window(s)."));
  }

  @Test
  @DisplayName("the waitlist throttle is swept even when the dedup ledger has nothing to remove")
  void theThrottleIsSweptIndependentlyOfTheDedupLedger() {
    List<String> sweptTables = new ArrayList<>();
    EventDedupStore dedupStore =
        new EventDedupStore(null) {
          @Override
          public int deleteExpired() {
            sweptTables.add("event_dedup");
            return 0;
          }
        };
    JdbcSignupThrottle throttle =
        new JdbcSignupThrottle(null) {
          @Override
          public int deleteLapsedWindows() {
            sweptTables.add("waitlist_throttle");
            return 0;
          }
        };

    new ExpiredRowsCleanupJob(dedupStore, throttle).sweep();

    assertThat(sweptTables).containsExactly("event_dedup", "waitlist_throttle");
  }

  private static EventDedupStore dedupStoreRemoving(int rows) {
    return new EventDedupStore(null) {
      @Override
      public int deleteExpired() {
        return rows;
      }
    };
  }

  private static JdbcSignupThrottle throttleRemoving(int windows) {
    return new JdbcSignupThrottle(null) {
      @Override
      public int deleteLapsedWindows() {
        return windows;
      }
    };
  }
}
