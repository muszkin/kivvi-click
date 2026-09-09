package click.kivvi.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * The two locales the panel serves. Every locale-prefixed URL segment must resolve to one of these
 * values or the request is unknown (404) — mirrors the old stack's route requirement {@code
 * '_locale' => 'pl|en'} with default {@code pl}.
 */
public enum SupportedLocale {
  PL("pl"),
  EN("en");

  public static final SupportedLocale DEFAULT = PL;

  private final String code;

  SupportedLocale(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  public SupportedLocale other() {
    return this == PL ? EN : PL;
  }

  public static Optional<SupportedLocale> fromCode(String candidate) {
    return Arrays.stream(values()).filter(locale -> locale.code.equals(candidate)).findFirst();
  }
}
