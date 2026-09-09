package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.CampaignsViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sliced (no full application context) coverage for the wire shape of {@code GET
 * /api/v1/{locale}/campaigns} and {@code GET /api/v1/{locale}/emails/{id}}. {@link
 * CampaignsViewService} needs no session/infrastructure collaborators, so it is imported as a real
 * bean rather than mocked.
 */
@WebMvcTest(CampaignsController.class)
@Import(CampaignsViewService.class)
class CampaignsControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("B27 GET /api/v1/pl/campaigns returns 4 KPIs, 4 filters, 8 columns and 5 rows")
  void campaignsPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/campaigns"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.kpis.length()").value(4))
        .andExpect(jsonPath("$.filters.length()").value(4))
        .andExpect(jsonPath("$.columns.length()").value(8))
        .andExpect(jsonPath("$.rows.length()").value(5))
        .andExpect(jsonPath("$.filters[0].active").value(true))
        .andExpect(jsonPath("$.rows[0].name").value("Powrót do koszyka — wariant A"))
        .andExpect(jsonPath("$.rows[3].sent").value("—"));
  }

  @Test
  @DisplayName("B27 ?filter=trigger marks the \"Wyzwalane\" chip active and no other")
  void filterQueryMarksTheRequestedChipActive() throws Exception {
    mvc.perform(get("/api/v1/pl/campaigns").param("filter", "trigger"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.filters[0].active").value(false))
        .andExpect(jsonPath("$.filters[1].active").value(true))
        .andExpect(jsonPath("$.rows.length()").value(5));
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/campaigns is not found")
  void unsupportedLocaleCampaignsIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/de/campaigns")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName(
      "B28 GET /api/v1/pl/emails/k1 returns the known template plus 10 blocks/5 variables/5 sections")
  void knownEmailPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/emails/k1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.template.name").value("Powrót do koszyka — wariant A"))
        .andExpect(jsonPath("$.blocks.length()").value(10))
        .andExpect(jsonPath("$.variables.length()").value(5))
        .andExpect(jsonPath("$.sections.length()").value(5))
        .andExpect(jsonPath("$.selectedBlock.blockId").value("hero_1"))
        .andExpect(jsonPath("$.selectedBlock.title").value("Hania, Twój koszyk czeka."));
  }

  @Test
  @DisplayName("B28 GET /api/v1/pl/emails/new returns the blank draft shape")
  void newEmailPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/emails/new"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.template.name").value("Nowy szablon email"))
        .andExpect(jsonPath("$.template.subject").value("Temat wiadomości"))
        .andExpect(jsonPath("$.blocks.length()").value(10));
  }

  @Test
  @DisplayName(
      "B28/DEV-7 GET /api/v1/pl/emails/{id} 404s for an id shape outside \"new\"/\"k\\d+\"")
  void unmatchedEmailIdShapeIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/pl/emails/bogus")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/emails/k1 is not found")
  void unsupportedLocaleEmailIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/de/emails/k1")).andExpect(status().isNotFound());
  }
}
