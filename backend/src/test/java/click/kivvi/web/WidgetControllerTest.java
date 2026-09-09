package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.WidgetViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sliced (no full application context) coverage for the wire shape of {@code GET
 * /api/v1/{locale}/popups} and {@code GET /api/v1/{locale}/popups/{id}}. {@link WidgetViewService}
 * needs no session/infrastructure collaborators, so it is imported as a real bean rather than
 * mocked.
 */
@WebMvcTest(WidgetController.class)
@Import(WidgetViewService.class)
class WidgetControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("B29 GET /api/v1/pl/popups returns 5 cards, a selected preview and 5 types")
  void popupsPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/popups"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards.length()").value(5))
        .andExpect(jsonPath("$.types.length()").value(5))
        .andExpect(jsonPath("$.selected.id").value("p1"))
        .andExpect(jsonPath("$.selected.type").value("modal"))
        .andExpect(jsonPath("$.cards[0].title").value("Exit intent — 10% rabatu"))
        .andExpect(jsonPath("$.cards[3].metrics[0].value").value("—"));
  }

  @Test
  @DisplayName("B29 ?preview=p2 selects the banner widget")
  void previewQuerySelectsTheRequestedWidget() throws Exception {
    mvc.perform(get("/api/v1/pl/popups").param("preview", "p2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.selected.id").value("p2"))
        .andExpect(jsonPath("$.selected.type").value("banner"));
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/popups is not found")
  void unsupportedLocalePopupsIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/de/popups")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName(
      "B29 GET /api/v1/pl/popups/p1 returns the known widget plus 5 types/10 blocks/4"
          + " variables/3 triggers/4 audience rows")
  void knownPopupPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/popups/p1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.widget.name").value("Exit intent — 10% rabatu"))
        .andExpect(jsonPath("$.widget.type").value("modal"))
        .andExpect(jsonPath("$.device").value("desktop"))
        .andExpect(jsonPath("$.viewport").value("1440 × 900"))
        .andExpect(jsonPath("$.types.length()").value(5))
        .andExpect(jsonPath("$.blocks.length()").value(10))
        .andExpect(jsonPath("$.variables.length()").value(4))
        .andExpect(jsonPath("$.triggers.length()").value(3))
        .andExpect(jsonPath("$.audience.length()").value(4));
  }

  @Test
  @DisplayName("B29 GET /api/v1/pl/popups/new returns the blank-draft widget shape")
  void newPopupPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/popups/new"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.widget.name").value("Nowy widget"))
        .andExpect(jsonPath("$.widget.type").value("modal"))
        .andExpect(jsonPath("$.widget.meta").value("Szkic · nieopublikowany"));
  }

  @Test
  @DisplayName(
      "B29 ?type= overrides the widget's shown type; ?device=mobile overrides the viewport")
  void typeAndDeviceQueryOverrideTheEditorPayload() throws Exception {
    mvc.perform(get("/api/v1/pl/popups/p1").param("type", "banner").param("device", "mobile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.widget.type").value("banner"))
        .andExpect(jsonPath("$.device").value("mobile"))
        .andExpect(jsonPath("$.viewport").value("390 × 844"));
  }

  @Test
  @DisplayName(
      "B29/DEV-7 GET /api/v1/pl/popups/{id} 404s for an id shape outside \"new\"/\"p\\d+\"")
  void unmatchedPopupIdShapeIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/pl/popups/bogus")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/popups/p1 is not found")
  void unsupportedLocalePopupIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/de/popups/p1")).andExpect(status().isNotFound());
  }
}
