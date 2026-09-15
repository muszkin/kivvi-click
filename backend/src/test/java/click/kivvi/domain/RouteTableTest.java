package click.kivvi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RouteTableTest {

  @Test
  @DisplayName("B01 every panel/public URL in the route table is known")
  void everyKnownRouteMatches() {
    assertThat(RouteTable.match("/").map(RouteTable.Match::routeName)).contains("home");
    assertThat(RouteTable.match("/pl").map(RouteTable.Match::routeName)).contains("home");
    assertThat(RouteTable.match("/pl/login").map(RouteTable.Match::routeName)).contains("login");
    assertThat(RouteTable.match("/pl/dashboard").map(RouteTable.Match::routeName))
        .contains("dashboard");
    assertThat(RouteTable.match("/pl/events").map(RouteTable.Match::routeName)).contains("events");
    assertThat(RouteTable.match("/pl/customers").map(RouteTable.Match::routeName))
        .contains("customers");
    assertThat(RouteTable.match("/pl/customers/c_123").map(RouteTable.Match::routeName))
        .contains("customer_show");
    assertThat(RouteTable.match("/pl/automations").map(RouteTable.Match::routeName))
        .contains("automations");
    assertThat(RouteTable.match("/pl/automations/new").map(RouteTable.Match::routeName))
        .contains("automation_new");
    assertThat(RouteTable.match("/pl/automations/a12").map(RouteTable.Match::routeName))
        .contains("automation_edit");
    assertThat(RouteTable.match("/pl/campaigns").map(RouteTable.Match::routeName))
        .contains("campaigns");
    assertThat(RouteTable.match("/pl/emails/new").map(RouteTable.Match::routeName))
        .contains("email_new");
    assertThat(RouteTable.match("/pl/emails/k7").map(RouteTable.Match::routeName))
        .contains("email_edit");
    assertThat(RouteTable.match("/pl/popups").map(RouteTable.Match::routeName)).contains("popups");
    assertThat(RouteTable.match("/pl/popups/new").map(RouteTable.Match::routeName))
        .contains("popup_new");
    assertThat(RouteTable.match("/pl/popups/p3").map(RouteTable.Match::routeName))
        .contains("popup_edit");
    assertThat(RouteTable.match("/pl/feeds").map(RouteTable.Match::routeName)).contains("feeds");
    assertThat(RouteTable.match("/pl/import").map(RouteTable.Match::routeName)).contains("import");
    assertThat(RouteTable.match("/pl/import/2").map(RouteTable.Match::routeName))
        .contains("import");
    assertThat(RouteTable.match("/pl/settings").map(RouteTable.Match::routeName))
        .contains("settings");
    assertThat(RouteTable.match("/en/dashboard").map(RouteTable.Match::locale))
        .contains(SupportedLocale.EN);
  }

  @Test
  @DisplayName("PIO-70 the privacy policy is a public document route in both languages")
  void thePrivacyPolicyIsAPublicRoute() {
    assertThat(RouteTable.match("/pl/privacy").map(RouteTable.Match::routeName))
        .contains("privacy");
    assertThat(RouteTable.match("/pl/privacy").map(RouteTable.Match::layout))
        .contains(RouteTable.Layout.PUBLIC);
    assertThat(RouteTable.match("/en/privacy").map(RouteTable.Match::locale))
        .contains(SupportedLocale.EN);
    assertThat(RouteTable.match("/de/privacy")).isEmpty();
  }

  @Test
  @DisplayName(
      "PIO-70 the waitlist form's POST url is also a document route, so a refused submission"
          + " — which leaves the browser on it — can be reloaded without a 404")
  void theWaitlistPostUrlIsAlsoADocumentRoute() {
    assertThat(RouteTable.match("/pl/waitlist").map(RouteTable.Match::routeName))
        .contains("waitlist");
    assertThat(RouteTable.match("/pl/waitlist").map(RouteTable.Match::layout))
        .contains(RouteTable.Layout.PUBLIC);
    assertThat(RouteTable.match("/de/waitlist")).isEmpty();
  }

  @Test
  @DisplayName("B07 unsupported locale prefix is unknown to the route table")
  void unsupportedLocaleIsUnknown() {
    assertThat(RouteTable.match("/de/dashboard")).isEmpty();
  }

  @Test
  @DisplayName("a path outside the route table is unknown")
  void unknownPathIsUnknown() {
    assertThat(RouteTable.match("/pl/nonexistent")).isEmpty();
    assertThat(RouteTable.match("/pl/customers/not-an-id")).isEmpty();
  }
}
