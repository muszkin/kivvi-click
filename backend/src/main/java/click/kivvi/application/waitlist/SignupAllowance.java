package click.kivvi.application.waitlist;

import click.kivvi.domain.Sha256;

/**
 * How much of the waitlist's machinery one caller may use in an hour, and the bucket keys that
 * count it.
 *
 * <p>Shared by the signup path and the resend path deliberately: PIO-71's spec asks the resend to
 * carry "the same rate limit as the signup", and two copies of that number are two numbers that
 * will eventually disagree. The per-IP bucket key is identical in both, so one caller has one
 * allowance however it chooses to spend it — signing up, asking for another link, or alternating.
 *
 * <p>Addresses and tokens are hashed into their keys rather than stored in them: {@code
 * waitlist_throttle} is a throwaway abuse counter anyone can fill, and it has no business holding
 * readable e-mail addresses or usable confirmation tokens. Hashing also keeps every key the same
 * short length, which is what lets the column stay narrow.
 */
final class SignupAllowance {

  /**
   * Generous enough that an office or a mobile carrier behind one NAT never notices, tight enough
   * that a single host cannot stuff the list.
   */
  static final int PER_IP_PER_HOUR = 10;

  /** A real person needs one attempt, or a couple after a typo. Nobody needs a fourth. */
  static final int PER_ADDRESS_PER_HOUR = 3;

  private static final String IP_PREFIX = "ip:";
  private static final String EMAIL_PREFIX = "email:";
  private static final String RESEND_PREFIX = "resend:";

  private SignupAllowance() {}

  static String ipBucket(String clientIp) {
    return IP_PREFIX + clientIp;
  }

  static String addressBucket(String normalizedEmail) {
    return EMAIL_PREFIX + Sha256.hex(normalizedEmail);
  }

  /**
   * Keyed by the token's hash rather than by the address, because the resend endpoint never learns
   * the address — that is the point of it. One live token is one subscriber, so the bucket lands in
   * the same place either way.
   */
  static String resendBucket(String tokenHash) {
    return RESEND_PREFIX + tokenHash;
  }
}
