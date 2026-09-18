package click.kivvi.web;

import click.kivvi.application.FeedsViewService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.FeedsFixtures;
import click.kivvi.web.dto.FeedsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The product-feeds page's data: catalogue sources, sync state and the price-matching diagnostic —
 * mirrors {@code ProductFeedController}. The payload never varies by locale (the old page's fixture
 * text was never translated either); {@code locale} only gates the route to the two the panel
 * supports, exactly like every other {@code /api/v1/{locale}/...} endpoint.
 */
@RestController
public class FeedsController {

  private final FeedsViewService feedsViewService;

  public FeedsController(FeedsViewService feedsViewService) {
    this.feedsViewService = feedsViewService;
  }

  @GetMapping("/api/v1/{locale:pl|en}/feeds")
  public FeedsResponse feeds(@PathVariable String locale) {
    FeedsViewService.Payload payload =
        feedsViewService.build(SupportedLocale.fromCode(locale).orElseThrow());
    return new FeedsResponse(
        payload.kpis().stream().map(FeedsController::toKpi).toList(),
        payload.sources().stream().map(FeedsController::toSource).toList(),
        payload.feeds().stream().map(FeedsController::toFeed).toList(),
        payload.coverage().stream().map(FeedsController::toCoverageBar).toList(),
        payload.fallbackRules().stream().map(FeedsController::toFallbackRule).toList(),
        payload.mismatched());
  }

  private static FeedsResponse.Kpi toKpi(FeedsFixtures.Kpi kpi) {
    return new FeedsResponse.Kpi(
        kpi.label(), kpi.value(), kpi.unit(), kpi.delta(), kpi.dir(), kpi.deltaIcon());
  }

  private static FeedsResponse.Source toSource(FeedsFixtures.Source source) {
    return new FeedsResponse.Source(
        source.id(),
        source.letter(),
        source.color(),
        source.bg(),
        source.title(),
        source.sub(),
        source.recommended());
  }

  private static FeedsResponse.Feed toFeed(FeedsViewService.Feed feed) {
    return new FeedsResponse.Feed(
        feed.name(),
        feed.url(),
        feed.source(),
        feed.status(),
        feed.error(),
        feed.products(),
        feed.mapped(),
        feed.mappedPercent(),
        feed.mismatched(),
        feed.lastSync(),
        feed.schedule());
  }

  private static FeedsResponse.CoverageBar toCoverageBar(FeedsFixtures.CoverageBar bar) {
    return new FeedsResponse.CoverageBar(bar.label(), bar.pct(), bar.value(), bar.tone());
  }

  private static FeedsResponse.FallbackRule toFallbackRule(FeedsFixtures.FallbackRule rule) {
    return new FeedsResponse.FallbackRule(rule.text(), rule.field());
  }
}
