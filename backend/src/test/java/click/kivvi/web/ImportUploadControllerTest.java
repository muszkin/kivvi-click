package click.kivvi.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.ImportUploadService;
import click.kivvi.infrastructure.importing.ImportUploadStorage;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sliced coverage for the wire shape of {@code POST /import/upload} — mirrors {@code
 * ImportControllerTest} (login journey's {@code LoginControllerTest}) in never needing a database:
 * {@link ImportUploadStorage} only touches the filesystem and the session. The upload directory is
 * redirected to a JUnit temp directory so this test never writes into the working copy.
 */
@WebMvcTest(ImportUploadController.class)
@Import({ImportUploadService.class, ImportUploadStorage.class})
class ImportUploadControllerTest {

  @TempDir static Path uploadDirectory;

  @DynamicPropertySource
  static void uploadDirectoryProperty(DynamicPropertyRegistry registry) {
    registry.add("kivvi.import.upload-directory", uploadDirectory::toString);
  }

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("B31 a POSTed file is stored and redirects (302) to step 2")
  void uploadStoresTheFileAndRedirectsToStepTwo() throws Exception {
    mvc.perform(multipart("/import/upload").file(csv()).param("locale", "pl"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/pl/import/2"));

    assertThat(uploadDirectory).isDirectoryContaining(p -> p.toString().endsWith(".csv"));
  }

  @ParameterizedTest(name = "posted from /{0} → {1}")
  @CsvSource({"pl, /pl/import/2", "en, /en/import/2"})
  @DisplayName("PIO-125 the redirect returns to the locale the upload was posted from")
  void theRedirectReturnsToThePostedLocale(String locale, String expectedLocation)
      throws Exception {
    mvc.perform(multipart("/import/upload").file(csv()).param("locale", locale))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", expectedLocation));
  }

  @Test
  @DisplayName("PIO-125 an upload that names no locale lands on step 2 in the default, English")
  void aMissingLocaleFallsBackToTheDefault() throws Exception {
    mvc.perform(multipart("/import/upload").file(csv()))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/en/import/2"));
  }

  @ParameterizedTest(name = "locale \"{0}\" → the default")
  @ValueSource(strings = {"de", "PL", "", " pl", "../pl"})
  @DisplayName(
      "PIO-125 an unsupported or malformed locale falls back to the default, never into the URL")
  void anUnsupportedLocaleFallsBackToTheDefault(String locale) throws Exception {
    mvc.perform(multipart("/import/upload").file(csv()).param("locale", locale))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/en/import/2"));
  }

  private static MockMultipartFile csv() {
    return new MockMultipartFile("file", "klienci-e2e.csv", "text/csv", "email;imie\n".getBytes());
  }

  @Test
  @DisplayName("B31 a request with no file part is not found")
  void missingFilePartIsNotFound() throws Exception {
    mvc.perform(multipart("/import/upload")).andExpect(status().isNotFound());
  }
}
