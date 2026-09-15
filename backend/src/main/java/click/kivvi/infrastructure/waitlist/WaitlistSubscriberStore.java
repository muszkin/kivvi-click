package click.kivvi.infrastructure.waitlist;

import click.kivvi.domain.waitlist.WaitlistSignup;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
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
}
