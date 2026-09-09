package click.kivvi.fixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomersFixturesTest {

  @Test
  @DisplayName("B25 the index has 24 seeded customers, first two matching the oracle exactly")
  void indexHasTwentyFourSeededCustomers() {
    assertThat(CustomersFixtures.all()).hasSize(24);
    assertThat(CustomersFixtures.all())
        .extracting(
            CustomersFixtures.Customer::id,
            CustomersFixtures.Customer::name,
            CustomersFixtures.Customer::email,
            CustomersFixtures.Customer::orders,
            CustomersFixtures.Customer::revenue,
            c -> c.segment().label())
        .startsWith(
            tuple("c_1000", "Anna K.", "anna.k@example.com", 0, 49, "VIP"),
            tuple("c_1001", "Kasia N.", "kasia.n@example.com", 1, 186, "Powracający"));
  }

  @Test
  @DisplayName(
      "B25 total() is the fixed grand total shown in the subtitle, not the seeded row count")
  void totalIsTheFixedGrandTotal() {
    assertThat(CustomersFixtures.total()).isEqualTo(4_218);
  }

  @Test
  @DisplayName("B05 byId finds a seeded customer by id")
  void byIdFindsASeededCustomer() {
    assertThat(CustomersFixtures.byId("c_1000").name()).isEqualTo("Anna K.");
  }

  @Test
  @DisplayName("B05 byId throws for an id outside the seeded set")
  void byIdThrowsForAnUnknownId() {
    assertThatThrownBy(() -> CustomersFixtures.byId("c_9999"))
        .isInstanceOf(NoSuchElementException.class);
  }
}
