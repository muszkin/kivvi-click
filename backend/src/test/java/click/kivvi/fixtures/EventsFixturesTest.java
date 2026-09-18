package click.kivvi.fixtures;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.EventsFixtures.SampleCustomer;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PIO-129 gave the demonstration shop an English half. What has to stay true of it is not just that
 * it exists, but that it is the <em>same</em> shop: the feed picks its rows by seed arithmetic
 * ({@code seed % list.size()}), so two lists of different lengths would make the English feed show
 * a different event at a given position than the Polish one, and a customer id would stop naming
 * one person.
 *
 * <p>{@code PanelTranslationCoverageTest} covers the other half of the question — that the English
 * entries are actually in English.
 */
class EventsFixturesTest {

  @Test
  @DisplayName("PIO-129 both languages carry the same number of sample customers")
  void sameNumberOfCustomers() {
    assertThat(EventsFixtures.customers(SupportedLocale.EN))
        .hasSameSizeAs(EventsFixtures.customers(SupportedLocale.PL));
  }

  @Test
  @DisplayName("PIO-129 a customer id names the same position in both languages")
  void customerIdsLineUp() {
    assertThat(ids(SupportedLocale.EN)).containsExactlyElementsOf(ids(SupportedLocale.PL));
  }

  @Test
  @DisplayName("PIO-129 every sample e-mail stays ASCII, whichever name it was derived from")
  void everyEmailIsAscii() {
    for (SupportedLocale locale : SupportedLocale.values()) {
      assertThat(EventsFixtures.customers(locale))
          .as("%s sample e-mails", locale.code())
          .allSatisfy(
              customer ->
                  assertThat(customer.email()).matches("[a-z0-9.]+@example\\.com").isLowerCase());
    }
  }

  @Test
  @DisplayName("PIO-129 the two shops stock the same number of products, phrases and delays")
  void everyListHasTheSameLengthInBothLanguages() {
    assertSameSize(EventsFixtures::productPaths);
    assertSameSize(EventsFixtures::productNames);
    assertSameSize(EventsFixtures::searchPhrases);
    assertSameSize(EventsFixtures::abandonDelays);
    assertSameSize(EventsFixtures::basketSizes);
  }

  @Test
  @DisplayName("PIO-129 a product path stays a slug — lower case, no spaces, no diacritics")
  void productPathsAreSlugs() {
    for (SupportedLocale locale : SupportedLocale.values()) {
      assertThat(EventsFixtures.productPaths(locale))
          .as("%s product paths", locale.code())
          .allSatisfy(path -> assertThat(path).matches("[a-z0-9]+(-[a-z0-9]+)*"));
    }
  }

  private static void assertSameSize(
      java.util.function.Function<SupportedLocale, List<String>> values) {
    assertThat(values.apply(SupportedLocale.EN)).hasSameSizeAs(values.apply(SupportedLocale.PL));
  }

  private static List<String> ids(SupportedLocale locale) {
    return EventsFixtures.customers(locale).stream().map(SampleCustomer::id).toList();
  }
}
