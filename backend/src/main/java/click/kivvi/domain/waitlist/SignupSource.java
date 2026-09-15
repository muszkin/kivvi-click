package click.kivvi.domain.waitlist;

/**
 * Where a signup came from. One source today — the public landing page's hero form — but the column
 * exists from the start so a later capture point (an in-app invite, an imported list) is
 * distinguishable from organic landing traffic without a migration.
 *
 * <p>As in {@link SubscriberStatus}, {@link #value()} is the stored string and is deliberately not
 * derived from {@link #name()}.
 */
public enum SignupSource {
  LANDING("landing");

  private final String value;

  SignupSource(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
