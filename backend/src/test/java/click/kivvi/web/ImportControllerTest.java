package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.ImportUploadService;
import click.kivvi.application.ImportViewService;
import click.kivvi.infrastructure.importing.ImportUploadStorage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sliced (no full application context) coverage for the wire shape of {@code GET
 * /api/v1/{locale}/import/{step}}. Neither {@link ImportViewService} nor {@link
 * ImportUploadService} needs a database, so both are imported as real beans rather than mocked —
 * mirrors {@code SettingsControllerTest}. This class never uploads a file, so {@link
 * ImportUploadStorage}'s default upload directory is never actually written to.
 */
@WebMvcTest(ImportController.class)
@Import({ImportViewService.class, ImportUploadService.class, ImportUploadStorage.class})
class ImportControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName(
      "B31 GET /api/v1/pl/import/1 returns the full wizard payload: 4 steps, the default file"
          + " name, 11 mapped columns, 6 validations, 4 dedup strategies, 4 KPI tiles, 6 preview"
          + " rows, 3 recent imports")
  void stepOnePayloadShape() throws Exception {
    mvc.perform(get("/api/v1/pl/import/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.step").value(1))
        .andExpect(jsonPath("$.steps.length()").value(4))
        .andExpect(jsonPath("$.file.name").value("klienci.csv"))
        .andExpect(jsonPath("$.columns.length()").value(11))
        .andExpect(jsonPath("$.columns[0].letter").value("A"))
        .andExpect(jsonPath("$.columns[10].skipped").value(true))
        .andExpect(jsonPath("$.detection.recognised").value(10))
        .andExpect(jsonPath("$.detection.total").value(11))
        .andExpect(jsonPath("$.validations.length()").value(6))
        .andExpect(jsonPath("$.dedupStrategies.length()").value(4))
        .andExpect(jsonPath("$.dedupStrategies[0].checked").value(true))
        .andExpect(jsonPath("$.summary.length()").value(4))
        .andExpect(jsonPath("$.summary[0].value").value("8 420"))
        .andExpect(jsonPath("$.preview.length()").value(6))
        .andExpect(jsonPath("$.preview[4].ltv").value("?"))
        .andExpect(jsonPath("$.rowCount").value(8420))
        .andExpect(jsonPath("$.rowCountLabel").value("8 420"))
        .andExpect(jsonPath("$.errorCount").value(44))
        .andExpect(jsonPath("$.recent.length()").value(3));
  }

  @Test
  @DisplayName("B31 GET /api/v1/pl/import/5 (outside 1-4) is not found")
  void outOfRangeStepIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/pl/import/5")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B31 GET /api/v1/pl/import/0 (outside 1-4) is not found")
  void zeroStepIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/pl/import/0")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B07 GET /api/v1/de/import/1 (unsupported locale) is not found")
  void unsupportedLocaleIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/de/import/1")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/v1/pl/import/step (non-numeric) is not found")
  void nonNumericStepIsNotFound() throws Exception {
    mvc.perform(get("/api/v1/pl/import/step")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B31 every step returns the same shared 14-option target list per column")
  void everyColumnSharesTheFullTargetList() throws Exception {
    mvc.perform(get("/api/v1/pl/import/2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.step").value(2))
        .andExpect(jsonPath("$.columns[0].targets.length()").value(14))
        .andExpect(jsonPath("$.columns[0].targets[0].value").value(""))
        .andExpect(jsonPath("$.columns[0].mapped").value("email"))
        .andExpect(
            jsonPath("$.validations[1].label")
                .value(Matchers.containsString("<span class=\"mono\">")));
  }
}
