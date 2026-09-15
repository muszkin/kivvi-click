package click.kivvi.infrastructure.mail;

import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * The seam between "what a message says" and "which engine turns it into HTML".
 *
 * <p>Thymeleaf lives here rather than in {@code application} for the same reason {@code
 * JdbcTemplate} and {@code JavaMailSender} do: it is a third-party engine reading files off the
 * classpath, and the layer above should be able to describe a message without importing it.
 *
 * <p>The locale is passed explicitly on every call and never read from the request. A confirmation
 * mail is written in the language the visitor signed up in, which may be hours or days ago and, by
 * the time the sender picks the row up, is nobody's current request.
 */
@Component
public class MailTemplateRenderer {

  private final ITemplateEngine templateEngine;

  public MailTemplateRenderer(ITemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  /**
   * @param template classpath-relative template name under {@code templates/}, without the suffix
   */
  public String render(String template, Locale locale, Map<String, Object> model) {
    Context context = new Context(locale, model);
    return templateEngine.process(template, context);
  }
}
