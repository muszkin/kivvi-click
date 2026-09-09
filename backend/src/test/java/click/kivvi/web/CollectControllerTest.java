package click.kivvi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import click.kivvi.application.tracking.EventIngestionService;
import click.kivvi.domain.tracking.TrackedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sliced coverage for {@code POST /collect}'s wire contract: the four Polish error messages, the
 * 202/200 split, and the JSON-body edge cases {@link TrackedEvent} itself never sees (empty body, a
 * scalar JSON value, a JSON array). {@link EventIngestionService} is stubbed — no DB, no hub — a
 * subclass overriding {@code ingest} in place of a mocking library the project has no dependency
 * on, mirroring the old stack's own {@code MockHub} substitution one layer up.
 */
@WebMvcTest(CollectController.class)
@Import(CollectControllerTest.Stubs.class)
class CollectControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private StubEventIngestionService ingestionService;

  static class StubEventIngestionService extends EventIngestionService {
    private boolean acceptNext = true;
    private TrackedEvent lastIngested;

    StubEventIngestionService() {
      super(null, null, null);
    }

    @Override
    public boolean ingest(TrackedEvent event) {
      lastIngested = event;
      return acceptNext;
    }
  }

  @TestConfiguration
  static class Stubs {
    @Bean
    StubEventIngestionService eventIngestionService() {
      return new StubEventIngestionService();
    }
  }

  @Test
  @DisplayName("B19 a new event is accepted with 202 {\"status\":\"accepted\"}")
  void acceptedEventAnswers202() throws Exception {
    ingestionService.acceptNext = true;

    mvc.perform(
            post("/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_id\":\"evt-1\",\"type\":\"purchase\"}"))
        .andExpect(status().isAccepted())
        .andExpect(content().json("{\"status\":\"accepted\"}"));
  }

  @Test
  @DisplayName("B19 a duplicate event is answered 200 {\"status\":\"duplicate\"}")
  void duplicateEventAnswers200() throws Exception {
    ingestionService.acceptNext = false;

    mvc.perform(
            post("/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_id\":\"evt-1\",\"type\":\"purchase\"}"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"status\":\"duplicate\"}"));
  }

  @Test
  @DisplayName("B15 a missing idempotency_id answers 400 with the exact Polish message")
  void missingIdempotencyIdAnswers400() throws Exception {
    mvc.perform(
            post("/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"purchase\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Pole „idempotency_id” jest wymagane."));
  }

  @Test
  @DisplayName("B16 an unknown type answers 400 with the exact Polish message")
  void unknownTypeAnswers400() throws Exception {
    mvc.perform(
            post("/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_id\":\"evt-1\",\"type\":\"teleport\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Nieznany typ zdarzenia „teleport”."));
  }

  @Test
  @DisplayName("an invalid occurred_at answers 400 with the exact Polish message")
  void invalidOccurredAtAnswers400() throws Exception {
    mvc.perform(
            post("/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"idempotency_id\":\"evt-1\",\"type\":\"purchase\","
                        + "\"occurred_at\":\"not-a-date\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Pole „occurred_at” nie jest poprawną datą."));
  }

  @Test
  @DisplayName("an empty body answers 400 with 'Oczekiwano obiektu JSON.'")
  void emptyBodyAnswers400() throws Exception {
    mvc.perform(post("/collect").contentType(MediaType.APPLICATION_JSON).content(""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Oczekiwano obiektu JSON."));
  }

  @Test
  @DisplayName("a scalar JSON body answers 400 with 'Oczekiwano obiektu JSON.'")
  void scalarJsonBodyAnswers400() throws Exception {
    mvc.perform(post("/collect").contentType(MediaType.APPLICATION_JSON).content("\"oops\""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Oczekiwano obiektu JSON."));
  }

  @Test
  @DisplayName("malformed JSON answers 400 with 'Oczekiwano obiektu JSON.'")
  void malformedJsonAnswers400() throws Exception {
    mvc.perform(post("/collect").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Oczekiwano obiektu JSON."));
  }

  @Test
  @DisplayName(
      "a JSON array body is accepted as an empty-fields payload, matching PHP's is_array()")
  void jsonArrayBodyReadsAsAnEmptyPayload() throws Exception {
    mvc.perform(post("/collect").contentType(MediaType.APPLICATION_JSON).content("[1,2,3]"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Pole „idempotency_id” jest wymagane."));
  }
}
