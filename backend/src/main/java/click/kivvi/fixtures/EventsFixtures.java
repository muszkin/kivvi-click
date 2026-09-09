package click.kivvi.fixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sample data behind the event stream's 30-row feed — ported from {@code EventFeed}'s private
 * constants and {@code CustomerDirectory}'s deterministic profile generator. {@link #customers()}
 * reproduces {@code CustomerDirectory::all()}'s exact seeded algorithm (rather than importing a
 * shared customer directory) so this fixture never depends on the parallel customers journey's own
 * fixtures, which this slice never touches and cannot assume exist yet.
 */
public final class EventsFixtures {

  /** One sample customer: only what {@code EventFeed::rows()} ever reads from a profile. */
  public record SampleCustomer(String id, String name, String email) {}

  private static final List<String> FIRST_NAMES =
      List.of(
          "Anna", "Kasia", "Marta", "Tomek", "Piotr", "Łukasz", "Olek", "Magda", "Bartek", "Iga",
          "Hania", "Wojtek", "Justyna", "Karol", "Sandra");
  private static final List<String> LAST_INITIALS =
      List.of("K.", "W.", "S.", "N.", "B.", "M.", "C.", "Z.", "R.", "P.", "D.", "L.");
  private static final int CUSTOMER_COUNT = 24;

  public static final List<String> PRODUCT_PATHS =
      List.of(
          "zielona-herbata-sencha",
          "filizanka-porcelana",
          "swieca-soja-figa",
          "pasta-pomidorowa",
          "plecak-canvas");
  public static final List<String> PRODUCT_NAMES =
      List.of(
          "Zielona herbata Sencha 100g",
          "Filiżanka porcelanowa Nora",
          "Świeca sojowa „Figa”",
          "Plecak canvas Olive",
          "Pasta z pomidorów");
  public static final List<String> SEARCH_PHRASES =
      List.of("herbata", "prezent", "kubek", "plecak", "świeca", "pasta", "olej kokosowy");
  public static final List<String> ABANDON_DELAYS =
      List.of("Po 3:12 minut", "Po 8:42 minut", "Po 14:09 minut");
  public static final List<String> BASKET_SIZES =
      List.of("1 produkt", "2 produkty", "3 produkty", "4 produkty");

  private static final List<SampleCustomer> CUSTOMERS = buildCustomers();

  private EventsFixtures() {}

  public static List<SampleCustomer> customers() {
    return CUSTOMERS;
  }

  private static List<SampleCustomer> buildCustomers() {
    List<SampleCustomer> customers = new ArrayList<>(CUSTOMER_COUNT);
    for (int seed = 0; seed < CUSTOMER_COUNT; seed++) {
      String firstName = FIRST_NAMES.get(seed % FIRST_NAMES.size());
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
