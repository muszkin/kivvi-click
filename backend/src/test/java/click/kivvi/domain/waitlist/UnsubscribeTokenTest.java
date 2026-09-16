package click.kivvi.domain.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The unsubscribe link is derived rather than drawn, and three properties have to hold for that to
 * be safe: the same subscriber always gets the same link, a different subscriber never does, and
 * nobody without the secret can work out either.
 */
class UnsubscribeTokenTest {

  private static final String SECRET = "a-secret-long-enough-to-be-worth-having";

  @Test
  @DisplayName("the same subscriber always gets the same link, or last year's message would break")
  void theTokenIsStableForOneSubscriber() {
    assertThat(UnsubscribeToken.forSubscriber(SECRET, 42).value())
        .isEqualTo(UnsubscribeToken.forSubscriber(SECRET, 42).value());
  }

  @Test
  @DisplayName("no two subscribers share a link")
  void everySubscriberGetsItsOwnToken() {
    assertThat(UnsubscribeToken.forSubscriber(SECRET, 1).value())
        .isNotEqualTo(UnsubscribeToken.forSubscriber(SECRET, 2).value());
  }

  @Test
  @DisplayName("the token is a token shape, so it survives a URL and the store's lookup unchanged")
  void theTokenHasTheShapeEverythingElseExpects() {
    String value = UnsubscribeToken.forSubscriber(SECRET, 7).value();

    assertThat(value).hasSize(64).matches("[0-9a-f]{64}");
    assertThat(OpaqueToken.parse(value)).isPresent();
  }

  @Test
  @DisplayName("the secret is what makes it unguessable: change it and every link changes")
  void theSecretIsLoadBearing() {
    assertThat(UnsubscribeToken.forSubscriber(SECRET, 42).value())
        .isNotEqualTo(
            UnsubscribeToken.forSubscriber("a-different-secret-of-decent-length", 42).value());
  }

  @Test
  @DisplayName("the derivation is namespaced, so a second kind of token can never collide with it")
  void theDerivationIsNamespaced() throws Exception {
    // The token is not the HMAC of the bare id. If it were, any later "reset password for id 42"
    // or "manage preferences for id 42" token derived from the same secret would come out
    // identical, and one could be redeemed as the other.
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String withoutPurpose =
        HexFormat.of().formatHex(mac.doFinal("42".getBytes(StandardCharsets.UTF_8)));

    assertThat(UnsubscribeToken.forSubscriber(SECRET, 42).value()).isNotEqualTo(withoutPurpose);
  }
}
