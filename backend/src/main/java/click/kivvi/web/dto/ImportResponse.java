package click.kivvi.web.dto;

import java.util.List;

/**
 * {@code GET /api/v1/{locale}/import/{step}} response — the whole wizard view-model, exactly like
 * the Twig page received every {@code ImportWizard} value and each of the four step partials could
 * call any of its methods: every collection is always serialized, regardless of which step is
 * active, so a step change never needs a second shape.
 */
public record ImportResponse(
    int step,
    List<StepDef> steps,
    FileInfo file,
    List<MapRow> columns,
    Detection detection,
    List<ValidationCheck> validations,
    List<DedupStrategy> dedupStrategies,
    List<SummaryTile> summary,
    List<PreviewRow> preview,
    int rowCount,
    String rowCountLabel,
    int errorCount,
    List<RecentImport> recent) {

  public record StepDef(int n, String label) {}

  public record Target(String value, String label) {}

  public record FileInfo(String name, String meta) {}

  public record MapRow(
      String letter,
      String name,
      List<String> samples,
      List<Target> targets,
      String mapped,
      int confidence,
      boolean skipped) {}

  public record Detection(int recognised, int total, int sure, int unsure, int skipped) {}

  public record ValidationCheck(String label, String tone) {}

  public record DedupStrategy(String value, String label, boolean checked) {}

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

  public record RecentImport(
      String file, String rows, String date, String who, boolean ok, String note) {}
}
