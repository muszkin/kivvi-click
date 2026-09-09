package click.kivvi.application;

import click.kivvi.domain.SidebarState;
import click.kivvi.domain.SpaDocument;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.Theme;
import click.kivvi.infrastructure.IndexHtmlTemplate;
import click.kivvi.infrastructure.SessionPreferencesStore;
import jakarta.servlet.http.HttpSession;
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
    return render(session, locale, null, null);
  }

  /** A failed login POST re-renders the same document carrying the error and the typed address. */
  public String render(
      HttpSession session, SupportedLocale locale, String loginError, String lastUsername) {
    Theme theme = preferences.theme(session);
    SidebarState sidebar = preferences.sidebarState(session);
    var attrs =
        new SpaDocument.Attributes(
            locale.code(), theme.value(), sidebar.value(), loginError, lastUsername);
    return SpaDocument.inject(template.content(), attrs);
  }
}
