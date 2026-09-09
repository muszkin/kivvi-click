package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomersViewServiceTest {

  private final CustomersViewService customersViewService = new CustomersViewService();
  private final Instant now = Instant.parse("2026-09-08T13:52:00Z");

  @Test
  @DisplayName(
      "B25 the subtitle is narrow-space-grouped: '4 218 zidentyfikowanych klientów · 7 632 anonimowych sesji'")
  void subtitleMatchesTheOracle() {
    CustomersViewService.ListPayload payload = customersViewService.list(1, now);

    assertThat(payload.subtitle())
        .isEqualTo("4 218 zidentyfikowanych klientów · 7 632 anonimowych sesji");
  }

  @Test
  @DisplayName(
      "B25 six segment tiles, plain-space-grouped counts, 'Wszyscy' active with the users icon")
  void segmentsMatchTheOracle() {
    CustomersViewService.ListPayload payload = customersViewService.list(1, now);

    assertThat(payload.segments())
        .extracting(
            CustomersViewService.SegmentTile::label, CustomersViewService.SegmentTile::count)
        .containsExactly(
            tuple("Wszyscy", "4 218"),
            tuple("VIP", "142"),
            tuple("Nowi (7 dni)", "412"),
            tuple("Porzucone koszyki", "287"),
            tuple("Subskrybenci", "1 829"),
            tuple("Reaktywować", "612"));
    assertThat(payload.segments().get(0).icon()).isEqualTo("users");
    assertThat(payload.segments().get(0).active()).isTrue();
    assertThat(payload.segments().get(1).icon()).isNull();
  }

  @Test
  @DisplayName("B25 24 rows, revenue money-formatted, lastSeen computed against the request clock")
  void rowsMatchTheOracle() {
    CustomersViewService.ListPayload payload = customersViewService.list(1, now);

    assertThat(payload.rows()).hasSize(24);
    CustomersViewService.CustomerRow first = payload.rows().get(0);
    assertThat(first.name()).isEqualTo("Anna K.");
    assertThat(first.revenue()).isEqualTo("49 zł");
    assertThat(first.lastSeen()).isEqualTo("teraz");
    CustomersViewService.CustomerRow second = payload.rows().get(1);
    assertThat(second.revenue()).isEqualTo("186 zł");
    assertThat(second.lastSeen()).isEqualTo("1 min temu");
  }

  @Test
  @DisplayName(
      "B25 page is clamped to at least 1; pages is always 192 (the pager, not real pagination)")
  void pageIsClampedAndPagesIsFixed() {
    assertThat(customersViewService.list(0, now).page()).isEqualTo(1);
    assertThat(customersViewService.list(-5, now).page()).isEqualTo(1);
    assertThat(customersViewService.list(2, now).pages()).isEqualTo(192);
    assertThat(customersViewService.list(2, now).rows()).hasSize(24);
  }

  @Test
  @DisplayName(
      "B25 the 360 profile: 7 facts, 3 active automations, 5 tabs, 3 scores, 9 timeline entries")
  void detailPayloadShapeMatchesTheOracle() {
    CustomersViewService.DetailPayload payload = customersViewService.detail("c_1000", now);

    assertThat(payload.facts()).hasSize(7);
    assertThat(payload.automations()).hasSize(3);
    assertThat(payload.tabs()).hasSize(5);
    assertThat(payload.scores()).hasSize(3);
    assertThat(payload.timeline()).hasSize(9);
  }

  @Test
  @DisplayName(
      "B25 the profile's tags are always VIP/subskrybent/PL, independent of the customer's own segment")
  void profileTagsAreAlwaysTheSameThree() {
    CustomersViewService.DetailPayload payload = customersViewService.detail("c_1001", now);

    assertThat(payload.customer().tags())
        .extracting(CustomersViewService.Tag::label, CustomersViewService.Tag::tone)
        .containsExactly(tuple("VIP", "accent"), tuple("subskrybent", "brown"), tuple("PL", null));
  }

  @Test
  @DisplayName(
      "B25 profileSub reads '<email> · klient od 14 stycznia 2024 · ostatnia aktywność <lastSeen>'")
  void profileSubMatchesTheOracle() {
    CustomersViewService.DetailPayload payload = customersViewService.detail("c_1000", now);

    assertThat(payload.profileSub())
        .isEqualTo("anna.k@example.com · klient od 14 stycznia 2024 · ostatnia aktywność teraz");
  }

  @Test
  @DisplayName(
      "B25 the timeline's 'Zalogowanie' entry carries the customer's own email as its detail")
  void timelineLoginEntryCarriesTheCustomersEmail() {
    CustomersViewService.DetailPayload payload = customersViewService.detail("c_1000", now);

    assertThat(payload.timeline())
        .filteredOn(entry -> "Zalogowanie".equals(entry.title()))
        .extracting(CustomersViewService.TimelineEntry::detail)
        .containsExactly("anna.k@example.com");
  }
}
