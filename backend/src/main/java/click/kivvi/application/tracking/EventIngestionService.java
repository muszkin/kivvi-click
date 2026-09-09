package click.kivvi.application.tracking;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.tracking.EventStreamTopic;
import click.kivvi.domain.tracking.TrackedEvent;
import click.kivvi.domain.tracking.TrackedSite;
import click.kivvi.infrastructure.mercure.MercurePublisher;
import click.kivvi.infrastructure.tracking.EventDedupLedger;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Accepts a tracked event, drops duplicates and publishes the accepted ones to the account's
 * Mercure topic — ported from {@code EventIngestion}. DEV-3: the old stack published
 * server-rendered row HTML (so the row markup lived in exactly one Twig template); this stack has
 * no server-side template to render into, so it publishes the row's data as JSON instead — the SPA
 * renders it with the same {@code EventRow} component the initial page load already used. The
 * translated {@code type} label is always Polish here (unlike {@code GET /api/v1/{locale}/events},
 * which is locale-scoped): {@code /collect} carries no locale, exactly like the old {@code
 * EventIngestion}, whose translator resolved against the request's default locale rather than a
 * per-page one.
 */
@Service
public class EventIngestionService {

  private static final DateTimeFormatter TIME_OF_DAY = DateTimeFormatter.ofPattern("HH:mm:ss");

  /** The wire shape published to the hub: {@code {"event": {...}}} (DEV-3). */
  private record PublishedEnvelope(PublishedRow event) {}

  private record PublishedRow(
      String time,
      String type,
      String typeIcon,
      String tone,
      String detail,
      String customerId,
      String customerName,
      String siteName,
      String siteColor) {}

  private final EventDedupLedger dedupStore;
  private final MercurePublisher publisher;
  private final ObjectMapper objectMapper;

  public EventIngestionService(
      EventDedupLedger dedupStore, MercurePublisher publisher, ObjectMapper objectMapper) {
    this.dedupStore = dedupStore;
    this.publisher = publisher;
    this.objectMapper = objectMapper;
  }

  /**
   * @return {@code true} when the event was newly accepted and published, {@code false} when its
   *     idempotency id was already claimed
   */
  public boolean ingest(TrackedEvent event) {
    if (!dedupStore.claim(event.idempotencyId())) {
      return false;
    }
    publisher.publish(
        EventStreamTopic.forCurrentAccount(), objectMapper.writeValueAsString(toEnvelope(event)));
    return true;
  }

  private PublishedEnvelope toEnvelope(TrackedEvent event) {
    String siteName = blankToNull(event.siteName());
    return new PublishedEnvelope(
        new PublishedRow(
            TIME_OF_DAY.format(event.occurredAt().atZone(ZoneId.systemDefault())),
            event.type().label(SupportedLocale.PL),
            event.type().icon(),
            event.type().tone(),
            blankToNull(event.detail()),
            blankToNull(event.customerId()),
            blankToNull(event.customerName()),
            siteName,
            siteName == null ? null : TrackedSite.colorForSiteName(siteName)));
  }

  private static String blankToNull(String value) {
    return value.isBlank() ? null : value;
  }
}
