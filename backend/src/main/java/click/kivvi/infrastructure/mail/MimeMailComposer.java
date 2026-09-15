package click.kivvi.infrastructure.mail;

import click.kivvi.domain.mail.OutboundMail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Fills an empty {@link MimeMessage} with one {@link OutboundMail}.
 *
 * <p>Shared by both transports on purpose: what lands on a developer's disk as an {@code .eml} is
 * then byte-for-byte the message production would have handed to the relay, envelope and multipart
 * structure included. A second, "simplified" builder for the dev path would make the thing a
 * developer inspects subtly different from the thing a subscriber receives.
 *
 * <p>{@code multipart/alternative} with the plain-text part first is what the standard asks for:
 * clients pick the last part they can render, so text first and HTML second means a text client
 * shows text and everything else shows HTML.
 */
@Component
public class MimeMailComposer {

  private static final String UTF_8 = "UTF-8";

  private final String fromAddress;
  private final String fromName;

  public MimeMailComposer(
      @Value("${kivvi.mail.from}") String fromAddress,
      @Value("${kivvi.mail.display-name}") String fromName) {
    this.fromAddress = fromAddress;
    this.fromName = fromName;
  }

  public MimeMessage compose(MimeMessage empty, OutboundMail mail) {
    try {
      MimeMessageHelper helper = new MimeMessageHelper(empty, true, UTF_8);
      helper.setFrom(fromAddress, fromName);
      helper.setTo(mail.recipient());
      helper.setSubject(mail.subject());
      helper.setText(mail.textBody(), mail.htmlBody());
      return empty;
    } catch (MessagingException | UnsupportedEncodingException exception) {
      throw new MailTransportException("Could not assemble the message.", exception);
    }
  }
}
