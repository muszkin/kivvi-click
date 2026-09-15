package click.kivvi.application.waitlist;

/**
 * What following a confirmation link turned out to mean. A result rather than an exception, for the
 * same reason {@link SignupOutcome} is one: none of these four is a fault. People click old links,
 * click the same link twice, and mistype URLs; the page simply has something different to say in
 * each case.
 *
 * <p>Sealed so a fifth state cannot be added without the controller's {@code switch} being made to
 * account for it.
 */
public sealed interface ConfirmationOutcome {

  /** The address is now confirmed, and this request is what confirmed it. */
  record Confirmed() implements ConfirmationOutcome {}

  /** A link that already did its job. Reassurance, not an error. */
  record AlreadyConfirmed() implements ConfirmationOutcome {}

  /**
   * The link is genuine but past its deadline.
   *
   * @param token echoed back so the page can offer a new link without asking for the address again
   */
  record Expired(String token) implements ConfirmationOutcome {}

  /** No such link — mistyped, truncated by a mail client, or from a list that no longer exists. */
  record Unknown() implements ConfirmationOutcome {}
}
