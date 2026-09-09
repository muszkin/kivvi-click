package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.SettingsViewService;
import click.kivvi.application.SpaDocumentService;
import click.kivvi.infrastructure.IndexHtmlTemplate;
import click.kivvi.infrastructure.SessionPreferencesStore;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sliced (no full application context) coverage for the wire shape of {@code GET
 * /api/v1/{locale}/settings/{tab}} and the {@code /{locale}/settings/{tab}} document route. Neither
 * {@link SettingsViewService} nor {@link SpaDocumentService} needs a database, so both are imported
 * as real beans rather than mocked.
 */
@WebMvcTest(SettingsController.class)
@Import({
  SettingsViewService.class,
  SpaDocumentService.class,
  IndexHtmlTemplate.class,
  SessionPreferencesStore.class
})
class SettingsControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName(
      "B32 GET /api/v1/pl/settings/account returns the full catalogue: 8 tabs, account active, "
          + "3 tracked sites, 5 team rows, 4 dns records, 3 webhooks, 8x3 notification matrix")
  void accountPayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/settings/account"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tab").value("account"))
        .andExpect(jsonPath("$.tabs.length()").value(8))
        .andExpect(jsonPath("$.tabs[0].active").value(true))
        .andExpect(jsonPath("$.tabs[0].href").value("/pl/settings"))
        .andExpect(jsonPath("$.tabs[1].href").value("/pl/settings/sites"))
        .andExpect(
            jsonPath("$.tabSubtitle").value("Dane firmy, faktury i preferencje właściciela konta."))
        .andExpect(jsonPath("$.settings.trackedSites.length()").value(3))
        .andExpect(jsonPath("$.settings.team.length()").value(5))
        .andExpect(jsonPath("$.settings.dnsRecords.length()").value(4))
        .andExpect(jsonPath("$.settings.apiKeys.length()").value(3))
        .andExpect(jsonPath("$.settings.webhooks.length()").value(3))
        .andExpect(jsonPath("$.settings.notificationMatrix.length()").value(8))
        .andExpect(jsonPath("$.settings.invoices.length()").value(4))
        .andExpect(jsonPath("$.settings.dataSubjectRequests.length()").value(3))
        .andExpect(jsonPath("$.settings.retentionPolicies.length()").value(4))
        .andExpect(jsonPath("$.trackerSnippet").value(Matchers.containsString("k.js")));
  }

  @Test
  @DisplayName("B32 GET /api/v1/pl/settings/sites marks the \"sites\" tab active")
  void sitesTabIsActive() throws Exception {
    mvc.perform(get("/api/v1/pl/settings/sites"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tab").value("sites"))
        .andExpect(jsonPath("$.tabs[1].active").value(true))
        .andExpect(jsonPath("$.tabs[0].active").value(false));
  }

  @Test
  @DisplayName("B06 GET /api/v1/pl/settings/nonexistent is not found")
  void unknownTabApiIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/pl/settings/nonexistent")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/settings/account is not found")
  void unsupportedLocaleApiIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/de/settings/account")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B32 GET /pl/settings/account renders the SPA document for a known tab")
  void knownTabDocumentRenders200() throws Exception {
    mvc.perform(get("/pl/settings/account"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("<html")));
  }

  @Test
  @DisplayName("B06/DEV-12 GET /pl/settings/nonexistent is a 404 document, not the 200 SPA shell")
  void unknownTabDocumentIsNotFound() throws Exception {
    mvc.perform(get("/pl/settings/nonexistent")).andExpect(status().isNotFound());
  }
}
