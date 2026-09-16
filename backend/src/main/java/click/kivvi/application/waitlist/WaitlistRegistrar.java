package click.kivvi.application.waitlist;

import click.kivvi.domain.waitlist.WaitlistSignup;

/**
 * The step {@link WaitlistSignupService} hands an accepted submission to: record the address and,
 * if it is still unconfirmed, put a confirmation link in the post.
 *
 * <p>A seam rather than a direct call so the decision service — which is about honeypots, rate
 * limits and validation — is testable without a database, a template engine and an outbox behind
 * it. {@link WaitlistConfirmationService} is the only implementation.
 */
public interface WaitlistRegistrar {

  /**
   * @return {@code true} when the address was not on the list and now is; {@code false} when it was
   *     already there. Either way a pending address ends up with a fresh confirmation link on its
   *     way — signing up again is how somebody asks for another one.
   */
  boolean register(WaitlistSignup signup);
}
