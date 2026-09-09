package click.kivvi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentityTest {

  @Test
  void derivesANameFromTheLocalPart() {
    assertThat(Identity.derive("anna@aureashop.pl").name()).isEqualTo("Anna");
  }

  @Test
  void splitsOnDotsUnderscoresAndHyphens() {
    assertThat(Identity.derive("anna.kowalska@example.com").name()).isEqualTo("Anna Kowalska");
    assertThat(Identity.derive("anna_kowalska@example.com").name()).isEqualTo("Anna Kowalska");
    assertThat(Identity.derive("anna-kowalska@example.com").name()).isEqualTo("Anna Kowalska");
  }
}
