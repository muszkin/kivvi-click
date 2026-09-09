package click.kivvi.web;

import click.kivvi.application.LoginService;
import click.kivvi.application.SpaDocumentService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.ShellFixtures;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The login form's two actions, native document POSTs exactly as today (no XHR): a valid address
 * redirects into the dashboard, an invalid one re-renders the SPA document with the error and the
 * typed value carried on {@code <html>} — mirrors {@code SecurityController}.
 */
@RestController
public class LoginController {

  // MediaType.TEXT_HTML carries no charset, and the servlet spec defaults an unspecified
  // response charset to ISO-8859-1 — every Polish diacritic in the error message would
  // come out mangled. The oracle records "text/html; charset=UTF-8" for this response too.
  private static final MediaType TEXT_HTML_UTF8 =
      new MediaType("text", "html", StandardCharsets.UTF_8);

  private final LoginService loginService;
  private final SpaDocumentService spaDocumentService;

  public LoginController(LoginService loginService, SpaDocumentService spaDocumentService) {
    this.loginService = loginService;
    this.spaDocumentService = spaDocumentService;
  }

  @PostMapping("/{locale:pl|en}/login")
  public ResponseEntity<String> login(
      @PathVariable String locale,
      @RequestParam(name = "_username", required = false, defaultValue = "") String username,
      HttpServletRequest request) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    Optional<String> error = loginService.attemptSignIn(request, username);

    if (error.isEmpty()) {
      return ResponseEntity.status(HttpStatus.FOUND)
          .location(URI.create("/" + supported.code() + "/dashboard"))
          .build();
    }

    // Mirrors pages/login.html.twig's `last_username|default('maciej@aureashop.pl')`: Twig's
    // default filter (without the strict flag) falls back on an EMPTY string too, not only
    // on an absent value — so the empty-e-mail case redisplays the sample address, exactly
    // like the malformed-e-mail case redisplays what was actually typed.
    String lastUsername = username.isBlank() ? ShellFixtures.DEFAULT_IDENTITY.email() : username;

    // A read from here on: never force a session into existence just to re-render the form.
    String document =
        spaDocumentService.render(request.getSession(false), supported, error.get(), lastUsername);
    return ResponseEntity.ok().contentType(TEXT_HTML_UTF8).body(document);
  }

  @PostMapping("/{locale:pl|en}/logout")
  public ResponseEntity<Void> logout(@PathVariable String locale, HttpServletRequest request) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    loginService.signOut(request);
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create("/" + supported.code() + "/login"))
        .build();
  }
}
