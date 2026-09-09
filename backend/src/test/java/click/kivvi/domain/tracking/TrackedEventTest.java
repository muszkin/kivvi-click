package click.kivvi.domain.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TrackedEventTest {

  @Test
  @DisplayName("B15 event without idempotency_id is rejected")
  void payloadWithoutIdempotencyIdIsRejected() {
    assertThatThrownBy(() -> TrackedEvent.fromPayload(Map.of("type", "purchase")))
        .isInstanceOf(InvalidEventPayload.class)
        .hasMessage("Pole „idempotency_id” jest wymagane.");
  }

  @Test
  @DisplayName("B15 a blank idempotency_id is rejected the same as a missing one")
  void blankIdempotencyIdIsRejected() {
    assertThatThrownBy(
            () -> TrackedEvent.fromPayload(Map.of("idempotency_id", "   ", "type", "purchase")))
        .isInstanceOf(InvalidEventPayload.class)
        .hasMessage("Pole „idempotency_id” jest wymagane.");
  }

  @Test
  @DisplayName("B16 unknown event type is rejected")
  void unknownEventTypeIsRejected() {
    assertThatThrownBy(
            () -> TrackedEvent.fromPayload(Map.of("idempotency_id", "evt-1", "type", "teleport")))
        .isInstanceOf(InvalidEventPayload.class)
        .hasMessage("Nieznany typ zdarzenia „teleport”.");
  }

  @Test
  @DisplayName("an invalid occurred_at is rejected")
  void invalidOccurredAtIsRejected() {
    Map<String, Object> payload =
        Map.of(
            "idempotency_id", "evt-1",
            "type", "purchase",
            "occurred_at", "not-a-date");

    assertThatThrownBy(() -> TrackedEvent.fromPayload(payload))
        .isInstanceOf(InvalidEventPayload.class)
        .hasMessage("Pole „occurred_at” nie jest poprawną datą.");
  }

  @Test
  @DisplayName("a valid ISO-8601 occurred_at is parsed")
  void validOccurredAtIsParsed() {
    Map<String, Object> payload =
        Map.of(
            "idempotency_id", "evt-1",
            "type", "purchase",
            "occurred_at", "2026-08-26T14:42:08Z");

    TrackedEvent event = TrackedEvent.fromPayload(payload);

    assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-08-26T14:42:08Z"));
  }

  @Test
  @DisplayName("a missing occurred_at defaults to now")
  void missingOccurredAtDefaultsToNow() {
    Instant before = Instant.now();

    TrackedEvent event =
        TrackedEvent.fromPayload(Map.of("idempotency_id", "evt-1", "type", "purchase"));

    assertThat(event.occurredAt()).isBetween(before, Instant.now().plusSeconds(1));
  }

  @ParameterizedTest
  @DisplayName("every supported type is accepted")
  @EnumSource(EventType.class)
  void everySupportedTypeIsAccepted(EventType type) {
    TrackedEvent event =
        TrackedEvent.fromPayload(
            Map.of("idempotency_id", "evt-" + type.code(), "type", type.code()));

    assertThat(event.type()).isEqualTo(type);
  }

  @Test
  @DisplayName("missing optional fields read as blank strings, not null")
  void optionalFieldsDefaultToBlank() {
    TrackedEvent event =
        TrackedEvent.fromPayload(Map.of("idempotency_id", "evt-1", "type", "purchase"));

    assertThat(event.detail()).isEmpty();
    assertThat(event.customerId()).isEmpty();
    assertThat(event.customerName()).isEmpty();
    assertThat(event.siteName()).isEmpty();
  }

  @Test
  @DisplayName("a non-string field value reads as a blank string rather than failing")
  void nonStringFieldValueReadsAsBlank() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("idempotency_id", "evt-1");
    payload.put("type", "purchase");
    payload.put("detail", 42);

    TrackedEvent event = TrackedEvent.fromPayload(payload);

    assertThat(event.detail()).isEmpty();
  }
}
