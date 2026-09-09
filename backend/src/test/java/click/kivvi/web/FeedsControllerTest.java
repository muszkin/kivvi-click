package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.FeedsViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sliced (no full application context) coverage for the wire shape of {@code GET
 * /api/v1/{locale}/feeds}. {@link FeedsViewService} needs no session/infrastructure collaborators,
 * so it is imported as a real bean rather than mocked.
 */
@WebMvcTest(FeedsController.class)
@Import(FeedsViewService.class)
class FeedsControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName(
      "B30 GET /api/v1/pl/feeds returns 4 sources, 4 feeds (one failing with HTTP 503), 4 "
          + "coverage bars, 3 fallback rules and 142 mismatched")
  void feedsPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/feeds"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.kpis.length()").value(4))
        .andExpect(jsonPath("$.sources.length()").value(4))
        .andExpect(jsonPath("$.feeds.length()").value(4))
        .andExpect(jsonPath("$.coverage.length()").value(4))
        .andExpect(jsonPath("$.fallbackRules.length()").value(3))
        .andExpect(jsonPath("$.mismatched").value(142))
        .andExpect(jsonPath("$.feeds[2].status").value("error"))
        .andExpect(jsonPath("$.feeds[2].error").value("HTTP 503 — Service Unavailable"))
        .andExpect(jsonPath("$.feeds[2].mappedPercent").doesNotExist());
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/feeds is not found")
  void unsupportedLocaleIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/de/feeds")).andExpect(status().isNotFound());
  }
}
