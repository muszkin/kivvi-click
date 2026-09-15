package click.kivvi.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Lower-case hexadecimal SHA-256, the one hashing rule this application uses.
 *
 * <p>Three places need it for three different reasons — the waitlist throttle keys its abuse
 * counter by it so a throwaway table never holds readable addresses, {@code EventDedupStore} keys
 * its idempotency ledger by it, and {@link click.kivvi.domain.waitlist.OpaqueToken} stores it
 * instead of the token itself. They must agree on the algorithm, the encoding and the case, or two
 * of them would disagree about what "the same value" means; a shared function is the cheapest way
 * to make that impossible.
 */
public final class Sha256 {

  private Sha256() {}

  public static String hex(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable on this JVM.", exception);
    }
  }
}
