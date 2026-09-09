package click.kivvi.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * End-to-end proof, against a real Spring context and a real Postgres 18 container, that the {@code
 * SchedulingConfig}/{@link HeartbeatTrigger}/{@link HeartbeatJob} wiring actually runs.
 * Deliberately keeps {@code kivvi.scheduling.heartbeat.interval} at its {@code application.yml}
 * default ({@code PT1H}) rather than shortening it: {@link HeartbeatTrigger}'s "no prior lock row"
 * branch already fires the very first tick immediately on a fresh {@code shedlock} table, which is
 * all B20/B33 need — and it means this context's scheduler never has a reason to fire again for the
 * rest of the test JVM's lifetime, so a Spring test-context cache hit after this class's
 * {@code @Testcontainers} container is stopped can never race a live background thread against a
 * dead container (a shortened interval was tried first and produced exactly that flakiness —
 * evidence/heartbeat-lingering-scheduler-repro.log).
 *
 * <p>B33's "lockAtLeastFor absorbs a repeat attempt" half is proven by directly re-invoking the
 * injected (Spring-proxied) {@link HeartbeatJob#tick()} a second time, rather than waiting for the
 * hourly trigger to re-fire — the same {@code @SchedulerLock} AOP interceptor applies to any
 * Spring-proxied call to the annotated method, not only ones the scheduler itself makes, so this
 * exercises the exact guard the missed-run semantics rely on, deterministically and in
 * milliseconds.
 *
 * <p>{@link AttachHeartbeatAppender} attaches the Logback appender via an {@link
 * ApplicationContextInitializer}, not {@code @BeforeEach}/{@code @BeforeAll}/a plain {@code static}
 * initializer: the very first tick fires within milliseconds of context refresh completing, and
 * every one of those simpler attachment points loses the race against it —
 * {@code @BeforeEach}/{@code @BeforeAll} run after {@code SpringApplication.run()} has already
 * finished (evidence/heartbeat-appender-race-repro.log, heartbeat-beforeall-still-races.log); a
 * plain {@code static} initializer runs once, the first time the JVM touches this class, which in a
 * shared Failsafe/Surefire fork can be long before this class's own {@code @Test} method actually
 * executes, and every intervening {@code @SpringBootTest} in between reinitializes the Logback
 * logging system, silently detaching it (evidence/heartbeat-static-init-detached.log). An {@code
 * ApplicationContextInitializer} is invoked by {@code SpringApplication.run()} itself, after
 * environment/logging setup but strictly before {@code refresh()} — i.e. before any bean, and so
 * before the scheduler, can exist — which is the only point that is both late enough to survive
 * Boot's own logging (re)initialization and early enough to win the race.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = HeartbeatSchedulerIT.AttachHeartbeatAppender.class)
class HeartbeatSchedulerIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  private static final ListAppender<ILoggingEvent> APPENDER = new ListAppender<>();

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private HeartbeatJob heartbeatJob;

  @Test
  @DisplayName(
      "B20/B33 the scheduler ticks once on a fresh shedlock table, persists exactly one "
          + "shedlock row, and lockAtLeastFor blocks a second attempt within the same window")
  void schedulerTicksOnceAndLockBlocksASecondAttempt() {
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(tickEvents()).hasSize(1));

    heartbeatJob.tick();
    assertThat(tickEvents())
        .as("a second attempt inside lockAtLeastFor must not log a second tick")
        .hasSize(1);

    Integer shedlockRows =
        jdbcTemplate.queryForObject(
            "select count(*) from shedlock where name = ?", Integer.class, HeartbeatJob.LOCK_NAME);
    assertThat(shedlockRows).as("exactly one shedlock row for the heartbeat lock").isEqualTo(1);
  }

  private List<ILoggingEvent> tickEvents() {
    return APPENDER.list.stream()
        .filter(
            event ->
                event.getLevel() == Level.INFO
                    && event.getFormattedMessage().equals(HeartbeatJob.TICK_MESSAGE))
        .toList();
  }

  /**
   * Runs inside {@code SpringApplication.run()}, after Boot has finished (re)configuring Logback
   * for this test's context but strictly before {@code refresh()} creates a single bean — see the
   * class Javadoc for why every simpler attachment point loses the race against the first tick.
   */
  static final class AttachHeartbeatAppender
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      Logger heartbeatLogger = (Logger) LoggerFactory.getLogger(HeartbeatJob.class);
      if (!APPENDER.isStarted()) {
        APPENDER.start();
      }
      heartbeatLogger.addAppender(APPENDER);
    }
  }
}
