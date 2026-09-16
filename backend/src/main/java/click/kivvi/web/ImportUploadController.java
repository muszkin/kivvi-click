package click.kivvi.web;

import click.kivvi.application.ImportUploadService;
import click.kivvi.domain.SupportedLocale;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The import wizard's file drop — mirrors {@code ImportController::upload()}. Stores the upload
 * under the configured directory and redirects into step 2, the same path a drag-and-drop and a
 * file picker take; the SPA's {@code Dropzone.vue} follows the redirect with a full document
 * navigation, exactly like {@code assets/controllers/upload.ts} did.
 *
 * <p>The redirect target's locale is always {@link SupportedLocale#DEFAULT}, never the locale the
 * browser was actually on: this route carries no {@code {locale}} segment of its own, so the old
 * stack's {@code redirectToRoute('import', ['step' => 2])} — which never passes {@code _locale}
 * explicitly — falls back to Symfony's configured {@code default_locale} ({@code pl}), not to
 * whatever locale-prefixed page the upload was posted from. Reproduced verbatim, quirk and all: the
 * oracle only ever recorded this from a {@code /pl/...} page, so it never had a chance to show
 * otherwise, but the mechanism is the framework default, not "the current page's locale".
 *
 * <p>PIO-125 moved that default to English, so the redirect now lands on {@code /en/import/2} and
 * the quirk points the other way: an upload posted from a {@code /pl/...} page leaves Polish.
 * Carrying the page's own locale through the upload is a panel change, left to its own decision.
 */
@RestController
public class ImportUploadController {

  private final ImportUploadService importUploadService;

  public ImportUploadController(ImportUploadService importUploadService) {
    this.importUploadService = importUploadService;
  }

  /**
   * Follow-up (review finding, LOW): only {@code file == null} (the {@code file} part missing from
   * the multipart body entirely) 404s — mirrors {@code ImportController::upload()}'s own {@code
   * !$file instanceof UploadedFile} check exactly. A present-but-empty (0-byte) file is stored and
   * redirected, same as the old stack: PHP's {@code $request->files->get('file')} returns a real
   * {@code UploadedFile} instance for an empty upload as long as the field itself was submitted,
   * and {@code ImportController} never calls {@code isEmpty()}/{@code getSize()} on it — an earlier
   * version of this method rejected an empty file too, which the old stack never did.
   */
  @PostMapping("/import/upload")
  public ResponseEntity<Void> upload(
      @RequestParam(name = "file", required = false) MultipartFile file,
      HttpServletRequest request) {
    if (file == null) {
      return ResponseEntity.notFound().build();
    }

    importUploadService.store(request.getSession(true), file);

    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create("/" + SupportedLocale.DEFAULT.code() + "/import/2"))
        .build();
  }
}
