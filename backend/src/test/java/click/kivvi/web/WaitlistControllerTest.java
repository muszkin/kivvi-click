package click.kivvi.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.SpaDocumentService;
import click.kivvi.application.waitlist.WaitlistRegistrar;
import click.kivvi.application.waitlist.WaitlistSignupService;
import click.kivvi.domain.waitlist.WaitlistSignup;
import click.kivvi.infrastructure.IndexHtmlTemplate;
import click.kivvi.infrastructure.SessionPreferencesStore;
import click.kivvi.infrastructure.config.MessageSourceConfig;
import click.kivvi.infrastructure.waitlist.SignupThrottle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The HTTP contract of {@code POST /{locale}/waitlist}, sliced at the web layer. Registration and
 * the limiter are in-memory stand-ins — what is under test here is the status code, the content
 * type and the state handed back to the SPA, not what Postgres does with the row or what the
 * confirmation mail says (those are {@code WaitlistApiIT}'s and {@code WaitlistConfirmationApiIT}'s
 * jobs).
 */
@WebMvcTest(WaitlistController.class)
@Import({
  WaitlistSignupService.class,
  SpaDocumentService.class,
  IndexHtmlTemplate.class,
  SessionPreferencesStore.class,
  MessageSourceConfig.class
})
class WaitlistControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private RecordingRegistrar registrar;
  @Autowired private CountingThrottle throttle;

  // The slice's context — and with it both stand-ins — is shared by every test in this class,
  // so each one starts from a clean counter rather than inheriting its neighbours' writes.
  @BeforeEach
  void resetBackends() {
    registrar.reset();
    throttle.reset();
  }

  @Test
  @DisplayName("a good signup redirects back to the landing page flagged as done")
  void aGoodSignupRedirects() throws Exception {
    mvc.perform(post("/pl/waitlist").param("email", "ala@sklep.pl").param("consent", "1"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/pl?waitlist=ok"));

    assertThat(registrar.registered).hasSize(1);
  }

  @Test
  @DisplayName("the redirect follows the locale the form was posted to")
  void theRedirectFollowsTheLocale() throws Exception {
    mvc.perform(post("/en/waitlist").param("email", "ala@sklep.pl").param("consent", "1"))
        .andExpect(header().string("Location", "/en?waitlist=ok"));
  }

  @Test
  @DisplayName(
      "a malformed address comes back as a 200 document carrying the message and the typed value")
  void aMalformedAddressRedisplaysTheForm() throws Exception {
    mvc.perform(post("/pl/waitlist").param("email", "nie-adres").param("consent", "1"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "data-waitlist-error=\"To nie wygląda na poprawny adres e-mail.\"")))
        .andExpect(content().string(containsString("data-waitlist-email=\"nie-adres\"")))
        .andExpect(content().string(containsString("data-waitlist-consent=\"true\"")));
  }

  @Test
  @DisplayName("an empty address comes back with the empty-address message")
  void anEmptyAddressRedisplaysTheForm() throws Exception {
    mvc.perform(post("/pl/waitlist").param("email", "").param("consent", "1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-waitlist-error=\"Podaj adres e-mail.\"")));
  }

  @Test
  @DisplayName("an unticked consent box comes back with the consent message and keeps the address")
  void missingConsentRedisplaysTheForm() throws Exception {
    mvc.perform(post("/pl/waitlist").param("email", "ala@sklep.pl"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "data-waitlist-error=\"Zaznacz zgodę, żebyśmy mogli się z Tobą"
                            + " skontaktować.\"")))
        .andExpect(content().string(containsString("data-waitlist-email=\"ala@sklep.pl\"")))
        .andExpect(content().string(containsString("data-waitlist-consent=\"false\"")));
  }

  @Test
  @DisplayName("the error message follows the locale the form was posted to")
  void theErrorMessageFollowsTheLocale() throws Exception {
    mvc.perform(post("/en/waitlist").param("email", "nie-adres").param("consent", "1"))
        .andExpect(
            content()
                .string(
                    containsString(
                        "data-waitlist-error=\"That does not look like a valid e-mail"
                            + " address.\"")));
  }

  @Test
  @DisplayName("an exhausted allowance answers 429 with the same document, and stores nothing")
  void anExhaustedAllowanceAnswers429() throws Exception {
    throttle.exhaustEverything();

    mvc.perform(post("/pl/waitlist").param("email", "ala@sklep.pl").param("consent", "1"))
        .andExpect(status().isTooManyRequests())
        .andExpect(
            content()
                .string(
                    containsString(
                        "data-waitlist-error=\"Za dużo prób z tego miejsca. Spróbuj ponownie za"
                            + " godzinę.\"")));

    assertThat(registrar.registered).isEmpty();
  }

  @Test
  @DisplayName("a filled honeypot is answered exactly like a success and stores nothing")
  void aFilledHoneypotLooksLikeSuccess() throws Exception {
    mvc.perform(
            post("/pl/waitlist")
                .param("email", "bot@spam.example")
                .param("consent", "1")
                .param("website", "http://spam.example"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/pl?waitlist=ok"));

    assertThat(registrar.registered).isEmpty();
  }

  @Test
  @DisplayName(
      "every HTML response declares UTF-8 explicitly, or the servlet default mangles the Polish")
  void htmlResponsesDeclareUtf8() throws Exception {
    mvc.perform(post("/pl/waitlist").param("email", "nie-adres").param("consent", "1"))
        .andExpect(content().contentType("text/html;charset=UTF-8"));
  }

  @Test
  @DisplayName("an unknown locale is not a waitlist endpoint at all")
  void anUnknownLocaleIsNotMapped() throws Exception {
    mvc.perform(post("/de/waitlist").param("email", "ala@sklep.pl").param("consent", "1"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("a refused submission never carries the login form's attributes")
  void aRefusedSubmissionCarriesOnlyItsOwnAttributes() throws Exception {
    mvc.perform(post("/pl/waitlist").param("email", "nie-adres").param("consent", "1"))
        .andExpect(content().string(not(containsString("data-login-error"))))
        .andExpect(content().string(not(containsString("data-last-username"))));
  }

  /**
   * Picked up automatically as a nested {@code @TestConfiguration}, which is why it is not also
   * listed in {@code @Import} above — naming it in both places registers it twice.
   */
  @TestConfiguration
  static class InMemoryWaitlistBackends {

    @Bean
    RecordingRegistrar waitlistRegistrar() {
      return new RecordingRegistrar();
    }

    @Bean
    CountingThrottle signupThrottle() {
      return new CountingThrottle();
    }
  }

  static final class RecordingRegistrar implements WaitlistRegistrar {

    private final List<WaitlistSignup> registered = new ArrayList<>();

    private void reset() {
      registered.clear();
    }

    @Override
    public boolean register(WaitlistSignup signup) {
      registered.add(signup);
      return true;
    }
  }

  static final class CountingThrottle implements SignupThrottle {

    private final Map<String, Integer> hits = new HashMap<>();
    private boolean everythingExhausted;

    private void reset() {
      hits.clear();
      everythingExhausted = false;
    }

    private void exhaustEverything() {
      everythingExhausted = true;
    }

    @Override
    public boolean tryAcquire(String bucketKey, int limit) {
      return !everythingExhausted && hits.merge(bucketKey, 1, Integer::sum) <= limit;
    }
  }
}
