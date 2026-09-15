package click.kivvi.domain.waitlist;

/**
 * Why a waitlist signup was refused, and which message tells the visitor so. The enum carries only
 * the message key — the wording lives in {@code messages_pl.properties} / {@code
 * messages_en.properties}, because a hard-coded Polish literal in new code is a review finding
 * (CODE_REVIEW.md, "Backend") and because the visitor may be reading either language.
 */
public enum WaitlistSignupError {
  EMAIL_EMPTY("waitlist.error.email.empty"),
  EMAIL_MALFORMED("waitlist.error.email.malformed"),
  CONSENT_REQUIRED("waitlist.error.consent.required"),
  /** Not a fault in what was typed: the caller simply asked too often. */
  THROTTLED("waitlist.error.throttled");

  private final String messageKey;

  WaitlistSignupError(String messageKey) {
    this.messageKey = messageKey;
  }

  public String messageKey() {
    return messageKey;
  }
}
