package click.kivvi.infrastructure.tracking;

/**
 * The idempotency-claim seam {@link click.kivvi.application.tracking.EventIngestionService} depends
 * on. The only production implementation is {@link EventDedupStore} (Postgres-backed); tests
 * substitute an in-memory fake, mirroring how {@link
 * click.kivvi.infrastructure.mercure.MercurePublisher} keeps {@code EventIngestionService} unit
 * testable without a real hub.
 */
public interface EventDedupLedger {

  /**
   * @return {@code true} when {@code idempotencyId} was not already claimed (and is now recorded);
   *     {@code false} when it is a live duplicate.
   */
  boolean claim(String idempotencyId);
}
