package click.kivvi.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The server-side half of PIO-117's key-drift guard; {@code frontend/test/unit/i18n.spec.ts} is the
 * client-side half.
 *
 * <p>A key present in one bundle and missing from the other does not fail loudly: the message
 * source falls back to another locale's bundle, so a waitlist error or a line of the confirmation
 * mail quietly arrives in the wrong language. PIO-125 sends that mail in whichever language the
 * form was submitted in, which only holds while both bundles carry every key.
 */
class MessageBundlesTest {

  private static final String POLISH_BUNDLE = "messages_pl.properties";
  private static final String ENGLISH_BUNDLE = "messages_en.properties";

  @Test
  @DisplayName("PIO-117 messages_pl and messages_en carry exactly the same keys")
  void bothBundlesCarryTheSameKeys() throws IOException {
    Properties polish = load(POLISH_BUNDLE);
    Properties english = load(ENGLISH_BUNDLE);

    assertThat(keysOf(english)).isEqualTo(keysOf(polish));
  }

  @Test
  @DisplayName("PIO-117 no English message is left as its Polish original")
  void noEnglishMessageIsLeftUntranslated() throws IOException {
    Properties polish = load(POLISH_BUNDLE);
    Properties english = load(ENGLISH_BUNDLE);

    for (String key : keysOf(polish)) {
      assertThat(english.getProperty(key)).as(key).isNotEqualTo(polish.getProperty(key));
    }
  }

  private static Set<String> keysOf(Properties bundle) {
    return new TreeSet<>(bundle.stringPropertyNames());
  }

  private static Properties load(String name) throws IOException {
    try (InputStream stream = MessageBundlesTest.class.getClassLoader().getResourceAsStream(name)) {
      assertThat(stream).as(name + " on the classpath").isNotNull();
      try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        Properties bundle = new Properties();
        bundle.load(reader);
        return bundle;
      }
    }
  }
}
