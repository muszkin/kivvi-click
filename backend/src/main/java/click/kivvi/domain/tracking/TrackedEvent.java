package click.kivvi.domain.tracking;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * One visitor event as it arrives from the tracking script — ported from {@code
 * App\Tracking\TrackedEvent}.
 *
 * <p>The idempotency id is mandatory: the tracking script retries on a flaky connection, and a
 * retried purchase must not show up twice in the stream or count twice in revenue.
 */
public record TrackedEvent(
    String idempotencyId,
    EventType type,
    Instant occurredAt,
    String detail,
    String customerId,
    String customerName,
    String siteName) {

  /**
   * @throws InvalidEventPayload with the exact Polish message {@code CollectController} returns,
   *     matching {@code TrackedEvent::fromPayload} field for field.
   */
  public static TrackedEvent fromPayload(Map<String, Object> payload) {
    String idempotencyId = readString(payload, "idempotency_id");
    if (idempotencyId.isEmpty()) {
      throw new InvalidEventPayload("Pole „idempotency_id” jest wymagane.");
    }

    String typeCode = readString(payload, "type");
    EventType type =
        EventType.fromCode(typeCode)
            .orElseThrow(
                () -> new InvalidEventPayload("Nieznany typ zdarzenia „" + typeCode + "”."));

    return new TrackedEvent(
        idempotencyId,
        type,
        readMoment(payload),
        readString(payload, "detail"),
        readString(payload, "customer_id"),
        readString(payload, "customer_name"),
        readString(payload, "site"));
  }

  private static String readString(Map<String, Object> payload, String key) {
    Object value = payload.get(key);
    return value instanceof String text ? text.trim() : "";
  }

  private static Instant readMoment(Map<String, Object> payload) {
    String occurredAt = readString(payload, "occurred_at");
    if (occurredAt.isEmpty()) {
      return Instant.now();
    }
    try {
      return parseFlexible(occurredAt);
    } catch (DateTimeException exception) {
      throw new InvalidEventPayload("Pole „occurred_at” nie jest poprawną datą.", exception);
    }
  }

  /**
   * Accepts the ISO-8601 shapes a tracking script realistically sends (with a zone offset, or bare
   * local time assumed UTC) — mirrors {@code new \DateTimeImmutable($value)}'s permissive parsing
   * without adopting its full free-form grammar.
   */
  private static Instant parseFlexible(String value) {
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ignored) {
      // Fall through to the next shape.
    }
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException ignored) {
      // Fall through to the next shape.
    }
    return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
  }
}
