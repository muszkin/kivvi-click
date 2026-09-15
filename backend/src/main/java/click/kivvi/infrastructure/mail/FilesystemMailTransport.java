package click.kivvi.infrastructure.mail;

import click.kivvi.domain.mail.OutboundMail;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The {@code dev} profile's transport: every message is written to disk as a real {@code .eml}
 * instead of being sent.
 *
 * <p>This exists so the end-to-end suite can read a confirmation link out of an actual message and
 * close the double opt-in loop. A mail-catcher container would have done the same job while
 * contradicting the rule that Postgres is the only backing service (CLAUDE.md, "Architecture
 * decisions"), so the loop is closed with a directory instead.
 *
 * <p>{@code @Profile("dev")} is not a nicety. A bean that quietly diverted production mail to a
 * directory nobody reads would lose every confirmation without a single error anywhere; outside
 * this profile it does not exist at all, and {@link MailConfigurationGuard} refuses to let the
 * application start without a relay to talk to instead.
 */
@Component
@Profile("dev")
public class FilesystemMailTransport implements MailTransport {

  private static final Logger LOG = LoggerFactory.getLogger(FilesystemMailTransport.class);

  /** Two messages in the same millisecond must not overwrite each other. */
  private final AtomicLong sequence = new AtomicLong();

  private final Path directory;
  private final MimeMailComposer composer;

  public FilesystemMailTransport(
      @Value("${kivvi.mail.dev-directory}") Path directory, MimeMailComposer composer) {
    this.directory = directory;
    this.composer = composer;
  }

  @Override
  public void send(OutboundMail mail) {
    MimeMessage message =
        composer.compose(new MimeMessage(Session.getInstance(new Properties())), mail);
    Path file = directory.resolve(fileName(mail));
    try {
      Files.createDirectories(directory);
      try (OutputStream out = Files.newOutputStream(file)) {
        message.writeTo(out);
      }
    } catch (IOException | jakarta.mail.MessagingException exception) {
      throw new MailTransportException("Could not write " + file + ".", exception);
    }
    LOG.info("Dev profile: wrote a message to {} instead of sending it.", file);
  }

  /**
   * The recipient is in the name so a test can wait for its own message rather than for whichever
   * one happens to land first — several specs share one dev stack.
   */
  private String fileName(OutboundMail mail) {
    String recipient = mail.recipient().replaceAll("[^A-Za-z0-9._@-]", "_");
    return Instant.now().toEpochMilli()
        + "-"
        + sequence.incrementAndGet()
        + "-"
        + recipient
        + ".eml";
  }
}
