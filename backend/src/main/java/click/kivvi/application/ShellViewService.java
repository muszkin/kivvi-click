package click.kivvi.application;

import click.kivvi.application.ShellView.NavGroupView;
import click.kivvi.application.ShellView.NavItemView;
import click.kivvi.application.ShellView.UserView;
import click.kivvi.application.ShellView.WorkspaceView;
import click.kivvi.domain.Identity;
import click.kivvi.domain.NavigationCatalog;
import click.kivvi.domain.SidebarState;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.Theme;
import click.kivvi.fixtures.ShellFixtures;
import click.kivvi.infrastructure.NavigationLabels;
import click.kivvi.infrastructure.SessionIdentityStore;
import click.kivvi.infrastructure.SessionPreferencesStore;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Assembles the panel shell view-model: navigation, workspace, identity and view preferences, all
 * scoped to the requesting locale and session — mirrors what {@code PanelContext} exposes to every
 * Twig page.
 */
@Service
public class ShellViewService {

  private final SessionPreferencesStore preferences;
  private final SessionIdentityStore identity;
  private final NavigationLabels labels;

  public ShellViewService(
      SessionPreferencesStore preferences, SessionIdentityStore identity, NavigationLabels labels) {
    this.preferences = preferences;
    this.identity = identity;
    this.labels = labels;
  }

  /**
   * @param currentRoute the SPA's current route name (query param {@code route}), mirroring {@code
   *     Navigation::currentSection()}'s use of the matched Symfony route name; blank when the
   *     caller has no route context yet.
   */
  public ShellView build(HttpSession session, SupportedLocale locale, String currentRoute) {
    Locale javaLocale = Locale.forLanguageTag(locale.code());
    Theme theme = preferences.theme(session);
    SidebarState sidebar = preferences.sidebarState(session);
    Identity user = identity.current(session);
    String section = NavigationCatalog.currentSection(currentRoute == null ? "" : currentRoute);
    String crumb = section.isEmpty() ? "" : labels.label(section, javaLocale);

    List<NavGroupView> groups =
        NavigationCatalog.groups().stream()
            .map(group -> toGroupView(group, locale, javaLocale))
            .toList();

    ShellFixtures.Workspace workspace = ShellFixtures.workspace();
    return new ShellView(
        locale.code(),
        theme.value(),
        sidebar.value(),
        groups,
        section,
        crumb,
        new WorkspaceView(workspace.name(), workspace.meta(), workspace.mark()),
        new UserView(user.name(), user.email()));
  }

  private NavGroupView toGroupView(
      NavigationCatalog.Group group, SupportedLocale locale, Locale javaLocale) {
    List<NavItemView> items =
        group.entries().stream()
            .map(
                entry ->
                    new NavItemView(
                        labels.label(entry.route(), javaLocale),
                        entry.icon(),
                        entry.route(),
                        "/" + locale.code() + "/" + entry.route(),
                        entry.badge()))
            .toList();
    return new NavGroupView(labels.label(group.sectionKey(), javaLocale), items);
  }
}
