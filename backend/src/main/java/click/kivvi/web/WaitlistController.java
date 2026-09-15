package click.kivvi.web;

import click.kivvi.application.SpaDocumentService;
import click.kivvi.application.waitlist.SignupOutcome;
import click.kivvi.application.waitlist.SignupRequest;
import click.kivvi.application.waitlist.WaitlistSignupService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.waitlist.WaitlistSignupError;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The landing page's waitlist form: a native document POST, the same shape {@code POST
 * /{locale}/login} already has. Success redirects (post/redirect/get, so a reload does not submit
 * twice); anything else re-renders the SPA document with the state the form needs to come back up
 * as the visitor left it.
 */
@RestController
public class WaitlistController {

  // MediaType.TEXT_HTML carries no charset, and the servlet spec defaults an unspecified
  // response charset to ISO-8859-1 — every Polish diacritic in the error message would come
  // out mangled. Same reason, same fix as LoginController and SpaDocumentController.
  private static final MediaType TEXT_HTML_UTF8 =
      new MediaType("text", "html", StandardCharsets.UTF_8);

  private static final String ERROR_ATTRIBUTE = "waitlist-error";
  private static final String EMAIL_ATTRIBUTE = "waitlist-email";
  private static final String CONSENT_ATTRIBUTE = "waitlist-consent";

  /** What the landing page looks for to swap the form for its thank-you state. */
  private static final String SUCCESS_QUERY = "?waitlist=ok";

  private final WaitlistSignupService signupService;
  private final SpaDocumentService spaDocumentService;
  private final MessageSource messageSource;

  public WaitlistController(
      WaitlistSignupService signupService,
      SpaDocumentService spaDocumentService,
      MessageSource messageSource) {
    this.signupService = signupService;
    this.spaDocumentService = spaDocumentService;
    this.messageSource = messageSource;
  }

  @PostMapping("/{locale:pl|en}/waitlist")
  public ResponseEntity<String> signUp(
      @PathVariable String locale,
      @RequestParam(name = "email", required = false, defaultValue = "") String email,
      @RequestParam(name = "consent", required = false) String consent,
      @RequestParam(name = "website", required = false, defaultValue = "") String honeypot,
      HttpServletRequest request) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    // An unticked checkbox is not submitted at all, so "absent" is the No here. Any value a
    // ticked box sends ("on", "1", whatever the markup says) reads as Yes.
    boolean consentGiven = consent != null && !consent.isBlank();

    SignupOutcome outcome =
        signupService.signUp(
            new SignupRequest(
                email,
                consentGiven,
                honeypot,
                supported,
                // getRemoteAddr(), not the X-Forwarded-For header: server.forward-headers-strategy
                // is `native`, so Tomcat's RemoteIpValve has already substituted the real client
                // address here — but only for a peer inside the trusted private ranges. Reading
                // the header directly would trust whatever any caller chose to put in it.
                request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT)));

    return switch (outcome) {
      case SignupOutcome.Accepted ignored -> redirectToThankYou(supported);
      case SignupOutcome.Rejected rejected ->
          redisplayForm(request, supported, rejected.error(), email, consentGiven, HttpStatus.OK);
      case SignupOutcome.ThrottleExceeded ignored ->
          redisplayForm(
              request,
              supported,
              WaitlistSignupError.THROTTLED,
              email,
              consentGiven,
              HttpStatus.TOO_MANY_REQUESTS);
    };
  }

  private ResponseEntity<String> redirectToThankYou(SupportedLocale locale) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create("/" + locale.code() + SUCCESS_QUERY))
        .build();
  }

  /**
   * Re-renders the landing document carrying the message, the address as typed and whether the box
   * was ticked — so a refused submission costs the visitor nothing but a click, rather than making
   * them type it all again.
   */
  private ResponseEntity<String> redisplayForm(
      HttpServletRequest request,
      SupportedLocale locale,
      WaitlistSignupError error,
      String email,
      boolean consentGiven,
      HttpStatus status) {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put(
        ERROR_ATTRIBUTE,
        messageSource.getMessage(error.messageKey(), null, Locale.forLanguageTag(locale.code())));
    attributes.put(EMAIL_ATTRIBUTE, email);
    attributes.put(CONSENT_ATTRIBUTE, Boolean.toString(consentGiven));

    // A read: never force a session into existence just to re-render a public page.
    String document = spaDocumentService.render(request.getSession(false), locale, attributes);
    return ResponseEntity.status(status).contentType(TEXT_HTML_UTF8).body(document);
  }
}
