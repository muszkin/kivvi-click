package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.CampaignsFixtures;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CampaignsViewServiceTest {

  private final CampaignsViewService campaignsViewService = new CampaignsViewService();

  @Test
  @DisplayName(
      "B27 the four campaign KPIs match the oracle, narrow-space-grouped and money-formatted")
  void kpisMatchTheOracle() {
    CampaignsViewService.ListPayload payload = campaignsViewService.list(SupportedLocale.PL, "all");

    assertThat(payload.kpis())
        .extracting(
            CampaignsViewService.Kpi::label,
            CampaignsViewService.Kpi::value,
            CampaignsViewService.Kpi::unit,
            CampaignsViewService.Kpi::delta,
            CampaignsViewService.Kpi::dir)
        .containsExactly(
            tuple("Wysłane (30 dni)", "142 410", null, "+18% vs poprzedni okres", "up"),
            tuple("Średni open rate", "38,4", "%", "+2,1pp", "up"),
            tuple("Średni CTR", "7,8", "%", "−0,4pp", "down"),
            tuple("Przychód z kampanii", "184 230 zł", null, "+24%", "up"));
  }

  @Test
  @DisplayName(
      "B27 four filters are offered; only the requested one is active, others fall back to \"all\"")
  void filtersMarkTheRequestedOneActive() {
    CampaignsViewService.ListPayload triggered =
        campaignsViewService.list(SupportedLocale.PL, "trigger");
    CampaignsViewService.ListPayload fallback = campaignsViewService.list(SupportedLocale.PL, null);

    assertThat(triggered.filters())
        .extracting(
            CampaignsViewService.Filter::label,
            CampaignsViewService.Filter::count,
            CampaignsViewService.Filter::active)
        .containsExactly(
            tuple("Wszystkie", "18", false),
            tuple("Wyzwalane", "12", true),
            tuple("Jednorazowe", "6", false),
            tuple("Wstrzymane", "2", false));
    assertThat(fallback.filters())
        .extracting(CampaignsViewService.Filter::active)
        .containsExactly(true, false, false, false);
  }

  @Test
  @DisplayName("B27 the filter never changes which 5 rows come back — only which chip is active")
  void rowsAreIdenticalRegardlessOfFilter() {
    CampaignsViewService.ListPayload all = campaignsViewService.list(SupportedLocale.PL, "all");
    CampaignsViewService.ListPayload triggered =
        campaignsViewService.list(SupportedLocale.PL, "trigger");

    assertThat(all.rows())
        .extracting(CampaignsViewService.Row::id)
        .containsExactly(
            triggered.rows().stream().map(CampaignsViewService.Row::id).toArray(String[]::new));
  }

  @Test
  @DisplayName(
      "B27 the 5 rows match the oracle: names, type/status chips, and \"—\" for the two unsent campaigns")
  void rowsMatchTheOracle() {
    CampaignsViewService.ListPayload payload = campaignsViewService.list(SupportedLocale.PL, "all");

    assertThat(payload.rows())
        .extracting(
            CampaignsViewService.Row::name,
            CampaignsViewService.Row::typeLabel,
            CampaignsViewService.Row::statusLabel,
            CampaignsViewService.Row::sent,
            CampaignsViewService.Row::open,
            CampaignsViewService.Row::click,
            CampaignsViewService.Row::revenue)
        .containsExactly(
            tuple(
                "Powrót do koszyka — wariant A",
                "Wyzwalana",
                "Aktywna",
                "1 287",
                "62,4%",
                "18,4%",
                "24 800 zł"),
            tuple("Witamy w Kivvi", "Wyzwalana", "Aktywna", "412", "78,1%", "41,2%", "8 930 zł"),
            tuple(
                "Newsletter — Tydzień smaków #18",
                "Masowa",
                "Wysłana",
                "8 420",
                "31,8%",
                "6,2%",
                "14 200 zł"),
            tuple("Black weekend — VIP", "Masowa", "Zaplanowana", "—", "—", "—", "—"),
            tuple("Reaktywacja po 60 dniach", "Wyzwalana", "Wstrzymana", "—", "—", "—", "—"));
  }

  @Test
  @DisplayName("B28 GET .../emails/k1 resolves a known template's name, meta and subject")
  void knownTemplateMatchesTheOracle() {
    CampaignsViewService.EditorPayload payload =
        campaignsViewService.editor(SupportedLocale.PL, "k1");

    assertThat(payload.template())
        .extracting(
            CampaignsViewService.Template::id,
            CampaignsViewService.Template::name,
            CampaignsViewService.Template::meta,
            CampaignsViewService.Template::subject,
            CampaignsViewService.Template::sender)
        .containsExactly(
            "k1",
            "Powrót do koszyka — wariant A",
            "Szablon wyzwalany · 3 produkty placeholders · 412 wysyłek (7d)",
            "Hania, Twój koszyk czeka — wróć i odbierz −10%",
            "sklep@aureashop.pl");
  }

  @Test
  @DisplayName(
      "B28 an id with no seeded campaign (including \"new\") falls back to the blank draft shape")
  void unknownIdFallsBackToTheBlankTemplate() {
    CampaignsViewService.Template fromNew =
        campaignsViewService.editor(SupportedLocale.PL, "new").template();
    CampaignsViewService.Template fromUnseeded =
        campaignsViewService.editor(SupportedLocale.PL, "k999").template();

    for (CampaignsViewService.Template template : List.of(fromNew, fromUnseeded)) {
      assertThat(template.name()).isEqualTo("Nowy szablon email");
      assertThat(template.meta()).isEqualTo("Szkic · nigdy nie wysłany");
      assertThat(template.subject()).isEqualTo("Temat wiadomości");
      assertThat(template.sender()).isEqualTo("sklep@aureashop.pl");
    }
    assertThat(fromNew.id()).isEqualTo("new");
    assertThat(fromUnseeded.id()).isEqualTo("k999");
  }

  @Test
  @DisplayName(
      "B28 the editor payload always carries 10 blocks, 5 variables, 5 sections and the same selected block")
  void editorPayloadShapeIsConstantAcrossIds() {
    CampaignsViewService.EditorPayload known =
        campaignsViewService.editor(SupportedLocale.PL, "k1");
    CampaignsViewService.EditorPayload blank =
        campaignsViewService.editor(SupportedLocale.PL, "new");

    for (CampaignsViewService.EditorPayload payload : List.of(known, blank)) {
      assertThat(payload.blocks()).hasSize(10);
      assertThat(payload.variables()).hasSize(5);
      assertThat(payload.sections()).hasSize(5);
      assertThat(payload.selectedBlock().blockId()).isEqualTo("hero_1");
    }
    assertThat(known.blocks())
        .extracting(CampaignsFixtures.Block::label)
        .containsExactly(
            "Nagłówek",
            "Tekst",
            "Obraz",
            "Produkty",
            "Kupon",
            "Przycisk CTA",
            "Recenzje",
            "Separator",
            "Stopka",
            "HTML własny");
  }
}
