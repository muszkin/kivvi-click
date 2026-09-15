package click.kivvi.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.SpaDocumentService;
import click.kivvi.application.waitlist.ConfirmationOutcome;
import click.kivvi.application.waitlist.UnsubscribeOutcome;
import click.kivvi.application.waitlist.WaitlistConfirmationService;
import click.kivvi.infrastructure.IndexHtmlTemplate;
import click.kivvi.infrastructure.SessionPreferencesStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
 * The HTTP contract of the three pages a confirmation mail leads to, sliced at the web layer. What
 * the database does with a token is {@code WaitlistConfirmationServiceTest}'s and {@code
 * WaitlistConfirmationApiIT}'s subject; what is pinned here is the status code, the content type
 * and the {@code data-*} state the SPA reads before first paint.
 */
@WebMvcTest(WaitlistConfirmationController.class)
@Import({SpaDocumentService.class, IndexHtmlTemplate.class, SessionPreferencesStore.class})
class WaitlistConfirmationControllerTest {

  private static final String TOKEN = "a".repeat(64);

  @Autowired private MockMvc mvc;
  @Autowired private ScriptedConfirmationService confirmations;

  @BeforeEach
  void resetTheScript() {
    confirmations.reset();
  }

  @Test
  @DisplayName("a live link renders the confirmed state")
  void aLiveLinkRendersConfirmed() throws Exception {
    confirmations.answerWith(new ConfirmationOutcome.Confirmed());

    mvc.perform(get("/pl/waitlist/confirm/" + TOKEN))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-waitlist-confirm=\"ok\"")));
  }

  @Test
  @DisplayName("a spent link renders the already-confirmed state, not an error")
  void aSpentLinkRendersAlreadyConfirmed() throws Exception {
    confirmations.answerWith(new ConfirmationOutcome.AlreadyConfirmed());

    mvc.perform(get("/pl/waitlist/confirm/" + TOKEN))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-waitlist-confirm=\"already\"")));
  }

  @Test
  @DisplayName("an expired link renders the expired state and hands the token back for the resend")
  void anExpiredLinkCarriesTheTokenForward() throws Exception {
    confirmations.answerWith(new ConfirmationOutcome.Expired(TOKEN));

    mvc.perform(get("/pl/waitlist/confirm/" + TOKEN))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-waitlist-confirm=\"expired\"")))
        .andExpect(content().string(containsString("data-waitlist-token=\"" + TOKEN + "\"")));
  }

  @Test
  @DisplayName("a token nobody issued renders the unknown state")
  void anUnknownTokenRendersUnknown() throws Exception {
    confirmations.answerWith(new ConfirmationOutcome.Unknown());

    mvc.perform(get("/pl/waitlist/confirm/" + TOKEN))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-waitlist-confirm=\"unknown\"")))
        .andExpect(content().string(not(containsString("data-waitlist-token"))));
  }

  @Test
  @DisplayName("the confirming IP and user agent are what reaches the service")
  void theConfirmationProofIsPassedThrough() throws Exception {
    confirmations.answerWith(new ConfirmationOutcome.Confirmed());

    mvc.perform(get("/pl/waitlist/confirm/" + TOKEN).header("User-Agent", "Mozilla/5.0 (test)"))
        .andExpect(status().isOk());

    assertThat(confirmations.confirmations).hasSize(1);
    assertThat(confirmations.confirmations.getFirst().userAgent()).isEqualTo("Mozilla/5.0 (test)");
    assertThat(confirmations.confirmations.getFirst().ip()).isNotBlank();
  }

  @Test
  @DisplayName("a path that is not a token shape is not this controller's business at all")
  void aMalformedTokenIsNotMapped() throws Exception {
    mvc.perform(get("/pl/waitlist/confirm/nonsense")).andExpect(status().isNotFound());
    mvc.perform(get("/pl/waitlist/confirm/" + "a".repeat(63))).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("the resend redirects to the 'check your inbox' page in the posted locale")
  void theResendRedirects() throws Exception {
    mvc.perform(post("/pl/waitlist/confirm/resend").param("token", TOKEN))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/pl/waitlist/confirm/sent"));

    mvc.perform(post("/en/waitlist/confirm/resend").param("token", TOKEN))
        .andExpect(header().string("Location", "/en/waitlist/confirm/sent"));
  }

  @Test
  @DisplayName("a resend for a token nobody issued is answered exactly like one that worked")
  void anUnknownResendLooksIdentical() throws Exception {
    mvc.perform(post("/pl/waitlist/confirm/resend").param("token", "f".repeat(64)))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/pl/waitlist/confirm/sent"));

    mvc.perform(post("/pl/waitlist/confirm/resend"))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/pl/waitlist/confirm/sent"));
  }

  @Test
  @DisplayName("the unsubscribe link renders its own state, needing no session")
  void theUnsubscribeLinkRenders() throws Exception {
    confirmations.answerUnsubscribeWith(new UnsubscribeOutcome.Unsubscribed());

    mvc.perform(get("/pl/waitlist/unsubscribe/" + TOKEN))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-waitlist-unsubscribe=\"ok\"")));
  }

  @Test
  @DisplayName("an unsubscribe token nobody issued renders the unknown state")
  void anUnknownUnsubscribeTokenRendersUnknown() throws Exception {
    confirmations.answerUnsubscribeWith(new UnsubscribeOutcome.Unknown());

    mvc.perform(get("/pl/waitlist/unsubscribe/" + TOKEN))
        .andExpect(content().string(containsString("data-waitlist-unsubscribe=\"unknown\"")));
  }

  @Test
  @DisplayName(
      "every HTML response declares UTF-8 explicitly, or the servlet default mangles the Polish")
  void htmlResponsesDeclareUtf8() throws Exception {
    confirmations.answerWith(new ConfirmationOutcome.Confirmed());

    mvc.perform(get("/pl/waitlist/confirm/" + TOKEN))
        .andExpect(content().contentType("text/html;charset=UTF-8"));
  }

  @Test
  @DisplayName("an unsupported locale is not a confirmation endpoint at all")
  void anUnknownLocaleIsNotMapped() throws Exception {
    mvc.perform(get("/de/waitlist/confirm/" + TOKEN)).andExpect(status().isNotFound());
  }

  /**
   * Picked up automatically as a nested {@code @TestConfiguration}, which is why it is not also
   * listed in {@code @Import} above — naming it in both places registers it twice.
   */
  @TestConfiguration
  static class ScriptedBackend {

    @Bean
    ScriptedConfirmationService waitlistConfirmationService() {
      return new ScriptedConfirmationService();
    }
  }

  record Confirmation(String token, String ip, String userAgent) {}

  /** Answers whatever the test told it to, and remembers what it was asked. */
  static final class ScriptedConfirmationService extends WaitlistConfirmationService {

    private final List<Confirmation> confirmations = new ArrayList<>();
    private ConfirmationOutcome confirmOutcome = new ConfirmationOutcome.Unknown();
    private UnsubscribeOutcome unsubscribeOutcome = new UnsubscribeOutcome.Unknown();

    private ScriptedConfirmationService() {
      super(null, null, null, "unused");
    }

    private void reset() {
      confirmations.clear();
      confirmOutcome = new ConfirmationOutcome.Unknown();
      unsubscribeOutcome = new UnsubscribeOutcome.Unknown();
    }

    private void answerWith(ConfirmationOutcome outcome) {
      confirmOutcome = outcome;
    }

    private void answerUnsubscribeWith(UnsubscribeOutcome outcome) {
      unsubscribeOutcome = outcome;
    }

    @Override
    public ConfirmationOutcome confirm(String rawToken, Instant now, String ip, String userAgent) {
      confirmations.add(new Confirmation(rawToken, ip, userAgent));
      return confirmOutcome;
    }

    @Override
    public void resend(String rawToken, Instant now) {
      // Nothing to record: the point of the resend endpoint is that it answers identically
      // whatever it is given.
    }

    @Override
    public UnsubscribeOutcome unsubscribe(String rawToken, Instant now) {
      return unsubscribeOutcome;
    }
  }
}
