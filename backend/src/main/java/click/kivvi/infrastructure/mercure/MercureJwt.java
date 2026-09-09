package click.kivvi.infrastructure.mercure;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * A minimal HS256 JWT carrying {@code {"mercure":{"publish":["*"]}}}, matching {@code
 * config/packages/mercure.yaml}'s {@code jwt.publish: '*'} — the publisher-side counterpart of the
 * subscriber JWT the Mercure edge already issues anonymous browser subscribers. Hand-rolled rather
 * than pulled in as a dependency: the token shape never varies (one fixed claim, one algorithm), so
 * a general-purpose JWT library would add a dependency to do less than this class already does.
 */
final class MercureJwt {

  private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
  private static final String PUBLISH_ALL_CLAIM = "{\"mercure\":{\"publish\":[\"*\"]}}";
  private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

  private final String token;

  MercureJwt(String secret) {
    this.token = sign(secret);
  }

  String token() {
    return token;
  }

  private static String sign(String secret) {
    String signingInput = encode(HEADER_JSON) + "." + encode(PUBLISH_ALL_CLAIM);
    return signingInput + "." + BASE64_URL.encodeToString(hmacSha256(secret, signingInput));
  }

  private static String encode(String json) {
    return BASE64_URL.encodeToString(json.getBytes(StandardCharsets.UTF_8));
  }

  private static byte[] hmacSha256(String secret, String signingInput) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
      throw new IllegalStateException("HmacSHA256 is unavailable on this JVM.", exception);
    }
  }
}
