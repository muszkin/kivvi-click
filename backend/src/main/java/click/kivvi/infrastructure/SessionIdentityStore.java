package click.kivvi.infrastructure;

import click.kivvi.domain.Identity;
import click.kivvi.fixtures.ShellFixtures;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * Reads and writes the signed-in identity in the Postgres-backed HTTP session — mirrors {@code
 * PanelIdentity}. Session key {@code panel.identity} stores the e-mail only; the display name is
 * always re-derived, never stored, so it can never drift from the e-mail.
 *
 * <p>Reads accept a {@code null} session (a request that never created one) and answer the default
 * identity, the same value a fresh session with no identity attribute would give — callers should
 * look one up with {@code request.getSession(false)} rather than force one into existence just to
 * render a page.
 */
@Component
public class SessionIdentityStore {

  private static final String SESSION_KEY = "panel.identity";

  public void signIn(HttpSession session, String email) {
    session.setAttribute(SESSION_KEY, email);
  }

  public void signOut(HttpSession session) {
    session.removeAttribute(SESSION_KEY);
  }

  public Identity current(HttpSession session) {
    Object stored = session == null ? null : session.getAttribute(SESSION_KEY);
    if (stored instanceof String email && !email.isEmpty()) {
      return Identity.derive(email);
    }
    return ShellFixtures.DEFAULT_IDENTITY;
  }
}
