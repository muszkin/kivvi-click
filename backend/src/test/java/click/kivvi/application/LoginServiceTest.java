package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.EmailValidation;
import click.kivvi.infrastructure.SessionIdentityStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class LoginServiceTest {

  private final LoginService loginService = new LoginService(new SessionIdentityStore());

  @Test
  @DisplayName("B11 valid e-mail sign-in shows the e-mail in the sidebar footer")
  void successfulSignInEstablishesTheIdentity() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    var identityStore = new SessionIdentityStore();

    assertThat(loginService.attemptSignIn(request, "anna@aureashop.pl")).isEmpty();
    assertThat(identityStore.current(request.getSession(false)).email())
        .isEqualTo("anna@aureashop.pl");
    assertThat(identityStore.current(request.getSession(false)).name()).isEqualTo("Anna");
  }

  @Test
  @DisplayName("a successful sign-in creates a session; nothing was needed before it")
  void successfulSignInCreatesASession() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThat(request.getSession(false)).isNull();
    loginService.attemptSignIn(request, "anna@aureashop.pl");
    assertThat(request.getSession(false)).isNotNull();
  }

  @Test
  @DisplayName("B12 empty e-mail is rejected with 'Podaj adres e-mail.' and creates no session")
  void emptyEmailIsRejected() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThat(loginService.attemptSignIn(request, "")).contains(EmailValidation.EMPTY_MESSAGE);
    assertThat(request.getSession(false)).as("a rejected sign-in writes nothing").isNull();
  }

  @Test
  @DisplayName("B13 malformed e-mail is rejected with 'poprawny adres e-mail'")
  void malformedEmailIsRejected() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThat(loginService.attemptSignIn(request, "not-an-email"))
        .contains(EmailValidation.MALFORMED_MESSAGE);
  }

  @Test
  @DisplayName("B14 sign-out restores the default identity")
  void signOutRestoresTheDefaultIdentity() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    var identityStore = new SessionIdentityStore();
    loginService.attemptSignIn(request, "anna@aureashop.pl");

    loginService.signOut(request);

    assertThat(identityStore.current(request.getSession(false)).email())
        .isEqualTo("maciej@aureashop.pl");
    assertThat(identityStore.current(request.getSession(false)).name())
        .isEqualTo("Maciej Kowalczyk");
  }

  @Test
  @DisplayName("signing out with no prior session is a safe no-op")
  void signOutWithNoSessionIsANoOp() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    loginService.signOut(request);

    assertThat(request.getSession(false)).isNull();
  }
}
