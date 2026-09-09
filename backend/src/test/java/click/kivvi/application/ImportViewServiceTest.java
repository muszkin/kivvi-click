package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImportViewServiceTest {

  private final ImportViewService service = new ImportViewService();

  @Test
  @DisplayName("B31 the file falls back to \"klienci.csv\" when no upload has happened yet")
  void fileFallsBackToTheFixtureNameWithoutAnUpload() {
    ImportViewService.Payload payload = service.build(1, null);

    assertThat(payload.file().name()).isEqualTo("klienci.csv");
    assertThat(payload.file().meta()).isEqualTo("8 420 wierszy · CSV UTF-8 · ; jako separator");
  }

  @Test
  @DisplayName("B31 an uploaded file's session-derived name reaches the payload verbatim")
  void uploadedFileNameOverridesTheFixtureDefault() {
    ImportViewService.Payload payload = service.build(2, "klienci-oracle.csv");

    assertThat(payload.file().name()).isEqualTo("klienci-oracle.csv");
  }

  @Test
  @DisplayName("B31 detection counts 9 confident (>=70), 1 unsure and 1 skipped (confidence 0)")
  void detectionCountsMatchTheOracle() {
    ImportViewService.Detection detection = service.build(1, null).detection();

    assertThat(detection.total()).isEqualTo(11);
    assertThat(detection.sure()).isEqualTo(9);
    assertThat(detection.unsure()).isEqualTo(1);
    assertThat(detection.skipped()).isEqualTo(1);
    assertThat(detection.recognised()).isEqualTo(10);
  }

  @Test
  @DisplayName("B31 the skipped column (confidence 0) is the one mapped to \"__skip\"")
  void theZeroConfidenceColumnIsMarkedSkipped() {
    ImportViewService.Payload payload = service.build(1, null);

    assertThat(payload.columns())
        .filteredOn(ImportViewService.MapRow::skipped)
        .extracting(ImportViewService.MapRow::letter)
        .containsExactly("K");
  }

  @Test
  @DisplayName("B31 the one preview row with a validation error has a null ltv, rendered as \"?\"")
  void theErrorRowsLtvRendersAsAQuestionMark() {
    ImportViewService.Payload payload = service.build(4, null);

    assertThat(payload.preview())
        .filteredOn(row -> "error".equals(row.status()))
        .extracting(ImportViewService.PreviewRow::ltv, ImportViewService.PreviewRow::statusLabel)
        .containsExactly(org.assertj.core.groups.Tuple.tuple("?", "Błąd"));
  }

  @Test
  @DisplayName(
      "B31 the run CTA's row count uses a plain space, distinct from the KPI tile's narrow"
          + " no-break space — both conventions ported verbatim, not reconciled")
  void rowCountLabelUsesAPlainAsciiSpace() {
    ImportViewService.Payload payload = service.build(4, null);

    assertThat(payload.rowCountLabel()).isEqualTo("8 420");
    assertThat(payload.rowCountLabel().codePointAt(1)).isEqualTo(' ');
    assertThat(payload.summary().get(0).value().codePointAt(1)).isEqualTo(0x202f);
  }

  @Test
  @DisplayName("B31/DEV-12 a step outside 1-4 throws, mapped to a 404 by ImportController")
  void outOfRangeStepThrows() {
    assertThatThrownBy(() -> service.build(5, null)).isInstanceOf(NoSuchElementException.class);
    assertThatThrownBy(() -> service.build(0, null)).isInstanceOf(NoSuchElementException.class);
  }
}
