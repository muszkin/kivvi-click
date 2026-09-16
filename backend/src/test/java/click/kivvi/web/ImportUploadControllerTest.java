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
  @DisplayName(
      "B31 a POSTed file is stored and redirects (302) to step 2 in the default locale, which"
          + " PIO-125 made /en/import/2")
  void uploadStoresTheFileAndRedirectsToStepTwo() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "klienci-e2e.csv", "text/csv", "email;imie\n".getBytes());

    mvc.perform(multipart("/import/upload").file(file))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/en/import/2"));

    assertThat(uploadDirectory).isDirectoryContaining(p -> p.toString().endsWith(".csv"));
  }

  @Test
  @DisplayName("B31 a request with no file part is not found")
  void missingFilePartIsNotFound() throws Exception {
    mvc.perform(multipart("/import/upload")).andExpect(status().isNotFound());
  }
}
