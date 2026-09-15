package click.kivvi.application;

import click.kivvi.domain.SidebarState;
import click.kivvi.domain.SpaDocument;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.Theme;
import click.kivvi.infrastructure.IndexHtmlTemplate;
import click.kivvi.infrastructure.SessionPreferencesStore;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Renders the SPA document for a known route: the built {@code index.html} with the session's
 * locale, theme and sidebar state injected before first paint.
 */
@Service
public class SpaDocumentService {

  private final IndexHtmlTemplate template;
  private final SessionPreferencesStore preferences;

  public SpaDocumentService(IndexHtmlTemplate template, SessionPreferencesStore preferences) {
    this.template = template;
    this.preferences = preferences;
  }

  public String render(HttpSession session, SupportedLocale locale) {
    return render(session, locale, Map.of());
  }

  /**
   * A form POST that could not be accepted re-renders the same document, carrying what the SPA
   * needs to redisplay the form: {@code dataAttributes} maps each attribute name without its {@code
   * data-} prefix to its value.
   */
  public String render(
      HttpSession session, SupportedLocale locale, Map<String, String> dataAttributes) {
    Theme theme = preferences.theme(session);
    SidebarState sidebar = preferences.sidebarState(session);
    var attrs =
        new SpaDocument.Attributes(locale.code(), theme.value(), sidebar.value(), dataAttributes);
    return SpaDocument.inject(template.content(), attrs);
  }
}
