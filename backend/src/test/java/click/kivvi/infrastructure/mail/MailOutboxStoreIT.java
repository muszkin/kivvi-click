package click.kivvi.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.mail.OutboundMail;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * {@link MailOutboxStore} against a real Postgres {@code mail_outbox} table. Every property this
 * store is built on — {@code ON CONFLICT} deduplication, an atomic claim, {@code SKIP LOCKED}
 * keeping two senders off one row — is a property of the database, so a stub could only ever prove
 * that the Java around it compiles. Mirrors {@code EventDedupStoreIT} and {@code
 * WaitlistSubscriberStoreIT}.
 */
@Testcontainers
@SpringBootTest
class MailOutboxStoreIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private MailOutboxStore store;
  @Autowired private JdbcTemplate jdbcTemplate;

  // Every test claims by "what is due now", so a row another test left behind would be handed to
  // this one as well. One table, one sender: the table starts empty.
  @BeforeEach
  void emptyTheOutbox() {
    jdbcTemplate.update("delete from mail_outbox");
  }

  @Test
  @DisplayName("a queued message lands as pending, due immediately, with nothing tried yet")
  void aQueuedMessageIsPendingAndDue() {
    Instant now = Instant.now();

    boolean queued = store.enqueue(mail("first@sklep.pl", "key-first"), now);

    assertThat(queued).isTrue();
    Map<String, Object> row = onlyRow();
    assertThat(row.get("status")).isEqualTo("pending");
    assertThat(row.get("attempts")).isEqualTo(0);
    assertThat(row.get("recipient")).isEqualTo("first@sklep.pl");
    assertThat(row.get("text_body")).isEqualTo("Potwierdź adres: https://kivvi.click/pl/x");
    assertThat(row.get("sent_at")).isNull();
  }

  @Test
  @DisplayName("the same deduplication key queues once, however many times it is offered")
  void aRepeatedDeduplicationKeyQueuesOnce() {
    Instant now = Instant.now();

    assertThat(store.enqueue(mail("dup@sklep.pl", "key-dup"), now)).isTrue();
    assertThat(store.enqueue(mail("dup@sklep.pl", "key-dup"), now)).isFalse();

    assertThat(countRows()).isEqualTo(1);
  }

  @Test
  @DisplayName("a message with no deduplication key queues every time it is offered")
  void aKeylessMessageAlwaysQueues() {
    Instant now = Instant.now();
    OutboundMail resend =
        new OutboundMail("resend@sklep.pl", "Potwierdź adres", "<p>hi</p>", "hi", null);

    assertThat(store.enqueue(resend, now)).isTrue();
    assertThat(store.enqueue(resend, now)).isTrue();

    assertThat(countRows()).isEqualTo(2);
  }

  @Test
  @DisplayName("claiming hands back what is due, marks it sending and counts the attempt")
  void claimingMarksTheRowSendingAndCountsTheAttempt() {
    Instant now = Instant.now();
    store.enqueue(mail("due@sklep.pl", "key-due"), now);

    List<MailOutboxStore.ClaimedMail> claimed = store.claimDue(now, 10);

    assertThat(claimed).hasSize(1);
    assertThat(claimed.getFirst().recipient()).isEqualTo("due@sklep.pl");
    assertThat(claimed.getFirst().attempts()).isEqualTo(1);
    assertThat(claimed.getFirst().htmlBody()).contains("<p>");
    assertThat(onlyRow().get("status")).isEqualTo("sending");
  }

  @Test
  @DisplayName("a claimed message is not handed out again until its lease lapses")
  void aClaimedMessageIsLeasedNotJustFlagged() {
    Instant now = Instant.now();
    store.enqueue(mail("leased@sklep.pl", "key-leased"), now);
    store.claimDue(now, 10);

    assertThat(store.claimDue(now, 10)).isEmpty();
    assertThat(store.claimDue(now.plus(MailOutboxStore.CLAIM_LEASE).plusSeconds(1), 10)).hasSize(1);
  }

  @Test
  @DisplayName("a message whose backoff has not elapsed is not due yet")
  void aRescheduledMessageWaitsForItsBackoff() {
    Instant now = Instant.now();
    store.enqueue(mail("backoff@sklep.pl", "key-backoff"), now);
    long id = store.claimDue(now, 10).getFirst().id();

    store.reschedule(id, now.plus(Duration.ofMinutes(5)), "550 mailbox unavailable");

    assertThat(store.claimDue(now.plusSeconds(60), 10)).isEmpty();
    assertThat(store.claimDue(now.plus(Duration.ofMinutes(6)), 10)).hasSize(1);
    assertThat(onlyRow().get("last_error")).isEqualTo("550 mailbox unavailable");
  }

  @Test
  @DisplayName("a sent message is stamped and never claimed again")
  void aSentMessageIsStampedAndDone() {
    Instant now = Instant.now();
    store.enqueue(mail("sent@sklep.pl", "key-sent"), now);
    long id = store.claimDue(now, 10).getFirst().id();

    store.markSent(id, now);

    Map<String, Object> row = onlyRow();
    assertThat(row.get("status")).isEqualTo("sent");
    assertThat(row.get("sent_at")).isNotNull();
    assertThat(store.claimDue(now.plus(Duration.ofDays(1)), 10)).isEmpty();
  }

  @Test
  @DisplayName("a message that ran out of attempts stays in the table as failed, never retried")
  void aGivenUpMessageStaysAsEvidence() {
    Instant now = Instant.now();
    store.enqueue(mail("failed@sklep.pl", "key-failed"), now);
    long id = store.claimDue(now, 10).getFirst().id();

    store.giveUp(id, "connect timed out");

    assertThat(onlyRow().get("status")).isEqualTo("failed");
    assertThat(onlyRow().get("last_error")).isEqualTo("connect timed out");
    assertThat(store.claimDue(now.plus(Duration.ofDays(30)), 10)).isEmpty();
  }

  @Test
  @DisplayName("two senders claiming at the same moment never receive the same message")
  void twoSendersNeverClaimTheSameMessage() throws Exception {
    Instant now = Instant.now();
    for (int index = 0; index < 8; index++) {
      store.enqueue(mail("race-" + index + "@sklep.pl", "key-race-" + index), now);
    }

    Callable<List<Long>> claimAll =
        () -> store.claimDue(now, 8).stream().map(MailOutboxStore.ClaimedMail::id).toList();
    List<Long> first;
    List<Long> second;
    try (ExecutorService senders = Executors.newFixedThreadPool(2)) {
      Future<List<Long>> left = senders.submit(claimAll);
      Future<List<Long>> right = senders.submit(claimAll);
      first = left.get();
      second = right.get();
    }

    // SKIP LOCKED is what makes this true: the loser of a row steps over it instead of waiting,
    // so between them the two senders see all eight messages and neither sees one twice. The
    // split between them is deliberately not asserted — how the two threads interleave is the
    // scheduler's business, and pinning it would make a correct result flaky.
    assertThat(Stream.concat(first.stream(), second.stream()).toList())
        .hasSize(8)
        .doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("the cleanup sweep drops delivered messages and keeps failed ones")
  void theSweepDropsDeliveredMessagesOnly() {
    Instant longAgo = Instant.now().minus(Duration.ofDays(40));
    store.enqueue(mail("old@sklep.pl", "key-old"), longAgo);
    store.enqueue(mail("dead@sklep.pl", "key-dead"), longAgo);
    List<MailOutboxStore.ClaimedMail> claimed = store.claimDue(longAgo, 10);
    store.markSent(claimed.get(0).id(), longAgo);
    store.giveUp(claimed.get(1).id(), "connect timed out");

    int removed = store.deleteSentBefore(Instant.now().minus(Duration.ofDays(30)));

    assertThat(removed).isEqualTo(1);
    assertThat(onlyRow().get("status")).isEqualTo("failed");
  }

  private static OutboundMail mail(String recipient, String dedupKey) {
    return new OutboundMail(
        recipient,
        "Potwierdź adres",
        "<p>Potwierdź adres</p>",
        "Potwierdź adres: https://kivvi.click/pl/x",
        dedupKey);
  }

  private Map<String, Object> onlyRow() {
    return jdbcTemplate.queryForMap("select * from mail_outbox");
  }

  private Integer countRows() {
    return jdbcTemplate.queryForObject("select count(*) from mail_outbox", Integer.class);
  }
}
