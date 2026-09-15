package click.kivvi.application.waitlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import click.kivvi.application.mail.MailQueue;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.mail.OutboundMail;
import click.kivvi.domain.waitlist.OpaqueToken;
import click.kivvi.domain.waitlist.SubscriberStatus;
import click.kivvi.domain.waitlist.UnsubscribeToken;
import click.kivvi.domain.waitlist.WaitlistSignup;
import click.kivvi.infrastructure.mail.MailOutboxStore;
import click.kivvi.infrastructure.mail.MailTemplateRenderer;
import click.kivvi.infrastructure.waitlist.SignupThrottle;
import click.kivvi.infrastructure.waitlist.WaitlistSubscriberStore;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Every state the double opt-in flow can end in, decided against an in-memory subscriber table.
 *
 * <p>What the SQL does with those decisions — the guarded confirm, the idempotent unsubscribe — is
 * proven against a real database by {@code WaitlistSubscriberStoreIT}; what is proven here is which
 * of them the service reaches for, and what it puts in the post as a result.
 */
class WaitlistConfirmationServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");
  private static final String SECRET = "a-test-unsubscribe-secret-long-enough-to-pass-the-guard";
  private static final String CONSENT = "Zgadzam się na otrzymanie powiadomienia o starcie.";

  private static final String CLIENT_IP = "203.0.113.7";

  private final InMemorySubscribers subscribers = new InMemorySubscribers();
  private final RecordingQueue queue = new RecordingQueue();
  private final CountingThrottle throttle = new CountingThrottle();
  private final WaitlistConfirmationService service =
      new WaitlistConfirmationService(subscribers, composer(), queue, throttle, SECRET);

  @Test
  @DisplayName("a missing or too-short unsubscribe secret stops the application from starting")
  void aWeakUnsubscribeSecretRefusesToStart() {
    // Not a nicety. Every unsubscribe link is an HMAC of a sequential BIGSERIAL id, so a secret
    // anyone can read or guess is a list anyone can unsubscribe by walking id = 1..N. An empty
    // one is worse still: SecretKeySpec throws "Empty key" on the first signup, so the application
    // looks healthy while every submission 500s.
    for (String unusable :
        new String[] {null, "", "   ", "too-short", "31-characters-is-one-short--!!!"}) {
      assertThatIllegalStateException()
          .as("secret: %s", unusable)
          .isThrownBy(
              () ->
                  new WaitlistConfirmationService(
                      subscribers, composer(), queue, throttle, unusable))
          .withMessageContaining("KIVVI_UNSUBSCRIBE_SECRET");
    }
  }

  @Test
  @DisplayName("a new address is stored and gets a confirmation link in the post")
  void aNewAddressIsRegisteredAndMailed() {
    boolean stored = service.register(signup("ala@sklep.pl"));

    assertThat(stored).isTrue();
    assertThat(queue.queued).hasSize(1);
    assertThat(queue.queued.getFirst().recipient()).isEqualTo("ala@sklep.pl");
    assertThat(queue.queued.getFirst().htmlBody())
        .contains("https://kivvi.click/pl/waitlist/confirm/");
    assertThat(subscribers.byEmail("ala@sklep.pl").confirmationTokenExpiresAt()).isNotNull();
  }

  @Test
  @DisplayName("signing up again with an unconfirmed address sends a new link and adds no row")
  void aRepeatSignupResendsWithoutASecondRow() {
    service.register(signup("ala@sklep.pl"));
    String firstHash = subscribers.confirmationHashOf("ala@sklep.pl");

    boolean stored = service.register(signup("ala@sklep.pl"));

    assertThat(stored).isFalse();
    assertThat(subscribers.rows).hasSize(1);
    assertThat(queue.queued).hasSize(2);
    assertThat(subscribers.confirmationHashOf("ala@sklep.pl")).isNotEqualTo(firstHash);
  }

  @Test
  @DisplayName("signing up again with an address that already confirmed sends nothing")
  void aConfirmedAddressIsNotMailedAgain() {
    service.register(signup("ala@sklep.pl"));
    service.confirm(tokenFor("ala@sklep.pl"), NOW, "203.0.113.7", "Mozilla/5.0");
    queue.queued.clear();

    service.register(signup("ala@sklep.pl"));

    assertThat(queue.queued).isEmpty();
  }

  @Test
  @DisplayName("a live link confirms the address and records who did it, from where")
  void aLiveLinkConfirms() {
    service.register(signup("ala@sklep.pl"));

    ConfirmationOutcome outcome =
        service.confirm(tokenFor("ala@sklep.pl"), NOW, "203.0.113.7", "Mozilla/5.0 (test)");

    assertThat(outcome).isEqualTo(new ConfirmationOutcome.Confirmed());
    Row row = subscribers.byEmailRow("ala@sklep.pl");
    assertThat(row.status).isEqualTo(SubscriberStatus.CONFIRMED);
    assertThat(row.confirmedAt).isEqualTo(NOW);
    assertThat(row.confirmedIp).isEqualTo("203.0.113.7");
    assertThat(row.confirmedUserAgent).isEqualTo("Mozilla/5.0 (test)");
  }

  @Test
  @DisplayName("following the same link twice says 'already confirmed', not 'never heard of it'")
  void aSpentLinkSaysAlreadyConfirmed() {
    service.register(signup("ala@sklep.pl"));
    String token = tokenFor("ala@sklep.pl");
    service.confirm(token, NOW, "203.0.113.7", "UA");

    ConfirmationOutcome second = service.confirm(token, NOW.plusSeconds(1), "203.0.113.7", "UA");

    assertThat(second).isEqualTo(new ConfirmationOutcome.AlreadyConfirmed());
  }

  @Test
  @DisplayName("a link past its deadline says so and hands the token back for a resend")
  void anExpiredLinkOffersAResend() {
    service.register(signup("ala@sklep.pl"));
    String token = tokenFor("ala@sklep.pl");

    ConfirmationOutcome outcome =
        service.confirm(token, NOW.plus(Duration.ofDays(8)), "203.0.113.7", "UA");

    assertThat(outcome).isEqualTo(new ConfirmationOutcome.Expired(token));
    assertThat(subscribers.byEmailRow("ala@sklep.pl").status).isEqualTo(SubscriberStatus.PENDING);
  }

  @Test
  @DisplayName("a token nobody issued is unknown, and a malformed one is unknown too")
  void anUnknownTokenIsUnknown() {
    assertThat(service.confirm("f".repeat(64), NOW, "203.0.113.7", "UA"))
        .isEqualTo(new ConfirmationOutcome.Unknown());
    assertThat(service.confirm("not-a-token", NOW, "203.0.113.7", "UA"))
        .isEqualTo(new ConfirmationOutcome.Unknown());
    assertThat(service.confirm("", NOW, "203.0.113.7", "UA"))
        .isEqualTo(new ConfirmationOutcome.Unknown());
  }

  @Test
  @DisplayName("a resend issues a fresh link for an expired token and queues one more message")
  void aResendIssuesAFreshLink() {
    service.register(signup("ala@sklep.pl"));
    String expired = tokenFor("ala@sklep.pl");
    queue.queued.clear();

    service.resend(expired, CLIENT_IP, NOW.plus(Duration.ofDays(8)));

    assertThat(queue.queued).hasSize(1);
    assertThat(subscribers.confirmationHashOf("ala@sklep.pl"))
        .isNotEqualTo(new OpaqueToken(expired).hash());
  }

  @Test
  @DisplayName("a resend for a token nobody issued queues nothing and says nothing either way")
  void aResendForAnUnknownTokenIsSilent() {
    service.resend("f".repeat(64), CLIENT_IP, NOW);
    service.resend("nonsense", CLIENT_IP, NOW);

    assertThat(queue.queued).isEmpty();
  }

  @Test
  @DisplayName("a resend for an address that already confirmed does not mail it again")
  void aResendForAConfirmedAddressIsSilent() {
    service.register(signup("ala@sklep.pl"));
    String token = tokenFor("ala@sklep.pl");
    service.confirm(token, NOW, "203.0.113.7", "UA");
    queue.queued.clear();

    service.resend(token, CLIENT_IP, NOW.plusSeconds(1));

    assertThat(queue.queued).isEmpty();
  }

  @Test
  @DisplayName("a stale confirmation link cannot put an address back after it unsubscribed")
  void aStaleLinkCannotResurrectAnUnsubscribedAddress() {
    // Both links travel in the same message, and a corporate link scanner visits every URL in one
    // in whatever order it likes — so "unsubscribe, then confirm" is an order that will happen.
    service.register(signup("ala@sklep.pl"));
    String confirmation = tokenFor("ala@sklep.pl");
    service.unsubscribe(unsubscribeTokenFor("ala@sklep.pl"), NOW);

    ConfirmationOutcome outcome =
        service.confirm(confirmation, NOW.plusSeconds(1), "203.0.113.7", "Scanner/1.0");

    assertThat(outcome).isEqualTo(new ConfirmationOutcome.Unknown());
    assertThat(subscribers.byEmailRow("ala@sklep.pl").status)
        .isEqualTo(SubscriberStatus.UNSUBSCRIBED);
    assertThat(subscribers.byEmailRow("ala@sklep.pl").confirmedAt).isNull();
  }

  @Test
  @DisplayName("signing up again after unsubscribing puts the address back, unconfirmed")
  void signingUpAgainAfterUnsubscribingWorks() {
    // Both pages tell people they can come back this way, so it has to actually work.
    service.register(signup("ala@sklep.pl"));
    service.unsubscribe(unsubscribeTokenFor("ala@sklep.pl"), NOW);
    queue.queued.clear();

    boolean stored = service.register(signup("ala@sklep.pl"));

    assertThat(stored).isFalse();
    assertThat(subscribers.rows).hasSize(1);
    assertThat(subscribers.byEmailRow("ala@sklep.pl").status).isEqualTo(SubscriberStatus.PENDING);
    assertThat(subscribers.byEmailRow("ala@sklep.pl").unsubscribedAt).isNull();
    assertThat(queue.queued).hasSize(1);
    assertThat(service.confirm(tokenFor("ala@sklep.pl"), NOW.plusSeconds(1), "203.0.113.7", "UA"))
        .isEqualTo(new ConfirmationOutcome.Confirmed());
  }

  @Test
  @DisplayName("the resend is rate limited, and refusing looks exactly like succeeding")
  void theResendIsRateLimited() {
    service.register(signup("ala@sklep.pl"));
    String token = tokenFor("ala@sklep.pl");
    queue.queued.clear();
    throttle.exhaustEverything();

    service.resend(token, CLIENT_IP, NOW);

    assertThat(queue.queued).isEmpty();
  }

  @Test
  @DisplayName("an unknown token costs the caller its allowance too, so the limiter is no oracle")
  void anUnknownResendStillChargesTheCaller() {
    // A well-formed token that belongs to nobody is charged against both buckets, exactly as a
    // real one is — an allowance that only bit on tokens it recognised would be the address
    // oracle this endpoint exists to avoid. A value that is not a token shape at all never
    // reaches the second bucket, because there is no hash to key it by; the per-IP charge is
    // what bounds that path.
    service.resend("f".repeat(64), CLIENT_IP, NOW);
    service.resend("nonsense", CLIENT_IP, NOW);

    assertThat(throttle.acquisitions)
        .filteredOn(key -> key.startsWith("ip:"))
        .containsExactly("ip:" + CLIENT_IP, "ip:" + CLIENT_IP);
    assertThat(throttle.acquisitions).filteredOn(key -> key.startsWith("resend:")).hasSize(1);
  }

  @Test
  @DisplayName("the unsubscribe link takes the address off the list without a login")
  void theUnsubscribeLinkWorks() {
    service.register(signup("ala@sklep.pl"));
    String token = unsubscribeTokenFor("ala@sklep.pl");

    assertThat(service.unsubscribe(token, NOW)).isEqualTo(new UnsubscribeOutcome.Unsubscribed());
    assertThat(subscribers.byEmailRow("ala@sklep.pl").status)
        .isEqualTo(SubscriberStatus.UNSUBSCRIBED);
  }

  @Test
  @DisplayName("clicking the unsubscribe link again is idempotent, not an error")
  void unsubscribingTwiceIsIdempotent() {
    service.register(signup("ala@sklep.pl"));
    String token = unsubscribeTokenFor("ala@sklep.pl");
    service.unsubscribe(token, NOW);

    assertThat(service.unsubscribe(token, NOW.plusSeconds(60)))
        .isEqualTo(new UnsubscribeOutcome.Unsubscribed());
    assertThat(subscribers.byEmailRow("ala@sklep.pl").unsubscribedAt).isEqualTo(NOW);
  }

  @Test
  @DisplayName("an unsubscribe token nobody issued is unknown")
  void anUnknownUnsubscribeTokenIsUnknown() {
    assertThat(service.unsubscribe("f".repeat(64), NOW))
        .isEqualTo(new UnsubscribeOutcome.Unknown());
    assertThat(service.unsubscribe("nonsense", NOW)).isEqualTo(new UnsubscribeOutcome.Unknown());
  }

  @Test
  @DisplayName("the unsubscribe link in every message for one subscriber is the same link")
  void theUnsubscribeLinkNeverChanges() {
    service.register(signup("ala@sklep.pl"));
    String first = linkIn(queue.queued.getFirst(), "/waitlist/unsubscribe/");
    service.register(signup("ala@sklep.pl"));

    assertThat(linkIn(queue.queued.getLast(), "/waitlist/unsubscribe/")).isEqualTo(first);
  }

  /**
   * The service only ever hands the store a hash, so the token is recovered the way a subscriber
   * would get it: out of the message that was queued. Every test therefore follows exactly the link
   * that went in the post, not one the test made up.
   */
  private String tokenFor(String email) {
    String hash = subscribers.confirmationHashOf(email);
    return queue.queued.stream()
        .map(mail -> tokenIn(mail, "/waitlist/confirm/"))
        .filter(token -> new OpaqueToken(token).hash().equals(hash))
        .reduce((first, second) -> second)
        .orElseThrow(() -> new AssertionError("No queued message carries the current token."));
  }

  private String unsubscribeTokenFor(String email) {
    return UnsubscribeToken.forSubscriber(SECRET, subscribers.byEmail(email).id()).value();
  }

  private static String linkIn(OutboundMail mail, String marker) {
    int start = mail.textBody().indexOf(marker);
    assertThat(start).as("the message should carry a %s link", marker).isNotNegative();
    return mail.textBody().substring(start, start + marker.length() + 64);
  }

  private static String tokenIn(OutboundMail mail, String marker) {
    return linkIn(mail, marker).substring(marker.length());
  }

  private static WaitlistSignup signup(String email) {
    return WaitlistSignup.landingSignup(
        email, SupportedLocale.PL, NOW, "203.0.113.7", "Mozilla/5.0", CONSENT);
  }

  private static WaitlistMailComposer composer() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setTemplateEngineMessageSource(messageSource());
    return new WaitlistMailComposer(
        new MailTemplateRenderer(engine), messageSource(), "https://kivvi.click");
  }

  private static MessageSource messageSource() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:messages");
    source.setDefaultEncoding("UTF-8");
    return source;
  }

  /** Counts attempts per bucket in memory, and can declare every bucket already exhausted. */
  private static final class CountingThrottle implements SignupThrottle {

    private final List<String> acquisitions = new ArrayList<>();
    private final Map<String, Integer> hits = new HashMap<>();
    private boolean everythingExhausted;

    private void exhaustEverything() {
      everythingExhausted = true;
    }

    @Override
    public boolean tryAcquire(String bucketKey, int limit) {
      acquisitions.add(bucketKey);
      int used = hits.merge(bucketKey, 1, Integer::sum);
      return !everythingExhausted && used <= limit;
    }
  }

  private static final class RecordingQueue extends MailQueue {
    private final List<OutboundMail> queued = new ArrayList<>();

    private RecordingQueue() {
      super(new MailOutboxStore(null));
    }

    @Override
    public boolean enqueue(OutboundMail mail) {
      queued.add(mail);
      return true;
    }
  }

  private static final class Row {
    private final long id;
    private final String email;
    private SubscriberStatus status = SubscriberStatus.PENDING;
    private String confirmationHash;
    private Instant confirmationExpiresAt;
    private String unsubscribeHash;
    private Instant confirmedAt;
    private String confirmedIp;
    private String confirmedUserAgent;
    private Instant unsubscribedAt;

    private Row(long id, String email) {
      this.id = id;
      this.email = email;
    }
  }

  /**
   * The subscriber table, in a HashMap. The service's own decisions are what this class is here to
   * expose — every guard the real SQL applies is mirrored, because a stand-in that is more
   * permissive than the database would let a broken service pass.
   */
  private static final class InMemorySubscribers extends WaitlistSubscriberStore {

    private final Map<String, Row> rows = new HashMap<>();
    private long nextId = 1;

    private InMemorySubscribers() {
      super(null);
    }

    private Row byEmailRow(String email) {
      return rows.get(email);
    }

    private Subscriber byEmail(String email) {
      return findByEmail(email).orElseThrow();
    }

    private String confirmationHashOf(String email) {
      return rows.get(email).confirmationHash;
    }

    @Override
    public boolean save(WaitlistSignup signup) {
      if (rows.containsKey(signup.email())) {
        return false;
      }
      rows.put(signup.email(), new Row(nextId++, signup.email()));
      return true;
    }

    @Override
    public Optional<Subscriber> findByEmail(String email) {
      return Optional.ofNullable(rows.get(email)).map(InMemorySubscribers::toSubscriber);
    }

    @Override
    public Optional<Subscriber> findByConfirmationTokenHash(String tokenHash) {
      return rows.values().stream()
          .filter(row -> tokenHash.equals(row.confirmationHash))
          .findFirst()
          .map(InMemorySubscribers::toSubscriber);
    }

    @Override
    public void issueTokens(
        long subscriberId, String confirmationHash, Instant expiresAt, String unsubscribeHash) {
      Row row = rowById(subscriberId);
      row.confirmationHash = confirmationHash;
      row.confirmationExpiresAt = expiresAt;
      row.unsubscribeHash = unsubscribeHash;
    }

    @Override
    public boolean confirm(String tokenHash, Instant now, String ip, String userAgent) {
      return rows.values().stream()
          .filter(row -> tokenHash.equals(row.confirmationHash))
          .filter(row -> row.confirmedAt == null)
          .filter(row -> row.unsubscribedAt == null)
          .filter(
              row -> row.confirmationExpiresAt != null && now.isBefore(row.confirmationExpiresAt))
          .findFirst()
          .map(
              row -> {
                row.status = SubscriberStatus.CONFIRMED;
                row.confirmedAt = now;
                row.confirmedIp = ip;
                row.confirmedUserAgent = userAgent;
                return true;
              })
          .orElse(false);
    }

    @Override
    public boolean unsubscribe(String tokenHash, Instant now) {
      return rows.values().stream()
          .filter(row -> tokenHash.equals(row.unsubscribeHash))
          .findFirst()
          .map(
              row -> {
                row.status = SubscriberStatus.UNSUBSCRIBED;
                row.unsubscribedAt = row.unsubscribedAt == null ? now : row.unsubscribedAt;
                return true;
              })
          .orElse(false);
    }

    @Override
    public void reopen(long subscriberId, WaitlistSignup signup) {
      Row row = rowById(subscriberId);
      row.status = SubscriberStatus.PENDING;
      row.unsubscribedAt = null;
      row.confirmedAt = null;
      row.confirmedIp = null;
      row.confirmedUserAgent = null;
    }

    private Row rowById(long id) {
      return rows.values().stream().filter(row -> row.id == id).findFirst().orElseThrow();
    }

    private static Subscriber toSubscriber(Row row) {
      return new Subscriber(
          row.id, row.email, SupportedLocale.PL, row.status, row.confirmationExpiresAt);
    }
  }
}
