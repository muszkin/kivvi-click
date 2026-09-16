package click.kivvi.application;

import java.util.List;

/**
 * The view-model behind {@code GET /api/v1/{locale}/landing} — the copy arrays {@code
 * LandingContent} returned to {@code pages/landing.html.twig}, in the exact shape the SPA response
 * contract defines.
 *
 * <p>The old stack never ran these fields through its translator, so the port served the same
 * Polish payload for every locale. PIO-117 translated them: the {@code locale} path segment now
 * picks the language of every string here. The shape is unchanged — same fields, same counts — only
 * the words differ.
 */
public record LandingView(
    List<FeatureView> features,
    List<StepView> steps,
    List<String> trustPoints,
    List<PreviewTileView> previewTiles,
    List<Double> previewSeries) {

  public record FeatureView(String icon, String title, String body) {}

  public record StepView(String number, String title, String body) {}

  public record PreviewTileView(String label, String value, String unit) {}
}
