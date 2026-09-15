package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.waitlist.SignupSource;
import click.kivvi.domain.waitlist.SubscriberStatus;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The whole waitlist path over real HTTP against a real database — the form POST, the row it leaves
 * behind, the repeat that leaves nothing, and the allowance running out. {@code
 * WaitlistControllerTest} slices the web layer with stand-ins behind it; this is the one that
 * proves the pieces fit together, including that {@code V2__waitlist.sql} applied.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class WaitlistApiIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  // Every test in this class submits from the same loopback address, so without this they would
  // share one per-IP bucket and the allowance test's result would depend on execution order.
  @BeforeEach
  void clearTheAllowanceCounters() {
    jdbcTemplate.update("delete from waitlist_throttle");
  }

  @Test
  @DisplayName("a signup lands in waitlist_subscriber as pending, sourced from the landing page")
  void aSignupIsStoredAsPending() {
    String email = uniqueEmail("stored");

    ResponseEntity<String> response = submit("pl", email, true, "");

    assertThat(response.getStatusCode().value()).isEqualTo(302);
    assertThat(response.getHeaders().getLocation()).hasToString("/pl?waitlist=ok");

    Map<String, Object> row = rowFor(email);
    assertThat(row.get("status")).isEqualTo(SubscriberStatus.PENDING.value());
    assertThat(row.get("source")).isEqualTo(SignupSource.LANDING.value());
    assertThat(row.get("locale")).isEqualTo("pl");
    assertThat(row.get("consent_text").toString()).contains("Zgadzam się");
    assertThat(row.get("consent_ip")).isNotNull();
    assertThat(row.get("confirmed_at")).isNull();
    assertThat(row.get("confirmation_token_hash")).isNull();
  }

  @Test
  @DisplayName("the address is stored normalized, whatever case it was typed in")
  void theAddressIsStoredNormalized() {
    String email = uniqueEmail("case");

    submit("pl", "  " + email.toUpperCase(java.util.Locale.ROOT) + "  ", true, "");

    assertThat(countFor(email)).isEqualTo(1);
  }

  @Test
  @DisplayName("signing up twice answers the same way and still leaves exactly one row")
  void aRepeatSignupIsIdempotent() {
    String email = uniqueEmail("repeat");

    ResponseEntity<String> first = submit("pl", email, true, "");
    ResponseEntity<String> second = submit("pl", email, true, "");

    assertThat(first.getStatusCode().value()).isEqualTo(302);
    assertThat(second.getStatusCode().value()).isEqualTo(302);
    assertThat(second.getHeaders().getLocation()).hasToString("/pl?waitlist=ok");
    assertThat(countFor(email)).isEqualTo(1);
  }

  @Test
  @DisplayName("an unticked consent box stores nothing and comes back with the message")
  void missingConsentStoresNothing() {
    String email = uniqueEmail("no-consent");

    ResponseEntity<String> response = submit("pl", email, false, "");

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).contains("data-waitlist-error=");
    assertThat(response.getBody()).contains("data-waitlist-consent=\"false\"");
    assertThat(countFor(email)).isZero();
  }

  @Test
  @DisplayName("a filled honeypot stores nothing and is answered exactly like a success")
  void aFilledHoneypotStoresNothing() {
    String email = uniqueEmail("honeypot");

    ResponseEntity<String> response = submit("pl", email, true, "http://spam.example");

    assertThat(response.getStatusCode().value()).isEqualTo(302);
    assertThat(countFor(email)).isZero();
  }

  @Test
  @DisplayName(
      "the fourth attempt on one address in an hour is refused with 429 and stores nothing")
  void thePerAddressAllowanceRunsOut() {
    String email = uniqueEmail("throttled");

    // Three attempts fit the per-address allowance; all three are refused on consent, so none
    // of them stores anything and the allowance is what the fourth runs into.
    for (int attempt = 0; attempt < 3; attempt++) {
      assertThat(submit("pl", email, false, "").getStatusCode().value()).isEqualTo(200);
    }

    ResponseEntity<String> refused = submit("pl", email, true, "");

    assertThat(refused.getStatusCode().value()).isEqualTo(429);
    assertThat(refused.getBody()).contains("data-waitlist-error=");
    assertThat(countFor(email)).isZero();
  }

  private ResponseEntity<String> submit(
      String locale, String email, boolean consent, String honeypot) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("email", email);
    if (consent) {
      form.add("consent", "1");
    }
    form.add("website", honeypot);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    // The autowired client follows redirects by default; the 302 itself is the contract here.
    return restTemplate
        .withRedirects(HttpRedirects.DONT_FOLLOW)
        .postForEntity("/" + locale + "/waitlist", new HttpEntity<>(form, headers), String.class);
  }

  private Map<String, Object> rowFor(String email) {
    return jdbcTemplate.queryForMap("select * from waitlist_subscriber where email = ?", email);
  }

  private Integer countFor(String email) {
    return jdbcTemplate.queryForObject(
        "select count(*) from waitlist_subscriber where email = ?", Integer.class, email);
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + System.nanoTime() + "@sklep.pl";
  }
}
