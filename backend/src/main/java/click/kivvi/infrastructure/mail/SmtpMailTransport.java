package click.kivvi.infrastructure.mail;

import click.kivvi.domain.mail.OutboundMail;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * The real transport: one SMTP conversation per message, over the relay configured by {@code
 * spring.mail.*}. Brevo today, and changing that is four environment variables — the integration is
 * deliberately standard SMTP rather than a provider SDK
 * (context/research/2026-09-15-transactional-email-provider.md).
 *
 * <p>{@code @Profile("!dev")} is the other half of {@link FilesystemMailTransport}'s
 * {@code @Profile("dev")}: exactly one transport exists in any given context, so there is no
 * ordering or primary-bean question about which one a message goes through.
 */
@Component
@Profile("!dev")
public class SmtpMailTransport implements MailTransport {

  private final JavaMailSender mailSender;
  private final MimeMailComposer composer;

  public SmtpMailTransport(JavaMailSender mailSender, MimeMailComposer composer) {
    this.mailSender = mailSender;
    this.composer = composer;
  }

  @Override
  public void send(OutboundMail mail) {
    try {
      mailSender.send(composer.compose(mailSender.createMimeMessage(), mail));
    } catch (MailException exception) {
      throw new MailTransportException("The relay refused the message.", exception);
    }
  }
}
