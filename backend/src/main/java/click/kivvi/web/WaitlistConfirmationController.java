package click.kivvi.web;

import click.kivvi.application.SpaDocumentService;
import click.kivvi.application.waitlist.ConfirmationOutcome;
import click.kivvi.application.waitlist.UnsubscribeOutcome;
import click.kivvi.application.waitlist.WaitlistConfirmationService;
import click.kivvi.domain.SupportedLocale;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The three pages a confirmation mail can lead to: following the link, asking for a new one, and
 * getting off the list.
 *
 * <p>Confirming mutates state on a {@code GET}. That is a deliberate departure from HTTP's
 * read-only reading of the verb, forced by the medium: a link in an e-mail is a {@code GET}, and
 * asking someone to submit a form after clicking it costs more confirmations than the purity is
 * worth. The cost is recorded in the spec's risk table and mitigated by storing the confirming IP
 * and user agent, which is what makes a prefetching mail client visible after the fact.
 *
 * <p>Each response is the SPA document with {@code data-*} attributes on {@code <html>} — the same
 * mechanism a refused login or a refused signup already uses, and additive to the attribute list
 * frozen in BACKWARD_COMPATIBILITY.md.
 */
@RestController
public class WaitlistConfirmationController {

  // MediaType.TEXT_HTML carries no charset and the servlet default is ISO-8859-1, which mangles
  // every Polish diacritic. Same reason, same fix as LoginController and WaitlistController.
  private static final MediaType TEXT_HTML_UTF8 =
      new MediaType("text", "html", StandardCharsets.UTF_8);

  private static final String CONFIRM_ATTRIBUTE = "waitlist-confirm";
  private static final String TOKEN_ATTRIBUTE = "waitlist-token";
  private static final String UNSUBSCRIBE_ATTRIBUTE = "waitlist-unsubscribe";

  private final WaitlistConfirmationService confirmationService;
  private final SpaDocumentService spaDocumentService;

  public WaitlistConfirmationController(
      WaitlistConfirmationService confirmationService, SpaDocumentService spaDocumentService) {
    this.confirmationService = confirmationService;
    this.spaDocumentService = spaDocumentService;
  }

  // The token's own shape is in the mapping so /{locale}/waitlist/confirm/sent — the page the
  // resend redirects to — is not read as a token named "sent"; it falls through to
  // SpaDocumentController and its RouteTable entry, like every other plain document.
  @GetMapping("/{locale:pl|en}/waitlist/confirm/{token:[0-9a-f]{64}}")
  public ResponseEntity<String> confirm(
      @PathVariable String locale, @PathVariable String token, HttpServletRequest request) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    ConfirmationOutcome outcome =
        confirmationService.confirm(
            token,
            Instant.now(),
            // getRemoteAddr(), not the X-Forwarded-For header: server.forward-headers-strategy is
            // `native`, so Tomcat has already substituted the real client address here — and only
            // for a peer inside the trusted private ranges.
            request.getRemoteAddr(),
            request.getHeader(HttpHeaders.USER_AGENT));

    Map<String, String> attributes = new LinkedHashMap<>();
    switch (outcome) {
      case ConfirmationOutcome.Confirmed ignored -> attributes.put(CONFIRM_ATTRIBUTE, "ok");
      case ConfirmationOutcome.AlreadyConfirmed ignored ->
          attributes.put(CONFIRM_ATTRIBUTE, "already");
      case ConfirmationOutcome.Expired expired -> {
        attributes.put(CONFIRM_ATTRIBUTE, "expired");
        // Echoed back so the "send me a new link" button has something to post, and the visitor
        // never has to remember which address they used.
        attributes.put(TOKEN_ATTRIBUTE, expired.token());
      }
      case ConfirmationOutcome.Unknown ignored -> attributes.put(CONFIRM_ATTRIBUTE, "unknown");
    }
    return document(request, supported, attributes);
  }

  /**
   * Post/redirect/get, so a reload of the "check your inbox" page cannot queue a second message.
   *
   * <p>Answers identically whatever the token turns out to be. A form reachable by anybody that
   * said "no such subscriber" would be an address-enumeration oracle wearing a helpful face.
   */
  @PostMapping("/{locale:pl|en}/waitlist/confirm/resend")
  public ResponseEntity<Void> resend(
      @PathVariable String locale,
      @RequestParam(name = "token", required = false, defaultValue = "") String token) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    confirmationService.resend(token, Instant.now());

    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create("/" + supported.code() + "/waitlist/confirm/sent"))
        .build();
  }

  @GetMapping("/{locale:pl|en}/waitlist/unsubscribe/{token:[0-9a-f]{64}}")
  public ResponseEntity<String> unsubscribe(
      @PathVariable String locale, @PathVariable String token, HttpServletRequest request) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    UnsubscribeOutcome outcome = confirmationService.unsubscribe(token, Instant.now());

    String state =
        switch (outcome) {
          case UnsubscribeOutcome.Unsubscribed ignored -> "ok";
          case UnsubscribeOutcome.Unknown ignored -> "unknown";
        };
    return document(request, supported, Map.of(UNSUBSCRIBE_ATTRIBUTE, state));
  }

  private ResponseEntity<String> document(
      HttpServletRequest request, SupportedLocale locale, Map<String, String> attributes) {
    // A read: never force a session into existence just to render a public page.
    String document = spaDocumentService.render(request.getSession(false), locale, attributes);
    return ResponseEntity.ok().contentType(TEXT_HTML_UTF8).body(document);
  }
}
