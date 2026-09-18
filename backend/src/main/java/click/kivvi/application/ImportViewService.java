package click.kivvi.application;

import click.kivvi.domain.Format;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.ImportFixtures;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

/**
 * Assembles the import-wizard page view-model for one step — mirrors {@code ImportController}
 * reading from the injected {@code ImportWizard} content service. Every collection the wizard's
 * four Twig partials could ever call is always returned in full, exactly like the Twig page held
 * the whole {@code ImportWizard} object and each partial called whichever methods it needed.
 */
@Service
public class ImportViewService {

  /**
   * PIO-129 translates the panel one page at a time. This page's copy is still Polish only, so its
   * figures stay Polish too — a Polish label above a euro amount would be worse than either
   * language on its own. The slice that translates this page replaces the marker with the real
   * locale; {@code PanelTranslationCoverageTest} holds the remaining markers to a declared list, so
   * the last page cannot be forgotten silently.
   */
  private static final SupportedLocale UNTRANSLATED = SupportedLocale.PL;

  private static final Map<String, StatusChip> STATUS_CHIP =
      Map.of(
          "new", new StatusChip("good", "Nowy"),
          "update", new StatusChip("info", "Aktualizacja"),
          "error", new StatusChip("bad", "Błąd"));

  private record StatusChip(String tone, String label) {}

  public record FileInfo(String name, String meta) {}

  /**
   * One mapping-table row, ready for {@code MapRow.vue}: a spreadsheet letter assigned by position
   * and the shared target list every row offers.
   */
  public record MapRow(
      String letter,
      String name,
      List<String> samples,
      List<ImportFixtures.Target> targets,
      String mapped,
      int confidence,
      boolean skipped) {}

  public record Detection(int recognised, int total, int sure, int unsure, int skipped) {}

  public record SummaryTile(
      String label, String value, String delta, String dir, String deltaIcon) {}

  public record PreviewRow(
      String status,
      String statusTone,
      String statusLabel,
      String email,
      String name,
      String optIn,
      String orders,
      String ltv,
      List<String> segments,
      String error) {}

  public record RecentImportView(
      String file, String rows, String date, String who, boolean ok, String note) {}

  public record Payload(
      int step,
      List<ImportFixtures.StepDef> steps,
      FileInfo file,
      List<MapRow> columns,
      Detection detection,
      List<ImportFixtures.ValidationCheck> validations,
      List<ImportFixtures.DedupStrategy> dedupStrategies,
      List<SummaryTile> summary,
      List<PreviewRow> preview,
      int rowCount,
      String rowCountLabel,
      int errorCount,
      List<RecentImportView> recent) {}

  /**
   * @throws NoSuchElementException when {@code step} is outside {@link ImportFixtures#FIRST_STEP}
   *     .. {@link ImportFixtures#LAST_STEP} — mirrors the old stack's route requirement {@code
   *     'step' => '[1-4]'}, which never let a request reach {@code ImportController::wizard} with
   *     any other value in the first place.
   */
  public Payload build(int step, String uploadedFileName) {
    if (!ImportFixtures.isValidStep(step)) {
      throw new NoSuchElementException("Unknown import step \"" + step + "\".");
    }
    return new Payload(
        step,
        ImportFixtures.steps(),
        file(uploadedFileName),
        columns(),
        detection(),
        ImportFixtures.validations(),
        ImportFixtures.dedupStrategies(),
        summary(),
        preview(),
        ImportFixtures.rowCount(),
        groupWithSpace(ImportFixtures.rowCount()),
        ImportFixtures.errorCount(),
        recent());
  }

  private static FileInfo file(String uploadedFileName) {
    String name = uploadedFileName != null ? uploadedFileName : ImportFixtures.DEFAULT_FILE_NAME;
    return new FileInfo(name, ImportFixtures.FILE_META);
  }

