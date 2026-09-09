package click.kivvi.application;

import java.util.List;

/**
 * The view-model behind {@code GET /api/v1/{locale}/shell} — every field the sidebar, topbar and
 * page chrome need, in the exact shape the SPA response contract defines. Serialized as-is: field
 * names are the wire JSON keys.
 */
public record ShellView(
    String locale,
    String theme,
    String sidebar,
    List<NavGroupView> navGroups,
    String currentSection,
    String crumb,
    WorkspaceView workspace,
    UserView user) {

  public record NavGroupView(String label, List<NavItemView> items) {}

  public record NavItemView(String label, String icon, String route, String href, String badge) {}

  public record WorkspaceView(String name, String meta, String mark) {}

  public record UserView(String name, String email) {}
}
