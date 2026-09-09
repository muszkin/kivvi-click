package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutomationsViewServiceTest {

  private final AutomationsViewService automationsViewService = new AutomationsViewService();

  @Test
  @DisplayName("B26 4 status filters, plain-space-grouped counts 6/4/1/1, 'all' active by default")
  void statusFiltersMatchTheOracle() {
    AutomationsViewService.ListPayload payload = automationsViewService.list("all");

    assertThat(payload.filters())
        .extracting(
            AutomationsViewService.Filter::label,
            AutomationsViewService.Filter::count,
            AutomationsViewService.Filter::active)
        .containsExactly(
            tuple("Wszystkie", "6", true),
            tuple("Aktywne", "4", false),
            tuple("Wstrzymane", "1", false),
            tuple("Szkice", "1", false));
    assertThat(payload.filters().get(0).icon()).isEqualTo("grid");
    assertThat(payload.filters().get(1).icon()).isNull();
  }

  @Test
  @DisplayName("B26 ?status=active marks only the 'Aktywne' chip active")
  void activeStatusMarksOnlyAktywneActive() {
    AutomationsViewService.ListPayload payload = automationsViewService.list("active");

    assertThat(payload.filters())
        .extracting(AutomationsViewService.Filter::active)
        .containsExactly(false, true, false, false);
  }

  @Test
  @DisplayName(
      "B26 an unrecognised status marks no chip active — same ternary as AutomationController")
  void unrecognisedStatusMarksNoChipActive() {
    AutomationsViewService.ListPayload payload = automationsViewService.list("bogus");

    assertThat(payload.filters())
        .extracting(AutomationsViewService.Filter::active)
        .containsExactly(false, false, false, false);
  }

  @Test
  @DisplayName(
      "B26 the status filter never actually filters — always 6 cards regardless of ?status=")
  void statusNeverFiltersTheCardList() {
    assertThat(automationsViewService.list("all").automations()).hasSize(6);
    assertThat(automationsViewService.list("active").automations()).hasSize(6);
    assertThat(automationsViewService.list("draft").automations()).hasSize(6);
  }

  @Test
  @DisplayName("B26 the first card matches the oracle: title, chips, three metrics")
  void firstCardMatchesTheOracle() {
    AutomationsViewService.AutomationCard first =
        automationsViewService.list("all").automations().get(0);

    assertThat(first.title()).isEqualTo("Powrót do porzuconego koszyka");
    assertThat(first.chips())
        .extracting(AutomationsViewService.Chip::label, AutomationsViewService.Chip::tone)
        .containsExactly(
            tuple("Aktywna", "good"),
            tuple("Porzucenie koszyka", "accent"),
            tuple("email", "brown"),
            tuple("popup", "brown"));
    assertThat(first.metrics())
        .extracting(AutomationsViewService.Metric::value, AutomationsViewService.Metric::label)
        .containsExactly(
            tuple("1 287", "uruchomień (7d)"),
            tuple("18,4%", "konwersja"),
            tuple("24 800 zł", "przychód (7d)"));
    assertThat(first.metrics().get(1).color()).isEqualTo("var(--good)");
    assertThat(first.metrics().get(0).color()).isNull();
    assertThat(first.action()).isEqualTo("go-automation");
    assertThat(first.payload()).isEqualTo("a1");
  }

  @Test
  @DisplayName("B26 a zero-conversion automation renders '—' for conversion and revenue, muted")
  void zeroConversionAutomationRendersEmDash() {
    AutomationsViewService.AutomationCard winback =
        automationsViewService.list("all").automations().get(4);

    assertThat(winback.title()).isEqualTo("Win-back po 60 dniach nieaktywności");
    assertThat(winback.metrics().get(1).value()).isEqualTo("—");
    assertThat(winback.metrics().get(1).color()).isEqualTo("var(--fg-muted)");
    assertThat(winback.metrics().get(2).value()).isEqualTo("—");
  }

  @Test
  @DisplayName("B26 editor(\"a1\", null) resolves to the list view by default")
  void editorDefaultsToListView() {
    AutomationsViewService.EditorPayload payload = automationsViewService.editor("a1", null);

    assertThat(payload.view()).isEqualTo("list");
    assertThat(payload.automation().name()).isEqualTo("Powrót do porzuconego koszyka");
    assertThat(payload.steps()).hasSize(3);
    assertThat(payload.nodes()).hasSize(6);
    assertThat(payload.edges()).hasSize(5);
    assertThat(payload.simulation()).hasSize(3);
    assertThat(payload.tabs()).hasSize(4);
  }

  @Test
  @DisplayName("B26 editor(\"a1\", \"flow\") resolves to the flow view; any other value stays list")
  void editorResolvesViewExactly() {
    assertThat(automationsViewService.editor("a1", "flow").view()).isEqualTo("flow");
    assertThat(automationsViewService.editor("a1", "FLOW").view()).isEqualTo("list");
    assertThat(automationsViewService.editor("a1", "").view()).isEqualTo("list");
  }

  @Test
  @DisplayName(
      "B26 editor(\"new\", ...) carries the same fixed rule content as a seeded automation")
  void editorForNewCarriesTheSameFixedRuleContent() {
    AutomationsViewService.EditorPayload payload = automationsViewService.editor("new", "list");

    assertThat(payload.automation().name()).isEqualTo("Nowa automatyzacja");
    assertThat(payload.automation().status()).isEqualTo("draft");
    assertThat(payload.steps()).hasSize(3);
    assertThat(payload.nodes()).hasSize(6);
  }
}
