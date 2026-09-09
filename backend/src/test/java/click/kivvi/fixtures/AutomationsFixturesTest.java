package click.kivvi.fixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutomationsFixturesTest {

  @Test
  @DisplayName("B26 six seeded automations, in the oracle's own order")
  void sixSeededAutomationsInOracleOrder() {
    assertThat(AutomationsFixtures.all())
        .extracting(
            AutomationsFixtures.Automation::id,
            AutomationsFixtures.Automation::name,
            AutomationsFixtures.Automation::status,
            AutomationsFixtures.Automation::trigger)
        .containsExactly(
            tuple("a1", "Powrót do porzuconego koszyka", "active", "cart_abandon"),
            tuple("a2", "Powitanie po rejestracji", "active", "signup"),
            tuple("a3", "Rekomendacje „podobne produkty”", "active", "pageview"),
            tuple("a4", "Kupon dla VIP po 5 zamówieniach", "active", "purchase"),
            tuple("a5", "Win-back po 60 dniach nieaktywności", "paused", "inactivity"),
            tuple("a6", "Powiadomienie o powrocie produktu", "draft", "product_back_in_stock"));
  }

  @Test
  @DisplayName("B26 header(\"a1\") is the seeded identity with its status label suffixed")
  void headerOfASeededAutomation() {
    AutomationsFixtures.Header header = AutomationsFixtures.header("a1");

    assertThat(header.id()).isEqualTo("a1");
    assertThat(header.name()).isEqualTo("Powrót do porzuconego koszyka");
    assertThat(header.status()).isEqualTo("active");
    assertThat(header.statusLabel())
        .isEqualTo("Aktywna · Edytuj logikę uruchamiania, warunki i akcje");
  }

  @Test
  @DisplayName("B26 header(\"new\") falls back to a generic draft header")
  void headerOfNewFallsBackToADraftHeader() {
    AutomationsFixtures.Header header = AutomationsFixtures.header("new");

    assertThat(header.id()).isEqualTo("new");
    assertThat(header.name()).isEqualTo("Nowa automatyzacja");
    assertThat(header.status()).isEqualTo("draft");
    assertThat(header.statusLabel())
        .isEqualTo("Wersja robocza · Edytuj logikę uruchamiania, warunki i akcje");
  }

  @Test
  @DisplayName(
      "B26 header of a shape-valid but unseeded id (e.g. a99) also falls back — AutomationCatalog "
          + "never 404s a shape-valid id, only a generic draft is shown")
  void headerOfAnUnseededShapeValidIdAlsoFallsBack() {
    AutomationsFixtures.Header header = AutomationsFixtures.header("a99");

    assertThat(header.id()).isEqualTo("a99");
    assertThat(header.name()).isEqualTo("Nowa automatyzacja");
    assertThat(header.status()).isEqualTo("draft");
  }

  @Test
  @DisplayName("B26 the rule pipeline has 3 steps, KIEDY/JEŚLI/WTEDY, 3 blocks each")
  void pipelineHasThreeStepsThreeBlocksEach() {
    assertThat(AutomationsFixtures.pipelineSteps()).hasSize(3);
    assertThat(AutomationsFixtures.pipelineSteps().get(0).kicker()).isEqualTo("KIEDY · trigger");
    assertThat(AutomationsFixtures.pipelineSteps().get(1).kicker()).isEqualTo("JEŚLI · warunki");
    assertThat(AutomationsFixtures.pipelineSteps().get(1).blocks()).hasSize(3);
    assertThat(AutomationsFixtures.pipelineSteps().get(2).kicker()).isEqualTo("WTEDY · akcje");
    assertThat(AutomationsFixtures.pipelineSteps().get(2).blocks()).hasSize(3);
  }

  @Test
  @DisplayName("B26 the flow graph has 6 nodes and 5 edges")
  void flowGraphHasSixNodesAndFiveEdges() {
    assertThat(AutomationsFixtures.flowNodes()).hasSize(6);
    assertThat(AutomationsFixtures.flowEdges()).hasSize(5);
  }

  @Test
  @DisplayName("B26 the simulation has 3 read-outs: matching, passing, estimated revenue")
  void simulationHasThreeReadouts() {
    assertThat(AutomationsFixtures.simulation())
        .extracting(AutomationsFixtures.SimulationItem::label)
        .containsExactly("Zdarzeń pasujących", "Spełniających warunki", "Estymowany przychód");
  }
}
