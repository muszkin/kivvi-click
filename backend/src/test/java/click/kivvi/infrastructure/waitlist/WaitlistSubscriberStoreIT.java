package click.kivvi.infrastructure.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.waitlist.SignupSource;
import click.kivvi.domain.waitlist.SubscriberStatus;
import click.kivvi.domain.waitlist.WaitlistSignup;
import java.time.Instant;
import java.util.Map;
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
 * {@link WaitlistSubscriberStore} against a real Postgres {@code waitlist_subscriber} table —
 * mirrors {@code EventDedupStoreIT}: the idempotent-insert semantics this store is built on are a
 * property of Postgres' {@code ON CONFLICT}, so they can only be proven against the real database,
 * never against a stub.
 */
@Testcontainers
@SpringBootTest
class WaitlistSubscriberStoreIT {

  private static final String CONSENT_TEXT = "Zgadzam się na otrzymanie powiadomienia o starcie.";

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private WaitlistSubscriberStore store;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("a new address is recorded as pending, sourced from the landing page")
  void aNewAddressIsRecordedAsPending() {
    String email = uniqueEmail("fresh");

    boolean recorded = store.save(signup(email));

    assertThat(recorded).isTrue();
    Map<String, Object> row = rowFor(email);
    assertThat(row.get("status")).isEqualTo(SubscriberStatus.PENDING.value());
    assertThat(row.get("source")).isEqualTo(SignupSource.LANDING.value());
    assertThat(row.get("locale")).isEqualTo("pl");
  }

  @Test
  @DisplayName("the consent proof is stored verbatim: time, IP, user agent and the clause itself")
  void theConsentProofIsStored() {
    String email = uniqueEmail("consent");

    store.save(signup(email));

    Map<String, Object> row = rowFor(email);
    assertThat(row.get("consent_ip")).isEqualTo("203.0.113.7");
    assertThat(row.get("consent_user_agent")).isEqualTo("Mozilla/5.0 (test)");
    assertThat(row.get("consent_text")).isEqualTo(CONSENT_TEXT);
    assertThat(row.get("consent_at")).isNotNull();
  }

  @Test
  @DisplayName("signing up twice with the same address is a no-op, not a duplicate and not a crash")
  void aDuplicateAddressIsIgnored() {
    String email = uniqueEmail("duplicate");

    assertThat(store.save(signup(email))).isTrue();
    assertThat(store.save(signup(email))).isFalse();

    Integer rows =
        jdbcTemplate.queryForObject(
            "select count(*) from waitlist_subscriber where email = ?", Integer.class, email);
    assertThat(rows).isEqualTo(1);
  }

  @Test
  @DisplayName("the address is normalized before it reaches the unique index")
  void theAddressIsNormalizedBeforeItIsStored() {
    String email = uniqueEmail("case");

    store.save(signup(email));
    boolean secondAttempt =
        store.save(signup("  " + email.toUpperCase(java.util.Locale.ROOT) + " "));

    assertThat(secondAttempt).isFalse();
  }

  private static WaitlistSignup signup(String rawEmail) {
    return WaitlistSignup.landingSignup(
        rawEmail,
        SupportedLocale.PL,
        Instant.now(),
        "203.0.113.7",
        "Mozilla/5.0 (test)",
        CONSENT_TEXT);
  }

  private Map<String, Object> rowFor(String email) {
    return jdbcTemplate.queryForMap("select * from waitlist_subscriber where email = ?", email);
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + System.nanoTime() + "@sklep.pl";
  }
}
