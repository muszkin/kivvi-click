package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.waitlist.SubscriberStatus;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * The whole double opt-in loop over real HTTP against a real database: sign up, read the link out
 * of the queued message the way a subscriber would read it out of their inbox, follow it, and come
 * back out the other side through the unsubscribe link.
 *
 * <p>Nothing here reaches into the service layer. The only way a token gets into this test is the
 * message that was composed for it, which is the same guarantee the production path gives: the
 * plaintext exists in the mail and nowhere else — the database holds a digest.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class WaitlistConfirmationApiIT {

  private static final Pattern CONFIRM_LINK =
      Pattern.compile("/(?<locale>pl|en)/waitlist/confirm/(?<token>[0-9a-f]{64})");
  private static final Pattern UNSUBSCRIBE_LINK =
      Pattern.compile("/(?<locale>pl|en)/waitlist/unsubscribe/(?<token>[0-9a-f]{64})");

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  // Every test submits from the same loopback address, so without this they would share one per-IP
  // bucket and the results would depend on execution order.
  @BeforeEach
  void clearTheAllowanceCounters() {
    jdbcTemplate.update("delete from waitlist_throttle");
  }

  @Test
  @DisplayName("signing up queues a confirmation message carrying a live link")
  void signingUpQueuesAConfirmationMail() {
    String email = uniqueEmail("queued");

    signUp(email);

    Map<String, Object> mail = mailFor(email);
    assertThat(mail.get("status")).isEqualTo("pending");
    assertThat(mail.get("subject").toString()).contains("Potwierdź");
    assertThat(mail.get("text_body").toString()).isNotBlank();
    assertThat(mail.get("html_body").toString()).doesNotContain("oklch(");
    assertThat(confirmTokenFrom(mail)).hasSize(64);

    Map<String, Object> subscriber = subscriberRow(email);
    assertThat(subscriber.get("status")).isEqualTo(SubscriberStatus.PENDING.value());
    assertThat(subscriber.get("confirmation_token_hash")).isNotNull();
    assertThat(subscriber.get("confirmation_token_expires_at")).isNotNull();
    assertThat(subscriber.get("unsubscribe_token_hash")).isNotNull();
    assertThat(subscriber.get("confirmed_at")).isNull();
  }

  @Test
  @DisplayName("following the link confirms the address and records the time, IP and user agent")
  void followingTheLinkConfirmsAndRecordsTheProof() {
    String email = uniqueEmail("confirm");
    signUp(email);
    String token = confirmTokenFrom(mailFor(email));

    ResponseEntity<String> page = follow("/pl/waitlist/confirm/" + token);

    assertThat(page.getStatusCode().value()).isEqualTo(200);
    assertThat(page.getHeaders().getContentType()).hasToString("text/html;charset=UTF-8");
    assertThat(page.getBody()).contains("data-waitlist-confirm=\"ok\"");

    Map<String, Object> subscriber = subscriberRow(email);
    assertThat(subscriber.get("status")).isEqualTo(SubscriberStatus.CONFIRMED.value());
    assertThat(subscriber.get("confirmed_at")).isNotNull();
    assertThat(subscriber.get("confirmed_ip")).isNotNull();
    assertThat(subscriber.get("confirmed_user_agent")).isNotNull();
  }

  @Test
  @DisplayName("following the same link again says the address is already confirmed")
  void followingTheLinkTwiceIsReassuring() {
    String email = uniqueEmail("twice");
    signUp(email);
    String token = confirmTokenFrom(mailFor(email));
    follow("/pl/waitlist/confirm/" + token);

    ResponseEntity<String> second = follow("/pl/waitlist/confirm/" + token);

    assertThat(second.getBody()).contains("data-waitlist-confirm=\"already\"");
  }

  @Test
  @DisplayName("a token nobody issued is answered with the unknown state, not a 500")
  void anUnknownTokenIsAnswered() {
    ResponseEntity<String> page = follow("/pl/waitlist/confirm/" + "f".repeat(64));

    assertThat(page.getStatusCode().value()).isEqualTo(200);
    assertThat(page.getBody()).contains("data-waitlist-confirm=\"unknown\"");
  }

  @Test
  @DisplayName("a link that is not a token shape is a 404, exactly like any other unknown path")
  void aMalformedLinkIs404() {
    assertThat(follow("/pl/waitlist/confirm/nonsense").getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @DisplayName("signing up again with an unconfirmed address queues a new link and adds no row")
  void aRepeatSignupResendsWithoutASecondRow() {
    String email = uniqueEmail("repeat");
    signUp(email);
    String firstToken = confirmTokenFrom(mailFor(email));

    signUp(email);

    assertThat(subscriberCount(email)).isEqualTo(1);
    assertThat(mailCount(email)).isEqualTo(2);
    String secondToken = latestTokenFor(email);
    assertThat(secondToken).isNotEqualTo(firstToken);
    // The replaced link is dead the moment a new one is issued, so a forwarded mail cannot be
    // redeemed after its owner has asked for a fresh one.
    assertThat(follow("/pl/waitlist/confirm/" + firstToken).getBody())
        .contains("data-waitlist-confirm=\"unknown\"");
    assertThat(follow("/pl/waitlist/confirm/" + secondToken).getBody())
        .contains("data-waitlist-confirm=\"ok\"");
  }

  @Test
  @DisplayName("asking for a new link redirects to the 'check your inbox' page and queues one")
  void askingForANewLinkQueuesOne() {
    String email = uniqueEmail("resend");
    signUp(email);
    String token = confirmTokenFrom(mailFor(email));

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("token", token);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    ResponseEntity<String> response =
        restTemplate
            .withRedirects(HttpRedirects.DONT_FOLLOW)
            .postForEntity(
                "/pl/waitlist/confirm/resend", new HttpEntity<>(form, headers), String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(302);
    assertThat(response.getHeaders().getLocation()).hasToString("/pl/waitlist/confirm/sent");
    assertThat(mailCount(email)).isEqualTo(2);
  }

  @Test
  @DisplayName("the 'check your inbox' page is a real document, reachable directly from the mail")
  void theSentPageIsADocument() {
    ResponseEntity<String> page = follow("/pl/waitlist/confirm/sent");

    assertThat(page.getStatusCode().value()).isEqualTo(200);
    assertThat(page.getHeaders().getContentType()).hasToString("text/html;charset=UTF-8");
  }

  @Test
  @DisplayName("the unsubscribe link in the message works without a login and is idempotent")
  void theUnsubscribeLinkWorksWithoutALogin() {
    String email = uniqueEmail("unsub");
    signUp(email);
    Matcher matcher = UNSUBSCRIBE_LINK.matcher(mailFor(email).get("text_body").toString());
    assertThat(matcher.find()).isTrue();
    String path = matcher.group();

    assertThat(follow(path).getBody()).contains("data-waitlist-unsubscribe=\"ok\"");
    assertThat(subscriberRow(email).get("status")).isEqualTo(SubscriberStatus.UNSUBSCRIBED.value());

    assertThat(follow(path).getBody()).contains("data-waitlist-unsubscribe=\"ok\"");
  }

  @Test
  @DisplayName("the mail is written in the language the visitor signed up in")
  void theMailFollowsTheSignupLanguage() {
    String polish = uniqueEmail("pl-copy");
    String english = uniqueEmail("en-copy");

    signUp("pl", polish);
    signUp("en", english);

    assertThat(mailFor(polish).get("subject").toString()).contains("Potwierdź swój adres");
    assertThat(mailFor(english).get("subject").toString()).contains("Confirm your address");
    assertThat(mailFor(english).get("text_body").toString())
        .contains("/en/waitlist/confirm/")
        .contains("Unsubscribe");
  }

  private void signUp(String email) {
    signUp("pl", email);
  }

  private void signUp(String locale, String email) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("email", email);
    form.add("consent", "1");
    form.add("website", "");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    ResponseEntity<String> response =
        restTemplate
            .withRedirects(HttpRedirects.DONT_FOLLOW)
            .postForEntity(
                "/" + locale + "/waitlist", new HttpEntity<>(form, headers), String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(302);
  }

  private ResponseEntity<String> follow(String path) {
    return restTemplate.withRedirects(HttpRedirects.DONT_FOLLOW).getForEntity(path, String.class);
  }

  private static String confirmTokenFrom(Map<String, Object> mail) {
    Matcher matcher = CONFIRM_LINK.matcher(mail.get("text_body").toString());
    assertThat(matcher.find()).as("the message should carry a confirmation link").isTrue();
    return matcher.group("token");
  }

  private String latestTokenFor(String email) {
    return confirmTokenFrom(
        jdbcTemplate.queryForMap(
            "select * from mail_outbox where recipient = ? order by id desc limit 1", email));
  }

  private Map<String, Object> mailFor(String email) {
    return jdbcTemplate.queryForMap(
        "select * from mail_outbox where recipient = ? order by id limit 1", email);
  }

  private Integer mailCount(String email) {
    return jdbcTemplate.queryForObject(
        "select count(*) from mail_outbox where recipient = ?", Integer.class, email);
  }

  private Map<String, Object> subscriberRow(String email) {
    return jdbcTemplate.queryForMap("select * from waitlist_subscriber where email = ?", email);
  }

  private Integer subscriberCount(String email) {
    return jdbcTemplate.queryForObject(
        "select count(*) from waitlist_subscriber where email = ?", Integer.class, email);
  }

  private static String uniqueEmail(String prefix) {
    return prefix + "-" + System.nanoTime() + "@sklep.pl";
  }
}
