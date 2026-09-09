package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/**
 * Supplementary coverage for the {@code server.forward-headers-strategy: native} switch
 * (application.yml). MockMvc never opens a real Tomcat connector — it feeds mock request/response
 * objects straight to the {@code DispatcherServlet} — so it cannot exercise Tomcat's {@code
 * RemoteIpValve}, the mechanism {@code native} actually configures; that only runs live, against
 * the real compose stack, under RR-1/CUT-1's curl and Playwright checks (README.md's "Next stack"
 * runbook section). What this test DOES prove, on every Docker-less build: {@link LoginController}
 * never derives its redirect {@code Location} from the request's scheme or host, so an {@code
 * X-Forwarded-Proto}/{@code X-Forwarded-Host} pair — trusted or spoofed — can never smuggle a
 * different origin into the {@code 302}. Behind {@code https://kivvi.click}'s TLS-terminating proxy
 * the client always receives the same site-relative path this test pins.
 */
@WebMvcTest(LoginController.class)
@Import({
  LoginService.class,
  SpaDocumentService.class,
  IndexHtmlTemplate.class,
  SessionIdentityStore.class,
  SessionPreferencesStore.class
})
class LoginControllerForwardedHeaderTest {

  @Autowired private MockMvc mvc;

  @Test
  @DisplayName("redirect Location ignores X-Forwarded-Proto/X-Forwarded-Host entirely")
  void redirectLocationIsUnaffectedByForwardedHeaders() throws Exception {
    mvc.perform(
            post("/pl/login")
                .header("X-Forwarded-Proto", "http")
                .header("X-Forwarded-Host", "attacker.example")
                .param("_username", "anna@aureashop.pl")
                .param("_password", "haslo-testowe"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/pl/dashboard"));
  }
}
