package click.kivvi.application.waitlist;

import click.kivvi.application.mail.MailQueue;
import click.kivvi.domain.waitlist.ConfirmationToken;
import click.kivvi.domain.waitlist.OpaqueToken;
import click.kivvi.domain.waitlist.SubscriberStatus;
import click.kivvi.domain.waitlist.UnsubscribeToken;
import click.kivvi.domain.waitlist.WaitlistSignup;
import click.kivvi.infrastructure.waitlist.SignupThrottle;
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

  /**
   * Below this the secret is not worth having. An unsubscribe token is an HMAC of a sequential
   * {@code BIGSERIAL} id, so the key is the only thing standing between a stranger and the ability
   * to walk {@code id = 1..N} unsubscribing the whole list.
   */
  private static final int MIN_SECRET_LENGTH = 32;

  private final WaitlistSubscriberStore store;
  private final WaitlistMailComposer composer;
  private final MailQueue mailQueue;
  private final SignupThrottle throttle;
  private final String unsubscribeSecret;

  public WaitlistConfirmationService(
      WaitlistSubscriberStore store,
      WaitlistMailComposer composer,
      MailQueue mailQueue,
      SignupThrottle throttle,
      @Value("${kivvi.mail.unsubscribe-secret:}") String unsubscribeSecret) {
    this.store = store;
    this.composer = composer;
    this.mailQueue = mailQueue;
    this.throttle = throttle;
    this.unsubscribeSecret = requireUsableSecret(unsubscribeSecret);
  }

  /**
   * Refuses to start the application rather than derive unsubscribe links from nothing.
   *
   * <p>Two failure modes, and both are quiet without this. An unset variable would fall back to
   * whatever default the repository ships — which is a value anyone can read, and therefore a list
   * anyone can unsubscribe. An empty one reaches {@code SecretKeySpec}, which throws "Empty key" on
   * the first signup: the application would look healthy and every submission would 500. Refusing
   * to start says which variable is wrong, once, to the person who is already looking.
   */
  private static String requireUsableSecret(String secret) {
    if (secret == null || secret.strip().length() < MIN_SECRET_LENGTH) {
      throw new IllegalStateException(
          "KIVVI_UNSUBSCRIBE_SECRET must be set to at least "
              + MIN_SECRET_LENGTH
              + " characters (openssl rand -base64 48). Every unsubscribe link is derived from it, "
              + "so a missing, empty or guessable value would let anyone walk the subscriber ids "
              + "and unsubscribe the whole list.");
    }
    return secret;
  }

  /**
   * Records the signup and sends a confirmation link to any address that still needs one.
   *
   * <p>The insert and the lookup that follows it are what make a repeat signup work the way the
   * ticket asks: {@code ON CONFLICT DO NOTHING} leaves the existing row untouched, the lookup finds
   * it, and an address that is still pending gets a fresh link — one row, one more chance to
   * confirm.
   *
   * <p>An address that unsubscribed and is now signing up again is put back as unconfirmed, with
   * the consent proof of this submission and its confirmed-at stamps cleared. Both pages already
   * tell people they can come back this way, and a form that silently does nothing while answering
   * "check your inbox" is the worst of the available behaviours. Coming back costs a fresh
   * confirmation, so nobody is re-added without proving the mailbox again.
   *
   * <p>An address that is already confirmed is left entirely alone: it is on the list, and a second
   * copy of the confirmation would be a message nobody asked for.
   */
  @Override
  @Transactional
  public boolean register(WaitlistSignup signup) {
    Instant now = Instant.now();
    boolean stored = store.save(signup);
    store
        .findByEmail(signup.email())
        .ifPresent(
            subscriber -> {
              if (subscriber.status() == SubscriberStatus.CONFIRMED) {
                return;
              }
              if (subscriber.status() == SubscriberStatus.UNSUBSCRIBED) {
                LOG.info("Waitlist signup: an unsubscribed address is re-joining the list.");
                store.reopen(subscriber.id(), signup);
              }
              issueAndQueue(subscriber, now);
            });
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

    // The guarded UPDATE touched nothing, so this link is lapsed, spent, withdrawn or unknown.
    // Which of the four decides what the page says, and the row is what knows.
    return store
        .findByConfirmationTokenHash(hash)
        .<ConfirmationOutcome>map(subscriber -> explain(subscriber, rawToken, now))
        .orElseGet(ConfirmationOutcome.Unknown::new);
  }

  private static ConfirmationOutcome explain(
      WaitlistSubscriberStore.Subscriber subscriber, String rawToken, Instant now) {
    if (subscriber.status() == SubscriberStatus.UNSUBSCRIBED) {
      // The token is real but no longer leads anywhere, and saying so plainly would tell whoever
      // holds the link that this address opted out. The way back is the signup form, which both
      // the unsubscribe page and this one already point at.
      return new ConfirmationOutcome.Unknown();
    }
    if (ConfirmationToken.hasLapsed(subscriber.confirmationTokenExpiresAt(), now)) {
      return new ConfirmationOutcome.Expired(rawToken);
    }
    // Confirmed, or pending-and-live and therefore confirmed by a request that committed while
    // this one was waiting on its row lock. Both mean the same thing to the person reading it.
    return new ConfirmationOutcome.AlreadyConfirmed();
  }

  /**
   * Issues a new link for a subscriber who still has not confirmed.
   *
   * <p>Returns nothing on purpose. The caller answers the same way whatever happened here — an
   * unknown token, an expired one and an already-confirmed one all lead to the same "check your
   * inbox" page, because the alternative is a form that tells a stranger which addresses are on the
   * list.
   *
   * <p>Rate limited on the same allowance as the signup, and for a sharper reason: every call
   * issues a token, which queues a message and retires the previous link. Without a limit, anyone
   * holding one live token — the subscriber, or whoever a message was forwarded to — could send an
   * unbounded stream of mail to that address, burn the provider's daily quota, and invalidate the
   * link they are trying to use with every attempt.
   *
   * <p>The per-IP bucket is charged before the token is even parsed, so an unknown token costs the
   * caller exactly what a known one does; an allowance that only bit on real tokens would itself be
   * the oracle this endpoint exists to avoid.
   */
  @Transactional
  public void resend(String rawToken, String clientIp, Instant now) {
    if (!throttle.tryAcquire(SignupAllowance.ipBucket(clientIp), SignupAllowance.PER_IP_PER_HOUR)) {
      LOG.info("Waitlist resend refused: hourly allowance reached.");
      return;
    }
    Optional<OpaqueToken> token = OpaqueToken.parse(rawToken);
    if (token.isEmpty()) {
      return;
    }
    if (!throttle.tryAcquire(
        SignupAllowance.resendBucket(token.get().hash()), SignupAllowance.PER_ADDRESS_PER_HOUR)) {
      LOG.info("Waitlist resend refused: this link has already asked for enough replacements.");
      return;
    }
    store
        .findByConfirmationTokenHash(token.get().hash())
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
