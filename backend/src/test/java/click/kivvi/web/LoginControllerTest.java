package click.kivvi.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.LoginService;
import click.kivvi.application.SpaDocumentService;
import click.kivvi.infrastructure.IndexHtmlTemplate;
import click.kivvi.infrastructure.SessionIdentityStore;
import click.kivvi.infrastructure.SessionPreferencesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoginController.class)
@Import({
  LoginService.class,
  SpaDocumentService.class,
  IndexHtmlTemplate.class,
  SessionIdentityStore.class,
  SessionPreferencesStore.class
})
class LoginControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("B11 valid e-mail sign-in redirects to the dashboard")
  void successfulSignInRedirectsToTheDashboard() throws Exception {
    mvc.perform(
            post("/pl/login")
                .param("_username", "anna@aureashop.pl")
                .param("_password", "haslo-testowe"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/pl/dashboard"));
  }

  @Test
  @DisplayName("B12 empty e-mail is rejected with 200 and the Polish message")
  void emptyEmailIsRejected() throws Exception {
    mvc.perform(post("/pl/login").param("_username", "").param("_password", ""))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-login-error=\"Podaj adres e-mail.\"")));
  }

  @Test
  @DisplayName("B13 malformed e-mail is rejected with 200 and the Polish message")
  void malformedEmailIsRejected() throws Exception {
    mvc.perform(post("/pl/login").param("_username", "not-an-email"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("To nie wygląda na poprawny adres e-mail.")))
        .andExpect(content().string(containsString("data-last-username=\"not-an-email\"")));
  }

  @Test
  @DisplayName("B14 sign-out redirects to login")
  void signOutRedirectsToLogin() throws Exception {
    mvc.perform(post("/pl/logout"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/pl/login"));
  }
}
