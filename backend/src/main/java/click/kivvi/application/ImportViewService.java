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

  private static final Map<String, StatusChip> STATUS_CHIP_PL =
      Map.of(
          "new", new StatusChip("good", "Nowy"),
          "update", new StatusChip("info", "Aktualizacja"),
          "error", new StatusChip("bad", "Błąd"));

  private static final Map<String, StatusChip> STATUS_CHIP_EN =
      Map.of(
          "new", new StatusChip("good", "New"),
          "update", new StatusChip("info", "Update"),
          "error", new StatusChip("bad", "Error"));

  private static final int NEW_CUSTOMERS = 7_124;
  private static final int UPDATED_CUSTOMERS = 1_252;

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
  public Payload build(SupportedLocale locale, int step, String uploadedFileName) {
    if (!ImportFixtures.isValidStep(step)) {
      throw new NoSuchElementException("Unknown import step \"" + step + "\".");
    }
    return new Payload(
        step,
        ImportFixtures.steps(locale),
        file(locale, uploadedFileName),
        columns(locale),
        detection(locale),
        ImportFixtures.validations(locale),
        ImportFixtures.dedupStrategies(locale),
        summary(locale),
        preview(locale),
        ImportFixtures.rowCount(),
        grouped(ImportFixtures.rowCount(), locale),
        ImportFixtures.errorCount(),
        recent(locale));
  }

  private static FileInfo file(SupportedLocale locale, String uploadedFileName) {
    String name =
        uploadedFileName != null ? uploadedFileName : ImportFixtures.defaultFileName(locale);
    return new FileInfo(name, ImportFixtures.fileMeta(locale));
  }

  private static List<MapRow> columns(SupportedLocale locale) {
    List<ImportFixtures.RawColumn> raw = ImportFixtures.rawColumns(locale);
    return IntStream.range(0, raw.size()).mapToObj(i -> toMapRow(locale, raw.get(i), i)).toList();
  }

  private static MapRow toMapRow(
      SupportedLocale locale, ImportFixtures.RawColumn column, int index) {
    char letter = (char) ('A' + index);
    return new MapRow(
        String.valueOf(letter),
        column.name(),
        column.samples(),
        ImportFixtures.targets(locale),
        column.mapped(),
        column.confidence(),
        "__skip".equals(column.mapped()));
  }

  private static Detection detection(SupportedLocale locale) {
    int sure = 0;
    int unsure = 0;
    int skipped = 0;
    for (ImportFixtures.RawColumn column : ImportFixtures.rawColumns(locale)) {
      if (column.confidence() == 0) {
        skipped++;
      } else if (column.confidence() >= 70) {
        sure++;
      } else {
        unsure++;
      }
    }
    int total = ImportFixtures.rawColumns(locale).size();
    return new Detection(sure + unsure, total, sure, unsure, skipped);
  }

  /**
   * Hardcoded deltas/directions, ported verbatim from {@code ImportWizard::summary()} — a simulated
   * run's own narrative text, not derived from any other field here.
   */
  private static List<SummaryTile> summary(SupportedLocale locale) {
    String rows = Format.number(ImportFixtures.rowCount(), locale);
    String created = Format.number(NEW_CUSTOMERS, locale);
    String updated = Format.number(UPDATED_CUSTOMERS, locale);
    String errors = String.valueOf(ImportFixtures.errorCount());
    return switch (locale) {
      case PL ->
          List.of(
              new SummaryTile("Wierszy łącznie", rows, "plik wczytany poprawnie", "up", "check"),
              new SummaryTile("Nowi klienci", created, "~84,6% wszystkich", "up", null),
              new SummaryTile(
                  "Aktualizacje istniejących",
                  updated,
                  "nadpisanie wg reguł z kroku 3",
                  "flat",
                  "check"),
              new SummaryTile("Z błędem walidacji", errors, "zostaną pominięte", "down", "info"));
      case EN ->
          List.of(
              new SummaryTile("Rows in total", rows, "the file read cleanly", "up", "check"),
              new SummaryTile("New customers", created, "~84.6% of them all", "up", null),
              new SummaryTile(
                  "Updates to existing ones",
                  updated,
                  "overwritten by the rules from step 3",
                  "flat",
                  "check"),
              new SummaryTile("Failing validation", errors, "these are skipped", "down", "info"));
    };
  }

  private static List<PreviewRow> preview(SupportedLocale locale) {
    return ImportFixtures.rawPreviewRows(locale).stream()
        .map(row -> toPreviewRow(locale, row))
        .toList();
  }

  private static PreviewRow toPreviewRow(SupportedLocale locale, ImportFixtures.RawPreviewRow row) {
    StatusChip chip =
        switch (locale) {
          case PL -> STATUS_CHIP_PL.get(row.status());
          case EN -> STATUS_CHIP_EN.get(row.status());
        };
    String ltv = row.ltv() == null ? "?" : Format.money(row.ltv(), locale);
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

  private static List<RecentImportView> recent(SupportedLocale locale) {
    return ImportFixtures.recentImports(locale).stream()
        .map(item -> toRecentImportView(locale, item))
        .toList();
  }

  private static RecentImportView toRecentImportView(
      SupportedLocale locale, ImportFixtures.RecentImport item) {
    return new RecentImportView(
        item.file(),
        Format.number(item.rows(), locale),
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
  private static String grouped(int value, SupportedLocale locale) {
    char separator =
        switch (locale) {
          case PL -> ' ';
          case EN -> ',';
        };
    String digits = Integer.toString(value);
    StringBuilder grouped = new StringBuilder();
    for (int i = 0; i < digits.length(); i++) {
      if (i > 0 && (digits.length() - i) % 3 == 0) {
        grouped.append(separator);
      }
      grouped.append(digits.charAt(i));
    }
    return grouped.toString();
  }
}
