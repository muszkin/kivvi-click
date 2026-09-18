package click.kivvi.fixtures;

import click.kivvi.domain.SupportedLocale;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sample data behind the event stream's 30-row feed — ported from {@code EventFeed}'s private
 * constants and {@code CustomerDirectory}'s deterministic profile generator. {@link
 * #customers(SupportedLocale)} reproduces {@code CustomerDirectory::all()}'s exact seeded algorithm
 * (rather than importing a shared customer directory) so this fixture never depends on the parallel
 * customers journey's own fixtures, which this slice never touches and cannot assume exist yet.
 *
 * <p>PIO-129 gave the demonstration shop an English half. The owner's decision was that sample data
 * is translated too, not only the interface around it: an English reader sees a shop that sells
 * "Sencha green tea 100g" to "Emma K.", not one that sells "Zielona herbata Sencha 100g" to "Anna
 * K." through English column headings.
 *
 * <p>Both halves are the same shape — the same number of names, products, phrases and delays, in
 * the same order — so the row at a given seed describes the same event in either language, and the
 * customer ids ({@code c_1000+seed}) identify the same person. No first name appears in both lists:
 * a name that survived translation unchanged would be indistinguishable from one nobody translated.
 * {@code EventsFixturesTest} and {@code PanelTranslationCoverageTest} hold them to all of that.
 */
public final class EventsFixtures {

  /** One sample customer: only what {@code EventFeed::rows()} ever reads from a profile. */
  public record SampleCustomer(String id, String name, String email) {}

  private static final List<String> FIRST_NAMES_PL =
      List.of(
          "Anna", "Kasia", "Marta", "Tomek", "Piotr", "Łukasz", "Olek", "Magda", "Bartek", "Iga",
          "Hania", "Wojtek", "Justyna", "Karol", "Sandra");
  private static final List<String> FIRST_NAMES_EN =
      List.of(
          "Emma", "Katie", "Martha", "Tom", "Peter", "Luke", "Ollie", "Maggie", "Bart", "Ivy",
          "Hannah", "Wes", "Justine", "Carl", "Sarah");
  private static final List<String> LAST_INITIALS =
      List.of("K.", "W.", "S.", "N.", "B.", "M.", "C.", "Z.", "R.", "P.", "D.", "L.");
  private static final int CUSTOMER_COUNT = 24;

  private static final List<String> PRODUCT_PATHS_PL =
      List.of(
          "zielona-herbata-sencha",
          "filizanka-porcelana",
          "swieca-soja-figa",
          "pasta-pomidorowa",
          "plecak-canvas");
  private static final List<String> PRODUCT_PATHS_EN =
      List.of(
          "sencha-green-tea", "porcelain-cup", "soy-candle-fig", "tomato-paste", "canvas-backpack");

  private static final List<String> PRODUCT_NAMES_PL =
      List.of(
          "Zielona herbata Sencha 100g",
          "Filiżanka porcelanowa Nora",
          "Świeca sojowa „Figa”",
          "Plecak canvas Olive",
          "Pasta z pomidorów");
  private static final List<String> PRODUCT_NAMES_EN =
      List.of(
          "Sencha green tea 100g",
          "Nora porcelain cup",
          "Soy candle “Fig”",
          "Olive canvas backpack",
          "Tomato paste");

  private static final List<String> SEARCH_PHRASES_PL =
      List.of("herbata", "prezent", "kubek", "plecak", "świeca", "pasta", "olej kokosowy");
  private static final List<String> SEARCH_PHRASES_EN =
      List.of("tea", "gift", "mug", "backpack", "candle", "paste", "coconut oil");

  private static final List<String> ABANDON_DELAYS_PL =
      List.of("Po 3:12 minut", "Po 8:42 minut", "Po 14:09 minut");
  private static final List<String> ABANDON_DELAYS_EN =
      List.of("After 3:12 minutes", "After 8:42 minutes", "After 14:09 minutes");

  private static final List<String> BASKET_SIZES_PL =
      List.of("1 produkt", "2 produkty", "3 produkty", "4 produkty");
  private static final List<String> BASKET_SIZES_EN =
      List.of("1 item", "2 items", "3 items", "4 items");

  private static final List<SampleCustomer> CUSTOMERS_PL = buildCustomers(FIRST_NAMES_PL);
  private static final List<SampleCustomer> CUSTOMERS_EN = buildCustomers(FIRST_NAMES_EN);

  private EventsFixtures() {}

  public static List<SampleCustomer> customers(SupportedLocale locale) {
    return switch (locale) {
      case PL -> CUSTOMERS_PL;
      case EN -> CUSTOMERS_EN;
    };
  }

  public static List<String> productPaths(SupportedLocale locale) {
    return switch (locale) {
      case PL -> PRODUCT_PATHS_PL;
      case EN -> PRODUCT_PATHS_EN;
    };
  }

  public static List<String> productNames(SupportedLocale locale) {
    return switch (locale) {
      case PL -> PRODUCT_NAMES_PL;
      case EN -> PRODUCT_NAMES_EN;
    };
  }

  public static List<String> searchPhrases(SupportedLocale locale) {
    return switch (locale) {
      case PL -> SEARCH_PHRASES_PL;
      case EN -> SEARCH_PHRASES_EN;
    };
  }

  public static List<String> abandonDelays(SupportedLocale locale) {
    return switch (locale) {
      case PL -> ABANDON_DELAYS_PL;
      case EN -> ABANDON_DELAYS_EN;
    };
  }

  public static List<String> basketSizes(SupportedLocale locale) {
    return switch (locale) {
      case PL -> BASKET_SIZES_PL;
      case EN -> BASKET_SIZES_EN;
    };
  }

  private static List<SampleCustomer> buildCustomers(List<String> firstNames) {
    List<SampleCustomer> customers = new ArrayList<>(CUSTOMER_COUNT);
    for (int seed = 0; seed < CUSTOMER_COUNT; seed++) {
      String firstName = firstNames.get(seed % firstNames.size());
      String lastInitial = LAST_INITIALS.get((seed * 3) % LAST_INITIALS.size());
      String name = firstName + " " + lastInitial;
      String email =
          asciiFold(firstName).toLowerCase(Locale.ROOT)
              + "."
              + asciiFold(lastInitial.substring(0, lastInitial.length() - 1))
                  .toLowerCase(Locale.ROOT)
              + "@example.com";
      customers.add(new SampleCustomer("c_" + (1000 + seed), name, email));
    }
    return List.copyOf(customers);
  }

  private static String asciiFold(String value) {
    return value
        .replace("ą", "a")
        .replace("ć", "c")
        .replace("ę", "e")
        .replace("ł", "l")
        .replace("ń", "n")
        .replace("ó", "o")
        .replace("ś", "s")
        .replace("ź", "z")
        .replace("ż", "z")
        .replace("Ł", "L");
  }
}
