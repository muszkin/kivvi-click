package click.kivvi.infrastructure.waitlist;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.waitlist.SubscriberStatus;
import click.kivvi.domain.waitlist.WaitlistSignup;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * Stores waitlist signups in {@code waitlist_subscriber}. Persistence in this project is {@code
 * JdbcTemplate} with explicit SQL — there is no JPA on the classpath and introducing one would need
 * its own decision record (CODE_REVIEW.md, "Architecture").
 *
 * <p>{@link #save} is a single {@code INSERT ... ON CONFLICT (email) DO NOTHING} rather than a
 * "select, then insert if absent" pair, for the same reason {@code EventDedupStore#claim} is: two
 * visitors submitting the same address at the same moment would both pass the check and one would
 * then hit the unique index as an exception. Here the database decides once, atomically, and a
 * duplicate is reported as an ordinary {@code false} — a repeat signup is a normal thing for a
 * visitor to do, not an error to surface.
 */
@Component
public class WaitlistSubscriberStore {

  private static final String INSERT_SQL =
      """
      INSERT INTO waitlist_subscriber (
          email, status, source, locale, signed_up_at,
          consent_at, consent_ip, consent_user_agent, consent_text)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT (email) DO NOTHING
      """;

  /** Mirrors {@code waitlist_subscriber.confirmed_user_agent}'s own width. */
  private static final int MAX_USER_AGENT_LENGTH = 512;

  private static final String SUBSCRIBER_COLUMNS =
      "id, email, locale, status, confirmation_token_expires_at";

  private static final String FIND_BY_EMAIL_SQL =
      "SELECT " + SUBSCRIBER_COLUMNS + " FROM waitlist_subscriber WHERE email = ?";

  private static final String FIND_BY_CONFIRMATION_TOKEN_SQL =
      "SELECT "
          + SUBSCRIBER_COLUMNS
          + " FROM waitlist_subscriber WHERE confirmation_token_hash = ?";

  private static final String ISSUE_TOKENS_SQL =
      """
      UPDATE waitlist_subscriber
         SET confirmation_token_hash = ?,
             confirmation_token_expires_at = ?,
             unsubscribe_token_hash = ?
       WHERE id = ?
      """;

  private static final String CONFIRM_SQL =
      """
      UPDATE waitlist_subscriber
         SET status = ?, confirmed_at = ?, confirmed_ip = ?, confirmed_user_agent = ?
       WHERE confirmation_token_hash = ?
         AND confirmed_at IS NULL
         AND unsubscribed_at IS NULL
         AND confirmation_token_expires_at > ?
      """;

  private static final String REOPEN_SQL =
      """
      UPDATE waitlist_subscriber
         SET status = ?, unsubscribed_at = NULL,
             locale = ?, signed_up_at = ?,
             consent_at = ?, consent_ip = ?, consent_user_agent = ?, consent_text = ?,
             confirmed_at = NULL, confirmed_ip = NULL, confirmed_user_agent = NULL
       WHERE id = ?
      """;

  private static final String UNSUBSCRIBE_SQL =
      """
      UPDATE waitlist_subscriber
         SET status = ?, unsubscribed_at = COALESCE(unsubscribed_at, ?)
       WHERE unsubscribe_token_hash = ?
      """;

  private static final RowMapper<Subscriber> SUBSCRIBER_MAPPER =
      WaitlistSubscriberStore::mapSubscriber;

  private final JdbcTemplate jdbcTemplate;

  public WaitlistSubscriberStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * @return {@code true} when the address was not on the list and is now recorded; {@code false}
   *     when it was already there and nothing changed.
   */
  public boolean save(WaitlistSignup signup) {
    int rowsAffected =
        jdbcTemplate.update(
            INSERT_SQL,
            signup.email(),
            signup.status().value(),
            signup.source().value(),
            signup.locale().code(),
            Timestamp.from(signup.signedUpAt()),
            Timestamp.from(signup.consent().at()),
            signup.consent().ip(),
            signup.consent().userAgent(),
            signup.consent().text());
    return rowsAffected == 1;
  }

  /** As much of a subscriber as the confirmation flow needs to decide anything. */
  public record Subscriber(
      long id,
      String email,
      SupportedLocale locale,
      SubscriberStatus status,
      Instant confirmationTokenExpiresAt) {}

  public Optional<Subscriber> findByEmail(String email) {
    return findOne(FIND_BY_EMAIL_SQL, email);
  }

  public Optional<Subscriber> findByConfirmationTokenHash(String tokenHash) {
    return findOne(FIND_BY_CONFIRMATION_TOKEN_SQL, tokenHash);
  }

  /**
   * Records a freshly issued pair of tokens against a subscriber, replacing whatever was there.
   *
   * <p>Replacing rather than keeping is the point of a resend: the old confirmation link stops
   * working the moment a new one is sent, so a link forwarded to somebody else cannot be redeemed
   * after the owner has asked for a fresh one. The unsubscribe hash is written every time too, but
   * it is derived from the subscriber's id ({@link click.kivvi.domain.waitlist.UnsubscribeToken}),
   * so writing it again writes the same value.
   */
  public void issueTokens(
      long subscriberId, String confirmationHash, Instant expiresAt, String unsubscribeHash) {
    jdbcTemplate.update(
        ISSUE_TOKENS_SQL,
        confirmationHash,
        Timestamp.from(expiresAt),
        unsubscribeHash,
        subscriberId);
  }

  /**
   * Confirms in one guarded statement: only a row that still holds this exact token hash, has never
   * been confirmed, and has not run past its deadline is touched. Two browser tabs opening the same
   * link at once therefore produce one confirmation and one "already confirmed", not two writes
   * racing over the same consent record.
   *
   * <p>The hash is deliberately left in place afterwards. Clearing it would make a second click
   * indistinguishable from a link that never existed, and "we have never heard of this link" is the
   * wrong thing to tell someone who has just successfully confirmed; the token is single-use
   * because of the {@code confirmed_at IS NULL} guard, not because the row forgets it.
   *
   * <p>{@code unsubscribed_at IS NULL} matters more than it looks. Both links travel in the same
   * message, and a corporate link scanner visits every URL in a message in whatever order it likes
   * — so "unsubscribe, then confirm" is an order that will happen. Without this clause that
   * sequence would put a row back on the list after an explicit opt-out, with {@code
   * unsubscribed_at} still set: a row contradicting itself. Coming back is possible, but only
   * through the front door — signing up again, with fresh consent ({@link #reopen}).
   *
   * @return {@code true} when this call is the one that confirmed the address
   */
  public boolean confirm(String tokenHash, Instant now, String ip, String userAgent) {
    return jdbcTemplate.update(
            CONFIRM_SQL,
            SubscriberStatus.CONFIRMED.value(),
            Timestamp.from(now),
            ip,
            truncate(userAgent),
            tokenHash,
            Timestamp.from(now))
        == 1;
  }

  /**
   * Idempotent by construction: {@code COALESCE} keeps the first unsubscribe timestamp, so clicking
   * the link again is a no-op that still answers "you are unsubscribed" rather than rewriting when
   * it happened.
   *
   * @return {@code true} when the token belongs to somebody
   */
  public boolean unsubscribe(String tokenHash, Instant now) {
    return jdbcTemplate.update(
            UNSUBSCRIBE_SQL, SubscriberStatus.UNSUBSCRIBED.value(), Timestamp.from(now), tokenHash)
        == 1;
  }

  /**
   * Puts an unsubscribed address back on the list as unconfirmed, with the consent proof of the
   * submission that asked for it.
   *
   * <p>Not a token-level operation, and deliberately not reachable from a link: the only way back
   * is to fill the form in again, which is what produces the fresh consent record this writes. The
   * old confirmation stamps are cleared with it — somebody who left and came back has to prove the
   * mailbox again, and a {@code confirmed_at} from before the opt-out would be attesting to a
   * consent that was withdrawn.
   */
  public void reopen(long subscriberId, WaitlistSignup signup) {
    jdbcTemplate.update(
        REOPEN_SQL,
        SubscriberStatus.PENDING.value(),
        signup.locale().code(),
        Timestamp.from(signup.signedUpAt()),
        Timestamp.from(signup.consent().at()),
        signup.consent().ip(),
        signup.consent().userAgent(),
        signup.consent().text(),
        subscriberId);
  }

  private Optional<Subscriber> findOne(String sql, String argument) {
    return jdbcTemplate.query(sql, SUBSCRIBER_MAPPER, argument).stream().findFirst();
  }

  /** Mirrors {@code waitlist_subscriber.confirmed_user_agent}'s width. */
  private static String truncate(String userAgent) {
    if (userAgent == null || userAgent.length() <= MAX_USER_AGENT_LENGTH) {
      return userAgent;
    }
    return userAgent.substring(0, MAX_USER_AGENT_LENGTH);
  }

  private static Subscriber mapSubscriber(ResultSet rs, int rowNum) throws SQLException {
    Timestamp expiresAt = rs.getTimestamp("confirmation_token_expires_at");
    return new Subscriber(
        rs.getLong("id"),
        rs.getString("email"),
        SupportedLocale.fromCode(rs.getString("locale")).orElse(SupportedLocale.DEFAULT),
        statusOf(rs.getString("status")),
        expiresAt == null ? null : expiresAt.toInstant());
  }

  /**
   * An unrecognized status is a bug in whatever wrote it, and guessing would quietly hand a
   * confirmed subscriber back as pending — which is how somebody ends up being mailed twice.
   */
  private static SubscriberStatus statusOf(String stored) {
    for (SubscriberStatus status : SubscriberStatus.values()) {
      if (status.value().equals(stored)) {
        return status;
      }
    }
    throw new IllegalStateException("Unknown waitlist_subscriber.status: " + stored);
  }
}
