package click.kivvi.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.mail.OutboundMail;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A message really leaving over SMTP, end to end: queued in Postgres, drained by the sender job,
 * received by an SMTP server.
 *
 * <p>The server is GreenMail, running inside this JVM — no Docker container and no service in
 * {@code compose.yaml}, so Postgres stays the only backing service (CLAUDE.md, "Architecture
 * decisions"). It listens on a port the operating system picks, which is why {@code
 * spring.mail.port} arrives through {@link DynamicPropertySource} rather than from a file.
 *
 * <p>What this proves that the unit tests cannot: that the composed MIME message is something a
 * real SMTP server accepts, that both alternatives survive the trip, and that the envelope carries
 * the configured sender rather than whatever the JVM's default happens to be.
 */
@Testcontainers
@SpringBootTest
class MailOutboxSenderIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @RegisterExtension
  static final GreenMailExtension greenMail =
      new GreenMailExtension(ServerSetupTest.SMTP).withPerMethodLifecycle(false);

  @DynamicPropertySource
  static void pointSpringAtGreenMail(DynamicPropertyRegistry registry) {
    // ServerSetupTest's fixed offset port (3025), not dynamicPort(): the sender resolves
    // spring.mail.port once, when its bean is built, while the extension may rebind the server
    // around a test — and a rebound dynamic port leaves the sender talking to a closed socket,
    // which looks exactly like a server that never started.
    //
    // The bind address rather than the literal "localhost": GreenMail listens on 127.0.0.1, and on
    // a dual-stack host "localhost" resolves to ::1 first and is refused.
    registry.add("spring.mail.host", ServerSetupTest.SMTP::getBindAddress);
    registry.add("spring.mail.port", ServerSetupTest.SMTP::getPort);
  }

  @Autowired private MailOutboxStore outbox;
  @Autowired private MailOutboxSenderJob senderJob;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ApplicationContext context;

  @BeforeEach
  void emptyTheOutbox() throws FolderException {
    jdbcTemplate.update("delete from mail_outbox");
    // Purge, never reset(): reset restarts the servers, and a restart on a dynamic port binds a
    // new one — while the JavaMailSender bean is still holding the port it resolved at startup.
    greenMail.purgeEmailFromAllMailboxes();
  }

  @Test
  @DisplayName(
      "a queued message reaches the SMTP server with both alternatives and the right sender")
  void aQueuedMessageIsDelivered() throws Exception {
    outbox.enqueue(
        new OutboundMail(
            "ala@sklep.pl",
            "Potwierdź swój adres",
            "<p>Potwierdź: <a href=\"https://kivvi.click/pl/waitlist/confirm/abc\">klik</a></p>",
            "Potwierdź: https://kivvi.click/pl/waitlist/confirm/abc",
            "waitlist-confirm:abc"),
        Instant.now());

    senderJob.send();

    MimeMessage received = awaitMessages(1)[0];
    assertThat(received.getSubject()).isEqualTo("Potwierdź swój adres");
    assertThat(received.getAllRecipients()[0]).hasToString("ala@sklep.pl");
    assertThat(received.getFrom()[0].toString()).contains("no-reply@kivvi.click");

    // Both alternatives, decoded: quoted-printable would otherwise hide every Polish diacritic
    // behind an "=C4=99" and the assertion would be about the transfer encoding, not the message.
    assertThat(received.getContentType()).startsWith("multipart/");
    List<String> parts = textParts(received.getContent());
    assertThat(parts)
        .anySatisfy(
            part ->
                assertThat(part)
                    .isEqualTo("Potwierdź: https://kivvi.click/pl/waitlist/confirm/abc"))
        .anySatisfy(part -> assertThat(part).contains("<p>Potwierdź:").contains("<a href="));

    assertThat(jdbcTemplate.queryForObject("select status from mail_outbox", String.class))
        .isEqualTo("sent");
  }

  @Test
  @DisplayName("the sender's pool is a different bean from the heartbeat's in the real context")
  void theSenderDoesNotShareTheHeartbeatPool() {
    // Risk R4, asserted where it actually matters: MailSchedulingConfigTest can only show that two
    // @Bean methods return different objects, which is not the same as Spring wiring two different
    // schedulers into one running application.
    Map<String, TaskScheduler> schedulers = context.getBeansOfType(TaskScheduler.class);

    assertThat(schedulers).hasSize(2).containsKeys("heartbeatTaskScheduler", "mailTaskScheduler");
    assertThat(schedulers.get("mailTaskScheduler"))
        .isNotSameAs(schedulers.get("heartbeatTaskScheduler"));
    assertThat(
            ((ThreadPoolTaskScheduler) schedulers.get("mailTaskScheduler")).getThreadNamePrefix())
        .isEqualTo("mail-sender-");
  }

  @Test
  @DisplayName("a delivered message is not delivered again on the next pass")
  void aDeliveredMessageIsNotSentTwice() throws Exception {
    outbox.enqueue(
        new OutboundMail("once@sklep.pl", "Raz", "<p>raz</p>", "raz", "key-once"), Instant.now());

    // Two real passes back to back. This only tests anything because the job holds no
    // lockAtLeastFor: with one, shedlock would skip the second call outright and the test would
    // pass without the outbox ever being consulted.
    senderJob.send();
    senderJob.send();

    assertThat(awaitMessages(1)).hasSize(1);
    assertThat(jdbcTemplate.queryForObject("select attempts from mail_outbox", Integer.class))
        .isEqualTo(1);
  }

  /**
   * Waits for the mailbox to hold {@code count} messages.
   *
   * <p>Deliberately not {@code waitForIncomingEmail}: that latch counts every message the server
   * has ever accepted and is only cleared by {@code reset()}, which this class must not call — so
   * it would be satisfied by the previous test's message and then hand this one an empty mailbox.
   */
  private static MimeMessage[] awaitMessages(int count) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5_000;
    while (System.currentTimeMillis() < deadline) {
      MimeMessage[] received = greenMail.getReceivedMessages();
      if (received.length >= count) {
        return received;
      }
      Thread.sleep(25);
    }
    throw new AssertionError(
        "Only "
            + greenMail.getReceivedMessages().length
            + " message(s) arrived, expected "
            + count
            + ".");
  }

  /**
   * Walks whatever nesting {@code MimeMessageHelper} produced (mixed around related around
   * alternative) and collects every textual leaf, already decoded by Jakarta Mail.
   */
  private static List<String> textParts(Object content) throws Exception {
    if (content instanceof String text) {
      return List.of(text);
    }
    if (content instanceof Multipart multipart) {
      List<String> collected = new ArrayList<>();
      for (int index = 0; index < multipart.getCount(); index++) {
        collected.addAll(textParts(multipart.getBodyPart(index).getContent()));
      }
      return collected;
    }
    return List.of();
  }
}
