package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.DashboardViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sliced (no full application context) coverage for the wire shape of {@code GET
 * /api/v1/{locale}/dashboard}. {@link DashboardViewService} needs no session/infrastructure
 * collaborators, so it is imported as a real bean rather than stubbed — mirrors {@code
 * EventsControllerTest}.
 */
@WebMvcTest(DashboardController.class)
@Import(DashboardViewService.class)
class DashboardControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName(
      "B23 GET /api/v1/pl/dashboard returns 4 KPIs, 3 legend entries, 10 events, 6 recent"
          + " customers and 4 top automations")
  void dashboardPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.kpis.length()").value(4))
        .andExpect(jsonPath("$.legend.length()").value(3))
        .andExpect(jsonPath("$.events.length()").value(10))
        .andExpect(jsonPath("$.recentCustomers.length()").value(6))
        .andExpect(jsonPath("$.topAutomations.length()").value(4))
        .andExpect(jsonPath("$.mercureTopic").value("/accounts/1/events"));
  }

  @Test
  @DisplayName("B23 the first recently seen customer carries id, email, orders and lastSeen")
  void firstRecentCustomer() throws Exception {
    mvc.perform(get("/api/v1/pl/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recentCustomers[0].id").value("c_1000"))
        .andExpect(jsonPath("$.recentCustomers[0].email").value("anna.k@example.com"))
        .andExpect(jsonPath("$.recentCustomers[0].orders").value(0))
        .andExpect(jsonPath("$.recentCustomers[0].lastSeen").value("teraz"));
  }

  @Test
  @DisplayName("B23 the first best-performing automation is the highest-revenue active one")
  void firstTopAutomation() throws Exception {
    mvc.perform(get("/api/v1/pl/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.topAutomations[0].id").value("a3"))
        .andExpect(jsonPath("$.topAutomations[0].revenue").value("41 200 zł"));
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/dashboard is not found")
  void unsupportedLocaleIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/de/dashboard")).andExpect(status().isNotFound());
  }
}
