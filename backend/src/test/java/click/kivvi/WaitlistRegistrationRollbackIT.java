package click.kivvi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import click.kivvi.application.mail.MailQueue;
import click.kivvi.application.waitlist.WaitlistConfirmationService;
import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.mail.OutboundMail;
import click.kivvi.domain.waitlist.WaitlistSignup;
import click.kivvi.infrastructure.mail.MailOutboxStore;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The claim the whole outbox design rests on: the subscriber row and its confirmation message are
 * written in one transaction, so there is no state in which the list holds an address that was
 * never sent anything.
 *
 * <p>Nothing else proves it. Every other test sees both writes succeed, which is exactly what a
 * broken transaction boundary also looks like — {@code @Transactional} silently does nothing when
 * the call arrives through {@code this} rather than through the proxy, and the only way to notice
 * is to make the second write fail and check whether the first one survived.
 */
@Testcontainers
@SpringBootTest
class WaitlistRegistrationRollbackIT {

  private static final String CONSENT = "Zgadzam się na otrzymanie powiadomienia o starcie.";

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired private WaitlistConfirmationService service;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("a message that cannot be queued takes the subscriber row down with it")
  void aFailedEnqueueRollsBackTheSubscriber() {
    String email = "rollback-" + System.nanoTime() + "@sklep.pl";

    assertThatThrownBy(() -> service.register(signup(email)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("refusing to queue");

    assertThat(countFor(email)).isZero();
  }

  private static WaitlistSignup signup(String email) {
    return WaitlistSignup.landingSignup(
        email, SupportedLocale.PL, Instant.now(), "203.0.113.7", "Mozilla/5.0", CONSENT);
  }

  private Integer countFor(String email) {
    return jdbcTemplate.queryForObject(
        "select count(*) from waitlist_subscriber where email = ?", Integer.class, email);
  }

  /**
   * Picked up automatically as a nested {@code @TestConfiguration}. {@code @Primary} so the
   * confirmation service gets this one rather than the real queue; everything up to the point of
   * queueing runs exactly as it does in production.
   */
  @TestConfiguration
  static class FailingQueue {

    @Bean
    @Primary
    MailQueue refusingMailQueue(MailOutboxStore outbox) {
      return new MailQueue(outbox) {
        @Override
        public boolean enqueue(OutboundMail mail) {
          throw new IllegalStateException("The outbox is refusing to queue anything right now.");
        }
      };
    }
  }
}
