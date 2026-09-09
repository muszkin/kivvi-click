package click.kivvi.infrastructure.importing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Ported from {@code App\Panel\ImportUploadStorage}'s own PHPUnit coverage: the stored file name is
 * always {@code <32 hex>.<ext>}, never derived from the client's own path, and the extension
 * whitelist ({@code ^[a-z0-9]{1,8}$}) is enforced exactly, falling back to {@code csv} otherwise.
 */
class ImportUploadStorageTest {

  private static final String STORED_NAME_PREFIX = "^[0-9a-f]{32}\\.";

  @TempDir Path uploadDirectory;

  private ImportUploadStorage storage(Path directory) {
    return new ImportUploadStorage(directory.toString());
  }

  @Test
  @DisplayName(
      "B31 a path-traversal original name never escapes the upload directory: the stored name is"
          + " a random 32-hex-char name plus the whitelisted extension of its basename")
  void pathTraversalOriginalNameNeverEscapesTheUploadDirectory() throws IOException {
    MockHttpSession session = new MockHttpSession();
    MockMultipartFile file =
        new MockMultipartFile("file", "../../x.sh", "application/x-sh", "echo hi".getBytes());

    String storedName = storage(uploadDirectory).store(session, file);

    assertThat(storedName).matches(STORED_NAME_PREFIX + "sh$");
    assertThat(uploadDirectory.resolve(storedName)).exists();
    // No entry escapes uploadDirectory itself — the only file this store() call could have
    // created anywhere is the one at the generated, fully-server-controlled name.
    assertThat(Files.list(uploadDirectory)).hasSize(1);
    assertThat(session.getAttribute("import.file_name"))
        .as("only the basename is remembered, never the traversal segments")
        .isEqualTo("x.sh");
    assertThat((String) session.getAttribute("import.file_path"))
        .isEqualTo(uploadDirectory.resolve(storedName).toString());
  }

  @Test
  @DisplayName(
      "B31 an 8-character alphanumeric extension is at the whitelist's own boundary and is accepted")
  void eightCharacterExtensionIsAccepted() {
    MockMultipartFile file =
        new MockMultipartFile("file", "report.abcdefgh", "text/plain", "x".getBytes());

    String storedName = storage(uploadDirectory).store(new MockHttpSession(), file);

    assertThat(storedName).matches(STORED_NAME_PREFIX + "abcdefgh$");
  }

  @Test
  @DisplayName("B31 a 9-character extension exceeds the whitelist and falls back to csv")
  void nineCharacterExtensionFallsBackToCsv() {
    MockMultipartFile file =
        new MockMultipartFile("file", "report.abcdefghi", "text/plain", "x".getBytes());

    String storedName = storage(uploadDirectory).store(new MockHttpSession(), file);

    assertThat(storedName).matches(STORED_NAME_PREFIX + "csv$");
  }

  @Test
  @DisplayName("B31 a mixed-case extension is lower-cased before the whitelist check")
  void mixedCaseExtensionIsLowerCased() {
    MockMultipartFile file = new MockMultipartFile("file", "evil.CSV", "text/csv", "x".getBytes());

    String storedName = storage(uploadDirectory).store(new MockHttpSession(), file);

    assertThat(storedName).matches(STORED_NAME_PREFIX + "csv$");
  }

  @Test
  @DisplayName("B31 no extension at all falls back to csv")
  void noExtensionFallsBackToCsv() {
    MockMultipartFile file = new MockMultipartFile("file", "README", "text/plain", "x".getBytes());

    String storedName = storage(uploadDirectory).store(new MockHttpSession(), file);

    assertThat(storedName).matches(STORED_NAME_PREFIX + "csv$");
  }

  @Test
  @DisplayName("B31 an extension containing a non-alphanumeric character is rejected")
  void nonAlphanumericExtensionFallsBackToCsv() {
    MockMultipartFile file =
        new MockMultipartFile("file", "archive.tar gz", "application/octet-stream", "x".getBytes());

    String storedName = storage(uploadDirectory).store(new MockHttpSession(), file);

    assertThat(storedName).matches(STORED_NAME_PREFIX + "csv$");
  }

  @Test
  @DisplayName("B31 two uploads in the same session never collide on the generated name")
  void twoUploadsNeverCollideOnTheGeneratedName() {
    ImportUploadStorage store = storage(uploadDirectory);
    MockHttpSession session = new MockHttpSession();
    MockMultipartFile first = new MockMultipartFile("file", "a.csv", "text/csv", "a".getBytes());
    MockMultipartFile second = new MockMultipartFile("file", "b.csv", "text/csv", "b".getBytes());

    String firstName = store.store(session, first);
    String secondName = store.store(session, second);

    assertThat(firstName).isNotEqualTo(secondName);
    assertThat(session.getAttribute("import.file_name")).isEqualTo("b.csv");
  }

  @Test
  @DisplayName("currentFileName is empty for a null session and for a session with no upload yet")
  void currentFileNameIsEmptyWithoutAnUpload() {
    ImportUploadStorage store = storage(uploadDirectory);

    assertThat(store.currentFileName(null)).isEqualTo(Optional.empty());
    assertThat(store.currentFileName(new MockHttpSession())).isEqualTo(Optional.empty());
  }

  @Test
  @DisplayName("currentFileName reads back the original name recorded by store()")
  void currentFileNameReadsBackTheOriginalName() {
    ImportUploadStorage store = storage(uploadDirectory);
    MockHttpSession session = new MockHttpSession();
    store.store(session, new MockMultipartFile("file", "klienci.csv", "text/csv", "x".getBytes()));

    assertThat(store.currentFileName(session)).isEqualTo(Optional.of("klienci.csv"));
  }
}
