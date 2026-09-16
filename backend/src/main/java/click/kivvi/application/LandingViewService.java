package click.kivvi.application;

import click.kivvi.fixtures.LandingFixtures;
import org.springframework.stereotype.Service;

/**
 * Assembles the public marketing page's view-model — mirrors what {@code LandingContent} exposed to
 * {@code pages/landing.html.twig}.
 */
@Service
public class LandingViewService {

  public LandingView build() {
    return new LandingView(
        LandingFixtures.features().stream().map(this::toFeatureView).toList(),
        LandingFixtures.steps().stream().map(this::toStepView).toList(),
        LandingFixtures.trustPoints(),
        LandingFixtures.previewTiles().stream().map(this::toPreviewTileView).toList(),
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
