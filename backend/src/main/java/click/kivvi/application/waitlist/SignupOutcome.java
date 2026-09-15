package click.kivvi.application.waitlist;

import click.kivvi.domain.waitlist.WaitlistSignupError;

/**
 * What {@link WaitlistSignupService#signUp} decided. A result rather than an exception: none of
 * these three is exceptional — a visitor mistyping an address, forgetting the consent box, or
 * submitting once too often are all ordinary things that happen on a public form, and the
 * controller has a different response for each.
 *
 * <p>Sealed so that controller adding a fourth case cannot be forgotten: a {@code switch} over
 * these is exhaustive and the compiler says so.
 */
public sealed interface SignupOutcome {

  /**
   * The signup was taken. Also what a honeypot hit and a repeat of an address already on the list
   * return — a bot learns nothing from the response, and a visitor who signs up twice is told the
   * same reassuring thing as the first time rather than being shown an error for succeeding.
   */
  record Accepted() implements SignupOutcome {}

  /** Nothing was stored; the form comes back with {@code error}'s message beside the field. */
  record Rejected(WaitlistSignupError error) implements SignupOutcome {}

  /** Nothing was stored; the caller is over its hourly allowance. */
  record ThrottleExceeded() implements SignupOutcome {}
}
