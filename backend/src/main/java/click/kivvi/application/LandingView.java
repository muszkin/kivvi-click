package click.kivvi.application;

import java.util.List;

/**
 * The view-model behind {@code GET /api/v1/{locale}/landing} — the copy arrays {@code
 * LandingContent} returned to {@code pages/landing.html.twig}, in the exact shape the SPA response
 * contract defines. None of these fields ever ran through the old stack's translator (the {@code
 * |trans} calls in the Twig template are for static page copy that never reached {@code
 * LandingContent}), so the payload is identical for every supported locale — the {@code locale}
 * path segment exists only to gate the request the same way every other endpoint does.
 */
public record LandingView(
    List<FeatureView> features,
    List<StepView> steps,
    List<PlanView> plans,
    List<String> trustPoints,
    List<PreviewTileView> previewTiles,
    List<Double> previewSeries) {

  public record FeatureView(String icon, String title, String body) {}

  public record StepView(String number, String title, String body) {}

  /** {@code unit} carries its own leading space, mirroring {@code LandingContent::plans()}. */
  public record PlanView(
      String tier,
      String price,
      String unit,
      List<String> items,
      String cta,
      boolean featured,
      String badge) {}

  public record PreviewTileView(String label, String value, String unit) {}
}
