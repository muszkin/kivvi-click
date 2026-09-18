package click.kivvi.application;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.fixtures.EventsFixtures;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Renders the "detail" column of one sample event row — {@code EventFeed::detail()}, ported case
 * for case, in the language the feed is read in.
 *
 * <p>{@link EventsViewService} and {@link DashboardViewService} both show the live feed and both
 * used to carry their own copy of this switch, deliberately: a page's data must never depend on
 * another page's assembly staying stable, so neither service reads the other. PIO-129 made every
 * branch locale-aware, and two copies of that would be two places for one language to fall behind
 * the other. This is a formatting helper both services call, not a sibling view service either of
 * them depends on — the rule it was protecting still holds.
 *
 * <p>The purchase amount keeps {@code EventFeed::detail()}'s own inline {@code
 * number_format($value, 2, ',', ' ')} rather than going through {@link click.kivvi.domain.Format}:
 * the old stack grouped this one figure with a plain ASCII space, not the narrow no-break space
 * {@code Format} uses elsewhere, and the two conventions are reproduced verbatim rather than
 * unified. Values here (89..438) never reach four digits, so grouping never actually triggers.
 */
final class EventDetail {

  private static final int PRICE_SPREAD = 350;
  private static final int PRICE_FLOOR = 89;
  private static final int PRICE_STEP = 47;

  private EventDetail() {}

  static String pagePath(SupportedLocale locale, int seed) {
    String prefix =
        switch (locale) {
          case PL -> "/produkt/";
          case EN -> "/product/";
        };
    return prefix + pick(EventsFixtures.productPaths(locale), seed);
  }

  static String productName(SupportedLocale locale, int seed) {
    return pick(EventsFixtures.productNames(locale), seed);
  }

  static String abandonDelay(SupportedLocale locale, int seed) {
    return pick(EventsFixtures.abandonDelays(locale), seed);
  }

  static String searchPhrase(SupportedLocale locale, int seed) {
    String phrase = pick(EventsFixtures.searchPhrases(locale), seed);
    return switch (locale) {
      case PL -> "„" + phrase + "”";
      case EN -> "“" + phrase + "”";
    };
  }

  static String purchase(SupportedLocale locale, int seed) {
    int amount = ((seed * PRICE_STEP) % PRICE_SPREAD) + PRICE_FLOOR;
    String basket = pick(EventsFixtures.basketSizes(locale), seed);
    return switch (locale) {
      case PL -> price(amount, ',') + " PLN · " + basket;
      case EN -> "€" + price(amount, '.') + " · " + basket;
    };
  }

  private static String pick(java.util.List<String> values, int seed) {
    return values.get(seed % values.size());
  }

  private static String price(int value, char decimalSeparator) {
    BigDecimal rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    String[] parts = rounded.toPlainString().split("\\.", 2);
    return groupWithSpace(parts[0]) + decimalSeparator + parts[1];
  }

  private static String groupWithSpace(String digits) {
    StringBuilder grouped = new StringBuilder();
    for (int i = 0; i < digits.length(); i++) {
      if (i > 0 && (digits.length() - i) % 3 == 0) {
        grouped.append(' ');
      }
      grouped.append(digits.charAt(i));
    }
    return grouped.toString();
  }
}
