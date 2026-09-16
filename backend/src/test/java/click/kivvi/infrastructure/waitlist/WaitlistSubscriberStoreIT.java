package click.kivvi.infrastructure.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.Sha256;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.waitlist.SignupSource;
import click.kivvi.domain.waitlist.SubscriberStatus;
import click.kivvi.domain.waitlist.WaitlistSignup;
import java.time.Duration;
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

  // Rows outlive the test that wrote them, and both token columns carry a partial unique index, so
  // every test needs its own pair of hashes. Deriving them from the test's own unique address is
  // the cheapest way to guarantee that without a teardown step.
  private static String confirmationHash(String email) {
    return Sha256.hex(email + ":confirm");
  }

  private static String unsubscribeHash(String email) {
    return Sha256.hex(email + ":unsubscribe");
  }

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

  @Test
  @DisplayName("PIO-71 a live token confirms exactly once and records who did it, from where")
  void aLiveTokenConfirmsOnce() {
    String email = uniqueEmail("confirm");
    store.save(signup(email));
    long id = store.findByEmail(email).orElseThrow().id();
    Instant now = Instant.now();
    store.issueTokens(
        id, confirmationHash(email), now.plus(Duration.ofDays(7)), unsubscribeHash(email));

    assertThat(store.confirm(confirmationHash(email), now, "203.0.113.9", "Mozilla/5.0 (confirm)"))
        .isTrue();
    assertThat(
            store.confirm(
                confirmationHash(email), now.plusSeconds(1), "203.0.113.9", "Mozilla/5.0"))
        .isFalse();

    Map<String, Object> row = rowFor(email);
    assertThat(row.get("status")).isEqualTo(SubscriberStatus.CONFIRMED.value());
    assertThat(row.get("confirmed_at")).isNotNull();
    assertThat(row.get("confirmed_ip")).isEqualTo("203.0.113.9");
    assertThat(row.get("confirmed_user_agent")).isEqualTo("Mozilla/5.0 (confirm)");
  }

  @Test
  @DisplayName("PIO-71 a token past its deadline confirms nothing and leaves the row pending")
  void anExpiredTokenConfirmsNothing() {
    String email = uniqueEmail("expired");
    store.save(signup(email));
    long id = store.findByEmail(email).orElseThrow().id();
    Instant issuedAt = Instant.now().minus(Duration.ofDays(8));
    store.issueTokens(
        id, confirmationHash(email), issuedAt.plus(Duration.ofDays(7)), unsubscribeHash(email));

    assertThat(store.confirm(confirmationHash(email), Instant.now(), "203.0.113.9", "UA"))
        .isFalse();

    assertThat(rowFor(email).get("status")).isEqualTo(SubscriberStatus.PENDING.value());
    // Still findable by the very token that failed — which is what lets the page offer a new link
    // instead of claiming it has never heard of this one.
    assertThat(store.findByConfirmationTokenHash(confirmationHash(email))).isPresent();
  }

  @Test
  @DisplayName(
      "PIO-71 issuing again replaces the confirmation token, so the old link stops working")
  void reissuingInvalidatesTheOldToken() {
    String email = uniqueEmail("reissue");
    store.save(signup(email));
    long id = store.findByEmail(email).orElseThrow().id();
    Instant now = Instant.now();
    store.issueTokens(
        id, confirmationHash(email), now.plus(Duration.ofDays(7)), unsubscribeHash(email));

    String replacement = Sha256.hex(email + ":second");
    store.issueTokens(id, replacement, now.plus(Duration.ofDays(7)), unsubscribeHash(email));

    assertThat(store.findByConfirmationTokenHash(confirmationHash(email))).isEmpty();
    assertThat(store.confirm(replacement, now, "203.0.113.9", "UA")).isTrue();
  }

  @Test
  @DisplayName("PIO-71 unsubscribing is idempotent and keeps the moment it first happened")
  void unsubscribingIsIdempotent() {
    String email = uniqueEmail("unsub");
    store.save(signup(email));
    long id = store.findByEmail(email).orElseThrow().id();
    Instant now = Instant.now();
    store.issueTokens(
        id, confirmationHash(email), now.plus(Duration.ofDays(7)), unsubscribeHash(email));

    assertThat(store.unsubscribe(unsubscribeHash(email), now)).isTrue();
    Object firstTime = rowFor(email).get("unsubscribed_at");
    assertThat(store.unsubscribe(unsubscribeHash(email), now.plusSeconds(3600))).isTrue();

    assertThat(rowFor(email).get("status")).isEqualTo(SubscriberStatus.UNSUBSCRIBED.value());
    assertThat(rowFor(email).get("unsubscribed_at")).isEqualTo(firstTime);
  }

  @Test
  @DisplayName("PIO-71 a live token cannot confirm an address that has unsubscribed")
  void anUnsubscribedAddressCannotBeConfirmed() {
    String email = uniqueEmail("withdrawn");
    store.save(signup(email));
    long id = store.findByEmail(email).orElseThrow().id();
    Instant now = Instant.now();
    store.issueTokens(
        id, confirmationHash(email), now.plus(Duration.ofDays(7)), unsubscribeHash(email));
    store.unsubscribe(unsubscribeHash(email), now);

    assertThat(store.confirm(confirmationHash(email), now, "203.0.113.9", "Scanner/1.0")).isFalse();

    Map<String, Object> row = rowFor(email);
    assertThat(row.get("status")).isEqualTo(SubscriberStatus.UNSUBSCRIBED.value());
    assertThat(row.get("confirmed_at")).isNull();
  }

  @Test
  @DisplayName("PIO-71 signing up again after unsubscribing reopens the row with fresh consent")
  void reopeningClearsTheWithdrawalAndTheOldConfirmation() {
    String email = uniqueEmail("rejoin");
    store.save(signup(email));
    long id = store.findByEmail(email).orElseThrow().id();
    Instant now = Instant.now();
    store.issueTokens(
        id, confirmationHash(email), now.plus(Duration.ofDays(7)), unsubscribeHash(email));
    store.confirm(confirmationHash(email), now, "203.0.113.9", "Mozilla/5.0");
    store.unsubscribe(unsubscribeHash(email), now);

    store.reopen(
        id,
        WaitlistSignup.landingSignup(
            email,
            SupportedLocale.EN,
            now,
            "198.51.100.4",
            "Mozilla/5.0 (rejoin)",
            "I agree to be told when it launches."));

    Map<String, Object> row = rowFor(email);
    assertThat(row.get("status")).isEqualTo(SubscriberStatus.PENDING.value());
    assertThat(row.get("unsubscribed_at")).isNull();
    // Cleared on purpose: a confirmed_at from before the opt-out would attest to a consent that
    // was withdrawn. Coming back costs a fresh confirmation.
    assertThat(row.get("confirmed_at")).isNull();
    assertThat(row.get("confirmed_ip")).isNull();
    assertThat(row.get("consent_ip")).isEqualTo("198.51.100.4");
    assertThat(row.get("locale")).isEqualTo("en");
  }

  @Test
  @DisplayName("PIO-71 a token nobody issued belongs to nobody")
  void anUnknownTokenBelongsToNobody() {
    String nobodys = Sha256.hex("nobody-" + System.nanoTime());

    assertThat(store.findByConfirmationTokenHash(nobodys)).isEmpty();
    assertThat(store.confirm(nobodys, Instant.now(), "203.0.113.9", "UA")).isFalse();
    assertThat(store.unsubscribe(nobodys, Instant.now())).isFalse();
  }

  @Test
  @DisplayName("PIO-71 an over-long user agent is truncated rather than turning into a 500")
  void anOverLongUserAgentIsTruncated() {
    String email = uniqueEmail("long-ua");
    store.save(signup(email));
    long id = store.findByEmail(email).orElseThrow().id();
    Instant now = Instant.now();
    store.issueTokens(
        id, confirmationHash(email), now.plus(Duration.ofDays(7)), unsubscribeHash(email));

    store.confirm(confirmationHash(email), now, "203.0.113.9", "U".repeat(900));

    assertThat(rowFor(email).get("confirmed_user_agent").toString()).hasSize(512);
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
