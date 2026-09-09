package click.kivvi.infrastructure.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * UTF-8 {@code messages_pl.properties} / {@code messages_en.properties} — Java's classic {@code
 * ResourceBundle} default encoding is ISO-8859-1, which would mangle every diacritic in the Polish
 * nav labels.
 */
@Configuration
public class MessageSourceConfig {

  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:messages");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(false);
    return source;
  }
}
