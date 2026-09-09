package click.kivvi.web;

import click.kivvi.application.SpaDocumentService;
import click.kivvi.domain.RouteTable;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the built SPA document for every known panel/public route, and a minimal 404 document for
 * everything else — mirrors the old stack's routing exactly: a path that never matched a Symfony
 * route never rendered anything either.
 *
 * <p>The mapping is anchored on {@code /} and {@code /{locale:pl|en}/**}, so it can never shadow
 * {@code /assets/**} (the built SPA's own JS/CSS, served by Spring Boot's default static-resource
 * handler) or any other controller's more specific mapping — Spring tries annotated mappings in
 * registration order only when the path actually matches the pattern.
 */
@RestController
public class SpaDocumentController {

  // MediaType.TEXT_HTML carries no charset, and the servlet spec defaults an unspecified
  // response charset to ISO-8859-1 — every Polish diacritic in the SPA document (and any
  // later 404 copy) would come out mangled. The oracle records "text/html; charset=UTF-8".
  private static final MediaType TEXT_HTML_UTF8 =
      new MediaType("text", "html", StandardCharsets.UTF_8);

  private static final String NOT_FOUND_DOCUMENT =
      "<!doctype html><html lang=\"pl\"><head><meta charset=\"utf-8\"><title>404</title></head>"
          + "<body><h1>404</h1><p>Nie znaleziono strony.</p></body></html>";

  private final SpaDocumentService spaDocumentService;

  public SpaDocumentController(SpaDocumentService spaDocumentService) {
    this.spaDocumentService = spaDocumentService;
  }

  @GetMapping({"/", "/{locale:pl|en}", "/{locale:pl|en}/**"})
  public ResponseEntity<String> document(HttpServletRequest request) {
    Optional<RouteTable.Match> match = RouteTable.match(request.getRequestURI());
    if (match.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .contentType(TEXT_HTML_UTF8)
          .body(NOT_FOUND_DOCUMENT);
    }
    // A read: never force a session into existence just to render a page.
    String document = spaDocumentService.render(request.getSession(false), match.get().locale());
    return ResponseEntity.ok().contentType(TEXT_HTML_UTF8).body(document);
  }
}
