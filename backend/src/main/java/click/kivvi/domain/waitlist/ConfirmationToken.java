package click.kivvi.domain.waitlist;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A freshly issued confirmation token together with the moment it stops working.
 *
 * <p>Seven days is long enough to survive a holiday and short enough that a link forwarded or left
 * in an archived mailbox does not stay live indefinitely. The token is single-use as well: the
 * store clears the hash on confirmation, so following the same link twice hits the "already
 * confirmed" page rather than re-running the confirmation.
 *
 * <p>There is deliberately no expiry on the unsubscribe token ({@link OpaqueToken} on its own). It
 * travels in the footer of every message this system will ever send and has to still work a year
 * later; an unsubscribe link that quietly lapses leaves writing to support as the only way off the
 * list, which is exactly what the law requires not to be the case.
 */
public record ConfirmationToken(OpaqueToken token, Instant expiresAt) {

  public static final Duration VALIDITY = Duration.ofDays(7);

  public ConfirmationToken {
    Objects.requireNonNull(token, "token");
    Objects.requireNonNull(expiresAt, "expiresAt");
  }

  public static ConfirmationToken issue(Instant now) {
    return new ConfirmationToken(OpaqueToken.generate(), now.plus(VALIDITY));
  }

  /** The value that goes into the link. */
  public String value() {
    return token.value();
  }

  /** The value that goes into {@code waitlist_subscriber.confirmation_token_hash}. */
  public String hash() {
    return token.hash();
  }

  /**
   * Whether a stored expiry has passed. Static because the check is nearly always made against a
   * timestamp read back from the database rather than against a token this process just issued.
   *
   * <p>A missing expiry counts as lapsed: a row whose token hash survived without its deadline is
   * corrupt, and treating it as live would hand out an eternal confirmation link.
   */
  public static boolean hasLapsed(Instant expiresAt, Instant now) {
    return expiresAt == null || !now.isBefore(expiresAt);
  }

  public boolean isExpired(Instant now) {
    return hasLapsed(expiresAt, now);
  }
}
