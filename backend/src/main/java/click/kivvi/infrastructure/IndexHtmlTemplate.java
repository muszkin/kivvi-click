package click.kivvi.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The built Vue SPA's {@code index.html}, loaded once from the classpath. The frontend build's
 * output is copied into {@code src/main/resources/static} before packaging (see {@code
 * backend/README.md}); a missing file fails application startup immediately rather than serving a
 * broken shell.
 */
@Component
public class IndexHtmlTemplate {

  private final String content;

  public IndexHtmlTemplate() {
    ClassPathResource resource = new ClassPathResource("static/index.html");
    try {
      this.content = resource.getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "frontend/dist/index.html was not copied into backend/src/main/resources/static "
              + "before this jar was built — see backend/README.md",
          e);
    }
  }

  public String content() {
    return content;
  }
}
