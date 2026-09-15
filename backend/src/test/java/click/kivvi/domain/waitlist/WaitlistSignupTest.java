package click.kivvi.domain.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.SupportedLocale;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WaitlistSignupTest {

  private static final String CONSENT_TEXT = "Zgadzam się na otrzymanie powiadomienia o starcie.";

  @Test
  @DisplayName("an address is trimmed and lower-cased, so capitalization cannot split a subscriber")
  void theAddressIsNormalized() {
    assertThat(WaitlistSignup.normalizeEmail("  Ala@SKLEP.PL ")).isEqualTo("ala@sklep.pl");
  }

  @Test
  @DisplayName("a missing address normalizes to empty rather than blowing up")
  void aMissingAddressNormalizesToEmpty() {
    assertThat(WaitlistSignup.normalizeEmail(null)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  @DisplayName("an empty address is refused")
  void anEmptyAddressIsRefused(String rawEmail) {
    assertThat(WaitlistSignup.validate(rawEmail, true)).contains(WaitlistSignupError.EMAIL_EMPTY);
  }

  @ParameterizedTest
  @ValueSource(strings = {"nie-adres", "ala@", "@sklep.pl", "ala@sklep"})
  @DisplayName("a malformed address is refused")
  void aMalformedAddressIsRefused(String rawEmail) {
    assertThat(WaitlistSignup.validate(rawEmail, true))
        .contains(WaitlistSignupError.EMAIL_MALFORMED);
  }

  @Test
  @DisplayName("an unticked consent box is refused even when the address is perfectly good")
  void missingConsentIsRefused() {
    assertThat(WaitlistSignup.validate("ala@sklep.pl", false))
        .contains(WaitlistSignupError.CONSENT_REQUIRED);
  }

  @Test
  @DisplayName("a good address with consent passes")
  void aValidSignupPasses() {
    assertThat(WaitlistSignup.validate("  Ala@SKLEP.PL ", true)).isEmpty();
  }

  @Test
  @DisplayName("a landing signup is pending, sourced from the landing page and carries its proof")
  void aLandingSignupIsPendingWithItsConsentProof() {
    Instant now = Instant.parse("2026-09-15T10:15:30Z");

    WaitlistSignup signup =
        WaitlistSignup.landingSignup(
            "  Ala@SKLEP.PL ", SupportedLocale.PL, now, "203.0.113.7", "Mozilla/5.0", CONSENT_TEXT);

    assertThat(signup.email()).isEqualTo("ala@sklep.pl");
    assertThat(signup.status()).isEqualTo(SubscriberStatus.PENDING);
    assertThat(signup.source()).isEqualTo(SignupSource.LANDING);
    assertThat(signup.locale()).isEqualTo(SupportedLocale.PL);
    assertThat(signup.signedUpAt()).isEqualTo(now);
    assertThat(signup.consent().at()).isEqualTo(now);
    assertThat(signup.consent().ip()).isEqualTo("203.0.113.7");
    assertThat(signup.consent().userAgent()).isEqualTo("Mozilla/5.0");
    assertThat(signup.consent().text()).isEqualTo(CONSENT_TEXT);
  }

  @Test
  @DisplayName("a request without a User-Agent header is still a valid signup")
  void aMissingUserAgentIsAcceptable() {
    WaitlistSignup signup =
        WaitlistSignup.landingSignup(
            "ala@sklep.pl", SupportedLocale.EN, Instant.now(), null, null, CONSENT_TEXT);

    assertThat(signup.consent().userAgent()).isNull();
  }
}
