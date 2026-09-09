package click.kivvi.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Polish-locale number, money and relative-time formatting — ported from {@code
 * src/Panel/Format.php}.
 *
 * <p>Values reach the SPA pre-formatted: a template (Twig, once) or a component (Vue, now) is
 * markup, not a place to decide how many decimals a conversion rate has.
 */
public final class Format {

  // U+202F NARROW NO-BREAK SPACE — matches Format.php's THOUSANDS_SEPARATOR exactly. A
  // plain ASCII space is a deliberately different, separate convention used elsewhere in
  // the old stack (Twig's own number_format filter, ported verbatim in
  // FeedsViewService's groupWithSpace) — the two are not interchangeable and both are
  // reproduced verbatim.
  private static final char THOUSANDS_SEPARATOR = ' ';

  private Format() {}

  public static String number(double value) {
    BigDecimal rounded = BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP);
    return group(rounded.toBigInteger().toString());
  }

  public static String money(double value) {
    return number(value) + " zł";
  }

  public static String percent(double value) {
    return percent(value, 1);
  }

  public static String percent(double value, int decimals) {
    BigDecimal rounded = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP);
    String[] parts = rounded.toPlainString().split("\\.", 2);
    String wholePart = group(parts[0]);
    String formatted = parts.length > 1 ? wholePart + ',' + parts[1] : wholePart;
    return formatted + "%";
  }

  public static String initials(String name) {
    String trimmed = name.strip();
    if (trimmed.isEmpty()) {
      return "";
    }
    String[] parts = trimmed.split("\\s+");
    StringBuilder initials = new StringBuilder();
    for (int i = 0; i < Math.min(2, parts.length); i++) {
      if (!parts[i].isEmpty()) {
        initials.appendCodePoint(Character.toUpperCase(parts[i].codePointAt(0)));
      }
    }
    return initials.toString();
  }

  public static String timeAgo(Instant moment, Instant now) {
    long seconds = Math.max(0, ChronoUnit.SECONDS.between(moment, now));
    if (seconds < 60) {
      return "teraz";
    }
    if (seconds < 3600) {
      return (seconds / 60) + " min temu";
    }
    if (seconds < 86400) {
      return (seconds / 3600) + " godz. temu";
    }
    if (seconds < 172800) {
      return "wczoraj";
    }
    return (seconds / 86400) + " dni temu";
  }

  /**
   * Groups a plain (possibly negative) integer string into thousands with {@link
   * #THOUSANDS_SEPARATOR}.
   */
  private static String group(String digits) {
    boolean negative = digits.startsWith("-");
    String magnitude = negative ? digits.substring(1) : digits;
    StringBuilder grouped = new StringBuilder();
    for (int i = 0; i < magnitude.length(); i++) {
      if (i > 0 && (magnitude.length() - i) % 3 == 0) {
        grouped.append(THOUSANDS_SEPARATOR);
      }
      grouped.append(magnitude.charAt(i));
    }
    return (negative ? "-" : "") + grouped;
  }
}
