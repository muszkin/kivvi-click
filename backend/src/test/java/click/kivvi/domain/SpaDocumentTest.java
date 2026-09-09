package click.kivvi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.SpaDocument.Attributes;
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
                "To nie wygląda na poprawny adres e-mail.",
                "not-an-email"));

    assertThat(result)
        .contains("data-login-error=\"To nie wygląda na poprawny adres e-mail.\"")
        .contains("data-last-username=\"not-an-email\"");
  }

  @Test
  void escapesAttributeValues() {
    String result =
        SpaDocument.inject(
            TEMPLATE, new Attributes("pl", "light", "expanded", "a \"quote\" & <tag>", null));

    assertThat(result).contains("data-login-error=\"a &quot;quote&quot; &amp; &lt;tag&gt;\"");
  }
}
