package click.kivvi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PIO-129 made {@link Format} answer in the language the page is read in. Before that it knew only
 * Polish, and an English panel showed Polish money and Polish relative times under English
 * headings.
 *
 * <p>The Polish cases here are the port's existing behaviour written down for the first time —
 * {@code Format.php}'s narrow no-break space, comma decimal and {@code zł} suffix. They are worth
 * pinning precisely because the separator is invisible: U+202F and a plain space look identical in
 * every diff, and swapping them changes every figure in the Polish panel silently. That is not
 * hypothetical — it happened once while this very change was being written, and only the oracle
 * tests caught it.
 *
 * <p>Flat methods rather than {@code @Nested} groups, like every other test in this module: with
 * nested classes present, a test declared on the outer class is quietly left out of a full Surefire
 * run and only executes when named explicitly.
 */
class FormatTest {

  private static final Instant NOW = Instant.parse("2026-09-18T12:00:00Z");

  @Test
  @DisplayName("PIO-129 Polish groups thousands with U+202F, English with a comma")
  void thousandsSeparator() {
    assertThat(Format.number(184230, SupportedLocale.PL)).isEqualTo("184 230");
    assertThat(Format.number(184230, SupportedLocale.EN)).isEqualTo("184,230");
  }

  @Test
  @DisplayName("PIO-129 both languages round to whole units and agree on the digits")
  void sameDigitsEitherWay() {
    assertThat(Format.number(8410.6, SupportedLocale.PL)).isEqualTo("8 411");
    assertThat(Format.number(8410.6, SupportedLocale.EN)).isEqualTo("8,411");
  }

  @Test
  @DisplayName("PIO-129 a negative figure keeps its sign in front of the grouping")
  void negativeNumber() {
    assertThat(Format.number(-1234, SupportedLocale.PL)).isEqualTo("-1 234");
    assertThat(Format.number(-1234, SupportedLocale.EN)).isEqualTo("-1,234");
  }

  @Test
  @DisplayName("PIO-129 under a thousand nothing is grouped")
  void noGroupingBelowAThousand() {
    assertThat(Format.number(847, SupportedLocale.PL)).isEqualTo("847");
    assertThat(Format.number(847, SupportedLocale.EN)).isEqualTo("847");
  }

  @Test
  @DisplayName("PIO-129 Polish writes złoty after the figure, English writes euro in front of it")
  void currency() {
    assertThat(Format.money(94200, SupportedLocale.PL)).isEqualTo("94 200 zł");
    assertThat(Format.money(94200, SupportedLocale.EN)).isEqualTo("€94,200");
  }

  @Test
  @DisplayName("PIO-129 an amount is not converted between the two — one shop, two conventions")
  void sameAmountEitherWay() {
    String polish = Format.money(382140, SupportedLocale.PL).replace(" ", "").replace(" zł", "");
    String english = Format.money(382140, SupportedLocale.EN).replace(",", "").replace("€", "");

    assertThat(english).isEqualTo(polish);
  }

  @Test
  @DisplayName("PIO-129 Polish separates decimals with a comma, English with a full stop")
  void decimalSeparator() {
    assertThat(Format.percent(38.42, SupportedLocale.PL)).isEqualTo("38,4%");
    assertThat(Format.percent(38.42, SupportedLocale.EN)).isEqualTo("38.4%");
  }

  @Test
  @DisplayName("PIO-129 asking for no decimals drops the separator entirely in both")
  void percentWithoutDecimals() {
    assertThat(Format.percent(38.42, 0, SupportedLocale.PL)).isEqualTo("38%");
    assertThat(Format.percent(38.42, 0, SupportedLocale.EN)).isEqualTo("38%");
  }

  @Test
  @DisplayName("PIO-129 under a minute reads as the present tense")
  void justNow() {
    assertThat(ago(30, ChronoUnit.SECONDS, SupportedLocale.PL)).isEqualTo("teraz");
    assertThat(ago(30, ChronoUnit.SECONDS, SupportedLocale.EN)).isEqualTo("just now");
  }

  @Test
  @DisplayName("PIO-129 minutes and hours")
  void minutesAndHours() {
    assertThat(ago(5, ChronoUnit.MINUTES, SupportedLocale.PL)).isEqualTo("5 min temu");
    assertThat(ago(5, ChronoUnit.MINUTES, SupportedLocale.EN)).isEqualTo("5 min ago");
    assertThat(ago(3, ChronoUnit.HOURS, SupportedLocale.PL)).isEqualTo("3 godz. temu");
    assertThat(ago(3, ChronoUnit.HOURS, SupportedLocale.EN)).isEqualTo("3 hr ago");
  }

  @Test
  @DisplayName("PIO-129 yesterday, then whole days")
  void daysAndYesterday() {
    assertThat(ago(30, ChronoUnit.HOURS, SupportedLocale.PL)).isEqualTo("wczoraj");
    assertThat(ago(30, ChronoUnit.HOURS, SupportedLocale.EN)).isEqualTo("yesterday");
    assertThat(ago(4, ChronoUnit.DAYS, SupportedLocale.PL)).isEqualTo("4 dni temu");
    assertThat(ago(4, ChronoUnit.DAYS, SupportedLocale.EN)).isEqualTo("4 days ago");
  }

  @Test
  @DisplayName("PIO-129 a moment in the future reads as the present, not as a negative age")
  void futureIsClamped() {
    Instant later = NOW.plusSeconds(600);

    assertThat(Format.timeAgo(later, NOW, SupportedLocale.PL)).isEqualTo("teraz");
    assertThat(Format.timeAgo(later, NOW, SupportedLocale.EN)).isEqualTo("just now");
  }

  @Test
  @DisplayName("PIO-129 initials come from the name itself and so have no language")
  void initialsAreLocaleIndependent() {
    assertThat(Format.initials("Maciej Kowalczyk")).isEqualTo("MK");
    assertThat(Format.initials("  ")).isEmpty();
  }

  private String ago(long amount, ChronoUnit unit, SupportedLocale locale) {
    return Format.timeAgo(NOW.minus(amount, unit), NOW, locale);
  }
}
