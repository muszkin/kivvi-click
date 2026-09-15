package click.kivvi.domain.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The ticket's retry schedule, pinned so a widened step cannot slip in unnoticed. */
class MailRetryPolicyTest {

  @Test
  @DisplayName("the backoff widens one step per failed attempt, exactly as the ticket lists it")
  void theBackoffFollowsTheTicketsSchedule() {
    assertThat(MailRetryPolicy.backoffAfter(1)).isEqualTo(Duration.ofMinutes(1));
    assertThat(MailRetryPolicy.backoffAfter(2)).isEqualTo(Duration.ofMinutes(5));
    assertThat(MailRetryPolicy.backoffAfter(3)).isEqualTo(Duration.ofMinutes(25));
    assertThat(MailRetryPolicy.backoffAfter(4)).isEqualTo(Duration.ofHours(2));
    assertThat(MailRetryPolicy.backoffAfter(5)).isEqualTo(Duration.ofHours(10));
  }

  @Test
  @DisplayName("the budget is the first attempt plus one retry per backoff step")
  void theBudgetIsSpentAfterTheLastBackoff() {
    assertThat(MailRetryPolicy.MAX_ATTEMPTS).isEqualTo(6);
    assertThat(MailRetryPolicy.isExhausted(5)).isFalse();
    assertThat(MailRetryPolicy.isExhausted(6)).isTrue();
    assertThat(MailRetryPolicy.isExhausted(7)).isTrue();
  }

  @Test
  @DisplayName("asking for a delay past the budget is a programming error, not a default")
  void askingPastTheBudgetThrows() {
    assertThatIllegalArgumentException().isThrownBy(() -> MailRetryPolicy.backoffAfter(6));
  }
}
