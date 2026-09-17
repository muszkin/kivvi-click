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
 * <p>The redirect returns to the locale the upload was posted from. This route carries no {@code
 * {locale}} segment of its own, so the SPA names it in the multipart body's {@value #LOCALE_FIELD}
 * field; a missing or unsupported value falls back to {@link SupportedLocale#DEFAULT}.
 *
 * <p>Until PIO-125 this always redirected to the default locale, reproducing the old stack's {@code
 * redirectToRoute('import', ['step' => 2])}, which fell back to Symfony's {@code default_locale}
 * whatever page the upload came from. That only ever pulled {@code /en} users into Polish; once
 * English became the default it pulled every {@code /pl} user into English instead, so the page's
 * own locale travels with the upload now. The field is a form value rather than the {@code Referer}
 * header, which a browser or proxy may strip or truncate.
 */
@RestController
public class ImportUploadController {

  static final String LOCALE_FIELD = "locale";

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
      @RequestParam(name = LOCALE_FIELD, required = false) String locale,
      HttpServletRequest request) {
    if (file == null) {
      return ResponseEntity.notFound().build();
    }

    importUploadService.store(request.getSession(true), file);

    SupportedLocale returnTo = SupportedLocale.fromCode(locale).orElse(SupportedLocale.DEFAULT);
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create("/" + returnTo.code() + "/import/2"))
        .build();
  }
}
