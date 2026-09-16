package click.kivvi.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import click.kivvi.infrastructure.mail.MailOutboxStore;
import click.kivvi.infrastructure.tracking.EventDedupStore;
import click.kivvi.infrastructure.waitlist.JdbcSignupThrottle;
import java.time.Duration;
import java.time.Instant;
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
 * by {@code EventDedupStoreIT}, {@code JdbcSignupThrottleIT} and {@code MailOutboxStoreIT}; what is
 * checked here is that the one scheduled method drives all three of them and reports what it
 * removed.
 */
class ExpiredRowsCleanupJobTest {

  private static final Duration RETENTION = Duration.ofDays(30);

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
  @DisplayName("sweep() clears all three tables in one pass and logs what each removed")
  void sweepClearsEveryTableAndLogsTheCounts() {
    new ExpiredRowsCleanupJob(
            dedupStoreRemoving(3), throttleRemoving(7), outboxRemoving(2), RETENTION)
        .sweep();

    assertThat(appender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(
            tuple(
                Level.INFO,
                "Cleanup removed 3 expired dedup row(s), 7 lapsed throttle window(s) and 2"
                    + " delivered message(s)."));
  }

  @Test
  @DisplayName("delivered messages older than the retention window are the ones swept")
  void theSweepCutsOffAtTheRetentionWindow() {
    List<Instant> cutoffs = new ArrayList<>();
    MailOutboxStore outbox =
        new MailOutboxStore(null) {
          @Override
          public int deleteSentBefore(Instant cutoff) {
            cutoffs.add(cutoff);
            return 0;
          }
        };
    Instant before = Instant.now();

    new ExpiredRowsCleanupJob(dedupStoreRemoving(0), throttleRemoving(0), outbox, RETENTION)
        .sweep();

    assertThat(cutoffs).hasSize(1);
    assertThat(cutoffs.getFirst())
        .isBetween(before.minus(RETENTION), Instant.now().minus(RETENTION));
  }

  @Test
  @DisplayName("every table is swept, even when the ones before it had nothing to remove")
  void everyTableIsSweptIndependently() {
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

    MailOutboxStore outbox =
        new MailOutboxStore(null) {
          @Override
          public int deleteSentBefore(Instant cutoff) {
            sweptTables.add("mail_outbox");
            return 0;
          }
        };

    new ExpiredRowsCleanupJob(dedupStore, throttle, outbox, RETENTION).sweep();

    assertThat(sweptTables).containsExactly("event_dedup", "waitlist_throttle", "mail_outbox");
  }

  private static MailOutboxStore outboxRemoving(int messages) {
    return new MailOutboxStore(null) {
      @Override
      public int deleteSentBefore(Instant cutoff) {
        return messages;
      }
    };
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
