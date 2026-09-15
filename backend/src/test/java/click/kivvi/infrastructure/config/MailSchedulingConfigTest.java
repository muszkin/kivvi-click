package click.kivvi.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Risk R4 from the spec, in test form: the mail sender must not run on the heartbeat's thread.
 *
 * <p>The heartbeat pool has exactly one thread, and {@code SchedulingConfig} installs it as the
 * scheduler for the whole task registrar — which is why the sender is registered by hand rather
 * than with {@code @Scheduled}. A relay that accepts a connection and then goes quiet would, on
 * that shared pool, stop the heartbeat for as long as the socket timeout allows, and that looks
 * like a scheduler bug for a week before anyone suspects the mail.
 */
class MailSchedulingConfigTest {

  @Test
  @DisplayName(
      "the sender's pool is a different pool from the heartbeat's, with its own thread name")
  void theSenderDoesNotShareTheHeartbeatPool() {
    // SchedulingConfig declares its bean as the TaskScheduler interface; what matters here is
    // that the object behind it is not the same pool.
    TaskScheduler heartbeat =
        new SchedulingConfig(null, null, Duration.ofHours(1)).heartbeatTaskScheduler();
    ThreadPoolTaskScheduler mail =
        new MailSchedulingConfig(Duration.ofSeconds(30)).mailTaskScheduler();

    assertThat(mail).isNotSameAs(heartbeat);
    assertThat(mail.getThreadNamePrefix()).isEqualTo("mail-sender-");
    assertThat(heartbeat)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(ThreadPoolTaskScheduler.class))
        .extracting(ThreadPoolTaskScheduler::getThreadNamePrefix)
        .isEqualTo("heartbeat-scheduler-");
  }

  @Test
  @DisplayName("the sender's pool has one thread, so two passes can never overlap on it")
  void theSendersPoolIsSingleThreaded() {
    ThreadPoolTaskScheduler mail =
        new MailSchedulingConfig(Duration.ofSeconds(30)).mailTaskScheduler();
    mail.initialize();

    try {
      assertThat(mail.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
    } finally {
      mail.shutdown();
    }
  }
}
