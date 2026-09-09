package click.kivvi.application.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.tracking.EventType;
import click.kivvi.domain.tracking.TrackedEvent;
import click.kivvi.infrastructure.mercure.MercurePublisher;
import click.kivvi.infrastructure.tracking.EventDedupLedger;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pure unit coverage of {@link EventIngestionService} — an in-memory {@link EventDedupLedger} fake
 * and a recording {@link MercurePublisher} fake stand in for the Postgres-backed store and the real
 * hub, mirroring how the old stack's own test swapped in {@code MockHub}. No Spring context, no I/O
 * — {@link click.kivvi.CollectApiIT} and {@link
 * click.kivvi.infrastructure.tracking.EventDedupStoreIT} cover the real JDBC/HTTP paths.
 */
class EventIngestionServiceTest {

  private static final class FakeEventDedupLedger implements EventDedupLedger {
    private final Set<String> claimed = new HashSet<>();

    @Override
    public boolean claim(String idempotencyId) {
      return claimed.add(idempotencyId);
    }
  }

  private static final class RecordingMercurePublisher implements MercurePublisher {
    record Call(String topic, String data) {}

    private final List<Call> calls = new ArrayList<>();

    @Override
    public void publish(String topic, String data) {
      calls.add(new Call(topic, data));
    }
  }

  private final FakeEventDedupLedger dedupLedger = new FakeEventDedupLedger();
  private final RecordingMercurePublisher publisher = new RecordingMercurePublisher();
  // Mirrors application.yml's spring.jackson.default-property-inclusion: non_null — Spring
  // Boot's own autoconfiguration applies that to the injected bean, so a hand-built mapper for
  // this Spring-free unit test needs the same setting to see the same wire shape (blank
  // optional fields omitted, not sent as null).
  private final JsonMapper objectMapper =
      JsonMapper.builder()
          .changeDefaultPropertyInclusion(v -> v.withValueInclusion(JsonInclude.Include.NON_NULL))
          .build();
  private final EventIngestionService service =
      new EventIngestionService(dedupLedger, publisher, objectMapper);

  @Test
  @DisplayName(
      "B17 an accepted event is published once on /accounts/1/events with the translated type,"
          + " detail, customer, site and the add_to_cart icon/tone from EventFeed::TYPES")
  void publishesAcceptedEventOnceWithTranslatedFields() {
    TrackedEvent event =
        new TrackedEvent(
            "evt-1",
            EventType.ADD_TO_CART,
            Instant.parse("2026-08-26T14:42:08Z"),
            "Zielona herbata Sencha 100g",
            "c_1001",
            "Hania Kowalska",
            "aureashop.pl");

    boolean accepted = service.ingest(event);

    assertThat(accepted).isTrue();
    assertThat(publisher.calls).hasSize(1);
    RecordingMercurePublisher.Call call = publisher.calls.get(0);
    assertThat(call.topic()).isEqualTo("/accounts/1/events");

    JsonNode published = eventNode(call.data());
    assertThat(published.get("type").asString()).isEqualTo("Dodanie do koszyka");
    assertThat(published.get("typeIcon").asString()).isEqualTo("cart");
    assertThat(published.get("tone").asString()).isEqualTo("accent");
    assertThat(published.get("detail").asString()).isEqualTo("Zielona herbata Sencha 100g");
    assertThat(published.get("customerId").asString()).isEqualTo("c_1001");
    assertThat(published.get("customerName").asString()).isEqualTo("Hania Kowalska");
    assertThat(published.get("siteName").asString()).isEqualTo("aureashop.pl");
    assertThat(published.get("siteColor").asString()).isEqualTo("#7a8763");
  }

  @Test
  @DisplayName(
      "a type outside EventFeed::TYPES's eight-entry table publishes the generic 'activity' icon"
          + " and an empty tone")
  void typeOutsideTheFeedTableFallsBackToGenericIconAndEmptyTone() {
    TrackedEvent event =
        new TrackedEvent("evt-2", EventType.EMAIL_OPEN, Instant.now(), "", "", "", "");

    service.ingest(event);

    JsonNode published = eventNode(publisher.calls.get(0).data());
    assertThat(published.get("typeIcon").asString()).isEqualTo("activity");
    assertThat(published.get("tone").asString()).isEqualTo("");
  }

  @Test
  @DisplayName("blank optional fields are omitted from the published payload, not sent as \"\"")
  void blankOptionalFieldsAreOmitted() {
    TrackedEvent event = new TrackedEvent("evt-3", EventType.SEARCH, Instant.now(), "", "", "", "");

    service.ingest(event);

    JsonNode published = eventNode(publisher.calls.get(0).data());
    assertThat(published.has("detail")).isFalse();
    assertThat(published.has("customerId")).isFalse();
    assertThat(published.has("customerName")).isFalse();
    assertThat(published.has("siteName")).isFalse();
    assertThat(published.has("siteColor")).isFalse();
  }

  @Test
  @DisplayName("B18 the same idempotency id is never published twice")
  void sameIdempotencyIdIsNeverPublishedTwice() {
    TrackedEvent event =
        new TrackedEvent(
            "evt-dup",
            EventType.PURCHASE,
            Instant.now(),
            "412,00 PLN",
            "c_1001",
            "Hania",
            "aureashop.pl");

    boolean first = service.ingest(event);
    boolean second = service.ingest(event);

    assertThat(first).isTrue();
    assertThat(second).isFalse();
    assertThat(publisher.calls).hasSize(1);
  }

  private JsonNode eventNode(String publishedJson) {
    return objectMapper.readTree(publishedJson).get("event");
  }
}
