package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.LandingViewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LandingController.class)
@Import(LandingViewService.class)
class LandingControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("B22 GET /api/v1/pl/landing returns the marketing page's copy arrays")
  void landingPayloadCarriesTheMarketingCopy() throws Exception {
    mvc.perform(get("/api/v1/pl/landing"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features.length()").value(6))
        .andExpect(jsonPath("$.steps.length()").value(3))
        .andExpect(jsonPath("$.plans").doesNotExist())
        .andExpect(jsonPath("$.trustPoints.length()").value(3))
        .andExpect(jsonPath("$.trustPoints[0]").value("Licencja MIT"))
        .andExpect(jsonPath("$.previewTiles.length()").value(4))
        .andExpect(jsonPath("$.previewSeries.length()").value(60));
  }

  @Test
  @DisplayName("B22 GET /api/v1/en/landing returns the same locale-invariant copy")
  void englishRouteReturnsTheSameContent() throws Exception {
    mvc.perform(get("/api/v1/en/landing"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.features.length()").value(6));
  }

  @Test
  @DisplayName("B22 GET /api/v1/de/landing (unsupported locale) is unknown to the mapping")
  void unsupportedLocaleIs404() throws Exception {
    mvc.perform(get("/api/v1/de/landing")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("the demo shortcut redirects to the localized dashboard")
  void demoRedirectsToDashboard() throws Exception {
    mvc.perform(get("/pl/demo"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/pl/dashboard"));
  }
}
