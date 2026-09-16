package click.kivvi.domain.waitlist;

import click.kivvi.domain.Sha256;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A secret that travels in a URL and is never stored.
 *
 * <p>Thirty-two bytes from {@link SecureRandom}, rendered as sixty-four hexadecimal characters.
 * Only the {@link #hash()} reaches the database, exactly as {@code EventDedupStore} keeps hashes
 * rather than the keys they came from: a leaked database backup then hands an attacker nothing they
 * can put in a link, and a confirmation URL cannot be reconstructed from a row.
 *
 * <p>Hexadecimal rather than base64url because the value ends up in a path segment, an e-mail
 * client's link parser and occasionally a support ticket; sixty-four characters that are always
 * {@code [0-9a-f]} survive all three without escaping, and the shape is cheap to validate before
 * anything touches the database.
 */
public record OpaqueToken(String value) {

  /** 256 bits: the same width as the digest, and far past anything worth guessing at. */
  private static final int BYTES = 32;

  private static final Pattern SHAPE = Pattern.compile("^[0-9a-f]{64}$");

  private static final SecureRandom RANDOM = new SecureRandom();

  public OpaqueToken {
    Objects.requireNonNull(value, "value");
    if (!SHAPE.matcher(value).matches()) {
      throw new IllegalArgumentException("A token is 64 lower-case hexadecimal characters.");
    }
  }

  public static OpaqueToken generate() {
    byte[] bytes = new byte[BYTES];
    RANDOM.nextBytes(bytes);
    return new OpaqueToken(HexFormat.of().formatHex(bytes));
  }

  /**
   * Reads a token out of a URL. A value of the wrong shape is simply absent rather than an
   * exception: a truncated or mistyped link is an ordinary thing to receive, and it deserves the
   * same "we do not know this link" page as a token that is merely unknown — never a 500, and never
   * a different answer that would tell a prober which of the two it hit.
   */
  public static Optional<OpaqueToken> parse(String candidate) {
    if (candidate == null || !SHAPE.matcher(candidate).matches()) {
      return Optional.empty();
    }
    return Optional.of(new OpaqueToken(candidate));
  }

  /** What the database stores in place of this token. */
  public String hash() {
    return Sha256.hex(value);
  }
}
