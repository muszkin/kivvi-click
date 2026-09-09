package click.kivvi.application;

import click.kivvi.domain.EmailValidation;
import click.kivvi.infrastructure.SessionIdentityStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * The login form's behaviour: validate the address, establish the session identity on success —
 * mirrors {@code SecurityController::login()} / {@code ::logout()}.
 *
 * <p>A session is created only where one is actually needed — on a successful sign-in and on a
 * sign-out that has something to undo — never on the read paths that merely render the form.
 */
@Service
public class LoginService {

  private final SessionIdentityStore identityStore;

  public LoginService(SessionIdentityStore identityStore) {
    this.identityStore = identityStore;
  }

  /**
   * Returns the Polish error message on failure; signs the session in and returns empty on success.
   */
  public Optional<String> attemptSignIn(HttpServletRequest request, String email) {
    Optional<String> error = EmailValidation.errorFor(email);
    if (error.isEmpty()) {
      identityStore.signIn(request.getSession(true), email);
    }
    return error;
  }

  public void signOut(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      identityStore.signOut(session);
    }
  }
}
