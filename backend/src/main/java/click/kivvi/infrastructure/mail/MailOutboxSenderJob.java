package click.kivvi.infrastructure.mail;

import click.kivvi.domain.mail.MailRetryPolicy;
import click.kivvi.domain.mail.OutboundMail;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Drains {@code mail_outbox}: claim what is due, hand each message to the transport, record what
 * happened.
 *
 * <p>Registered from {@link click.kivvi.infrastructure.config.MailSchedulingConfig} rather than
 * with {@code @Scheduled}. {@code SchedulingConfig} installs the heartbeat's single-thread pool as
 * the scheduler for the whole task registrar, so an {@code @Scheduled} method here would share one
 * thread with the heartbeat — and a relay that stops answering mid-conversation would stop the
 * heartbeat with it. The lock still applies: shedlock's method interceptor matches any
 * Spring-proxied call to an annotated method, however that call was triggered.
 *
 * <p>A failure is never fatal to the run. Each message is settled on its own, so one unreachable
 * mailbox cannot hold up the nineteen behind it.
 */
// The bean always exists; only its schedule is conditional (MailSchedulingConfig). That split is
// what lets an integration test switch the background run off — a run claiming a row between a
// test's setup and its assertion fails a correct test, and does it only sometimes — and still call
// send() itself to prove what one pass does.
@Component
public class MailOutboxSenderJob {

  /**
   * A new schedule slot, so a new lock row. Unlike {@code ExpiredRowsCleanupJob}'s name, this one
   * has no history in production's {@code shedlock} table to preserve.
   */
  static final String LOCK_NAME = "mail-outbox-sender";

  private static final Logger LOG = LoggerFactory.getLogger(MailOutboxSenderJob.class);

  private final MailOutboxStore outbox;
  private final MailTransport transport;
  private final int batchSize;

  public MailOutboxSenderJob(
      MailOutboxStore outbox,
      MailTransport transport,
      @Value("${kivvi.mail.sender.batch-size}") int batchSize) {
    this.outbox = outbox;
    this.transport = transport;
    this.batchSize = batchSize;
  }

  /**
   * {@code lockAtMostFor} is shorter than the claim lease in {@link MailOutboxStore} on purpose: if
   * this process dies mid-run, another may start sending again long before the rows it had claimed
   * become due, and those rows stay off limits until their own lease lapses. The two together mean
   * a crash costs at most one lease, never a duplicate delivery.
   *
   * <p>A full batch of twenty messages against a relay that is timing out can outlast that two
   * minutes, so the lock is not what guarantees a single sender here — the row lease is, and it
   * holds whether or not the lock has lapsed. What the lock buys is that two healthy instances do
   * not both wake up and query for work every thirty seconds.
   *
   * <p>No {@code lockAtLeastFor}, unlike {@link
   * click.kivvi.infrastructure.scheduling.HeartbeatJob}. That setting exists to stop a rapid
   * re-trigger from doing the work twice, and here the row lease already does that — a second pass
   * a millisecond later claims nothing, because every due row is already claimed. Holding the lock
   * past the run would only mean a message queued during a pass waits for the lock to lapse rather
   * than for the next tick.
   */
  @SchedulerLock(name = LOCK_NAME, lockAtMostFor = "PT2M")
  public void send() {
    List<MailOutboxStore.ClaimedMail> due = outbox.claimDue(Instant.now(), batchSize);
    if (due.isEmpty()) {
      return;
    }
    for (MailOutboxStore.ClaimedMail claimed : due) {
      deliver(claimed);
    }
    LOG.info("Mail outbox: handled {} message(s).", due.size());
  }

  private void deliver(MailOutboxStore.ClaimedMail claimed) {
    try {
      transport.send(
          new OutboundMail(
              claimed.recipient(),
              claimed.subject(),
              claimed.htmlBody(),
              claimed.textBody(),
              // The stored row is the message; its deduplication key has already done its work at
              // the door and means nothing to a transport.
              null));
      outbox.markSent(claimed.id(), Instant.now());
    } catch (RuntimeException exception) {
      settleFailure(claimed, exception);
    }
  }

  private void settleFailure(MailOutboxStore.ClaimedMail claimed, RuntimeException exception) {
    String reason = describe(exception);
    if (MailRetryPolicy.isExhausted(claimed.attempts())) {
      // The one path in this application that logs above INFO, and it earns it: an unconfirmed
      // address is a subscriber lost silently, and nothing else will ever mention it again.
      //
      // The reason is stored, not logged. An SMTP rejection routinely quotes the recipient
      // ("550 5.1.1 <ala@sklep.pl> unknown"), and a subscriber's address does not belong in a log
      // file with a different retention policy from the table it came out of. The row id is here
      // and mail_outbox.last_error holds the detail.
      LOG.error(
          "Mail outbox: giving up on message {} after {} attempts. See its last_error column.",
          claimed.id(),
          claimed.attempts());
      outbox.giveUp(claimed.id(), reason);
      return;
    }
    Duration backoff = MailRetryPolicy.backoffAfter(claimed.attempts());
    LOG.info(
        "Mail outbox: message {} failed on attempt {}, retrying in {}.",
        claimed.id(),
        claimed.attempts(),
        backoff);
    outbox.reschedule(claimed.id(), Instant.now().plus(backoff), reason);
  }

  /**
   * The cause carries what the relay actually said ("550 mailbox unavailable"); the wrapper only
   * says that something failed. Both go into {@code last_error}, because a row nobody can diagnose
   * from is a row nobody acts on.
   */
  private static String describe(RuntimeException exception) {
    Throwable cause = exception.getCause();
    return cause == null
        ? String.valueOf(exception.getMessage())
        : exception.getMessage() + " " + cause;
  }
}
