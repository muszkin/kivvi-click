package click.kivvi.infrastructure;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Translates a navigation key ({@code "dashboard"}, {@code "main"}, …) into its label — mirrors
 * {@code Navigation}'s {@code $translator->trans('nav.'.$key)} calls, both for group headings and
 * for item labels.
 */
@Component
public class NavigationLabels {

  private final MessageSource messageSource;

  public NavigationLabels(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public String label(String key, Locale locale) {
    return messageSource.getMessage("nav." + key, null, locale);
  }
}
