package click.kivvi.domain;

import java.util.List;
import java.util.Map;

/**
 * Sidebar navigation structure and the current-section rule: a detail route (a customer profile, an
 * automation editor, …) keeps its index entry highlighted. Mirrors {@code Navigation} — labels are
 * translated by the caller from {@code entry.route()}, using the same {@code "nav." + route} key
 * convention as the old stack's label map.
 */
public final class NavigationCatalog {

  /** One sidebar row. {@code badge} is {@code null} when the row carries none. */
  public record Entry(String route, String icon, String badge) {
    public Entry(String route, String icon) {
      this(route, icon, null);
    }
  }

  /** A labelled group of rows, keyed by its own translation key {@code "nav." + sectionKey}. */
  public record Group(String sectionKey, List<Entry> entries) {}

  private static final List<Group> GROUPS =
      List.of(
          new Group(
              "main",
              List.of(
                  new Entry("dashboard", "dashboard"),
                  new Entry("events", "activity", "·"),
                  new Entry("customers", "users"))),
          new Group(
              "automate",
              List.of(
                  new Entry("automations", "bolt"),
                  new Entry("campaigns", "mail"),
                  new Entry("popups", "layout"))),
          new Group("data", List.of(new Entry("feeds", "cart"), new Entry("import", "upload"))),
          new Group("config", List.of(new Entry("settings", "settings"))));

  private static final Map<String, String> INDEX_OF_DETAIL =
      Map.of(
          "customer_show", "customers",
          "automation_edit", "automations",
          "automation_new", "automations",
          "email_edit", "campaigns",
          "email_new", "campaigns",
          "popup_edit", "popups",
          "popup_new", "popups");

  private NavigationCatalog() {}

  public static List<Group> groups() {
    return GROUPS;
  }

  /** A detail route reports the section it belongs under; every other route reports itself. */
  public static String currentSection(String route) {
    return INDEX_OF_DETAIL.getOrDefault(route, route);
  }
}
