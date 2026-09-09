package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.SidebarState;
import click.kivvi.domain.Theme;
import click.kivvi.infrastructure.SessionPreferencesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class PreferencesServiceTest {

  private final PreferencesService preferencesService =
      new PreferencesService(new SessionPreferencesStore());

  @Test
  @DisplayName("B08 theme choice persists in the session")
  void themeChoicePersists() {
    MockHttpSession session = new MockHttpSession();

    Theme applied = preferencesService.applyTheme(session, "dark");

    assertThat(applied).isEqualTo(Theme.DARK);
    assertThat(new SessionPreferencesStore().theme(session)).isEqualTo(Theme.DARK);
  }

  @Test
  @DisplayName("B09 unknown theme value falls back to light")
  void unknownThemeFallsBackToLight() {
    MockHttpSession session = new MockHttpSession();

    Theme applied = preferencesService.applyTheme(session, "sepia");

    assertThat(applied).isEqualTo(Theme.LIGHT);
  }

  @Test
  @DisplayName("B10 collapsed sidebar choice persists in the session")
  void sidebarChoicePersists() {
    MockHttpSession session = new MockHttpSession();

    SidebarState applied = preferencesService.applySidebar(session, "collapsed");

    assertThat(applied).isEqualTo(SidebarState.COLLAPSED);
    assertThat(new SessionPreferencesStore().sidebarState(session))
        .isEqualTo(SidebarState.COLLAPSED);
  }

  @Test
  @DisplayName("unknown sidebar value falls back to expanded")
  void unknownSidebarFallsBackToExpanded() {
    MockHttpSession session = new MockHttpSession();

    SidebarState applied = preferencesService.applySidebar(session, "half-open");

    assertThat(applied).isEqualTo(SidebarState.EXPANDED);
  }
}
