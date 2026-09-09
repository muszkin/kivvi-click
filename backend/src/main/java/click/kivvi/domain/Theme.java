package click.kivvi.domain;

/**
 * Colour theme preference. Mirrors {@code PanelPreferences::THEME_*} — an unknown or missing stored
 * value always falls back to {@link #LIGHT}, never an error.
 */
public enum Theme {
  LIGHT("light"),
  DARK("dark");

  private final String value;

  Theme(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static Theme fromValueOrDefault(String candidate) {
    return DARK.value.equals(candidate) ? DARK : LIGHT;
  }
}
