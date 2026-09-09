package click.kivvi.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Injects server state into the built SPA's {@code index.html} before it reaches the browser, so
 * the very first paint already has the right language, theme and sidebar state — no flash of the
 * wrong one. A failed login POST additionally carries the error message and the last-typed address,
 * so the SPA can render the login page's callout without a second round trip.
 */
public final class SpaDocument {

  private static final Pattern HTML_TAG = Pattern.compile("<html[^>]*>");

  /** {@code loginError} and {@code lastUsername} are {@code null} outside a failed login POST. */
  public record Attributes(
      String locale, String theme, String sidebar, String loginError, String lastUsername) {
    public Attributes(String locale, String theme, String sidebar) {
      this(locale, theme, sidebar, null, null);
    }
  }

  private SpaDocument() {}

  public static String inject(String template, Attributes attrs) {
    Matcher matcher = HTML_TAG.matcher(template);
    if (!matcher.find()) {
      throw new IllegalStateException("SPA template has no <html> tag to inject attributes into");
    }
    return matcher.replaceFirst(Matcher.quoteReplacement(htmlTag(attrs)));
  }

  private static String htmlTag(Attributes attrs) {
    StringBuilder tag =
        new StringBuilder("<html lang=\"")
            .append(escape(attrs.locale()))
            .append("\" data-theme=\"")
            .append(escape(attrs.theme()))
            .append("\" data-sidebar=\"")
            .append(escape(attrs.sidebar()))
            .append('"');
    if (attrs.loginError() != null) {
      tag.append(" data-login-error=\"").append(escape(attrs.loginError())).append('"');
    }
    if (attrs.lastUsername() != null) {
      tag.append(" data-last-username=\"").append(escape(attrs.lastUsername())).append('"');
    }
    return tag.append('>').toString();
  }

  private static String escape(String value) {
    return value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}
