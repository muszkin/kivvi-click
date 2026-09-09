package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.AutomationsViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sliced (no full application context) coverage for the wire shape of {@code GET
 * /api/v1/{locale}/automations[/{id}]}.
 */
@WebMvcTest(AutomationsController.class)
@Import(AutomationsViewService.class)
class AutomationsControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("B26 GET /api/v1/pl/automations returns 6 cards and 4 filters")
  void listPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/automations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.automations.length()").value(6))
        .andExpect(jsonPath("$.filters.length()").value(4))
        .andExpect(jsonPath("$.automations[0].title").value("Powrót do porzuconego koszyka"))
        .andExpect(jsonPath("$.automations[0].chips.length()").value(4))
        .andExpect(jsonPath("$.automations[0].action").value("go-automation"))
        .andExpect(jsonPath("$.automations[0].payload").value("a1"))
        .andExpect(jsonPath("$.filters[0].label").value("Wszystkie"))
        .andExpect(jsonPath("$.filters[0].active").value(true));
  }

  @Test
  @DisplayName("B26 ?status=active marks the 'Aktywne' filter active, without filtering the cards")
  void statusQueryOnlyMarksAFilterActive() throws Exception {
    mvc.perform(get("/api/v1/pl/automations").param("status", "active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.automations.length()").value(6))
        .andExpect(jsonPath("$.filters[0].active").value(false))
        .andExpect(jsonPath("$.filters[1].active").value(true));
  }

  @Test
  @DisplayName(
      "B26 GET /api/v1/pl/automations/a1 returns the editor shape: header, 3 steps, 6 nodes, 5 edges")
  void editorPayloadShapeForASeededAutomation() throws Exception {
    mvc.perform(get("/api/v1/pl/automations/a1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.automation.id").value("a1"))
        .andExpect(jsonPath("$.automation.name").value("Powrót do porzuconego koszyka"))
        .andExpect(jsonPath("$.view").value("list"))
        .andExpect(jsonPath("$.tabs.length()").value(4))
        .andExpect(jsonPath("$.steps.length()").value(3))
        .andExpect(jsonPath("$.steps[1].blocks.length()").value(3))
        .andExpect(jsonPath("$.nodes.length()").value(6))
        .andExpect(jsonPath("$.edges.length()").value(5))
        .andExpect(jsonPath("$.simulation.length()").value(3));
  }

  @Test
  @DisplayName("B26 GET /api/v1/pl/automations/a1?view=flow reports the flow view")
  void editorPayloadReportsTheFlowView() throws Exception {
    mvc.perform(get("/api/v1/pl/automations/a1").param("view", "flow"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.view").value("flow"));
  }

  @Test
  @DisplayName("B26 GET /api/v1/pl/automations/new returns the generic draft header")
  void editorPayloadForNewIsAGenericDraft() throws Exception {
    mvc.perform(get("/api/v1/pl/automations/new"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.automation.id").value("new"))
        .andExpect(jsonPath("$.automation.name").value("Nowa automatyzacja"))
        .andExpect(jsonPath("$.automation.status").value("draft"))
        .andExpect(jsonPath("$.steps.length()").value(3));
  }

  @Test
  @DisplayName("B26 GET /api/v1/pl/automations/xyz (not \"new\" or a\\d+) is not found")
  void unknownIdShapeApiIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/pl/automations/xyz")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/automations is not found")
  void unsupportedLocaleApiIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/de/automations")).andExpect(status().isNotFound());
  }
}
