package click.kivvi.web;

import click.kivvi.application.ImportUploadService;
import click.kivvi.application.ImportViewService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.web.dto.ImportResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The four-step customer import wizard's read side — mirrors {@code ImportController::wizard()}.
 * Every step is its own URL ({@code GET /api/v1/{locale}/import/{step}}), and the session-derived
 * uploaded file name (see {@link ImportUploadController}) is folded into whichever step is
 * requested next, exactly like the old stack's {@code ImportUploadStorage::currentFileName()} was
 * read on every render, not only the step right after an upload.
 *
 * <p>The document route ({@code GET /{locale}/import/{1-4}}) needs no controller of its own: {@link
 * click.kivvi.domain.RouteTable}'s {@code import(?:/[1-4])?} pattern already restricts the SPA
 * document to exactly steps 1-4 (or no step), so {@link SpaDocumentController} 404s any other value
 * on its own — unlike {@code CustomersController}/{@code SettingsController}, whose "known id/tab"
 * check cannot be expressed as a path regex.
 */
@RestController
public class ImportController {

  private final ImportViewService importViewService;
  private final ImportUploadService importUploadService;

  public ImportController(
      ImportViewService importViewService, ImportUploadService importUploadService) {
    this.importViewService = importViewService;
    this.importUploadService = importUploadService;
  }

  @GetMapping("/api/v1/{locale:pl|en}/import/{step:\\d+}")
  public ResponseEntity<ImportResponse> wizard(
      @PathVariable String locale, @PathVariable int step, HttpServletRequest request) {
    SupportedLocale.fromCode(locale).orElseThrow();
    try {
      String uploadedFileName =
          importUploadService.currentFileName(request.getSession(false)).orElse(null);
      ImportViewService.Payload payload = importViewService.build(step, uploadedFileName);
      return ResponseEntity.ok(toResponse(payload));
    } catch (NoSuchElementException outOfRange) {
      return ResponseEntity.notFound().build();
    }
  }

  private static ImportResponse toResponse(ImportViewService.Payload payload) {
    return new ImportResponse(
        payload.step(),
        payload.steps().stream().map(s -> new ImportResponse.StepDef(s.n(), s.label())).toList(),
        new ImportResponse.FileInfo(payload.file().name(), payload.file().meta()),
        payload.columns().stream().map(ImportController::toMapRow).toList(),
        toDetection(payload.detection()),
        payload.validations().stream()
            .map(v -> new ImportResponse.ValidationCheck(v.label(), v.tone()))
            .toList(),
        payload.dedupStrategies().stream()
            .map(d -> new ImportResponse.DedupStrategy(d.value(), d.label(), d.checked()))
            .toList(),
        payload.summary().stream().map(ImportController::toSummaryTile).toList(),
        payload.preview().stream().map(ImportController::toPreviewRow).toList(),
        payload.rowCount(),
        payload.rowCountLabel(),
        payload.errorCount(),
        payload.recent().stream().map(ImportController::toRecentImport).toList());
  }

  private static ImportResponse.MapRow toMapRow(ImportViewService.MapRow row) {
    List<ImportResponse.Target> targets =
        row.targets().stream().map(t -> new ImportResponse.Target(t.value(), t.label())).toList();
    return new ImportResponse.MapRow(
        row.letter(),
        row.name(),
        row.samples(),
        targets,
        row.mapped(),
        row.confidence(),
        row.skipped());
  }

  private static ImportResponse.Detection toDetection(ImportViewService.Detection detection) {
    return new ImportResponse.Detection(
        detection.recognised(),
        detection.total(),
        detection.sure(),
        detection.unsure(),
        detection.skipped());
  }

  private static ImportResponse.SummaryTile toSummaryTile(ImportViewService.SummaryTile tile) {
    return new ImportResponse.SummaryTile(
        tile.label(), tile.value(), tile.delta(), tile.dir(), tile.deltaIcon());
  }

  private static ImportResponse.PreviewRow toPreviewRow(ImportViewService.PreviewRow row) {
    return new ImportResponse.PreviewRow(
        row.status(),
        row.statusTone(),
        row.statusLabel(),
        row.email(),
        row.name(),
        row.optIn(),
        row.orders(),
        row.ltv(),
        row.segments(),
        row.error());
  }

  private static ImportResponse.RecentImport toRecentImport(
      ImportViewService.RecentImportView item) {
    return new ImportResponse.RecentImport(
        item.file(), item.rows(), item.date(), item.who(), item.ok(), item.note());
  }
}
