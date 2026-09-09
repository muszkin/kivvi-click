package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.PreferencesService;
import click.kivvi.infrastructure.SessionPreferencesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PreferencesController.class)
@Import({PreferencesService.class, SessionPreferencesStore.class})
class PreferencesControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("B08 POST /preferences/theme (JSON) stores and echoes the theme")
  void themeChoiceIsStoredFromJson() throws Exception {
    mvc.perform(
            post("/preferences/theme")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"theme\":\"dark\"}"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"theme\":\"dark\"}"));
  }

  @Test
  @DisplayName("B09 unknown theme value falls back to light")
  void unknownThemeFallsBackToLight() throws Exception {
    mvc.perform(
            post("/preferences/theme")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"theme\":\"sepia\"}"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"theme\":\"light\"}"));
  }

  @Test
  @DisplayName("B10 POST /preferences/sidebar (JSON) stores and echoes the sidebar state")
  void sidebarStateIsStoredFromJson() throws Exception {
    mvc.perform(
            post("/preferences/sidebar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"state\":\"collapsed\"}"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"state\":\"collapsed\"}"));
  }

  @Test
  @DisplayName("form-encoded bodies are also accepted, mirroring PreferencesController::readValue")
  void formEncodedBodyIsAlsoAccepted() throws Exception {
    mvc.perform(post("/preferences/theme").param("theme", "dark"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"theme\":\"dark\"}"));
  }
}
