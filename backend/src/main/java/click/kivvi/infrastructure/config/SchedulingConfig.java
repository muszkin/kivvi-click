package click.kivvi.infrastructure.config;

import click.kivvi.infrastructure.scheduling.HeartbeatJob;
import click.kivvi.infrastructure.scheduling.HeartbeatLockHistory;
import click.kivvi.infrastructure.scheduling.HeartbeatTrigger;
import java.time.Duration;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Wave-1 scheduler-heartbeat journey (contract.md). {@code defaultLockAtMostFor = "PT55M"} bounds
 * how long a crashed run can hold the {@code heartbeat} lock — under the hourly cadence, so a stuck
 * lock never survives past the next tick's due time. The heartbeat task is registered through
 * {@link SchedulingConfigurer} rather than {@code @Scheduled} because its cadence needs a custom
 * {@link HeartbeatTrigger} (missed-run semantics), which {@code @Scheduled}'s fixedRate/fixedDelay
 * attributes cannot express; {@link HeartbeatJob#tick()}'s {@code @SchedulerLock} still applies to
 * a trigger-task invocation exactly as it would to a {@code @Scheduled} one (shedlock-spring's
 * method-level AOP interceptor, the library default, matches any Spring-proxied call to an
 * annotated method).
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT55M")
public class SchedulingConfig implements SchedulingConfigurer {

  private final HeartbeatJob heartbeatJob;
  private final HeartbeatLockHistory heartbeatLockHistory;
  private final Duration heartbeatInterval;

  public SchedulingConfig(
      HeartbeatJob heartbeatJob,
      HeartbeatLockHistory heartbeatLockHistory,
      @Value("${kivvi.scheduling.heartbeat.interval}") Duration heartbeatInterval) {
    this.heartbeatJob = heartbeatJob;
    this.heartbeatLockHistory = heartbeatLockHistory;
    this.heartbeatInterval = heartbeatInterval;
  }

  /**
   * ShedLock's Postgres-backed lock store — the {@code shedlock} table frozen in V1__baseline.sql.
   */
  @Bean
  public LockProvider lockProvider(DataSource dataSource) {
    return new JdbcTemplateLockProvider(dataSource);
  }

  /**
   * A dedicated single-thread scheduler so the heartbeat task never competes for threads with
   * request handling; Spring manages its start/stop as a bean, so it shuts down cleanly with the
   * application context.
   */
  @Bean
  public TaskScheduler heartbeatTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("heartbeat-scheduler-");
    return scheduler;
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar registrar) {
    registrar.setTaskScheduler(heartbeatTaskScheduler());
    registrar.addTriggerTask(
        heartbeatJob::tick, new HeartbeatTrigger(heartbeatLockHistory, heartbeatInterval));
  }
}
