package click.kivvi.infrastructure.scheduling;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Wave-1 scheduler-heartbeat journey (contract.md): reproduces {@code
 * src/MessageHandler/HeartbeatHandler.php} — one INFO log line per tick, proving the
 * scheduler-to-handler path is alive. {@link #LOCK_NAME} names the {@code shedlock} row (B20) that
 * {@code click.kivvi.infrastructure.config.SchedulingConfig} and {@link JdbcHeartbeatLockHistory}
 * both key off; {@code lockAtLeastFor = "PT1M"} keeps a rapid re-trigger (a restart, or the trigger
 * firing again seconds later under a shortened test interval) from logging a second tick within the
 * same minute (B33).
 */
@Component
public class HeartbeatJob {

  static final String LOCK_NAME = "heartbeat";
  static final String TICK_MESSAGE = "Scheduler heartbeat tick.";

  private static final Logger LOG = LoggerFactory.getLogger(HeartbeatJob.class);

  @SchedulerLock(name = LOCK_NAME, lockAtLeastFor = "PT1M")
  public void tick() {
    LOG.info(TICK_MESSAGE);
  }
}
