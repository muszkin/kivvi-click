package click.kivvi.application;

import click.kivvi.infrastructure.importing.ImportUploadStorage;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores the file an in-progress import wizard has uploaded and reads its name back for a later
 * step — mirrors {@code ImportController} reading from the injected {@code ImportUploadStorage}.
 */
@Service
public class ImportUploadService {

  private final ImportUploadStorage storage;

  public ImportUploadService(ImportUploadStorage storage) {
    this.storage = storage;
  }

  public String store(HttpSession session, MultipartFile file) {
    return storage.store(session, file);
  }

  public Optional<String> currentFileName(HttpSession session) {
    return storage.currentFileName(session);
  }
}
