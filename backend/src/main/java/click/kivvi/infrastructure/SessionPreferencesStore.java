package click.kivvi.infrastructure;

import click.kivvi.domain.SidebarState;
import click.kivvi.domain.Theme;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * Reads and writes view preferences in the Postgres-backed HTTP session — mirrors {@code
 * PanelPreferences}. Rendered server-side into the SPA document before first paint so the page
 * never flashes the wrong theme or sidebar state.
 *
 * <p>Reads accept a {@code null} session (a request that never created one) and answer the same
 * fallback a fresh, empty session would — callers should look one up with {@code
 * request.getSession(false)} rather than force one into existence just to render a page.
 */
@Component
public class SessionPreferencesStore {

  private static final String SESSION_THEME = "panel.theme";
  private static final String SESSION_SIDEBAR = "panel.sidebar";

  public Theme theme(HttpSession session) {
    return Theme.fromValueOrDefault(
        session == null ? null : (String) session.getAttribute(SESSION_THEME));
  }

  public SidebarState sidebarState(HttpSession session) {
    return SidebarState.fromValueOrDefault(
        session == null ? null : (String) session.getAttribute(SESSION_SIDEBAR));
  }

  public Theme storeTheme(HttpSession session, String requested) {
    Theme resolved = Theme.fromValueOrDefault(requested);
    session.setAttribute(SESSION_THEME, resolved.value());
    return resolved;
  }

  public SidebarState storeSidebarState(HttpSession session, String requested) {
    SidebarState resolved = SidebarState.fromValueOrDefault(requested);
    session.setAttribute(SESSION_SIDEBAR, resolved.value());
    return resolved;
  }
}
