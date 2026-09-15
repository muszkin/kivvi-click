package click.kivvi.domain.waitlist;

/**
 * Where a waitlist subscriber stands. Only {@link #PENDING} is ever written today: PIO-71 moves a
 * row to {@link #CONFIRMED} once the double opt-in link is followed, and PIO-72's panel screen is
 * what will surface {@link #UNSUBSCRIBED}.
 *
 * <p>The {@link #value()} strings are the {@code waitlist_subscriber.status} column's contents —
 * lower case, stable, and not derived from {@link #name()} so a future rename of a constant cannot
 * silently rewrite what is already stored.
 */
public enum SubscriberStatus {
  PENDING("pending"),
  CONFIRMED("confirmed"),
  UNSUBSCRIBED("unsubscribed");

  private final String value;

  SubscriberStatus(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
