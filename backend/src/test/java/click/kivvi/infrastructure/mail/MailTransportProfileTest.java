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
        .isThrownBy(() -> guard("   ", "user", "key"))
        .withMessageContaining("SMTP_HOST");
    assertThatIllegalStateException().isThrownBy(() -> guard("", "user", "key"));
    assertThatIllegalStateException().isThrownBy(() -> guard(null, "user", "key"));
  }

  @Test
  @DisplayName("missing credentials stop it too, which is the case the host default would hide")
  void missingCredentialsRefuseToStart() {
    // Both production compose files default the host to smtp-relay.brevo.com, so a half-configured
    // stack never presents as a blank host. It presents as a relay that answers and rejects every
    // AUTH — the container healthy, the site serving, and every confirmation retrying for thirteen
    // hours before being parked as failed, in a system with no monitoring.
    assertThatIllegalStateException()
        .isThrownBy(() -> guard("smtp-relay.brevo.com", "", "key"))
        .withMessageContaining("SMTP_USERNAME");
    assertThatIllegalStateException()
        .isThrownBy(() -> guard("smtp-relay.brevo.com", "user", "  "))
        .withMessageContaining("SMTP_PASSWORD");
    assertThatIllegalStateException().isThrownBy(() -> guard("smtp-relay.brevo.com", null, null));
  }

  @Test
  @DisplayName("a relay that needs no authentication needs no credentials either")
  void anUnauthenticatedRelayNeedsNoCredentials() {
    // GreenMail in the integration tests, or an internal smarthost: demanding credentials here
    // would refuse to start a configuration that works perfectly well.
    assertThat(new MailConfigurationGuard("127.0.0.1", "", "", false)).isNotNull();
  }

  @Test
  @DisplayName("a fully configured relay starts normally")
  void aConfiguredRelayIsAccepted() {
    assertThat(guard("smtp-relay.brevo.com", "user", "smtp-key")).isNotNull();
  }

  private static MailConfigurationGuard guard(String host, String username, String password) {
    return new MailConfigurationGuard(host, username, password, true);
  }

  private static String[] profilesOf(Class<?> type) {
    Profile profile = type.getAnnotation(Profile.class);
    assertThat(profile).as("%s must declare a profile", type.getSimpleName()).isNotNull();
    return Arrays.stream(profile.value()).toArray(String[]::new);
  }
}
