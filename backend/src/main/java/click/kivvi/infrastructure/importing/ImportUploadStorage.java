package click.kivvi.infrastructure.importing;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Keeps an uploaded import file until the wizard is finished — mirrors {@code
 * App\Panel\ImportUploadStorage}.
 *
 * <p>The file lands under {@link #uploadDirectory} with a generated name — the original name is
 * only shown back to the user — so a crafted filename can never escape the directory: the stored
 * name is always {@code <32 hex>.<ext>}, never derived from the client's own path.
 */
@Component
public class ImportUploadStorage {

  private static final String SESSION_NAME = "import.file_name";
  private static final String SESSION_PATH = "import.file_path";
  private static final String DEFAULT_EXTENSION = "csv";

  // The extension comes from the client-supplied name, so it is whitelisted rather than
  // trusted — mirrors the old stack's own `preg_match('/^[a-z0-9]{1,8}$/', $extension)`.
  private static final Pattern EXTENSION_PATTERN = Pattern.compile("^[a-z0-9]{1,8}$");

  private final SecureRandom random = new SecureRandom();
  private final Path uploadDirectory;

  public ImportUploadStorage(
      @Value("${kivvi.import.upload-directory:./var/import}") String uploadDirectory) {
    this.uploadDirectory = Path.of(uploadDirectory);
  }

  /**
   * @return the generated stored file name (e.g. {@code "3f9c...e1.csv"}).
   */
  public String store(HttpSession session, MultipartFile file) {
    String originalName = basename(file.getOriginalFilename());
    String storedName = randomHex() + "." + extensionOf(originalName);
    try {
      Files.createDirectories(uploadDirectory);
      Path target = uploadDirectory.resolve(storedName);
      file.transferTo(target);
    } catch (IOException exception) {
      throw new UncheckedIOException("Nie udało się zapisać wgranego pliku.", exception);
    }

    session.setAttribute(SESSION_NAME, originalName);
    session.setAttribute(SESSION_PATH, uploadDirectory.resolve(storedName).toString());
    return storedName;
  }

  public Optional<String> currentFileName(HttpSession session) {
    if (session == null) {
      return Optional.empty();
    }
    Object stored = session.getAttribute(SESSION_NAME);
    return stored instanceof String name && !name.isEmpty() ? Optional.of(name) : Optional.empty();
  }

  private String randomHex() {
    byte[] bytes = new byte[16];
    random.nextBytes(bytes);
    StringBuilder hex = new StringBuilder(32);
    for (byte b : bytes) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }

  /**
   * Strips any client-supplied path components down to a plain file name — mirrors Symfony's {@code
   * UploadedFile::getClientOriginalName()}, which applies this same stripping internally before the
   * old stack's {@code extensionOf()} ever sees the name, so {@code "../../x.sh"} reaches it as
   * {@code "x.sh"}.
   */
  private static String basename(String originalFilename) {
    if (originalFilename == null) {
      return "";
    }
    String normalized = originalFilename.replace('\\', '/');
    int slash = normalized.lastIndexOf('/');
    return slash >= 0 ? normalized.substring(slash + 1) : normalized;
  }

  private static String extensionOf(String basename) {
    int dot = basename.lastIndexOf('.');
    String rawExtension = dot >= 0 ? basename.substring(dot + 1) : "";
    String extension = rawExtension.toLowerCase(Locale.ROOT);
    return EXTENSION_PATTERN.matcher(extension).matches() ? extension : DEFAULT_EXTENSION;
  }
}
