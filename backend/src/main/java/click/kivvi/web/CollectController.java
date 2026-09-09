package click.kivvi.web;

import click.kivvi.application.tracking.EventIngestionService;
import click.kivvi.domain.tracking.InvalidEventPayload;
import click.kivvi.domain.tracking.TrackedEvent;
import click.kivvi.web.dto.EventCollectResponse;
import click.kivvi.web.dto.EventErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Ingestion endpoint for the tracking script — mirrors {@code EventIngestionController}. Answers
 * 202 for a newly stored event and 200 for a duplicate, so a retrying tracker can tell "we already
 * have it" from "we took it" without treating either as an error.
 */
@RestController
public class CollectController {

  private static final String EXPECTED_JSON_OBJECT_MESSAGE = "Oczekiwano obiektu JSON.";

  private final EventIngestionService eventIngestionService;
  private final ObjectMapper objectMapper;

  public CollectController(EventIngestionService eventIngestionService, ObjectMapper objectMapper) {
    this.eventIngestionService = eventIngestionService;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/collect")
  public ResponseEntity<?> collect(HttpServletRequest request) throws IOException {
    try {
      TrackedEvent event = TrackedEvent.fromPayload(readPayload(request));
      boolean accepted = eventIngestionService.ingest(event);
      HttpStatus status = accepted ? HttpStatus.ACCEPTED : HttpStatus.OK;
      String responseStatus =
          accepted ? EventCollectResponse.ACCEPTED : EventCollectResponse.DUPLICATE;
      return ResponseEntity.status(status).body(new EventCollectResponse(responseStatus));
    } catch (InvalidEventPayload exception) {
      return ResponseEntity.badRequest().body(new EventErrorResponse(exception.getMessage()));
    }
  }

  /**
   * @throws InvalidEventPayload with {@link #EXPECTED_JSON_OBJECT_MESSAGE} for an empty, malformed
   *     or non-object/non-array JSON body — mirrors {@code json_decode(...)} followed by {@code
   *     !is_array($payload)}. A JSON array body (like a JSON object under PHP's {@code
   *     json_decode(..., true)}) is accepted as an array-typed PHP value too, so it reaches {@code
   *     TrackedEvent::fromPayload} with every field reading as absent, not rejected here.
   */
  private Map<String, Object> readPayload(HttpServletRequest request) throws IOException {
    JsonNode root;
    try {
      root = objectMapper.readTree(request.getInputStream());
    } catch (JacksonException exception) {
      throw new InvalidEventPayload(EXPECTED_JSON_OBJECT_MESSAGE, exception);
    }
    if (root == null || root.isMissingNode() || root.isNull()) {
      throw new InvalidEventPayload(EXPECTED_JSON_OBJECT_MESSAGE);
    }
    if (root.isArray()) {
      return Map.of();
    }
    if (!root.isObject()) {
      throw new InvalidEventPayload(EXPECTED_JSON_OBJECT_MESSAGE);
    }
    return objectMapper.treeToValue(root, new TypeReference<Map<String, Object>>() {});
  }
}
