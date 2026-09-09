package click.kivvi.domain;

/**
 * Sidebar collapse state. Mirrors {@code PanelPreferences::SIDEBAR_*} — an unknown or missing
 * stored value always falls back to {@link #EXPANDED}, never an error.
 */
public enum SidebarState {
  EXPANDED("expanded"),
  COLLAPSED("collapsed");

  private final String value;

  SidebarState(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static SidebarState fromValueOrDefault(String candidate) {
    return COLLAPSED.value.equals(candidate) ? COLLAPSED : EXPANDED;
  }
}
