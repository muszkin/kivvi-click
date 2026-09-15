package click.kivvi.application.waitlist;

import click.kivvi.domain.waitlist.WaitlistSignup;
import click.kivvi.domain.waitlist.WaitlistSignupError;
import click.kivvi.infrastructure.waitlist.SignupThrottle;
import click.kivvi.infrastructure.waitlist.WaitlistSubscriberStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 * Decides what happens to a waitlist submission.
 *
 * <p>The three checks run in a fixed order — honeypot, then rate limit, then validation — and the
 * order is the point. A bot that fills the hidden field is answered before it costs a database
 * write. A caller over its allowance is refused before validation runs, so the form cannot be used
 * to test whether an address is well-formed, and the limiter cannot be probed by submitting
 * deliberate rubbish. Only what survives all three is written.
 *
 * <p>Every refusal is logged at INFO. A rejected form, a rate limit and a honeypot hit are all
 * expected traffic on a public endpoint, not faults: logging them at WARN would fail the whole test
 * suite ({@code FailOnWarnLogExtension}) and, in production, would bury real warnings under noise
 * any passer-by can generate at will.
 */
@Service
public class WaitlistSignupService {

  /**
   * Generous enough that an office or a mobile carrier behind one NAT never notices, tight enough
   * that a single host cannot stuff the list.
   */
  private static final int SIGNUPS_PER_IP_PER_HOUR = 10;

  /** A real person needs one attempt, or a couple after a typo. Nobody needs a fourth. */
  private static final int SIGNUPS_PER_ADDRESS_PER_HOUR = 3;

  private static final String IP_BUCKET_PREFIX = "ip:";
  private static final String EMAIL_BUCKET_PREFIX = "email:";

  /**
   * The clause whose wording is stored as the consent proof. It must stay word-for-word identical
   * to what the form renders — {@code landingPage.waitlist.consent} in {@code
   * frontend/src/i18n/messages/landing.{pl,en}.ts} — or the record would attest to something the
   * visitor never read.
   */
  private static final String CONSENT_TEXT_KEY = "waitlist.consent.text";

  private static final Logger LOG = LoggerFactory.getLogger(WaitlistSignupService.class);

  private final WaitlistSubscriberStore store;
  private final SignupThrottle throttle;
  private final MessageSource messageSource;

  public WaitlistSignupService(
      WaitlistSubscriberStore store, SignupThrottle throttle, MessageSource messageSource) {
    this.store = store;
    this.throttle = throttle;
    this.messageSource = messageSource;
  }

  public SignupOutcome signUp(SignupRequest request) {
    if (isBot(request)) {
      LOG.info("Waitlist signup discarded: the honeypot field was filled in.");
      return new SignupOutcome.Accepted();
    }

    if (!withinAllowance(request)) {
      LOG.info("Waitlist signup refused: hourly allowance reached.");
      return new SignupOutcome.ThrottleExceeded();
    }

    Optional<WaitlistSignupError> error =
        WaitlistSignup.validate(request.email(), request.consentGiven());
    if (error.isPresent()) {
      LOG.info("Waitlist signup rejected: {}.", error.get());
      return new SignupOutcome.Rejected(error.get());
    }

    record(request);
    return new SignupOutcome.Accepted();
  }

  /** No human sees the honeypot field, so anything at all in it came from a script. */
  private static boolean isBot(SignupRequest request) {
    return request.honeypot() != null && !request.honeypot().isBlank();
  }

  /**
   * Both buckets must have room. The per-IP one is consumed first and short-circuits: a caller
   * already over that limit must not also burn down the allowance belonging to whatever address it
   * happened to type.
   */
  private boolean withinAllowance(SignupRequest request) {
    if (!throttle.tryAcquire(IP_BUCKET_PREFIX + request.clientIp(), SIGNUPS_PER_IP_PER_HOUR)) {
      return false;
    }

    // An empty field is not an address, and it must not be counted as one: every visitor who
    // submits the form blank normalizes to the same empty string, so a shared bucket would let
    // three blank submissions from anywhere on earth start answering everyone else with "too
    // many attempts" instead of "enter an e-mail address". The per-IP bucket above has already
    // been charged, which is the limit that actually protects anything here.
    String normalizedEmail = WaitlistSignup.normalizeEmail(request.email());
    if (normalizedEmail.isEmpty()) {
      return true;
    }

    return throttle.tryAcquire(emailBucketKey(normalizedEmail), SIGNUPS_PER_ADDRESS_PER_HOUR);
  }

  private void record(SignupRequest request) {
    String consentText =
        messageSource.getMessage(
            CONSENT_TEXT_KEY, null, Locale.forLanguageTag(request.locale().code()));
    WaitlistSignup signup =
        WaitlistSignup.landingSignup(
            request.email(),
            request.locale(),
            Instant.now(),
            request.clientIp(),
            request.userAgent(),
            consentText);

    boolean stored = store.save(signup);
    if (!stored) {
      LOG.info("Waitlist signup ignored: the address is already on the list.");
    }
  }

  /**
   * The address is hashed rather than stored in the bucket key: {@code waitlist_throttle} is a
   * throwaway abuse counter that anyone can fill, and it has no business holding readable e-mail
   * addresses. Hashing also keeps every key the same short length, which is what lets the column
   * stay narrow while the address column allows the full 320 characters a mailbox may have.
   */
  private static String emailBucketKey(String normalizedEmail) {
    return EMAIL_BUCKET_PREFIX + sha256Hex(normalizedEmail);
  }

  private static String sha256Hex(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable on this JVM.", exception);
    }
  }
}
