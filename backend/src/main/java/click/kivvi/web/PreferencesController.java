package click.kivvi.web;

import click.kivvi.application.PreferencesService;
import click.kivvi.web.dto.SidebarResponse;
import click.kivvi.web.dto.ThemeResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Endpoints behind the theme and sidebar toggles. The browser flips the attribute immediately for a
 * snappy toggle; this stores the choice so the next full page load renders with it and nothing
 * flashes — mirrors {@code PreferencesController}, including accepting either a JSON body or a
 * form-encoded one.
 */
@RestController
@RequestMapping("/preferences")
public class PreferencesController {

  private final PreferencesService preferencesService;
  private final ObjectMapper objectMapper;

  public PreferencesController(PreferencesService preferencesService, ObjectMapper objectMapper) {
    this.preferencesService = preferencesService;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/theme")
  public ThemeResponse theme(HttpServletRequest request, HttpSession session) throws IOException {
    String requested = readValue(request, "theme");
    return new ThemeResponse(preferencesService.applyTheme(session, requested).value());
  }

  @PostMapping("/sidebar")
  public SidebarResponse sidebar(HttpServletRequest request, HttpSession session)
      throws IOException {
    String requested = readValue(request, "state");
    return new SidebarResponse(preferencesService.applySidebar(session, requested).value());
  }

  private String readValue(HttpServletRequest request, String key) throws IOException {
    String contentType = request.getContentType();
    if (contentType != null
        && contentType.contains(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)) {
      JsonNode payload = objectMapper.readTree(request.getInputStream());
      JsonNode value = payload.get(key);
      return value != null && value.isString() ? value.asString() : "";
    }
    String param = request.getParameter(key);
    return param != null ? param : "";
  }
}
