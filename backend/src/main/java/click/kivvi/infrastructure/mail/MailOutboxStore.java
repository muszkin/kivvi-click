package click.kivvi.infrastructure.mail;

import click.kivvi.domain.mail.OutboundMail;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The {@code mail_outbox} table. Persistence here is {@code JdbcTemplate} with explicit SQL — there
 * is no JPA on the classpath and adding one would need its own decision record (CODE_REVIEW.md,
 * "Architecture").
 *
 * <p>The interesting statement is {@link #claimDue}. It marks rows {@code sending} and hands them
 * back in one round trip, so the decision "who owns this message" is made once, inside the
 * database, rather than in a read-then-write pair that two senders could both win. {@code FOR
 * UPDATE SKIP LOCKED} is what lets a second caller step over a row the first is already claiming
 * instead of blocking behind it — the same reflex as {@code EventDedupStore}'s atomic claim and
 * {@code WaitlistSubscriberStore}'s {@code ON CONFLICT}.
 *
 * <p>Claiming also pushes {@code next_attempt_at} into the future, which turns the claim into a
 * lease: a row left {@code sending} by a process that died mid-send becomes due again by itself
 * once the lease lapses, so there is no separate recovery job to forget about.
 *
 * <p>The status words are spelled out in the SQL rather than interpolated from constants. They are
 * column values, not code: reading the statement should not require holding four Java fields in
 * your head, and a text block spliced around a constant is harder to check against what Postgres
 * actually receives.
 */
@Component
public class MailOutboxStore {

  /** Long enough that no honest SMTP conversation outlives it, short enough to notice. */
  static final Duration CLAIM_LEASE = Duration.ofMinutes(10);

  private static final String INSERT_SQL =
      """
      INSERT INTO mail_outbox (
          dedup_key, recipient, subject, html_body, text_body,
          status, attempts, next_attempt_at, created_at)
      VALUES (?, ?, ?, ?, ?, 'pending', 0, ?, ?)
      ON CONFLICT (dedup_key) DO NOTHING
      """;

  private static final String CLAIM_SQL =
      """
      UPDATE mail_outbox
         SET status = 'sending', attempts = attempts + 1, next_attempt_at = ?
       WHERE id IN (
             SELECT id
               FROM mail_outbox
              WHERE status IN ('pending', 'sending')
                AND next_attempt_at <= ?
              ORDER BY next_attempt_at
                 FOR UPDATE SKIP LOCKED
              LIMIT ?)
      RETURNING id, recipient, subject, html_body, text_body, attempts
      """;

  private static final String MARK_SENT_SQL =
      "UPDATE mail_outbox SET status = 'sent', sent_at = ?, last_error = NULL WHERE id = ?";

  private static final String RESCHEDULE_SQL =
      "UPDATE mail_outbox SET status = 'pending', next_attempt_at = ?, last_error = ? WHERE id = ?";

  private static final String GIVE_UP_SQL =
      "UPDATE mail_outbox SET status = 'failed', last_error = ? WHERE id = ?";

  private static final String DELETE_SENT_SQL =
      "DELETE FROM mail_outbox WHERE status = 'sent' AND sent_at <= ?";

  private final JdbcTemplate jdbcTemplate;

  public MailOutboxStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** A message the sender has taken ownership of for the length of one claim lease. */
  public record ClaimedMail(
      long id, String recipient, String subject, String htmlBody, String textBody, int attempts) {}

  /**
   * @return {@code true} when the message was queued; {@code false} when its deduplication key was
   *     already in the table and nothing was written. A caller that supplied no key always queues.
   */
  public boolean enqueue(OutboundMail mail, Instant now) {
    Timestamp timestamp = Timestamp.from(now);
    int rowsAffected =
        jdbcTemplate.update(
            INSERT_SQL,
            mail.dedupKey(),
            mail.recipient(),
            mail.subject(),
            mail.htmlBody(),
            mail.textBody(),
            timestamp,
            timestamp);
    return rowsAffected == 1;
  }

  /**
   * Takes ownership of up to {@code batchSize} messages that are due, counting the attempt against
   * each of them up front. Counting on claim rather than on failure is deliberate: a send that
   * crashes the process mid-flight still burns an attempt, so a message that reliably kills the
   * sender cannot be retried forever.
   */
  public List<ClaimedMail> claimDue(Instant now, int batchSize) {
    return jdbcTemplate.query(
        CLAIM_SQL,
        (rs, rowNum) ->
            new ClaimedMail(
                rs.getLong("id"),
                rs.getString("recipient"),
                rs.getString("subject"),
                rs.getString("html_body"),
                rs.getString("text_body"),
                rs.getInt("attempts")),
        Timestamp.from(now.plus(CLAIM_LEASE)),
        Timestamp.from(now),
        batchSize);
  }

  public void markSent(long id, Instant sentAt) {
    jdbcTemplate.update(MARK_SENT_SQL, Timestamp.from(sentAt), id);
  }

  /** Puts a failed message back in the queue, due again once its backoff has elapsed. */
  public void reschedule(long id, Instant nextAttemptAt, String error) {
    jdbcTemplate.update(RESCHEDULE_SQL, Timestamp.from(nextAttemptAt), error, id);
  }

  /**
   * Parks a message whose attempt budget is spent. The row stays: a failed row is the evidence that
   * something never arrived, and deleting it would make the loss invisible.
   */
  public void giveUp(long id, String error) {
    jdbcTemplate.update(GIVE_UP_SQL, error, id);
  }

  /** Housekeeping hook: drops delivered messages once nobody needs the receipt any more. */
  public int deleteSentBefore(Instant cutoff) {
    return jdbcTemplate.update(DELETE_SENT_SQL, Timestamp.from(cutoff));
  }
}
