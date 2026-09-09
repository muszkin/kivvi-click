package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.EventsViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sliced (no full application context) coverage for the wire shape of {@code GET
 * /api/v1/{locale}/events}. {@link EventsViewService} needs no session/infrastructure
 * collaborators, so it is imported as a real bean rather than stubbed — mirrors {@code
 * FeedsControllerTest}.
 */
@WebMvcTest(EventsController.class)
@Import(EventsViewService.class)
class EventsControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("B24 GET /api/v1/pl/events returns 30 rows, 9 type filters and 4 site filters")
  void eventsPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.events.length()").value(30))
        .andExpect(jsonPath("$.typeFilters.length()").value(9))
        .andExpect(jsonPath("$.siteFilters.length()").value(4))
        .andExpect(jsonPath("$.ranges.length()").value(4))
        .andExpect(jsonPath("$.total").value("9 360"))
        .andExpect(jsonPath("$.shown").value(30))
        .andExpect(jsonPath("$.mercureTopic").value("/accounts/1/events"));
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/events is not found")
  void unsupportedLocaleIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/de/events")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("query parameters mark the matching filter chip active")
  void queryParametersMarkTheMatchingChipActive() throws Exception {
    mvc.perform(get("/api/v1/pl/events?type=purchase&site=aurea&range=24h"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.typeFilters[3].payload").value("purchase"))
        .andExpect(jsonPath("$.typeFilters[3].active").value(true))
        .andExpect(jsonPath("$.siteFilters[1].payload").value("aurea"))
        .andExpect(jsonPath("$.siteFilters[1].active").value(true))
        .andExpect(jsonPath("$.ranges[2].value").value("24h"))
        .andExpect(jsonPath("$.ranges[2].active").value(true));
  }
}
