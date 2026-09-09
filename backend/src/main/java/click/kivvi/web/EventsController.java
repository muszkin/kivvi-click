package click.kivvi.web;

import click.kivvi.application.EventsViewService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.web.dto.EventsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The event log's data: the sample feed, its type/site/range filters and the Mercure topic the SPA
 * subscribes to — mirrors {@code EventStreamController}.
 */
@RestController
public class EventsController {

  private final EventsViewService eventsViewService;

  public EventsController(EventsViewService eventsViewService) {
    this.eventsViewService = eventsViewService;
  }

  @GetMapping("/api/v1/{locale:pl|en}/events")
  public EventsResponse events(
      @PathVariable String locale,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String site,
      @RequestParam(required = false) String range) {
    SupportedLocale resolvedLocale = SupportedLocale.fromCode(locale).orElseThrow();
    EventsViewService.Payload payload = eventsViewService.build(resolvedLocale, type, site, range);
    return new EventsResponse(
        payload.typeFilters().stream().map(EventsController::toFilter).toList(),
        payload.siteFilters().stream().map(EventsController::toFilter).toList(),
        payload.ranges().stream().map(EventsController::toRange).toList(),
        payload.events().stream().map(EventsController::toRow).toList(),
        payload.total(),
        payload.shown(),
        payload.mercureTopic());
  }

  private static EventsResponse.Filter toFilter(EventsViewService.FilterView filter) {
    return new EventsResponse.Filter(
        filter.label(),
        filter.icon(),
        filter.dotColor(),
        filter.active(),
        filter.action(),
        filter.payload());
  }

  private static EventsResponse.Range toRange(EventsViewService.RangeView range) {
    return new EventsResponse.Range(range.value(), range.label(), range.active());
  }

  private static EventsResponse.Row toRow(EventsViewService.RowView row) {
    return new EventsResponse.Row(
        row.time(),
        row.typeIcon(),
        row.tone(),
        row.type(),
        row.detail(),
        row.customerName(),
        row.customerId(),
        row.siteName(),
        row.siteColor());
  }
}
