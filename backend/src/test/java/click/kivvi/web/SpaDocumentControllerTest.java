package click.kivvi.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.SpaDocumentService;
import click.kivvi.infrastructure.IndexHtmlTemplate;
import click.kivvi.infrastructure.SessionPreferencesStore;
import click.kivvi.infrastructure.config.MessageSourceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SpaDocumentController.class)
@Import({
  SpaDocumentService.class,
  IndexHtmlTemplate.class,
  SessionPreferencesStore.class,
  MessageSourceConfig.class
})
class SpaDocumentControllerTest {

  @Autowired private MockMvc mvc;

  @ParameterizedTest
  @DisplayName("B01 every panel/public URL renders 200 and carries the SPA document")
  @ValueSource(
      strings = {
        "/",
        "/pl",
        "/pl/login",
        "/pl/dashboard",
        "/pl/events",
        "/pl/customers",
        "/pl/customers/c_1",
        "/pl/automations",
        "/pl/automations/new",
        "/pl/automations/a1",
        "/pl/campaigns",
        "/pl/emails/new",
        "/pl/emails/k1",
        "/pl/popups",
        "/pl/popups/new",
        "/pl/popups/p1",
        "/pl/feeds",
        "/pl/import",
        "/pl/import/2",
        "/pl/settings",
        "/en/dashboard"
      })
  void everyKnownRouteRenders200(String path) throws Exception {
    mvc.perform(get(path))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<html")));
  }

  @Test
  @DisplayName("B07 unsupported locale prefix is not found")
  void unsupportedLocaleIsNotFound() throws Exception {
    mvc.perform(get("/de/dashboard")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("PIO-125 the bare root is served as an English document")
  void theBareRootIsAnEnglishDocument() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<html lang=\"en\"")));
  }

  @Test
  @DisplayName("PIO-125 /pl is still served as a Polish document")
  void thePolishPrefixIsStillAPolishDocument() throws Exception {
    mvc.perform(get("/pl"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<html lang=\"pl\"")));
  }

  @Test
  @DisplayName("a path outside the route table is not found with a minimal HTML document")
  void unknownPathIsNotFound() throws Exception {
    mvc.perform(get("/pl/nonexistent"))
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString("404")));
  }

  @Test
  @DisplayName(
      "PIO-125 the 404 document answers in the path's language — it was Polish for /en/ too")
  void theNotFoundDocumentFollowsThePathsLocale() throws Exception {
    mvc.perform(get("/en/privcy"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType("text/html;charset=UTF-8"))
        .andExpect(content().string(containsString("<html lang=\"en\">")))
        .andExpect(content().string(containsString("Page not found.")))
        .andExpect(content().string(not(containsString("Nie znaleziono"))));
    mvc.perform(get("/pl/prywatnosc"))
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString("<html lang=\"pl\">")))
        .andExpect(content().string(containsString("Nie znaleziono strony.")));
  }
}
