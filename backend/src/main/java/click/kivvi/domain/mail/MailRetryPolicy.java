package click.kivvi.domain.mail;

import java.time.Duration;
import java.util.List;

/**
 * How long to wait before trying a failed message again, and when to stop trying.
 *
 * <p>The steps widen fast on purpose. The first retry covers a relay hiccup a minute later; by the
 * last one the gap is ten hours, which is long enough to ride out a provider outage without burning
 * the budget on a service that is plainly down. The five delays are the ticket's own schedule, so
 * the ceiling is the first attempt plus five retries — a little over thirteen hours of trying. Past
 * that the address is almost certainly unreachable, and a message retried forever is a message
 * nobody ever notices is stuck.
 */
public final class MailRetryPolicy {

  private static final List<Duration> BACKOFF =
      List.of(
          Duration.ofMinutes(1),
          Duration.ofMinutes(5),
          Duration.ofMinutes(25),
          Duration.ofHours(2),
          Duration.ofHours(10));

  /**
   * After this many attempts a message is parked as {@code failed} and left alone: the first try
   * plus one per backoff step.
   */
  public static final int MAX_ATTEMPTS = BACKOFF.size() + 1;

  private MailRetryPolicy() {}

  /**
   * @param attemptsSoFar how many attempts have already been made, including the one that just
   *     failed
   * @return how long to wait before the next one
   * @throws IllegalArgumentException when the attempt budget is already spent — the caller is meant
   *     to check {@link #isExhausted} first, and asking for a delay that does not exist is a bug,
   *     not a state to paper over
   */
  public static Duration backoffAfter(int attemptsSoFar) {
    if (isExhausted(attemptsSoFar)) {
      throw new IllegalArgumentException(
          "No backoff after attempt " + attemptsSoFar + ": the attempt budget is spent.");
    }
    return BACKOFF.get(Math.max(attemptsSoFar, 1) - 1);
  }

  public static boolean isExhausted(int attemptsSoFar) {
    return attemptsSoFar >= MAX_ATTEMPTS;
  }
}
