package click.kivvi.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import click.kivvi.domain.mail.MailRetryPolicy;
import click.kivvi.domain.mail.OutboundMail;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * One pass of the sender, Spring-free and database-free. What each store method does to Postgres is
 * {@code MailOutboxStoreIT}'s job; what is decided here is which of them gets called, and when.
 *
 * <p>The job's own logger is detached from its parent for the duration of every test in this class.
 * Giving up on a message logs at {@code ERROR} deliberately — a confirmation nobody receives is a
 * subscriber lost in silence — and {@code FailOnWarnLogExtension} fails any test in which {@code
 * click.kivvi.*} logs at WARN or above. Both rules are right; this is the one place they meet, so
 * the events are captured here instead of propagating to the policy's appender.
 */
class MailOutboxSenderJobTest {

  private static final int BATCH_SIZE = 20;

  private Logger jobLogger;
  private ListAppender<ILoggingEvent> appender;
  private boolean originalAdditivity;

  @BeforeEach
  void captureTheJobsOwnLog() {
    jobLogger = (Logger) LoggerFactory.getLogger(MailOutboxSenderJob.class);
    originalAdditivity = jobLogger.isAdditive();
    jobLogger.setAdditive(false);
    appender = new ListAppender<>();
    appender.start();
    jobLogger.addAppender(appender);
  }

  @AfterEach
  void releaseTheJobsOwnLog() {
    jobLogger.detachAppender(appender);
    jobLogger.setAdditive(originalAdditivity);
    appender.stop();
  }

  @Test
  @DisplayName("a message that leaves is marked sent and never touched again")
  void aDeliveredMessageIsMarkedSent() {
    RecordingStore store = new RecordingStore(claimed(7, 1));
    RecordingTransport transport = new RecordingTransport();

    new MailOutboxSenderJob(store, transport, BATCH_SIZE).send();

    assertThat(transport.sent).hasSize(1);
    assertThat(store.sent).containsExactly(7L);
    assertThat(store.rescheduled).isEmpty();
    assertThat(store.givenUp).isEmpty();
  }

  @Test
  @DisplayName("the transport receives both body parts, and no deduplication key to be confused by")
  void theTransportReceivesTheWholeMessage() {
    RecordingStore store = new RecordingStore(claimed(1, 1));
    RecordingTransport transport = new RecordingTransport();

    new MailOutboxSenderJob(store, transport, BATCH_SIZE).send();

    OutboundMail sent = transport.sent.getFirst();
    assertThat(sent.recipient()).isEqualTo("ala@sklep.pl");
    assertThat(sent.subject()).isEqualTo("Potwierdź adres");
    assertThat(sent.htmlBody()).isEqualTo("<p>Potwierdź</p>#1");
    assertThat(sent.textBody()).isEqualTo("Potwierdź: https://kivvi.click/pl/x");
    assertThat(sent.deduplicationKey()).isEmpty();
  }

  @Test
  @DisplayName("a first failure comes back in a minute, not immediately")
  void aFirstFailureIsRescheduledWithTheFirstBackoff() {
    RecordingStore store = new RecordingStore(claimed(3, 1));
    Instant before = Instant.now();

    new MailOutboxSenderJob(store, failingWith("550 mailbox unavailable"), BATCH_SIZE).send();

    assertThat(store.sent).isEmpty();
    assertThat(store.rescheduled).hasSize(1);
    Reschedule retry = store.rescheduled.getFirst();
    assertThat(retry.id()).isEqualTo(3L);
    assertThat(retry.nextAttemptAt()).isAfterOrEqualTo(before.plus(Duration.ofMinutes(1)));
    assertThat(retry.nextAttemptAt()).isBefore(before.plus(Duration.ofMinutes(2)));
    assertThat(retry.error()).contains("550 mailbox unavailable");
  }

  @Test
  @DisplayName("each further failure waits longer, following the ticket's schedule")
  void theBackoffWidensWithEveryAttempt() {
    for (int attempt = 1; attempt < MailRetryPolicy.MAX_ATTEMPTS; attempt++) {
      RecordingStore store = new RecordingStore(claimed(attempt, attempt));
      Instant before = Instant.now();

      new MailOutboxSenderJob(store, failingWith("timeout"), BATCH_SIZE).send();

      Duration expected = MailRetryPolicy.backoffAfter(attempt);
      assertThat(store.rescheduled.getFirst().nextAttemptAt())
          .as("attempt %d", attempt)
          .isAfterOrEqualTo(before.plus(expected))
          .isBefore(before.plus(expected).plus(Duration.ofMinutes(1)));
    }
  }

