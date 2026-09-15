package click.kivvi.application.mail;

import click.kivvi.domain.mail.OutboundMail;
import click.kivvi.infrastructure.mail.MailOutboxStore;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The one way a message enters the system. Callers hand over an {@link OutboundMail} and are done —
 * no SMTP conversation happens on the request thread, so a relay having a bad minute cannot turn
 * into a visitor staring at a spinner.
 *
 * <p>Enqueueing is meant to run inside the caller's transaction, next to whatever state change the
 * message describes. That is the whole point of an outbox: either the subscriber is marked as
 * having been sent a confirmation and the confirmation is queued, or neither happened.
 *
 * <p>Deliberately not a general-purpose queue. PIO-76's password reset will use this too; anything
 * that is not e-mail does not belong here.
 */
@Service
public class MailQueue {

  private static final Logger LOG = LoggerFactory.getLogger(MailQueue.class);

  private final MailOutboxStore outbox;

  public MailQueue(MailOutboxStore outbox) {
    this.outbox = outbox;
  }

  /**
   * @return {@code true} when the message was queued, {@code false} when an identical one already
   *     was. A refusal is INFO, not WARN: two browser tabs submitting the same form is ordinary
   *     traffic, and {@code FailOnWarnLogExtension} rightly fails the suite over a warning on an
   *     expected path (CODE_REVIEW.md, "Backend").
   */
  public boolean enqueue(OutboundMail mail) {
    boolean queued = outbox.enqueue(mail, Instant.now());
    if (!queued) {
      LOG.info("Mail not queued: an identical message is already waiting to go out.");
    }
    return queued;
  }
}
