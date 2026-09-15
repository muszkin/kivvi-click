package click.kivvi.domain.waitlist;

import click.kivvi.domain.EmailValidation;
import click.kivvi.domain.SupportedLocale;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * One accepted waitlist signup, ready to be stored: the normalized address that decides duplicates,
 * where and in what language it was captured, and the proof of consent that makes the record usable
 * under GDPR.
 *
 * <p>Construction goes through {@link #landingSignup}, which normalizes the address the same way
 * {@link #normalizeEmail} does for the throttle's per-address bucket — the two must agree, or the
 * same visitor would get a fresh quota by changing capitalization.
 */
public record WaitlistSignup(
    String email,
    SupportedLocale locale,
    SignupSource source,
    SubscriberStatus status,
    Instant signedUpAt,
    ConsentProof consent) {

  /** Mirrors {@code waitlist_subscriber.email}'s own width, which is the RFC's maximum. */
  private static final int MAX_EMAIL_LENGTH = 320;

  /** Mirrors {@code waitlist_subscriber.consent_user_agent}'s width. */
  private static final int MAX_USER_AGENT_LENGTH = 512;

  /**
   * What was agreed to, when, and from where. {@code ip} and {@code userAgent} are nullable: a
   * request can legitimately arrive without a {@code User-Agent} header, and that is not a reason
   * to refuse a signup.
   */
  public record ConsentProof(Instant at, String ip, String userAgent, String text) {

    public ConsentProof {
      Objects.requireNonNull(at, "consent timestamp");
      Objects.requireNonNull(text, "consent text");
    }
  }

  public WaitlistSignup {
    Objects.requireNonNull(email, "email");
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(signedUpAt, "signedUpAt");
    Objects.requireNonNull(consent, "consent");
  }

  /**
   * Trims and lower-cases the address so {@code " Ala@SKLEP.PL "} and {@code "ala@sklep.pl"} are
   * one subscriber, not two. {@link Locale#ROOT} rather than the default locale: under a Turkish
   * default, {@code "I"} would lower-case to a dotless {@code "ı"} and quietly split an address in
   * two.
   */
  public static String normalizeEmail(String rawEmail) {
    return rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Checks a submitted form against the rules that decide whether it may be stored at all.
   *
   * <p>The address goes through {@link click.kivvi.domain.EmailValidation}, the validator the login
   * form already uses, rather than a second regex that could disagree with it. Only the outcome is
   * reused, not its Polish message: that literal is a documented login-parity case, while this form
   * speaks whichever language the visitor is reading.
   *
   * @return the first rule that was broken, or empty when the signup may proceed
   */
  public static Optional<WaitlistSignupError> validate(String rawEmail, boolean consentGiven) {
    String email = normalizeEmail(rawEmail);
    if (email.isEmpty()) {
      return Optional.of(WaitlistSignupError.EMAIL_EMPTY);
    }
    // Before the pattern, not after: EmailValidation's local-part is unbounded, so a
    // several-hundred-character address passes it happily and then fails the column's own
    // 320-character limit — turning a typo into a 500 on a page anyone can reach.
    if (email.length() > MAX_EMAIL_LENGTH) {
      return Optional.of(WaitlistSignupError.EMAIL_MALFORMED);
    }
    if (EmailValidation.errorFor(email).isPresent()) {
      return Optional.of(WaitlistSignupError.EMAIL_MALFORMED);
    }
    if (!consentGiven) {
      return Optional.of(WaitlistSignupError.CONSENT_REQUIRED);
    }
    return Optional.empty();
  }

  /** A pending signup captured on the public landing page. */
  public static WaitlistSignup landingSignup(
      String rawEmail,
      SupportedLocale locale,
      Instant now,
      String consentIp,
      String consentUserAgent,
      String consentText) {
    return new WaitlistSignup(
        normalizeEmail(rawEmail),
        locale,
        SignupSource.LANDING,
        SubscriberStatus.PENDING,
        now,
        new ConsentProof(now, consentIp, truncate(consentUserAgent), consentText));
  }

  /**
   * A {@code User-Agent} header is whatever the caller chose to send and has no length limit of its
   * own, while the column that records it holds 512 characters. Truncating keeps an over-long
   * header from turning a signup into a 500; what is kept still identifies the browser, which is
   * all this field is evidence of.
   */
  private static String truncate(String userAgent) {
    if (userAgent == null || userAgent.length() <= MAX_USER_AGENT_LENGTH) {
      return userAgent;
    }
    return userAgent.substring(0, MAX_USER_AGENT_LENGTH);
  }
}