  private static List<MapRow> columns() {
    List<ImportFixtures.RawColumn> raw = ImportFixtures.rawColumns();
    return IntStream.range(0, raw.size()).mapToObj(i -> toMapRow(raw.get(i), i)).toList();
  }

  private static MapRow toMapRow(ImportFixtures.RawColumn column, int index) {
    char letter = (char) ('A' + index);
    return new MapRow(
        String.valueOf(letter),
        column.name(),
        column.samples(),
        ImportFixtures.targets(),
        column.mapped(),
        column.confidence(),
        "__skip".equals(column.mapped()));
  }

  private static Detection detection() {
    int sure = 0;
    int unsure = 0;
    int skipped = 0;
    for (ImportFixtures.RawColumn column : ImportFixtures.rawColumns()) {
      if (column.confidence() == 0) {
        skipped++;
      } else if (column.confidence() >= 70) {
        sure++;
      } else {
        unsure++;
      }
    }
    int total = ImportFixtures.rawColumns().size();
    return new Detection(sure + unsure, total, sure, unsure, skipped);
  }

  /**
   * Hardcoded deltas/directions, ported verbatim from {@code ImportWizard::summary()} — a simulated
   * run's own narrative text, not derived from any other field here.
   */
  private static List<SummaryTile> summary() {
    return List.of(
        new SummaryTile(
            "Wierszy łącznie",
            Format.number(8420, UNTRANSLATED),
            "plik wczytany poprawnie",
            "up",
            "check"),
        new SummaryTile(
            "Nowi klienci", Format.number(7124, UNTRANSLATED), "~84,6% wszystkich", "up", null),
        new SummaryTile(
            "Aktualizacje istniejących",
            Format.number(1252, UNTRANSLATED),
            "nadpisanie wg reguł z kroku 3",
            "flat",
            "check"),
        new SummaryTile(
            "Z błędem walidacji",
            String.valueOf(ImportFixtures.errorCount()),
            "zostaną pominięte",
            "down",
            "info"));
  }

  private static List<PreviewRow> preview() {
    return ImportFixtures.rawPreviewRows().stream().map(ImportViewService::toPreviewRow).toList();
  }

  private static PreviewRow toPreviewRow(ImportFixtures.RawPreviewRow row) {
    StatusChip chip = STATUS_CHIP.get(row.status());
    String ltv = row.ltv() == null ? "?" : Format.money(row.ltv(), UNTRANSLATED);
    return new PreviewRow(
        row.status(),
        chip.tone(),
        chip.label(),
        row.email(),
        row.name(),
        row.optIn(),
        row.orders(),
        ltv,
        row.segments(),
        row.error() != null ? row.error() : "");
  }

  private static List<RecentImportView> recent() {
    return ImportFixtures.recentImports().stream()
        .map(ImportViewService::toRecentImportView)
        .toList();
  }

  private static RecentImportView toRecentImportView(ImportFixtures.RecentImport item) {
    return new RecentImportView(
        item.file(),
        Format.number(item.rows(), UNTRANSLATED),
        item.date(),
        item.who(),
        item.ok(),
        item.note());
  }

  /**
   * step-run.html.twig's {@code {{ row_count|number_format(0, ',', ' ') }}}: Twig's own {@code
   * number_format} filter, a plain ASCII space — a different separator from {@link Format#number},
   * which the KPI tile above uses (U+202F narrow no-break space). Both conventions exist verbatim
   * in the old stack and are reproduced verbatim here, not reconciled into one — see {@code
   * FeedsViewService#groupWithSpace} for the same distinction on that page.
   */
  private static String groupWithSpace(int value) {
    String digits = Integer.toString(value);
    StringBuilder grouped = new StringBuilder();
    for (int i = 0; i < digits.length(); i++) {
      if (i > 0 && (digits.length() - i) % 3 == 0) {
        grouped.append(' ');
      }
      grouped.append(digits.charAt(i));
    }
    return grouped.toString();
  }
}
