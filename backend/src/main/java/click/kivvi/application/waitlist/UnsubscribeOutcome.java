package click.kivvi.application.waitlist;

/**
 * What following an unsubscribe link turned out to mean. Two states only: either the link belongs
 * to somebody, in which case they are off the list — whether that was already true or not — or it
 * belongs to nobody.
 */
public sealed interface UnsubscribeOutcome {

  /** Off the list. Also the answer to a second click on the same link. */
  record Unsubscribed() implements UnsubscribeOutcome {}

  record Unknown() implements UnsubscribeOutcome {}
}
