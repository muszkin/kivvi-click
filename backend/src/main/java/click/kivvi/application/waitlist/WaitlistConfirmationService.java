package click.kivvi.application.waitlist;

import click.kivvi.application.mail.MailQueue;
import click.kivvi.domain.waitlist.ConfirmationToken;
import click.kivvi.domain.waitlist.OpaqueToken;
import click.kivvi.domain.waitlist.SubscriberStatus;
import click.kivvi.domain.waitlist.UnsubscribeToken;
import click.kivvi.domain.waitlist.WaitlistSignup;
import click.kivvi.infrastructure.waitlist.WaitlistSubscriberStore;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything that happens to a waitlist address between "someone typed it in" and "the person who
 * owns that mailbox said yes" — plus the way back out.
 *
 * <p>Every method is transactional, and that is the whole reason the outbox exists. Writing the
 * subscriber and queueing its confirmation in one transaction means the two can never disagree:
 * there is no state in which the list holds an address that was never written to, and none in which
 * a confirmation goes out for a row that was rolled back.
 *
 * <p>Nothing here ever reveals whether an address is on the list. A resend for an unknown token
 * behaves exactly like one for a known token, because the page that offers it is reachable by
 * anybody and the difference would be an address-enumeration oracle.
 */
@Service
public class WaitlistConfirmationService implements WaitlistRegistrar {

  private static final Logger LOG = LoggerFactory.getLogger(WaitlistConfirmationService.class);

  private final WaitlistSubscriberStore store;
  private final WaitlistMailComposer composer;
  private final MailQueue mailQueue;
  private final String unsubscribeSecret;

  public WaitlistConfirmationService(
      WaitlistSubscriberStore store,
      WaitlistMailComposer composer,
      MailQueue mailQueue,
      @Value("${kivvi.mail.unsubscribe-secret}") String unsubscribeSecret) {
    this.store = store;
    this.composer = composer;
    this.mailQueue = mailQueue;
    this.unsubscribeSecret = unsubscribeSecret;
  }

  /**
   * Records the signup and sends a confirmation link to any address that still needs one.
   *
   * <p>The insert and the lookup that follows it are what make a repeat signup work the way the
   * ticket asks: {@code ON CONFLICT DO NOTHING} leaves the existing row untouched, the lookup finds
   * it, and an address that is still pending gets a fresh link — one row, one more chance to
   * confirm. An address that is already confirmed, or that has unsubscribed, is left entirely
   * alone; mailing it again would be exactly the thing it opted out of.
   */
  @Override
  @Transactional
  public boolean register(WaitlistSignup signup) {
    Instant now = Instant.now();
    boolean stored = store.save(signup);
    store
        .findByEmail(signup.email())
        .filter(subscriber -> subscriber.status() == SubscriberStatus.PENDING)
        .ifPresent(subscriber -> issueAndQueue(subscriber, now));
    return stored;
  }

  /**
   * @param ip and {@code userAgent} are stored as the proof of who confirmed, from where. They are
   *     also the only way to spot a mail client's link prefetcher confirming on a human's behalf: a
   *     confirmation from a different network than the signup, seconds later, looks like exactly
   *     what it is.
   */
  @Transactional
  public ConfirmationOutcome confirm(String rawToken, Instant now, String ip, String userAgent) {
    Optional<OpaqueToken> token = OpaqueToken.parse(rawToken);
    if (token.isEmpty()) {
      return new ConfirmationOutcome.Unknown();
    }

    String hash = token.get().hash();
    if (store.confirm(hash, now, ip, userAgent)) {
      LOG.info("Waitlist address confirmed.");
      return new ConfirmationOutcome.Confirmed();
    }

    // The guarded UPDATE touched nothing, so this link is old, spent or unknown. Which of the three
    // decides what the page says, and the row is what knows.
    return store
        .findByConfirmationTokenHash(hash)
        .<ConfirmationOutcome>map(
            subscriber -> {
              if (subscriber.status() == SubscriberStatus.PENDING) {
                return new ConfirmationOutcome.Expired(rawToken);
              }
              return new ConfirmationOutcome.AlreadyConfirmed();
            })
        .orElseGet(ConfirmationOutcome.Unknown::new);
  }

  /**
   * Issues a new link for a subscriber who still has not confirmed.
   *
   * <p>Returns nothing on purpose. The caller answers the same way whatever happened here — an
   * unknown token, an expired one and an already-confirmed one all lead to the same "check your
   * inbox" page, because the alternative is a form that tells a stranger which addresses are on the
   * list.
   */
  @Transactional
  public void resend(String rawToken, Instant now) {
    OpaqueToken.parse(rawToken)
        .flatMap(token -> store.findByConfirmationTokenHash(token.hash()))
        .filter(subscriber -> subscriber.status() == SubscriberStatus.PENDING)
        .ifPresent(subscriber -> issueAndQueue(subscriber, now));
  }

  @Transactional
  public UnsubscribeOutcome unsubscribe(String rawToken, Instant now) {
    boolean known =
        OpaqueToken.parse(rawToken)
            .map(token -> store.unsubscribe(token.hash(), now))
            .orElse(false);
    if (!known) {
      return new UnsubscribeOutcome.Unknown();
    }
    LOG.info("Waitlist address unsubscribed.");
    return new UnsubscribeOutcome.Unsubscribed();
  }

  private void issueAndQueue(WaitlistSubscriberStore.Subscriber subscriber, Instant now) {
    ConfirmationToken confirmation = ConfirmationToken.issue(now);
    OpaqueToken unsubscribe = UnsubscribeToken.forSubscriber(unsubscribeSecret, subscriber.id());

    store.issueTokens(
        subscriber.id(), confirmation.hash(), confirmation.expiresAt(), unsubscribe.hash());
    mailQueue.enqueue(
        composer.confirmation(subscriber.email(), subscriber.locale(), confirmation, unsubscribe));
  }
}
