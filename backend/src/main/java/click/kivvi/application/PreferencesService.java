package click.kivvi.application;

import click.kivvi.domain.SidebarState;
import click.kivvi.domain.Theme;
import click.kivvi.infrastructure.SessionPreferencesStore;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/**
 * Stores the theme and sidebar choices the browser flips instantly, so the next full page load
 * renders with them and nothing flashes — mirrors {@code PreferencesController}.
 */
@Service
public class PreferencesService {

  private final SessionPreferencesStore store;

  public PreferencesService(SessionPreferencesStore store) {
    this.store = store;
  }

  public Theme applyTheme(HttpSession session, String requested) {
    return store.storeTheme(session, requested);
  }

  public SidebarState applySidebar(HttpSession session, String requested) {
    return store.storeSidebarState(session, requested);
  }
}
