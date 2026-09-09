package click.kivvi.domain;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The person the panel is rendered for: the e-mail typed at sign-in and the display name derived
 * from it. Deliberately not a security identity — see {@code PanelIdentity} in the old stack for
 * why: the login form only sets what the shell displays, nothing more.
 */
public record Identity(String email, String name) {

  /**
   * Derives a display name from an e-mail's local part, splitting on {@code . _ -} and capitalising
   * each piece — mirrors {@code PanelIdentity::name()} exactly.
   */
  public static Identity derive(String email) {
    String localPart = email.substring(0, email.indexOf('@'));
    String name =
        Arrays.stream(localPart.split("[._-]+"))
            .map(Identity::capitalize)
            .collect(Collectors.joining(" "));
    return new Identity(email, name);
  }

  private static String capitalize(String part) {
    if (part.isEmpty()) {
      return part;
    }
    return Character.toUpperCase(part.charAt(0)) + part.substring(1);
  }
}
