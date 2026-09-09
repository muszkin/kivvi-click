package click.kivvi.infrastructure.tracking;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sweeps expired {@code event_dedup} rows so the table never grows unbounded — the packet's "hourly
 * cleanup piggybacking on the existing scheduler". Registered purely through
 * {@code @Scheduled}/{@code @SchedulerLock}: {@code SchedulingConfig} (wave-1) already enables
 * scheduling application-wide and publishes the sole {@code TaskScheduler} bean every
 * {@code @Scheduled} method runs on, and {@code @EnableSchedulerLock} already covers every
 * {@code @SchedulerLock} annotated method — this class adds a task to that shared machinery without
 * touching either. {@code lockAtLeastFor} guards the same "rapid re-trigger" case {@link
 * click.kivvi.infrastructure.scheduling.HeartbeatJob} documents.
 */
@Component
public class EventDedupCleanupJob {

  static final String LOCK_NAME = "event-dedup-cleanup";

  private static final Logger LOG = LoggerFactory.getLogger(EventDedupCleanupJob.class);

  private final EventDedupStore store;

  public EventDedupCleanupJob(EventDedupStore store) {
    this.store = store;
  }

  @Scheduled(fixedRateString = "${kivvi.tracking.dedup-cleanup.interval:PT1H}")
  @SchedulerLock(name = LOCK_NAME, lockAtLeastFor = "PT1M", lockAtMostFor = "PT55M")
  public void sweep() {
    int removed = store.deleteExpired();
    LOG.info("Event dedup cleanup removed {} expired row(s).", removed);
  }
}
