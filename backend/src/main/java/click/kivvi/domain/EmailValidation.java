package click.kivvi.domain;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Login form validation. Credential verification arrives with the User entity and a real firewall;
 * until then the form only refuses empty or malformed addresses — mirrors {@code
 * SecurityController::validate()}, including its two literal Polish messages, which are never
 * translated (the old stack never ran them through the translator either).
 */
public final class EmailValidation {

  public static final String EMPTY_MESSAGE = "Podaj adres e-mail.";
  public static final String MALFORMED_MESSAGE = "To nie wygląda na poprawny adres e-mail.";

  // A strict RFC 5322-ish address: local-part@label(.label)+, requiring a dotted domain the
  // same way PHP's FILTER_VALIDATE_EMAIL accepts every oracle input (a real address and a
  // string with no "@" at all).
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile(
          "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9]"
              + "(?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
              + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");

  private EmailValidation() {}

  /** Returns the Polish error message for an invalid address, or empty when it is valid. */
  public static Optional<String> errorFor(String email) {
    if (email == null || email.isBlank()) {
      return Optional.of(EMPTY_MESSAGE);
    }
    if (!EMAIL_PATTERN.matcher(email).matches()) {
      return Optional.of(MALFORMED_MESSAGE);
    }
    return Optional.empty();
  }
}
