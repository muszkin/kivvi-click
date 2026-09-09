package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WidgetViewServiceTest {

  private final WidgetViewService widgetViewService = new WidgetViewService();

  @Test
  @DisplayName("B29 the popup index carries 5 cards matching the oracle's names and metrics")
  void cardsMatchTheOracle() {
    WidgetViewService.ListPayload payload = widgetViewService.list(null);

    assertThat(payload.cards())
        .extracting(WidgetViewService.Card::title)
        .containsExactly(
            "Exit intent — 10% rabatu",
            "Pasek darmowej dostawy od 199 zł",
            "Slide-in: zapisz się do newslettera",
            "Social proof — „ktoś właśnie kupił”",
            "Promo wakacyjne — full screen");
    assertThat(payload.cards())
        .extracting(card -> card.metrics().get(0).value(), card -> card.metrics().get(1).value())
        .containsExactly(
            tuple("48 210", "7,4%"),
            tuple("182 400", "2,1%"),
            tuple("62 100", "4,8%"),
            tuple("—", "—"),
            tuple("—", "—"));
  }

  @Test
  @DisplayName("B29 a missing ?preview= falls back to the first seeded widget (p1, modal)")
  void missingPreviewFallsBackToTheFirstWidget() {
    WidgetViewService.ListPayload payload = widgetViewService.list(null);

    assertThat(payload.selected().id()).isEqualTo("p1");
    assertThat(payload.selected().type()).isEqualTo("modal");
    assertThat(payload.selected().name()).isEqualTo("Exit intent — 10% rabatu");
  }

  @Test
  @DisplayName("B29 ?preview=p2 selects the banner widget's own type and content")
  void knownPreviewSelectsItsOwnWidget() {
    WidgetViewService.ListPayload payload = widgetViewService.list("p2");

    assertThat(payload.selected().id()).isEqualTo("p2");
    assertThat(payload.selected().type()).isEqualTo("banner");
    assertThat(payload.selected().content().kicker()).isEqualTo("DARMOWA DOSTAWA");
    assertThat(payload.selected().content().body()).isNull();
  }

  @Test
  @DisplayName(
      "B29 an unknown ?preview= id never 404s — falls back to the same blank-draft shape as an"
          + " unmatched editor id")
  void unknownPreviewFallsBackToTheBlankDraftShapeWithoutFailing() {
    WidgetViewService.ListPayload payload = widgetViewService.list("p999");

    assertThat(payload.selected().id()).isEqualTo("p999");
    assertThat(payload.selected().name()).isEqualTo("Nowy widget");
    assertThat(payload.selected().type()).isEqualTo("modal");
  }

  @Test
  @DisplayName("B29 GET .../popups/p1 resolves the known widget's name, meta and own type")
  void knownWidgetEditorMatchesTheOracle() {
    WidgetViewService.EditorPayload payload = widgetViewService.editor("p1", null, null);

    assertThat(payload.widget().id()).isEqualTo("p1");
    assertThat(payload.widget().name()).isEqualTo("Exit intent — 10% rabatu");
    assertThat(payload.widget().meta())
        .isEqualTo("Aktywny na aureashop.pl · 48 210 wyświetleń · 7,4% konwersji");
    assertThat(payload.widget().type()).isEqualTo("modal");
    assertThat(payload.widget().content().title()).isEqualTo("Zostań na 10% taniej");
  }

  @Test
  @DisplayName(
      "B29 an id with no seeded widget (including \"new\") falls back to the blank-draft shape")
  void unknownIdFallsBackToTheBlankWidget() {
    WidgetViewService.EditorWidget fromNew = widgetViewService.editor("new", null, null).widget();
    WidgetViewService.EditorWidget fromUnseeded =
        widgetViewService.editor("p999", null, null).widget();

    for (WidgetViewService.EditorWidget widget : java.util.List.of(fromNew, fromUnseeded)) {
      assertThat(widget.name()).isEqualTo("Nowy widget");
      assertThat(widget.meta()).isEqualTo("Szkic · nieopublikowany");
      assertThat(widget.type()).isEqualTo("modal");
    }
    assertThat(fromNew.id()).isEqualTo("new");
    assertThat(fromUnseeded.id()).isEqualTo("p999");
  }

  @Test
  @DisplayName("B29 ?type= overrides the shown content's shape but never the widget's own name")
  void typeQueryOverridesContentOnly() {
    WidgetViewService.EditorPayload payload = widgetViewService.editor("p1", "banner", null);

    assertThat(payload.widget().type()).isEqualTo("banner");
    assertThat(payload.widget().name()).isEqualTo("Exit intent — 10% rabatu");
    assertThat(payload.widget().content().kicker()).isEqualTo("DARMOWA DOSTAWA");
    assertThat(payload.widget().content().title()).isEqualTo("Od 199 zł wysyłamy na nasz koszt");
    assertThat(payload.widget().content().body()).isNull();
  }

  @Test
  @DisplayName("B29 a missing ?type= falls back to the widget's own type")
  void missingTypeFallsBackToTheWidgetsOwnType() {
    WidgetViewService.EditorPayload payload = widgetViewService.editor("p2", null, null);

    assertThat(payload.widget().type()).isEqualTo("banner");
  }

  @Test
  @DisplayName(
      "B29 ?device=mobile switches the viewport chip; anything else (including absent) is"
          + " desktop")
  void deviceQuerySelectsTheViewport() {
    assertThat(widgetViewService.editor("p1", null, "mobile").device()).isEqualTo("mobile");
    assertThat(widgetViewService.editor("p1", null, "mobile").viewport()).isEqualTo("390 × 844");
    assertThat(widgetViewService.editor("p1", null, null).device()).isEqualTo("desktop");
    assertThat(widgetViewService.editor("p1", null, null).viewport()).isEqualTo("1440 × 900");
    assertThat(widgetViewService.editor("p1", null, "bogus").device()).isEqualTo("desktop");
  }

  @Test
  @DisplayName(
      "B29 the editor payload always carries 5 types, 10 blocks, 4 variables, 3 triggers and 4"
          + " audience rows")
  void editorPayloadShapeIsConstantAcrossIds() {
    WidgetViewService.EditorPayload known = widgetViewService.editor("p1", null, null);
    WidgetViewService.EditorPayload blank = widgetViewService.editor("new", null, null);

    for (WidgetViewService.EditorPayload payload : java.util.List.of(known, blank)) {
      assertThat(payload.types()).hasSize(5);
      assertThat(payload.blocks()).hasSize(10);
      assertThat(payload.variables()).hasSize(4);
      assertThat(payload.triggers()).hasSize(3);
      assertThat(payload.audience()).hasSize(4);
      assertThat(payload.accentColors()).hasSize(4);
    }
  }
}
