package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real-HTTP, real-Postgres coverage for the import wizard's 404s, the upload's 302 and the session
 * round trip of the uploaded file name — mirrors {@code SessionRoundTripIT} (login journey), the
 * one scenario a sliced {@code @WebMvcTest} cannot prove: that the file name survives through the
 * JDBC-backed {@code SessionRepository} across two real requests, not merely a same-JVM in-memory
 * session.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ImportApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  /**
   * Repair-3 (R3-B): {@code uploadDirectory} used to be the {@code @TempDir} itself, created
   * directly under the JVM's shared {@code java.io.tmpdir} (this host: {@code /tmp}, ~3000+ entries
   * from unrelated concurrent tooling, observed mutating on a sub-second cadence — see {@code
   * waves/wave-4/integration/import-wizard-failure-diagnosis.txt}). The traversal test below
   * asserted an exact before/after snapshot of that shared directory's children, which failed 2/2
   * for reasons unrelated to the code under test. {@code root} is this class's own private
   * {@code @TempDir}; the configured upload directory is a subdirectory of it ({@code root/import},
   * created lazily by {@code ImportUploadStorage.store()} on first write), so every filesystem
   * assertion in this class can be scoped to {@code root} — a directory only this test class ever
   * writes to — and never has to look at, let alone diff, the shared {@code /tmp}.
   */
  @TempDir static Path root;

  static Path uploadDirectory;

  @DynamicPropertySource
  static void uploadDirectoryProperty(DynamicPropertyRegistry registry) {
    uploadDirectory = root.resolve("import");
    registry.add("kivvi.import.upload-directory", uploadDirectory::toString);
  }

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @DisplayName("B31 GET /api/v1/pl/import/5 (outside 1-4) is not found")
  void outOfRangeStepApiIsNotFound() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/pl/import/5", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName("B31/DEV-12 GET /pl/import/5 (outside 1-4) is a 404 document, not the 200 SPA shell")
  void outOfRangeStepDocumentIsNotFound() {
    ResponseEntity<String> response = restTemplate.getForEntity("/pl/import/5", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName(
      "B01 every import-wizard document URL (bare, step 1-4) renders 200 and carries the SPA"
          + " document, over a real HTTP round trip — not only the sliced SpaDocumentControllerTest")
  void everyImportDocumentUrlRenders200() {
    for (String path :
        new String[] {
          "/pl/import", "/pl/import/1", "/pl/import/2", "/pl/import/3", "/pl/import/4"
        }) {
      ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

      assertThat(response.getStatusCode().value()).as("GET %s", path).isEqualTo(200);
      assertThat(response.getBody()).as("GET %s body", path).contains("<html");
    }
  }

  @Test
  @DisplayName("B31 a request to /import/upload with no file part is not found")
  void missingFilePartIsNotFound() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    ResponseEntity<Void> response =
        restTemplate.postForEntity(
            "/import/upload",
            new HttpEntity<>(new LinkedMultiValueMap<String, Object>(), headers),
            Void.class);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName(
      "B31 uploading a file from /pl redirects (302) to exactly /pl/import/2, and the next GET on"
          + " the same session reflects the uploaded file's original name — a real spring_session"
          + " round trip, not a same-JVM in-memory session")
  void uploadRedirectsAndTheFileNameSurvivesASecondRequest() {
    TestRestTemplate noRedirects = restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", namedResource("klienci-oracle.csv", "email;imie\nhania.k@aurea.pl;Hania\n"));
    // PIO-125: what Dropzone.vue sends from a /pl page, so the redirect stays in Polish.
    body.add("locale", "pl");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    ResponseEntity<Void> upload =
        noRedirects.postForEntity("/import/upload", new HttpEntity<>(body, headers), Void.class);

    assertThat(upload.getStatusCode().value()).isEqualTo(302);
    assertThat(upload.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("/pl/import/2");
    String sessionCookie = upload.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertThat(sessionCookie).as("Spring Session issues a SESSION cookie").startsWith("SESSION=");

    HttpHeaders cookieHeader = new HttpHeaders();
    cookieHeader.add(HttpHeaders.COOKIE, sessionCookie);
    ResponseEntity<String> stepTwo =
        restTemplate.exchange(
            "/api/v1/pl/import/2", HttpMethod.GET, new HttpEntity<>(cookieHeader), String.class);

    assertThat(stepTwo.getStatusCode().value()).isEqualTo(200);
    assertThat(stepTwo.getBody()).contains("\"name\":\"klienci-oracle.csv\"");
  }

  @Test
  @DisplayName("B31 without any prior upload, step 2's file name falls back to \"klienci.csv\"")
  void withoutAnUploadStepTwoFallsBackToTheFixtureFileName() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("/api/v1/pl/import/2", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).contains("\"name\":\"klienci.csv\"");
  }

  /**
   * Follow-up (review finding, LOW): an earlier version of {@code ImportUploadController} also
   * 404'd on a present-but-empty file (via {@code MultipartFile#isEmpty()}), which the old stack
   * never did — {@code ImportController::upload()} only checks {@code !$file instanceof
   * UploadedFile} (a wholly absent field), never the file's size. A 0-byte upload is a real,
   * reachable case (an empty CSV, or a file picker cancelled after the field was already
   * multipart-encoded) and must be stored and redirected exactly like a non-empty one.
   */
  @Test
  @DisplayName("B31 empty file is stored and redirects like the old stack")
  void emptyFileIsStoredAndRedirectsLikeTheOldStack() throws IOException {
    Set<String> storedBefore = storedFileNames();
    TestRestTemplate noRedirects = restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", namedResource("empty.csv", ""));
    body.add("locale", "en");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    ResponseEntity<Void> upload =
        noRedirects.postForEntity("/import/upload", new HttpEntity<>(body, headers), Void.class);

    assertThat(upload.getStatusCode().value()).isEqualTo(302);
    assertThat(upload.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("/en/import/2");

    List<String> newFiles = newEntries(storedBefore, storedFileNames());
    assertThat(newFiles).as("the empty file was stored, not rejected").hasSize(1);
    assertThat(newFiles.get(0)).matches("^[0-9a-f]{32}\\.csv$");
    assertThat(Files.size(uploadDirectory.resolve(newFiles.get(0)))).isZero();
  }

  /**
   * Follow-up (review finding, LOW): {@code ImportUploadStorageTest} already proves the traversal
   * control at the unit level (a directly-constructed {@code MockMultipartFile}); this proves the
   * same control end to end, over a real HTTP multipart upload through the actual servlet
   * container's own request parsing, storing under a real configured directory.
   *
   * <p>Repair-3 (R3-B): every filesystem assertion here is scoped to {@code root} (this class's own
   * private {@code @TempDir} — see its Javadoc), never to the shared {@code /tmp}: (1) {@code root}
   * contains only the {@code import} subdirectory the storage code itself created — an absolute
   * invariant, true regardless of method execution order, not a racy before/after diff of a
   * directory other host processes also write to; (2) exactly one new, correctly-named file lands
   * inside it; (3) the raw traversal segment's own filename, {@code x.sh}, never appears anywhere
   * under {@code root} — the strongest possible negative: not merely "unchanged sibling count" but
   * "this specific name was never written, anywhere in this whole subtree".
   */
  @Test
  @DisplayName(
      "B31 a path-traversal original name never escapes the upload directory over a real HTTP"
          + " round trip: stored as <32 hex>.sh directly inside it, nothing written to its parent")
  void pathTraversalOriginalNameNeverEscapesTheUploadDirectoryOverHttp() throws IOException {
    Set<String> storedBefore = storedFileNames();
    TestRestTemplate noRedirects = restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", namedResource("../../x.sh", "echo hi"));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    ResponseEntity<Void> upload =
        noRedirects.postForEntity("/import/upload", new HttpEntity<>(body, headers), Void.class);

    assertThat(upload.getStatusCode().value()).isEqualTo(302);
    // No locale field in this body, so the redirect falls back to the default locale.
    assertThat(upload.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("/en/import/2");

    assertThat(childNames(root))
        .as(
            "root's only direct child is the configured upload directory itself — nothing this"
                + " class ever writes escapes to root's own level")
        .containsExactly("import");

    List<String> newFiles = newEntries(storedBefore, storedFileNames());
    assertThat(newFiles).as("exactly one new file, stored inside uploadDirectory").hasSize(1);
    assertThat(newFiles.get(0)).matches("^[0-9a-f]{32}\\.sh$");
    assertThat(uploadDirectory.resolve(newFiles.get(0))).exists();

    assertThat(filesNamed(root, "x.sh"))
        .as("the raw traversal filename \"x.sh\" must never appear anywhere under root")
        .isEmpty();
  }

  private static ByteArrayResource namedResource(String filename, String content) {
    return new ByteArrayResource(content.getBytes()) {
      @Override
      public String getFilename() {
        return filename;
      }
    };
  }

  private static Set<String> storedFileNames() throws IOException {
    return listEntries(uploadDirectory).stream()
        .map(p -> p.getFileName().toString())
        .collect(Collectors.toSet());
  }

  private static List<String> newEntries(Set<String> before, Set<String> after) {
    return after.stream().filter(name -> !before.contains(name)).toList();
  }

  private static Set<Path> listEntries(Path directory) throws IOException {
    if (!Files.exists(directory)) {
      return Set.of();
    }
    try (Stream<Path> entries = Files.list(directory)) {
      return entries.collect(Collectors.toSet());
    }
  }

  /** The direct children's own file names (not full paths) of {@code directory}, order-free. */
  private static Set<String> childNames(Path directory) throws IOException {
    return listEntries(directory).stream()
        .map(p -> p.getFileName().toString())
        .collect(Collectors.toSet());
  }

  /**
   * Every path anywhere under {@code directory} (recursive) whose own file name is {@code name}.
   */
  private static List<Path> filesNamed(Path directory, String name) throws IOException {
    try (Stream<Path> walk = Files.walk(directory)) {
      return walk.filter(p -> p.getFileName().toString().equals(name)).toList();
    }
  }
}
