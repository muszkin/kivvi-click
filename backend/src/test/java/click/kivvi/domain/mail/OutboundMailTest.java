package click.kivvi.domain.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutboundMailTest {

  @Test
  @DisplayName("a message without a plain-text alternative cannot be built at all")
  void thePlainTextAlternativeIsMandatory() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new OutboundMail("ala@sklep.pl", "Temat", "<p>hi</p>", "  ", null))
        .withMessageContaining("plain-text");
  }

  @Test
  @DisplayName("a repeatable message carries no deduplication key, so queueing it twice sends two")
  void aRepeatableMessageHasNoKey() {
    OutboundMail mail = OutboundMail.repeatable("ala@sklep.pl", "Temat", "<p>hi</p>", "hi");

    assertThat(mail.deduplicationKey()).isEmpty();
  }

  @Test
  @DisplayName("a keyed message exposes its key, which is what the outbox deduplicates on")
  void aKeyedMessageExposesItsKey() {
    OutboundMail mail =
        new OutboundMail("ala@sklep.pl", "Temat", "<p>hi</p>", "hi", "waitlist-confirm:abc");

    assertThat(mail.deduplicationKey()).contains("waitlist-confirm:abc");
  }
}
