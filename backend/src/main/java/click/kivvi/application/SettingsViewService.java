package click.kivvi.application;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.SettingsFixtures;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/**
 * Assembles the settings page view-model for one tab — mirrors {@code SettingsController} reading
 * from the injected {@code SettingsCatalog} service. The whole catalogue is always returned (every
 * sub-collection), exactly like the Twig page held the whole catalogue object and each partial
 * called whichever methods it needed.
 */
@Service
public class SettingsViewService {

  public record TabView(String id, String icon, String label, String href, boolean active) {}

  public record Payload(
      String tab, List<TabView> tabs, String tabSubtitle, String trackerSnippet) {}

  /**
   * @throws NoSuchElementException when {@code tab} is not one of the eight known tabs — mirrors
   *     {@code SettingsController::index}'s {@code createNotFoundException} when {@code
   *     SettingsCatalog::isKnownTab} fails.
   */
  public Payload build(String tab, SupportedLocale locale) {
    if (!SettingsFixtures.isKnownTab(tab)) {
      throw new NoSuchElementException("Unknown settings tab \"" + tab + "\".");
    }
    List<TabView> tabs =
        SettingsFixtures.tabs().stream().map(t -> toTabView(t, tab, locale)).toList();
    return new Payload(tab, tabs, SettingsFixtures.subtitle(tab), SettingsFixtures.TRACKER_SNIPPET);
  }

  private static TabView toTabView(
      SettingsFixtures.Tab source, String currentTab, SupportedLocale locale) {
    // Symfony's URL generator omits a route parameter that already equals its route default, so
    // the "Konto" tab's own link never carries the "/account" suffix even while it is the active
    // tab — reproduced verbatim (see the oracle's settings step-1 a11y snapshot: the "Konto" link
    // targets "/pl/settings", not "/pl/settings/account").
    String suffix = source.id().equals(SettingsFixtures.DEFAULT_TAB) ? "" : "/" + source.id();
    String href = "/" + locale.code() + "/settings" + suffix;
    return new TabView(
        source.id(), source.icon(), source.label(), href, source.id().equals(currentTab));
  }
}
