package click.kivvi.web;

import click.kivvi.application.CustomersViewService;
import click.kivvi.application.SpaDocumentService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.AutomationsFixtures;
import click.kivvi.fixtures.CustomersFixtures;
import click.kivvi.web.dto.CustomerDetailResponse;
import click.kivvi.web.dto.CustomerListResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer index and 360 profile — mirrors {@code CustomerController}.
 *
 * <p>The document route for one customer ({@code GET /{locale}/customers/{id}}) is handled here
 * rather than by {@link SpaDocumentController}'s generic {@code /{locale}/**} route table, because
 * it is the only panel route whose 404-ness depends on more than the URL's shape: an unknown id is
 * a 404 document, exactly like the old stack's {@code createNotFoundException}. Spring's handler
 * mapping always prefers this method's exact-shape pattern over SpaDocumentController's wildcard
 * for any request that reaches both, so no ambiguity results from the two controllers overlapping
 * on this one path.
 */
@RestController
public class CustomersController {

  // Mirrors SpaDocumentController's own reasoning: an unspecified response charset defaults to
  // ISO-8859-1 in the servlet spec, which would mangle this page's Polish text.
  private static final MediaType TEXT_HTML_UTF8 =
      new MediaType("text", "html", StandardCharsets.UTF_8);

  private static final String NOT_FOUND_DOCUMENT =
      "<!doctype html><html lang=\"pl\"><head><meta charset=\"utf-8\"><title>404</title></head>"
          + "<body><h1>404</h1><p>Nie ma takiego klienta.</p></body></html>";

  private final CustomersViewService customersViewService;
  private final SpaDocumentService spaDocumentService;

  public CustomersController(
      CustomersViewService customersViewService, SpaDocumentService spaDocumentService) {
    this.customersViewService = customersViewService;
    this.spaDocumentService = spaDocumentService;
  }

  @GetMapping("/api/v1/{locale:pl|en}/customers")
  public CustomerListResponse list(
      @PathVariable String locale, @RequestParam(defaultValue = "1") int page) {
    CustomersViewService.ListPayload payload =
        customersViewService.list(
            SupportedLocale.fromCode(locale).orElseThrow(), page, Instant.now());
    return new CustomerListResponse(
        payload.subtitle(),
        payload.segments().stream().map(CustomersController::toSegment).toList(),
        payload.rows().stream().map(CustomersController::toRow).toList(),
        payload.page(),
        payload.pages());
  }

  @GetMapping("/api/v1/{locale:pl|en}/customers/{id}")
  public ResponseEntity<CustomerDetailResponse> detail(
      @PathVariable String locale, @PathVariable String id) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    try {
      return ResponseEntity.ok(
          toDetailResponse(customersViewService.detail(supported, id, Instant.now())));
    } catch (NoSuchElementException unknownCustomer) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/{locale:pl|en}/customers/{id:c_\\d+}")
  public ResponseEntity<String> document(
      @PathVariable String locale, @PathVariable String id, HttpServletRequest request) {
    SupportedLocale supported = SupportedLocale.fromCode(locale).orElseThrow();
    try {
      CustomersFixtures.byId(supported, id);
    } catch (NoSuchElementException unknownCustomer) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .contentType(TEXT_HTML_UTF8)
          .body(NOT_FOUND_DOCUMENT);
    }
    // A read: never force a session into existence just to render a page.
    String document = spaDocumentService.render(request.getSession(false), supported);
    return ResponseEntity.ok().contentType(TEXT_HTML_UTF8).body(document);
  }

  private static CustomerListResponse.Segment toSegment(CustomersViewService.SegmentTile tile) {
    return new CustomerListResponse.Segment(
        tile.label(), tile.icon(), tile.count(), tile.active(), tile.action(), tile.payload());
  }

  private static CustomerListResponse.Row toRow(CustomersViewService.CustomerRow row) {
    return new CustomerListResponse.Row(
        row.id(),
        row.name(),
        row.initials(),
        row.email(),
        new CustomerListResponse.RowSegment(row.segment().label(), row.segment().tone()),
        row.orders(),
        row.revenue(),
        row.lastSeen());
  }

  private static CustomerDetailResponse toDetailResponse(
      CustomersViewService.DetailPayload payload) {
    return new CustomerDetailResponse(
        toCustomer(payload.customer()),
        payload.profileSub(),
        payload.facts().stream().map(CustomersController::toFact).toList(),
        payload.automations().stream().map(CustomersController::toAutomation).toList(),
        payload.tabs().stream().map(CustomersController::toTab).toList(),
        payload.scores().stream().map(CustomersController::toScore).toList(),
        payload.timeline().stream().map(CustomersController::toTimelineEntry).toList());
  }

  private static CustomerDetailResponse.Customer toCustomer(
      CustomersViewService.CustomerDetail customer) {
    return new CustomerDetailResponse.Customer(
        customer.id(),
        customer.name(),
        customer.initials(),
        customer.email(),
        customer.tags().stream()
            .map(tag -> new CustomerDetailResponse.Tag(tag.label(), tag.tone()))
            .toList());
  }

  private static CustomerDetailResponse.Fact toFact(CustomersViewService.Fact fact) {
    return new CustomerDetailResponse.Fact(fact.label(), fact.value(), fact.small());
  }

  private static CustomerDetailResponse.Automation toAutomation(
      AutomationsFixtures.ActiveAutomation automation) {
    return new CustomerDetailResponse.Automation(automation.name());
  }

  private static CustomerDetailResponse.Tab toTab(CustomersViewService.Tab tab) {
    return new CustomerDetailResponse.Tab(tab.label(), tab.active());
  }

  private static CustomerDetailResponse.Score toScore(CustomersViewService.Score score) {
    return new CustomerDetailResponse.Score(
        score.label(), score.value(), score.unit(), score.delta(), score.dir(), score.deltaIcon());
  }

  private static CustomerDetailResponse.TimelineEntry toTimelineEntry(
      CustomersViewService.TimelineEntry entry) {
    return new CustomerDetailResponse.TimelineEntry(
        entry.time(), entry.title(), entry.detail(), entry.icon());
  }
}
