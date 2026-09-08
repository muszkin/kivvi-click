package click.kivvi.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves {@link FailOnWarnLogExtension}'s detection logic actually reports a failure for a
 * deliberate warning — exercised directly against manually-built log events, not through a real
 * {@code LOG.warn(...)} call under the live extension, which would leave a permanently-failing test
 * in the suite instead of a test that verifies the policy.
 */
class FailOnWarnLogExtensionTest {

  @Test
  @DisplayName("a deliberate click.kivvi WARN event is reported as a failure")
  void warnEventIsReported() {
    ILoggingEvent warning =
        event(Level.WARN, "click.kivvi.web.LoginController", "deliberate warning");

    var verdict = FailOnWarnLogExtension.warningVerdict(List.of(warning));

    assertThat(verdict).isPresent();
    assertThat(verdict.get())
        .contains("WARN")
        .contains("click.kivvi.web.LoginController")
        .contains("deliberate warning");
  }

  @Test
  @DisplayName("a deliberate click.kivvi ERROR event is also reported (>= WARN)")
  void errorEventIsReported() {
    ILoggingEvent error =
        event(Level.ERROR, "click.kivvi.application.LoginService", "deliberate error");

    var verdict = FailOnWarnLogExtension.warningVerdict(List.of(error));

    assertThat(verdict).isPresent();
    assertThat(verdict.get()).contains("ERROR");
  }

  @Test
  @DisplayName("INFO and DEBUG events are not reported")
  void infoAndDebugAreIgnored() {
    var verdict =
        FailOnWarnLogExtension.warningVerdict(
            List.of(
                event(Level.INFO, "click.kivvi.KivviApplication", "started"),
                event(Level.DEBUG, "click.kivvi.web.ShellController", "debugging")));

    assertThat(verdict).isEmpty();
  }

  @Test
  @DisplayName("no events at all is not reported")
  void noEventsIsNotReported() {
    assertThat(FailOnWarnLogExtension.warningVerdict(List.of())).isEmpty();
  }

  @Test
  @DisplayName("a first-party-looking System.err line is reported")
  void unexpectedStderrIsReported() {
    var verdict =
        FailOnWarnLogExtension.stderrVerdict(
            "click.kivvi.web.LoginController: something printed directly\n");

    assertThat(verdict).isPresent();
    assertThat(verdict.get()).contains("something printed directly");
  }

  @Test
  @DisplayName("known JVM/agent noise on System.err is not reported")
  void knownNoiseIsNotReported() {
    String noise =
        "WARNING: A terminally deprecated method in sun.misc.Unsafe has been called\n"
            + "Mockito is currently self-attaching to enable the inline-mock-maker.\n"
            + "WARNING: A Java agent has been loaded dynamically\n"
            + "WARNING: If a serviceability tool is in use, please run with -Djdk.instrument.traceUsage\n"
            + "WARNING: Dynamic loading of agents will be disallowed by default in a future release\n";

    assertThat(FailOnWarnLogExtension.stderrVerdict(noise)).isEmpty();
  }

  @Test
  @DisplayName("blank System.err output is not reported")
  void blankStderrIsNotReported() {
    assertThat(FailOnWarnLogExtension.stderrVerdict("")).isEmpty();
    assertThat(FailOnWarnLogExtension.stderrVerdict("   \n\n")).isEmpty();
  }

  private static ILoggingEvent event(Level level, String loggerName, String message) {
    LoggingEvent event = new LoggingEvent();
    event.setLevel(level);
    event.setLoggerName(loggerName);
    event.setMessage(message);
    return event;
  }
}
