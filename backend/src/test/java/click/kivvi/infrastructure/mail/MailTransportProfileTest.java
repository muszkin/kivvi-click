package click.kivvi.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

/**
 * Risk R5 from the spec, in test form: the transport that writes mail to a directory must never be
 * the one production uses, and an environment with no relay must refuse to start rather than
 * pretend to send.
 *
 * <p>The profile annotations are asserted directly. They are the whole mechanism — a dropped
 * {@code @Profile("dev")} compiles, passes every other test, and loses every confirmation the day
 * it reaches production.
 */
class MailTransportProfileTest {

  @Test
  @DisplayName("the filesystem transport exists only under the dev profile")
  void theFilesystemTransportIsDevOnly() {
    assertThat(profilesOf(FilesystemMailTransport.class)).containsExactly("dev");
  }

  @Test
  @DisplayName("the SMTP transport exists everywhere except dev, so exactly one is ever present")
  void theSmtpTransportIsEverywhereElse() {
    assertThat(profilesOf(SmtpMailTransport.class)).containsExactly("!dev");
  }

  @Test
  @DisplayName("the guard is skipped under dev, where there is deliberately no relay")
  void theGuardIsSkippedUnderDev() {
    assertThat(profilesOf(MailConfigurationGuard.class)).containsExactly("!dev");
  }

  @Test
  @DisplayName("a blank SMTP host stops the application from starting at all")
  void aBlankHostRefusesToStart() {
    assertThatIllegalStateException()
        .isThrownBy(() -> new MailConfigurationGuard("   "))
        .withMessageContaining("SPRING_MAIL_HOST");
    assertThatIllegalStateException().isThrownBy(() -> new MailConfigurationGuard(""));
    assertThatIllegalStateException().isThrownBy(() -> new MailConfigurationGuard(null));
  }

  @Test
  @DisplayName("a configured host starts normally")
  void aConfiguredHostIsAccepted() {
    assertThat(new MailConfigurationGuard("smtp-relay.brevo.com")).isNotNull();
  }

  private static String[] profilesOf(Class<?> type) {
    Profile profile = type.getAnnotation(Profile.class);
    assertThat(profile).as("%s must declare a profile", type.getSimpleName()).isNotNull();
    return Arrays.stream(profile.value()).toArray(String[]::new);
  }
}
