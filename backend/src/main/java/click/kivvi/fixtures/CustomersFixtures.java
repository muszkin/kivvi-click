package click.kivvi.fixtures;

import click.kivvi.domain.Format;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

/**
 * Customer profiles shown in the customers index and the 360 profile — ported from {@code
 * CustomerDirectory}. The set is generated from a fixed seed, so a row keeps the same name, revenue
 * and order count between requests and between test runs.
 */
public final class CustomersFixtures {

  /** A customer's segment tag: its label and the {@code Chip} tone it renders with. */
  public record Segment(String label, String tone) {}

  /**
   * One customer profile, before {@link click.kivvi.application.CustomersViewService} formats it.
   */
  public record Customer(
      String id,
      String name,
      String email,
      String initials,
      int orders,
      int revenue,
      int lastSeenMinutes,
      Segment segment) {}

  private static final List<String> FIRST_NAMES =
      List.of(
          "Anna", "Kasia", "Marta", "Tomek", "Piotr", "Łukasz", "Olek", "Magda", "Bartek", "Iga",
          "Hania", "Wojtek", "Justyna", "Karol", "Sandra");
  private static final List<String> LAST_INITIALS =
      List.of("K.", "W.", "S.", "N.", "B.", "M.", "C.", "Z.", "R.", "P.", "D.", "L.");
  private static final List<Segment> SEGMENTS =
      List.of(
          new Segment("VIP", "accent"),
          new Segment("Powracający", "good"),
          new Segment("Nowy", "brown"),
          new Segment("Ryzyko odejścia", "warn"));
  private static final int TOTAL = 24;
  private static final int GRAND_TOTAL = 4_218;

  private CustomersFixtures() {}

  public static List<Customer> all() {
    return IntStream.range(0, TOTAL).mapToObj(CustomersFixtures::profile).toList();
  }

  /**
   * @throws NoSuchElementException when {@code id} is not one of the {@link #TOTAL} seeded
   *     customers — mirrors {@code CustomerDirectory::byId}'s {@code InvalidArgumentException},
   *     which {@code CustomerController::show} turns into a 404.
   */
  public static Customer byId(String id) {
    return all().stream()
        .filter(customer -> customer.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Unknown customer \"" + id + "\"."));
  }

  public static int total() {
    return GRAND_TOTAL;
  }

  private static Customer profile(int seed) {
    String firstName = FIRST_NAMES.get(seed % FIRST_NAMES.size());
    String lastInitial = LAST_INITIALS.get((seed * 3) % LAST_INITIALS.size());
    String name = firstName + " " + lastInitial;
    String email = emailFor(firstName, lastInitial);
    int orders = seed % 7;
    int revenue = ((seed * 137) % 1900) + 49;
    int lastSeenMinutes = seed % 12;
    Segment segment = SEGMENTS.get(seed % SEGMENTS.size());
    return new Customer(
        "c_" + (1000 + seed),
        name,
        email,
        Format.initials(name),
        orders,
        revenue,
        lastSeenMinutes,
        segment);
  }

  private static String emailFor(String firstName, String lastInitial) {
    String local = asciiFold(firstName).toLowerCase(Locale.ROOT);
    String initial = lastInitial.replace(".", "").toLowerCase(Locale.ROOT);
    return local + "." + initial + "@example.com";
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
