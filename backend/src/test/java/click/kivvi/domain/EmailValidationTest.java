package click.kivvi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailValidationTest {

  @Test
  @DisplayName("B12 empty e-mail is rejected with 'Podaj adres e-mail.'")
  void emptyEmailIsRejected() {
    assertThat(EmailValidation.errorFor("")).contains(EmailValidation.EMPTY_MESSAGE);
    assertThat(EmailValidation.errorFor("   ")).contains(EmailValidation.EMPTY_MESSAGE);
    assertThat(EmailValidation.errorFor(null)).contains(EmailValidation.EMPTY_MESSAGE);
  }

  @Test
  @DisplayName("B13 malformed e-mail is rejected with 'poprawny adres e-mail'")
  void malformedEmailIsRejected() {
    assertThat(EmailValidation.errorFor("not-an-email"))
        .contains(EmailValidation.MALFORMED_MESSAGE);
  }

  @Test
  @DisplayName("a well-formed address passes validation")
  void validEmailPasses() {
    assertThat(EmailValidation.errorFor("anna@aureashop.pl")).isEmpty();
  }
}
