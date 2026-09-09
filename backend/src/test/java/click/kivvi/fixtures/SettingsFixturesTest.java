package click.kivvi.fixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingsFixturesTest {

  @Test
  @DisplayName("B32 eight settings tabs, in the oracle's exact order")
  void eightTabsInOracleOrder() {
    assertThat(SettingsFixtures.tabs())
        .extracting(SettingsFixtures.Tab::id, SettingsFixtures.Tab::label)
        .containsExactly(
            tuple("account", "Konto"),
            tuple("sites", "Śledzone strony"),
            tuple("team", "Zespół"),
            tuple("providers", "Dostawcy email"),
            tuple("api", "Webhooks i API"),
            tuple("notifications", "Powiadomienia"),
            tuple("billing", "Plan i płatności"),
            tuple("gdpr", "RODO / DPA"));
  }

  @Test
  @DisplayName("B06 isKnownTab is true for every seeded tab and false for anything else")
  void isKnownTabDistinguishesSeededTabs() {
    assertThat(SettingsFixtures.isKnownTab("account")).isTrue();
    assertThat(SettingsFixtures.isKnownTab("gdpr")).isTrue();
    assertThat(SettingsFixtures.isKnownTab("nonexistent")).isFalse();
  }

  @Test
  @DisplayName("B32 the default tab is \"account\"")
  void defaultTabIsAccount() {
    assertThat(SettingsFixtures.DEFAULT_TAB).isEqualTo("account");
  }

  @Test
  @DisplayName("B32 three tracked sites, event counts narrow-space-grouped")
  void trackedSitesMatchTheOracle() {
    assertThat(SettingsFixtures.trackedSites())
        .extracting(SettingsFixtures.TrackedSite::name, SettingsFixtures.TrackedSite::events)
        .containsExactly(
            tuple("aureashop.pl", "28 410"),
            tuple("mlot-narzedzia.pl", "14 820"),
            tuple("polna-bistro.pl", "12 240"));
  }

  @Test
  @DisplayName("B32 four DNS records, three verified and one warning (BIMI)")
  void dnsRecordsMatchTheOracle() {
    assertThat(SettingsFixtures.dnsRecords())
        .extracting(SettingsFixtures.DnsRecord::record, SettingsFixtures.DnsRecord::ok)
        .containsExactly(
            tuple("SPF", true), tuple("DKIM", true), tuple("DMARC", true), tuple("BIMI", false));
  }

  @Test
  @DisplayName("B32 three webhooks, the Slack one failing with HTTP 410")
  void webhooksMatchTheOracle() {
    assertThat(SettingsFixtures.webhooks())
        .extracting(SettingsFixtures.Webhook::code)
        .containsExactly(200, 200, 410);
  }

  @Test
  @DisplayName(
      "B32 the notification matrix has 8 rows and exactly 15 of its 24 checkboxes are checked")
  void notificationMatrixMatchesTheOracle() {
    assertThat(SettingsFixtures.notificationMatrix()).hasSize(8);
    long checked =
        SettingsFixtures.notificationMatrix().stream()
            .flatMap(row -> java.util.stream.Stream.of(row.email(), row.slack(), row.sms()))
            .filter(Boolean::booleanValue)
            .count();
    assertThat(checked).isEqualTo(15);
  }

  @Test
  @DisplayName("B32 the tracker snippet is the raw, unhighlighted source")
  void trackerSnippetIsRawSource() {
    assertThat(SettingsFixtures.TRACKER_SNIPPET)
        .contains("cdn.kivvi-click.io/k.js")
        .doesNotContain("<span");
  }
}
