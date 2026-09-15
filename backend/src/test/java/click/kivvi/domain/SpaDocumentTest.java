package click.kivvi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.SpaDocument.Attributes;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpaDocumentTest {

  private static final String TEMPLATE =
      "<!doctype html><html lang=\"pl\"><head></head><body></body></html>";

  @Test
  void injectsLocaleThemeAndSidebarOnTheHtmlTag() {
    String result = SpaDocument.inject(TEMPLATE, new Attributes("en", "dark", "collapsed"));

    assertThat(result)
        .contains("<html lang=\"en\" data-theme=\"dark\" data-sidebar=\"collapsed\">");
    assertThat(result).doesNotContain("data-login-error");
  }

  @Test
  void injectsLoginErrorAndLastUsernameWhenPresent() {
    String result =
        SpaDocument.inject(
            TEMPLATE,
            new Attributes(
                "pl",
                "light",
                "expanded",
                loginAttributes("To nie wygląda na poprawny adres e-mail.", "not-an-email")));

    assertThat(result)
        .contains("data-login-error=\"To nie wygląda na poprawny adres e-mail.\"")
        .contains("data-last-username=\"not-an-email\"");
  }

  @Test
  void escapesAttributeValues() {
    String result =
        SpaDocument.inject(
            TEMPLATE,
            new Attributes(
                "pl", "light", "expanded", Map.of("login-error", "a \"quote\" & <tag>")));

    assertThat(result).contains("data-login-error=\"a &quot;quote&quot; &amp; &lt;tag&gt;\"");
  }

  @Test
  @DisplayName(
      "PIO-70 regression: swapping the positional loginError/lastUsername pair for an attribute"
          + " map must not move a single byte of the HTML either path already produced")
  void theExistingPathsRenderExactlyTheHtmlTheyRenderedBefore() {
    String plainDocument = SpaDocument.inject(TEMPLATE, new Attributes("pl", "light", "expanded"));
    String failedLoginDocument =
        SpaDocument.inject(
            TEMPLATE,
            new Attributes(
                "pl", "light", "expanded", loginAttributes("Podaj adres e-mail.", "ala@sklep.pl")));

    // The two literals below are what the record produced before this refactor, typed out
    // rather than derived, so the assertion cannot drift along with the implementation.
    assertThat(plainDocument)
        .isEqualTo(
            "<!doctype html><html lang=\"pl\" data-theme=\"light\" data-sidebar=\"expanded\">"
                + "<head></head><body></body></html>");
    assertThat(failedLoginDocument)
        .isEqualTo(
            "<!doctype html><html lang=\"pl\" data-theme=\"light\" data-sidebar=\"expanded\""
                + " data-login-error=\"Podaj adres e-mail.\""
                + " data-last-username=\"ala@sklep.pl\">"
                + "<head></head><body></body></html>");
  }

  @Test
  @DisplayName("attributes render in insertion order, whatever the attribute names happen to be")
  void attributesRenderInInsertionOrder() {
    Map<String, String> waitlistAttributes = new LinkedHashMap<>();
    waitlistAttributes.put("waitlist-error", "Podaj adres e-mail.");
    waitlistAttributes.put("waitlist-email", "");
    waitlistAttributes.put("waitlist-consent", "false");

    String result =
        SpaDocument.inject(TEMPLATE, new Attributes("pl", "light", "expanded", waitlistAttributes));

    assertThat(result)
        .contains(
            " data-waitlist-error=\"Podaj adres e-mail.\""
                + " data-waitlist-email=\"\""
                + " data-waitlist-consent=\"false\">");
  }

  @Test
  @DisplayName("the attribute map is defensively copied, so a later edit cannot rewrite a document")
  void theAttributeMapIsCopied() {
    Map<String, String> mutable = new LinkedHashMap<>();
    mutable.put("login-error", "Podaj adres e-mail.");
    Attributes attributes = new Attributes("pl", "light", "expanded", mutable);

    mutable.put("login-error", "something else entirely");

    assertThat(SpaDocument.inject(TEMPLATE, attributes))
        .contains("data-login-error=\"Podaj adres e-mail.\"");
  }

  private static Map<String, String> loginAttributes(String error, String lastUsername) {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("login-error", error);
    attributes.put("last-username", lastUsername);
    return attributes;
  }
}
