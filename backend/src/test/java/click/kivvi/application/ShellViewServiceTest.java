package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.infrastructure.NavigationLabels;
import click.kivvi.infrastructure.SessionIdentityStore;
import click.kivvi.infrastructure.SessionPreferencesStore;
import click.kivvi.infrastructure.config.MessageSourceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpSession;

class ShellViewServiceTest {

  private final NavigationLabels labels =
      new NavigationLabels(new MessageSourceConfig().messageSource());
  private final ShellViewService shellViewService =
      new ShellViewService(new SessionPreferencesStore(), new SessionIdentityStore(), labels);

  @Test
  @DisplayName("B04 /en/... renders English navigation and page title")
  void englishLocaleTranslatesTheNavigation() {
    ShellView view = shellViewService.build(new MockHttpSession(), SupportedLocale.EN, "events");

    assertThat(view.navGroups())
        .flatExtracting(ShellView.NavGroupView::items)
        .extracting(ShellView.NavItemView::label)
        .contains("Dashboard", "Event stream", "Customers", "Rules", "Email campaigns");
    assertThat(view.crumb()).isEqualTo("Event stream");
  }

  @Test
  @DisplayName("Polish locale renders the Polish navigation and crumb")
  void polishLocaleTranslatesTheNavigation() {
    ShellView view = shellViewService.build(new MockHttpSession(), SupportedLocale.PL, "dashboard");

    assertThat(view.navGroups())
        .flatExtracting(ShellView.NavGroupView::items)
        .extracting(ShellView.NavItemView::label)
        .contains("Pulpit", "Strumień zdarzeń", "Klienci");
    assertThat(view.crumb()).isEqualTo("Pulpit");
  }

  /**
   * B02 (repair-1: no unit-level test existed for "the breadcrumb names the current section" — only
   * the integration-level {@code ShellApiIT::dashboardShellPayloadMatchesTheOracle}, which only
   * covers {@code dashboard}). Every one of {@link click.kivvi.domain.NavigationCatalog}'s nine
   * top-level sections, cross-checked against the oracle's own breadcrumb text (each journey's own
   * {@code steps/*}/{@code texts.json} "aureashop.pl / &lt;crumb&gt;" line).
   */
  @ParameterizedTest
  @DisplayName("B02 the crumb names every section, matching its own nav label")
  @CsvSource({
    "dashboard, Pulpit",
    "events, Strumień zdarzeń",
    "customers, Klienci",
    "automations, Reguły",
    "campaigns, Kampanie email",
    "popups, Popupy i widgety",
    "feeds, Feedy produktów",
    "import, Import klientów",
    "settings, Ustawienia"
  })
  void crumbNamesEverySection(String route, String expectedCrumb) {
    ShellView view = shellViewService.build(new MockHttpSession(), SupportedLocale.PL, route);

    assertThat(view.crumb()).isEqualTo(expectedCrumb);
  }

  @Test
  @DisplayName(
      "B03 a detail route keeps its index section current, mirroring Navigation::currentSection")
  void detailRouteKeepsItsIndexSectionCurrent() {
    ShellView view =
        shellViewService.build(new MockHttpSession(), SupportedLocale.PL, "customer_show");

    assertThat(view.currentSection()).isEqualTo("customers");
  }

  @Test
  @DisplayName("the default identity and workspace appear when nobody has signed in")
  void defaultIdentityAndWorkspace() {
    ShellView view = shellViewService.build(new MockHttpSession(), SupportedLocale.PL, "dashboard");

    assertThat(view.user().email()).isEqualTo("maciej@aureashop.pl");
    assertThat(view.user().name()).isEqualTo("Maciej Kowalczyk");
    assertThat(view.workspace().name()).isEqualTo("aureashop.pl");
    assertThat(view.theme()).isEqualTo("light");
    assertThat(view.sidebar()).isEqualTo("expanded");
  }
}
