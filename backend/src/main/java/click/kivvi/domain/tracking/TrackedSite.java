package click.kivvi.domain.tracking;

import java.util.List;
import java.util.Optional;

/**
 * The three sites the workspace tracks, ported from {@code Workspace::SITES}. Drives the site
 * filter rail, the sample feed's round-robin site assignment, and the site-colour lookup a
 * collected event's {@code site} field resolves against before publishing.
 */
public enum TrackedSite {
  AUREA("aurea", "aureashop.pl", "#7a8763"),
  MLOT("mlot", "mlot-narzedzia.pl", "#a3825b"),
  POL("pol", "polna-bistro.pl", "#8b6f53");

  private static final List<TrackedSite> ALL = List.of(values());
  private static final String UNKNOWN_SITE_COLOR = "var(--fg-muted)";

  private final String id;
  private final String siteName;
  private final String color;

  TrackedSite(String id, String siteName, String color) {
    this.id = id;
    this.siteName = siteName;
    this.color = color;
  }

  public String id() {
    return id;
  }

  public String siteName() {
    return siteName;
  }

  public String color() {
    return color;
  }

  /** {@code Workspace::site($index)}: round-robins through the tracked sites by position. */
  public static TrackedSite byIndex(int index) {
    return ALL.get(Math.floorMod(index, ALL.size()));
  }

  public static Optional<TrackedSite> byId(String candidate) {
    return ALL.stream().filter(site -> site.id.equals(candidate)).findFirst();
  }

  /**
   * {@code EventIngestion::siteColor()}: resolves a collected event's free-text {@code site} name
   * against the tracked sites, falling back to the muted colour token for an untracked name.
   */
  public static String colorForSiteName(String candidateName) {
    return ALL.stream()
        .filter(site -> site.siteName.equals(candidateName))
        .map(TrackedSite::color)
        .findFirst()
        .orElse(UNKNOWN_SITE_COLOR);
  }

  public static List<TrackedSite> all() {
    return ALL;
  }
}
