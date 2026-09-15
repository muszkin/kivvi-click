package click.kivvi.domain.waitlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import click.kivvi.domain.Sha256;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What a confirmation link is allowed to be. Everything here is a security property rather than a
 * convenience: a predictable token, a token stored in the clear, or one that outlives its deadline
 * each turn double opt-in into decoration.
 */
class ConfirmationTokenTest {

  private static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");

  @Test
  @DisplayName("a token is 64 lower-case hexadecimal characters, so it survives a URL untouched")
  void aTokenIsSixtyFourHexCharacters() {
    String value = ConfirmationToken.issue(NOW).value();

    assertThat(value).hasSize(64).matches("[0-9a-f]{64}");
  }

  @Test
  @DisplayName("no two issued tokens are the same")
  void everyIssuedTokenIsDifferent() {
    Set<String> issued = new HashSet<>();
    for (int i = 0; i < 500; i++) {
      issued.add(ConfirmationToken.issue(NOW).value());
    }

    assertThat(issued).hasSize(500);
  }

  @Test
  @DisplayName(
      "the stored hash is SHA-256 of the token and nothing else, so the link is not in the database")
  void theStoredHashIsTheDigestOfTheToken() {
    ConfirmationToken token = ConfirmationToken.issue(NOW);

    assertThat(token.hash()).isEqualTo(Sha256.hex(token.value())).isNotEqualTo(token.value());
  }

  @Test
  @DisplayName("hashing the same token twice gives the same answer, or no link would ever match")
  void theHashIsStable() {
    OpaqueToken token = OpaqueToken.generate();

    assertThat(token.hash()).isEqualTo(new OpaqueToken(token.value()).hash());
  }

  @Test
  @DisplayName("a token lives for seven days from the moment it is issued")
  void aTokenLivesForSevenDays() {
    ConfirmationToken token = ConfirmationToken.issue(NOW);

    assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    assertThat(ConfirmationToken.VALIDITY).isEqualTo(Duration.ofDays(7));
  }

  @Test
  @DisplayName("a token is live right up to its deadline and lapsed from the deadline on")
  void theDeadlineIsExclusive() {
    ConfirmationToken token = ConfirmationToken.issue(NOW);

    assertThat(token.isExpired(NOW.plus(Duration.ofDays(7)).minusSeconds(1))).isFalse();
    assertThat(token.isExpired(NOW.plus(Duration.ofDays(7)))).isTrue();
    assertThat(token.isExpired(NOW.plus(Duration.ofDays(8)))).isTrue();
  }

  @Test
  @DisplayName("a stored hash with no deadline counts as lapsed rather than as an eternal link")
  void aMissingDeadlineCountsAsLapsed() {
    assertThat(ConfirmationToken.hasLapsed(null, NOW)).isTrue();
  }

  @Test
  @DisplayName("a value that is not a token shape is unknown, not an exception")
  void aMalformedTokenIsSimplyUnknown() {
    assertThat(OpaqueToken.parse(null)).isEmpty();
    assertThat(OpaqueToken.parse("")).isEmpty();
    assertThat(OpaqueToken.parse("nope")).isEmpty();
    assertThat(OpaqueToken.parse("A".repeat(64))).isEmpty();
    assertThat(OpaqueToken.parse("0".repeat(63))).isEmpty();
    assertThat(OpaqueToken.parse("0".repeat(64))).isPresent();
  }

  @Test
  @DisplayName("constructing a token from a malformed value is a programming error")
  void constructingFromRubbishThrows() {
    assertThatIllegalArgumentException().isThrownBy(() -> new OpaqueToken("nope"));
  }
}
