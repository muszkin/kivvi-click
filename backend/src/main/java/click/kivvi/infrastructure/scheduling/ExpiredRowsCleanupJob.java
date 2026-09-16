package click.kivvi.infrastructure.scheduling;

import click.kivvi.infrastructure.mail.MailOutboxStore;
import click.kivvi.infrastructure.tracking.EventDedupStore;
import click.kivvi.infrastructure.waitlist.JdbcSignupThrottle;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sweeps the three tables that accumulate rows nobody reads again — {@code event_dedup}'s lapsed
 * idempotency claims, {@code waitlist_throttle}'s lapsed rate-limit windows, and {@code
 * mail_outbox}'s delivered messages — so none of them grows unbounded.
 *
 * <p>PIO-70 added the second sweep here rather than scheduling a job of its own: one lock and one
 * cadence already exist for exactly this kind of housekeeping, and a second {@code @Scheduled}
 * method would double the moving parts to keep the same promise. The class was named {@code
 * EventDedupCleanupJob} while it had one table to clear and moved out of {@code
 * infrastructure.tracking} when it gained a second.
 *
 * <p>Registered purely through {@code @Scheduled}/{@code @SchedulerLock}: {@code SchedulingConfig}
 * already enables scheduling application-wide and publishes the sole {@code TaskScheduler} bean,
 * and {@code @EnableSchedulerLock} already covers every {@code @SchedulerLock} annotated method —
 * this class adds a task to that shared machinery without touching either. {@code lockAtLeastFor}
 * guards the same "rapid re-trigger" case {@link HeartbeatJob} documents.
 */
@Component
// On by default; only the integration tests that assert on the very tables this job sweeps turn
// it off. A @Scheduled fixedRate task runs once the moment its context is ready, so without this
// switch the startup sweep can land between such a test's own setup and its assertion and delete
// the row out from under it — a race that fails a correct test and, worse, fails it only
// sometimes. Disabling it there costs no coverage: what sweep() does is proven directly by
// ExpiredRowsCleanupJobTest, and what each delete does by EventDedupStoreIT and
// JdbcSignupThrottleIT.
@ConditionalOnProperty(name = "kivvi.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class ExpiredRowsCleanupJob {

  // Deliberately still the name this job locked under when it only swept event_dedup: the row
  // already exists in production's shedlock table, and renaming it would orphan that row and
  // briefly let two instances sweep at once during a rolling restart. The lock name identifies
  // the schedule slot, not the class.
  static final String LOCK_NAME = "event-dedup-cleanup";

  private static final Logger LOG = LoggerFactory.getLogger(ExpiredRowsCleanupJob.class);

  private final EventDedupStore dedupStore;
  private final JdbcSignupThrottle signupThrottle;
  private final MailOutboxStore mailOutbox;
  private final Duration sentMailRetention;

  public ExpiredRowsCleanupJob(
      EventDedupStore dedupStore,
      JdbcSignupThrottle signupThrottle,
      MailOutboxStore mailOutbox,
      @Value("${kivvi.mail.sent-retention}") Duration sentMailRetention) {
    this.dedupStore = dedupStore;
    this.signupThrottle = signupThrottle;
    this.mailOutbox = mailOutbox;
    this.sentMailRetention = sentMailRetention;
  }

  @Scheduled(fixedRateString = "${kivvi.cleanup.interval:PT1H}")
  @SchedulerLock(name = LOCK_NAME, lockAtLeastFor = "PT1M", lockAtMostFor = "PT55M")
  public void sweep() {
    int dedupRows = dedupStore.deleteExpired();
    int throttleWindows = signupThrottle.deleteLapsedWindows();
    int deliveredMail = mailOutbox.deleteSentBefore(Instant.now().minus(sentMailRetention));
    LOG.info(
        "Cleanup removed {} expired dedup row(s), {} lapsed throttle window(s) and {} delivered"
            + " message(s).",
        dedupRows,
        throttleWindows,
        deliveredMail);
  }
}
