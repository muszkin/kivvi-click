package click.kivvi.application.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.waitlist.WaitlistSignup;
import click.kivvi.domain.waitlist.WaitlistSignupError;
import click.kivvi.infrastructure.waitlist.SignupThrottle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

/**
 * {@link WaitlistSignupService} against stand-ins for the registrar and the limiter — the point of
 * each test is the decision the service makes and, just as importantly, what it declines to touch
 * when it refuses: a honeypot hit and an exhausted allowance must both cost zero database writes
 * and zero queued messages.
 *
 * <p>PIO-71 turned the store dependency into {@link WaitlistRegistrar}, because registering is no
 * longer only an insert — it also queues a confirmation link. What that step does is {@code
 * WaitlistConfirmationServiceTest}'s subject; what matters here is only whether it is reached at
 * all.
 */
class WaitlistSignupServiceTest {

  private static final String CONSENT_TEXT = "Zgadzam się na kontakt w sprawie wdrożenia.";

  private final RecordingRegistrar registrar = new RecordingRegistrar();
  private final CountingThrottle throttle = new CountingThrottle();
  private final WaitlistSignupService service =
      new WaitlistSignupService(registrar, throttle, consentTextSource());

  @Test
  @DisplayName("a good submission is accepted and stored as a pending landing signup")
  void aGoodSubmissionIsStored() {
    SignupOutcome outcome = service.signUp(request("ala@sklep.pl", true, ""));

    assertThat(outcome).isEqualTo(new SignupOutcome.Accepted());
    assertThat(registrar.registered).hasSize(1);
    assertThat(registrar.registered.getFirst().email()).isEqualTo("ala@sklep.pl");
    assertThat(registrar.registered.getFirst().consent().text()).isEqualTo(CONSENT_TEXT);
  }

  @Test
  @DisplayName("a filled honeypot looks like success and never reaches the store or the limiter")
  void aFilledHoneypotIsSilentlyDiscarded() {
    SignupOutcome outcome = service.signUp(request("bot@spam.example", true, "http://spam"));

    assertThat(outcome).isEqualTo(new SignupOutcome.Accepted());
    assertThat(registrar.registered).isEmpty();
    assertThat(throttle.acquisitions).isEmpty();
  }

  @Test
  @DisplayName("an exhausted allowance is refused before the address is ever validated or stored")
  void anExhaustedAllowanceIsRefusedBeforeValidation() {
    throttle.exhaustEverything();

    SignupOutcome outcome = service.signUp(request("nie-adres", false, ""));

    assertThat(outcome).isEqualTo(new SignupOutcome.ThrottleExceeded());
    assertThat(registrar.registered).isEmpty();
  }

  @Test
  @DisplayName("the per-IP allowance is checked first and short-circuits the per-address one")
  void thePerIpAllowanceShortCircuitsThePerAddressOne() {
    throttle.exhaustEverything();

    service.signUp(request("ala@sklep.pl", true, ""));

    assertThat(throttle.acquisitions).containsExactly("ip:203.0.113.7");
  }

  @Test
  @DisplayName("both allowances are consumed for a submission that gets through")
  void bothAllowancesAreConsumed() {
    service.signUp(request("ala@sklep.pl", true, ""));

    assertThat(throttle.acquisitions).hasSize(2);
    assertThat(throttle.acquisitions.getFirst()).isEqualTo("ip:203.0.113.7");
    assertThat(throttle.acquisitions.getLast()).startsWith("email:");
  }

  @Test
  @DisplayName("the per-address bucket never holds a readable address")
  void thePerAddressBucketIsHashed() {
    service.signUp(request("ala@sklep.pl", true, ""));

    assertThat(throttle.acquisitions.getLast()).doesNotContain("ala@sklep.pl");
  }