  @Test
  @DisplayName("once the attempt budget is spent the message is parked as failed, loudly")
  void anExhaustedMessageIsParkedAndLogged() {
    RecordingStore store = new RecordingStore(claimed(9, MailRetryPolicy.MAX_ATTEMPTS));

    new MailOutboxSenderJob(store, failingWith("connect timed out"), BATCH_SIZE).send();

    assertThat(store.rescheduled).isEmpty();
    assertThat(store.givenUp).containsExactly(9L);
    assertThat(appender.list)
        .anySatisfy(
            event -> {
              assertThat(event.getLevel()).isEqualTo(Level.ERROR);
              assertThat(event.getFormattedMessage()).contains("giving up on message 9");
            });
  }

  @Test
  @DisplayName("one unreachable mailbox does not hold up the rest of the batch")
  void oneFailureDoesNotStopTheBatch() {
    RecordingStore store = new RecordingStore(claimed(1, 1), claimed(2, 1), claimed(3, 1));
    MailTransport refusingTheSecond =
        mail -> {
          if (mail.htmlBody().contains("#2")) {
            throw new MailTransportException("refused", new IllegalStateException("550"));
          }
        };

    new MailOutboxSenderJob(store, refusingTheSecond, BATCH_SIZE).send();

    assertThat(store.sent).containsExactly(1L, 3L);
    assertThat(store.rescheduled).extracting(Reschedule::id).containsExactly(2L);
  }

  @Test
  @DisplayName("an empty outbox is a no-op, not a log line every thirty seconds")
  void anEmptyOutboxSaysNothing() {
    RecordingStore store = new RecordingStore();

    new MailOutboxSenderJob(store, new RecordingTransport(), BATCH_SIZE).send();

    assertThat(appender.list).isEmpty();
  }

  @Test
  @DisplayName("the configured batch size is what the store is asked for")
  void theBatchSizeIsPassedThrough() {
    RecordingStore store = new RecordingStore();

    new MailOutboxSenderJob(store, new RecordingTransport(), 5).send();

    assertThat(store.requestedBatchSizes).containsExactly(5);
  }

  private static MailOutboxStore.ClaimedMail claimed(long id, int attempts) {
    return new MailOutboxStore.ClaimedMail(
        id,
        "ala@sklep.pl",
        "Potwierdź adres",
        "<p>Potwierdź</p>#" + id,
        "Potwierdź: https://kivvi.click/pl/x",
        attempts);
  }

  private static MailTransport failingWith(String detail) {
    return mail -> {
      throw new MailTransportException(
          "The relay refused the message.", new RuntimeException(detail));
    };
  }

  private record Reschedule(long id, Instant nextAttemptAt, String error) {}

  private static final class RecordingTransport implements MailTransport {
    private final List<OutboundMail> sent = new ArrayList<>();

    @Override
    public void send(OutboundMail mail) {
      sent.add(mail);
    }
  }

  /**
   * A stand-in for the store: the real one needs Postgres for every one of its statements, and what
   * this test is about is which statement the job reaches for.
   */
  private static final class RecordingStore extends MailOutboxStore {
    private final List<ClaimedMail> due;
    private final List<Long> sent = new ArrayList<>();
    private final List<Long> givenUp = new ArrayList<>();
    private final List<Reschedule> rescheduled = new ArrayList<>();
    private final List<Integer> requestedBatchSizes = new ArrayList<>();

    private RecordingStore(ClaimedMail... due) {
      super(null);
      this.due = List.of(due);
    }

    @Override
    public List<ClaimedMail> claimDue(Instant now, int batchSize) {
      requestedBatchSizes.add(batchSize);
      return due;
    }

    @Override
    public void markSent(long id, Instant sentAt) {
      sent.add(id);
    }

    @Override
    public void reschedule(long id, Instant nextAttemptAt, String error) {
      rescheduled.add(new Reschedule(id, nextAttemptAt, error));
    }

    @Override
    public void giveUp(long id, String error) {
      givenUp.add(id);
    }
  }
}
