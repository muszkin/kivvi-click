package click.kivvi.domain.tracking;

/**
 * Mercure topic for the live event stream — ported from {@code App\Panel\EventStreamTopic}. One
 * topic per account keeps a tenant's events off every other tenant's dashboard, which is why the
 * topic is built here and never assembled elsewhere: {@code EventIngestionService} (the publisher)
 * and {@code EventsViewService} (which hands the topic to the SPA so it knows what to subscribe to)
 * both call this instead of each holding their own copy of the literal — the wave-2 architecture
 * rule ("Mercure topic built only server-side", rules-translated.md) requires the {@code
 * /accounts/} literal to live in exactly one place.
 */
public final class EventStreamTopic {

  private static final String PATTERN = "/accounts/%s/events";
  private static final String CURRENT_ACCOUNT = "1";

  private EventStreamTopic() {}

  public static String forAccount(String accountId) {
    return PATTERN.formatted(accountId);
  }

  public static String forCurrentAccount() {
    return forAccount(CURRENT_ACCOUNT);
  }
}
