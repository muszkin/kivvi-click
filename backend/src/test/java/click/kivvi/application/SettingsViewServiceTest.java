package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import click.kivvi.domain.SupportedLocale;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingsViewServiceTest {

  private final SettingsViewService settingsViewService = new SettingsViewService();

  @Test
  @DisplayName("B32 the \"account\" tab is marked active and carries the account subtitle")
  void accountTabIsActive() {
    SettingsViewService.Payload payload = settingsViewService.build("account", SupportedLocale.PL);

    assertThat(payload.tab()).isEqualTo("account");
    assertThat(payload.tabSubtitle()).isEqualTo("Dane firmy i preferencje właściciela konta.");
    assertThat(payload.tabs())
        .filteredOn(SettingsViewService.TabView::active)
        .extracting(SettingsViewService.TabView::id)
        .containsExactly("account");
  }

  @Test
  @DisplayName(
      "B32 the account tab's own nav link omits the default tab's URL segment, every other tab "
          + "keeps it — Symfony's URL generator drops a route parameter equal to its default")
  void accountHrefOmitsTheDefaultSegment() {
    SettingsViewService.Payload payload = settingsViewService.build("sites", SupportedLocale.PL);

    assertThat(payload.tabs())
        .extracting(SettingsViewService.TabView::id, SettingsViewService.TabView::href)
        .containsExactly(
            tuple("account", "/pl/settings"),
            tuple("sites", "/pl/settings/sites"),
            tuple("team", "/pl/settings/team"),
            tuple("providers", "/pl/settings/providers"),
            tuple("api", "/pl/settings/api"),
            tuple("notifications", "/pl/settings/notifications"),
            tuple("gdpr", "/pl/settings/gdpr"));
  }

  @Test
  @DisplayName("B32 hrefs carry the requested locale prefix")
  void hrefsCarryTheLocalePrefix() {
    SettingsViewService.Payload payload = settingsViewService.build("gdpr", SupportedLocale.EN);

    assertThat(payload.tabs().get(0).href()).isEqualTo("/en/settings");
    assertThat(payload.tabs().get(6).href()).isEqualTo("/en/settings/gdpr");
  }

  @Test
  @DisplayName("B06 an unknown tab throws, mirroring SettingsController's 404")
  void unknownTabThrows() {
    assertThatThrownBy(() -> settingsViewService.build("nonexistent", SupportedLocale.PL))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName(
      "PIO-123 the retired \"billing\" tab throws like any other unknown tab — the panel no "
          + "longer has a subscription surface to render")
  void retiredBillingTabThrows() {
    assertThatThrownBy(() -> settingsViewService.build("billing", SupportedLocale.PL))
        .isInstanceOf(NoSuchElementException.class);
  }
}
