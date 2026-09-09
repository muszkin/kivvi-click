package click.kivvi.testsupport;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;

/**
 * The wave-0 analogue of PHPUnit's {@code failOnWarning}/{@code failOnNotice}, restricted to
 * first-party code — mirrors PHPUnit restricting that policy to {@code src}
 * (architecture/rules-translated.md row 3, "Test policy fail-on-warning"). Fails a test if any
 * {@code click.kivvi.*} logger logs at {@code WARN} or above during it, or if {@code System.err}
 * receives output that is not already-known JVM/framework noise.
 *
 * <p>Registered for every test through JUnit's extension auto-detection — see {@code
 * src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension} and {@code
 * junit.jupiter.extensions.autodetection.enabled=true} in {@code junit-platform.properties} —
 * rather than {@code @ExtendWith} on individual test classes, so a future test cannot opt out of
 * the policy by omission. The detection logic itself is exposed as pure static methods ({@link
 * #warningVerdict} / {@link #stderrVerdict}) precisely so it can be exercised directly in {@link
 * FailOnWarnLogExtensionTest} without needing a permanently-failing test in the suite to prove the
 * policy works.
 */
public class FailOnWarnLogExtension implements BeforeEachCallback, AfterEachCallback {

  static final String ROOT_LOGGER_NAME = "click.kivvi";

  // JVM/agent/library noise already observed on System.err in this project's own test runs —
  // none of it originates from click.kivvi code. Cheap to allowlist by substring rather than
  // inspecting the call stack behind every write(), which is what "if cheap" in the packet
  // asks for.
  static final List<String> ALLOWED_STDERR_SUBSTRINGS =
      List.of(
          "sun.misc.Unsafe",
          "terminally deprecated",
          "Mockito is currently self-attaching",
          "A Java agent has been loaded dynamically",
          "If a serviceability tool is",
          "Dynamic loading of agents",
          "byte-buddy-agent");

  private ListAppender<ILoggingEvent> appender;
  private Logger logger;
  private PrintStream originalStderr;
  private ByteArrayOutputStream stderrCapture;

  @Override
  public void beforeEach(ExtensionContext context) {
    logger = (Logger) LoggerFactory.getLogger(ROOT_LOGGER_NAME);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);

    originalStderr = System.err;
    stderrCapture = new ByteArrayOutputStream();
    System.setErr(new PrintStream(stderrCapture, true, StandardCharsets.UTF_8));
  }

  @Override
  public void afterEach(ExtensionContext context) {
    logger.detachAppender(appender);
    appender.stop();
    System.setErr(originalStderr);

    warningVerdict(appender.list)
        .ifPresent(
            detail -> {
              throw new AssertionError(
                  "click.kivvi.* logged at WARN or above during "
                      + context.getDisplayName()
                      + " (fail-on-warning test policy, architecture/rules-translated.md row 3):\n"
                      + detail);
            });

    stderrVerdict(stderrCapture.toString(StandardCharsets.UTF_8))
        .ifPresent(
            detail -> {
              throw new AssertionError(
                  "Unexpected System.err output during "
                      + context.getDisplayName()
                      + ":\n"
                      + detail);
            });
  }

  /** Empty when nothing at WARN-or-above was logged; otherwise a human-readable summary. */
  static Optional<String> warningVerdict(List<ILoggingEvent> events) {
    List<ILoggingEvent> warnings =
        events.stream().filter(e -> e.getLevel().isGreaterOrEqual(Level.WARN)).toList();
    if (warnings.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        warnings.stream()
            .map(
                e -> "[" + e.getLevel() + "] " + e.getLoggerName() + ": " + e.getFormattedMessage())
            .collect(Collectors.joining("\n")));
  }

  /** Empty when every non-blank line is known noise; otherwise the unexpected lines. */
  static Optional<String> stderrVerdict(String captured) {
    String unexpected =
        captured
            .lines()
            .filter(line -> !line.isBlank())
            .filter(line -> ALLOWED_STDERR_SUBSTRINGS.stream().noneMatch(line::contains))
            .collect(Collectors.joining("\n"));
    return unexpected.isBlank() ? Optional.empty() : Optional.of(unexpected);
  }
}
