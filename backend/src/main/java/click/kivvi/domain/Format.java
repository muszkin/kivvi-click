package click.kivvi.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Number, money and relative-time formatting for the language a page is being read in — ported from
 * {@code src/Panel/Format.php}, which knew only Polish.
 *
 * <p>Values reach the SPA pre-formatted: a template (Twig, once) or a component (Vue, now) is
 * markup, not a place to decide how many decimals a conversion rate has.
 *
 * <p>PIO-129 made it locale-aware. Polish writes {@code 184 230 zł} and {@code 38,4%}; English
 * writes {@code €184,230} and {@code 38.4%}. The figures themselves are identical in both — the
 * panel shows one demonstration shop written in two conventions, not two different shops — so a
 * test may compare a Polish and an English tile digit for digit.
 *
 * <p>Every method takes the locale rather than defaulting to one. A default would be Polish (that
 * is where these rules came from), and the callers that forgot to pass a locale would be exactly
 * the ones rendering an English page.
 */
public final class Format {

  // U+202F NARROW NO-BREAK SPACE — matches Format.php's THOUSANDS_SEPARATOR exactly. A
  // plain ASCII space is a deliberately different, separate convention used elsewhere in
  // the old stack (Twig's own number_format filter, ported verbatim in
  // FeedsViewService's groupWithSpace) — the two are not interchangeable and both are
  // reproduced verbatim. Written as an escape rather than the character itself: the two are
  // indistinguishable on screen, and an editor or a patch that silently swaps one for the other
  // changes every Polish figure in the panel without showing a thing in the diff.
  private static final char POLISH_THOUSANDS_SEPARATOR = ' ';
  private static final char ENGLISH_THOUSANDS_SEPARATOR = ',';
  private static final char POLISH_DECIMAL_SEPARATOR = ',';
  private static final char ENGLISH_DECIMAL_SEPARATOR = '.';

  // The demonstration shop trades in złoty for Polish readers and in euro for English ones.
  // Amounts are not converted between the two: 94 200 zł and €94,200 are the same fixture figure.
  private static final String POLISH_CURRENCY_SUFFIX = " zł";
  private static final String ENGLISH_CURRENCY_PREFIX = "€";

  private Format() {}

  public static String number(double value, SupportedLocale locale) {
    BigDecimal rounded = BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP);
    return group(rounded.toBigInteger().toString(), locale);
  }

  public static String money(double value, SupportedLocale locale) {
    String digits = number(value, locale);
    return switch (locale) {
      case PL -> digits + POLISH_CURRENCY_SUFFIX;
      case EN -> ENGLISH_CURRENCY_PREFIX + digits;
    };
  }

  public static String percent(double value, SupportedLocale locale) {
    return percent(value, 1, locale);
  }

  public static String percent(double value, int decimals, SupportedLocale locale) {
    BigDecimal rounded = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP);
    String[] parts = rounded.toPlainString().split("\\.", 2);
    String wholePart = group(parts[0], locale);
    String formatted =
        parts.length > 1 ? wholePart + decimalSeparator(locale) + parts[1] : wholePart;
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

  public static String timeAgo(Instant moment, Instant now, SupportedLocale locale) {
    long seconds = Math.max(0, ChronoUnit.SECONDS.between(moment, now));
    if (seconds < 60) {
      return switch (locale) {
        case PL -> "teraz";
        case EN -> "just now";
      };
    }
    if (seconds < 3600) {
      long minutes = seconds / 60;
      return switch (locale) {
        case PL -> minutes + " min temu";
        case EN -> minutes + " min ago";
      };
    }
    if (seconds < 86400) {
      long hours = seconds / 3600;
      return switch (locale) {
        case PL -> hours + " godz. temu";
        case EN -> hours + " hr ago";
      };
    }
    if (seconds < 172800) {
      return switch (locale) {
        case PL -> "wczoraj";
        case EN -> "yesterday";
      };
    }
    long days = seconds / 86400;
    return switch (locale) {
      case PL -> days + " dni temu";
      case EN -> days + " days ago";
    };
  }

  private static char decimalSeparator(SupportedLocale locale) {
    return switch (locale) {
      case PL -> POLISH_DECIMAL_SEPARATOR;
      case EN -> ENGLISH_DECIMAL_SEPARATOR;
    };
  }

  private static char thousandsSeparator(SupportedLocale locale) {
    return switch (locale) {
      case PL -> POLISH_THOUSANDS_SEPARATOR;
      case EN -> ENGLISH_THOUSANDS_SEPARATOR;
    };
  }

  /** Groups a plain (possibly negative) integer string into thousands for {@code locale}. */
  private static String group(String digits, SupportedLocale locale) {
    boolean negative = digits.startsWith("-");
    String magnitude = negative ? digits.substring(1) : digits;
    StringBuilder grouped = new StringBuilder();
    for (int i = 0; i < magnitude.length(); i++) {
      if (i > 0 && (magnitude.length() - i) % 3 == 0) {
        grouped.append(thousandsSeparator(locale));
      }
      grouped.append(magnitude.charAt(i));
    }
    return (negative ? "-" : "") + grouped;
  }
}
