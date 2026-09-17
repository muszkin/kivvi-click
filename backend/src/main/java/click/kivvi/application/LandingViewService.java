package click.kivvi.application;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.LandingFixtures;
import org.springframework.stereotype.Service;

/**
 * Assembles the public marketing page's view-model — mirrors what {@code LandingContent} exposed to
 * {@code pages/landing.html.twig}, in the language the page is being read in (PIO-117).
 */
@Service
public class LandingViewService {

  public LandingView build(SupportedLocale locale) {
    return new LandingView(
        LandingFixtures.features(locale).stream().map(this::toFeatureView).toList(),
        LandingFixtures.steps(locale).stream().map(this::toStepView).toList(),
        LandingFixtures.trustPoints(locale),
        LandingFixtures.previewTiles(locale).stream().map(this::toPreviewTileView).toList(),
        LandingFixtures.previewSeries());
  }

  private LandingView.FeatureView toFeatureView(LandingFixtures.Feature feature) {
    return new LandingView.FeatureView(feature.icon(), feature.title(), feature.body());
  }

  private LandingView.StepView toStepView(LandingFixtures.Step step) {
    return new LandingView.StepView(step.number(), step.title(), step.body());
  }

  private LandingView.PreviewTileView toPreviewTileView(LandingFixtures.PreviewTile tile) {
    return new LandingView.PreviewTileView(tile.label(), tile.value(), tile.unit());
  }
}
