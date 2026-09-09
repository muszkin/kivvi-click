package click.kivvi.infrastructure.scheduling;

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
 * Fast, Spring-free coverage of the log line itself (B33), separate from {@link
 * HeartbeatSchedulerIT}'s end-to-end proof that the real scheduler/lock wiring calls this method on
 * the expected cadence.
 */
class HeartbeatJobTest {

  private Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void attachAppender() {
    logger = (Logger) LoggerFactory.getLogger(HeartbeatJob.class);
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
  @DisplayName("B33 tick() logs exactly \"Scheduler heartbeat tick.\" at INFO")
  void tickLogsTheHeartbeatMessageAtInfo() {
    new HeartbeatJob().tick();

    assertThat(appender.list)
        .extracting(ILoggingEvent::getLevel, ILoggingEvent::getFormattedMessage)
        .containsExactly(tuple(Level.INFO, HeartbeatJob.TICK_MESSAGE));
    assertThat(HeartbeatJob.TICK_MESSAGE).isEqualTo("Scheduler heartbeat tick.");
  }
}
