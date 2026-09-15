package click.kivvi.infrastructure.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Refuses to start the application when there is nowhere to send mail.
 *
 * <p>Boot's mail auto-configuration is happy to build a sender around a blank host and only
 * discovers there is no relay when the first message is already in the outbox. That failure mode is
 * the worst available one: the application looks healthy, visitors sign up, and every confirmation
 * quietly piles up as a retry until it is marked failed. Refusing to start is louder, earlier, and
 * fixable by the person who is already looking at the deployment.
 *
 * <p>Not active under {@code dev}, where {@link FilesystemMailTransport} is the transport and there
 * is deliberately no relay at all.
 */
@Component
@Profile("!dev")
public class MailConfigurationGuard {

  public MailConfigurationGuard(@Value("${spring.mail.host:}") String host) {
    if (host == null || host.isBlank()) {
      throw new IllegalStateException(
          "No SMTP host is configured. Set SPRING_MAIL_HOST (plus SPRING_MAIL_USERNAME and "
              + "SPRING_MAIL_PASSWORD) for this environment, or run under the `dev` profile, "
              + "where messages are written to disk instead of being sent.");
    }
  }
}
