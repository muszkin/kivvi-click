package click.kivvi.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Refuses to start the application when the relay is not fully configured.
 *
 * <p>Boot's mail auto-configuration is happy to build a sender around a blank host or absent
 * credentials, and only discovers there is nobody to talk to when the first message is already in
 * the outbox. That failure mode is the worst available one: the application looks healthy, the
 * post-deploy check passes because the site serves perfectly well, visitors sign up, and every
 * confirmation quietly retries for thirteen hours before being parked as {@code failed} — in a
 * system that has no monitoring yet. Refusing to start is louder, earlier, and fixable by the
 * person who is already looking at the deployment.
 *
 * <p>The credentials are checked as well as the host, and that is not belt-and-braces. The host has
 * a sensible default in both production compose files ({@code smtp-relay.brevo.com}), so a
 * half-configured stack never shows up as a blank host — it shows up as a relay that answers and
 * then rejects every AUTH. The username and password have no defaults anywhere, which is exactly
 * why they are the pair worth checking.
 *
 * <p>Only when SMTP authentication is actually on: a relay reached without it (GreenMail in the
 * integration tests, or an internal smarthost) has no credentials to supply, and demanding them
 * would refuse to start a configuration that works.
 *
 * <p>Not active under {@code dev}, where {@link FilesystemMailTransport} is the transport and there
 * is deliberately no relay at all.
 */
@Component
@Profile("!dev")
public class MailConfigurationGuard {

  public MailConfigurationGuard(
      @Value("${spring.mail.host:}") String host,
      @Value("${spring.mail.username:}") String username,
      @Value("${spring.mail.password:}") String password,
      @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean authenticationRequired) {
    if (isBlank(host)) {
      throw new IllegalStateException(
          "No SMTP host is configured. Set SMTP_HOST (spring.mail.host) for this environment, or "
              + "run under the `dev` profile, where messages are written to disk instead of being "
              + "sent.");
    }
    if (authenticationRequired && (isBlank(username) || isBlank(password))) {
      throw new IllegalStateException(
          "The SMTP relay at "
              + host
              + " requires authentication, but SMTP_USERNAME (spring.mail.username) and/or "
              + "SMTP_PASSWORD (spring.mail.password) is empty. Set both in this deployment's "
              + "environment — the password is the provider's SMTP key, not an API key. Starting "
              + "without them would look healthy and fail every single message at AUTH.");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