  @Test
  @DisplayName("capitalization and padding do not earn a fresh per-address allowance")
  void thePerAddressBucketIsNormalizedFirst() {
    service.signUp(request("ala@sklep.pl", true, ""));
    service.signUp(request("  ALA@SKLEP.PL  ", true, ""));

    assertThat(throttle.acquisitions.get(1)).isEqualTo(throttle.acquisitions.get(3));
  }

  @Test
  @DisplayName("a blank address is never charged to a per-address bucket everyone would share")
  void aBlankAddressDoesNotConsumeASharedAllowance() {
    service.signUp(request("", true, ""));

    assertThat(throttle.acquisitions).containsExactly("ip:203.0.113.7");
  }

  @Test
  @DisplayName(
      "blank submissions from many visitors keep getting told to enter an address, never that"
          + " they have tried too often")
  void blankSubmissionsDoNotExhaustEachOthersAllowance() {
    for (int visitor = 0; visitor < 10; visitor++) {
      SignupOutcome outcome =
          service.signUp(
              new SignupRequest("", true, "", SupportedLocale.PL, "198.51.100." + visitor, "UA"));

      assertThat(outcome)
          .describedAs("visitor %d", visitor)
          .isEqualTo(new SignupOutcome.Rejected(WaitlistSignupError.EMAIL_EMPTY));
    }
  }

  @Test
  @DisplayName("a repeat of an address already on the list still reads as success")
  void aDuplicateStillLooksLikeSuccess() {
    registrar.reportEverythingAsAlreadyPresent();

    SignupOutcome outcome = service.signUp(request("ala@sklep.pl", true, ""));

    assertThat(outcome).isEqualTo(new SignupOutcome.Accepted());
  }

  @Test
  @DisplayName("a malformed address is rejected and stored nowhere")
  void aMalformedAddressIsRejected() {
    SignupOutcome outcome = service.signUp(request("nie-adres", true, ""));

    assertThat(outcome).isEqualTo(new SignupOutcome.Rejected(WaitlistSignupError.EMAIL_MALFORMED));
    assertThat(registrar.registered).isEmpty();
  }

  @Test
  @DisplayName("an unticked consent box is rejected and stored nowhere")
  void missingConsentIsRejected() {
    SignupOutcome outcome = service.signUp(request("ala@sklep.pl", false, ""));

    assertThat(outcome).isEqualTo(new SignupOutcome.Rejected(WaitlistSignupError.CONSENT_REQUIRED));
    assertThat(registrar.registered).isEmpty();
  }

  @Test
  @DisplayName("the stored consent clause is the one written in the visitor's own language")
  void theConsentClauseFollowsTheVisitorsLanguage() {
    service.signUp(
        new SignupRequest(
            "ala@sklep.pl", true, "", SupportedLocale.EN, "203.0.113.7", "Mozilla/5.0"));

    assertThat(registrar.registered.getFirst().consent().text())
        .isEqualTo("I agree to be contacted about a deployment.");
  }

  private static SignupRequest request(String email, boolean consentGiven, String honeypot) {
    return new SignupRequest(
        email, consentGiven, honeypot, SupportedLocale.PL, "203.0.113.7", "Mozilla/5.0");
  }

  private static StaticMessageSource consentTextSource() {
    StaticMessageSource source = new StaticMessageSource();
    source.addMessage("waitlist.consent.text", Locale.forLanguageTag("pl"), CONSENT_TEXT);
    source.addMessage(
        "waitlist.consent.text",
        Locale.forLanguageTag("en"),
        "I agree to be contacted about a deployment.");
    return source;
  }

  /** Records what reached registration, and can pretend every address is already on the list. */
  private static final class RecordingRegistrar implements WaitlistRegistrar {

    private final List<WaitlistSignup> registered = new ArrayList<>();
    private boolean everythingAlreadyPresent;

    private void reportEverythingAsAlreadyPresent() {
      everythingAlreadyPresent = true;
    }

    @Override
    public boolean register(WaitlistSignup signup) {
      registered.add(signup);
      return !everythingAlreadyPresent;
    }
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
}
