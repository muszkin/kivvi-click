package click.kivvi.domain.waitlist;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The link that gets someone off the list, derived rather than drawn.
 *
 * <p>A confirmation token can be random because it is used once, within a week, by the person who
 * just received it. An unsubscribe link cannot: it rides in the footer of every message this system
 * will ever send, and the one in a mail from last spring has to still work. A random token would
 * have to be kept in plaintext somewhere to be put in a second message — and the moment it is
 * stored in the clear, a leaked backup is a list of links that unsubscribe other people.
 *
 * <p>So it is an HMAC of the subscriber's id under a server-side secret: unguessable without the
 * secret, identical every time it is computed, and stored only as a digest for lookup. HMAC-SHA-256
 * is 32 bytes, which is exactly the shape {@link OpaqueToken} wants.
 *
 * <p>The consequence, stated plainly because it is the price of the design: rotating the secret
 * invalidates every unsubscribe link already in the wild. Rotate it only with that in mind.
 */
public final class UnsubscribeToken {

  private static final String ALGORITHM = "HmacSHA256";

  /**
   * Namespaced so a second kind of derived token under the same secret can never collide with this
   * one.
   */
  private static final String PURPOSE = "waitlist-unsubscribe:";

  private UnsubscribeToken() {}

  public static OpaqueToken forSubscriber(String secret, long subscriberId) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
      byte[] digest = mac.doFinal((PURPOSE + subscriberId).getBytes(StandardCharsets.UTF_8));
      return new OpaqueToken(HexFormat.of().formatHex(digest));
    } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new IllegalStateException("HMAC-SHA-256 is unavailable on this JVM.", exception);
    }
  }
}
