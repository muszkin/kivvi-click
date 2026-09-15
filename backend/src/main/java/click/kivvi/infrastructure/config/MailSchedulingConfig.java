package click.kivvi.infrastructure.config;

import click.kivvi.infrastructure.mail.MailOutboxSenderJob;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * The mail sender's own thread, and the schedule that uses it.
 *
 * <p>{@link SchedulingConfig} publishes a single-thread {@code TaskScheduler} and installs it on
 * the task registrar, which makes it the pool behind every {@code @Scheduled} method in the
 * application. Sharing it with the sender would mean an SMTP conversation that hangs — a relay
 * accepting a connection and then going quiet is an ordinary thing for a relay to do — stops the
 * heartbeat with it. So the sender gets a pool of its own and is registered here rather than by
 * annotation.
 *
 * <p>Two {@code TaskScheduler} beans now exist, and that is not ambiguous: {@code
 * SchedulingConfig.configureTasks} sets the registrar's scheduler explicitly, so Boot never has to
 * pick one by type.
 *
 * <p>The schedule is a {@link SmartLifecycle} rather than a call inside the {@code @Bean} method,
 * so the first pass happens once the context is up instead of half way through building it.
 * Scheduling the injected bean's method (not a hand-constructed instance) is what keeps
 * {@code @SchedulerLock} working: shedlock's interceptor lives on the Spring proxy.
 */
@Configuration
public class MailSchedulingConfig {

  private final Duration interval;

  public MailSchedulingConfig(@Value("${kivvi.mail.sender.interval}") Duration interval) {
    this.interval = interval;
  }

  @Bean(destroyMethod = "shutdown")
  public ThreadPoolTaskScheduler mailTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("mail-sender-");
    // A hung SMTP conversation must not hold the whole context open on the way down. The socket
    // timeouts in application.yml keep one message well inside this.
    scheduler.setAwaitTerminationSeconds(30);
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    return scheduler;
  }

  /**
   * On by default; the integration tests that drive the outbox by hand switch it off — the same
   * reasoning, and the same switch shape, as {@code ExpiredRowsCleanupJob}'s.
   */
  @Bean
  @ConditionalOnProperty(
      name = "kivvi.mail.sender.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public SmartLifecycle mailOutboxSenderSchedule(
      ThreadPoolTaskScheduler mailTaskScheduler, MailOutboxSenderJob senderJob) {
    return new FixedDelaySchedule(mailTaskScheduler, senderJob::send, interval);
  }

  /**
   * A fixed-delay task bound to the context's lifecycle: started once everything is wired, stopped
   * before the pool is torn down. Fixed delay rather than fixed rate, so a slow pass is followed by
   * a pause rather than by an immediate second pass piling onto the same single thread.
   */
  private static final class FixedDelaySchedule implements SmartLifecycle {

    private final ThreadPoolTaskScheduler scheduler;
    private final Runnable task;
    private final Duration delay;
    private ScheduledFuture<?> scheduled;

    private FixedDelaySchedule(ThreadPoolTaskScheduler scheduler, Runnable task, Duration delay) {
      this.scheduler = scheduler;
      this.task = task;
      this.delay = delay;
    }

    @Override
    public synchronized void start() {
      if (scheduled == null) {
        scheduled = scheduler.scheduleWithFixedDelay(task, delay);
      }
    }

    @Override
    public synchronized void stop() {
      if (scheduled != null) {
        scheduled.cancel(false);
        scheduled = null;
      }
    }

    @Override
    public synchronized boolean isRunning() {
      return scheduled != null;
    }
  }
}
