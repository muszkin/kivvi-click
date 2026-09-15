package click.kivvi.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Injects server state into the built SPA's {@code index.html} before it reaches the browser, so
 * the very first paint already has the right language, theme and sidebar state — no flash of the
 * wrong one. A form POST that could not be accepted additionally carries what went wrong and what
 * was typed, so the SPA can redisplay the form without a second round trip.
 */
public final class SpaDocument {

  private static final Pattern HTML_TAG = Pattern.compile("<html[^>]*>");

  /**
   * The three attributes every document carries, plus whatever a failed POST needs to add.
   *
   * <p>{@code dataAttributes} maps an attribute name without its {@code data-} prefix to its value
   * ({@code "login-error"} renders as {@code data-login-error="…"}), and renders in insertion
   * order. It replaced a pair of positional {@code loginError}/{@code lastUsername} components when
   * the waitlist form arrived needing three more of its own: five nullable trailing parameters,
   * four of which are null on any given call, is a worse record than one map, and every further
   * form would have added to the pile. Existing callers produce byte-for-byte the same tag they did
   * before — {@code SpaDocumentTest} pins that.
   */
  public record Attributes(
      String locale, String theme, String sidebar, Map<String, String> dataAttributes) {

    public Attributes(String locale, String theme, String sidebar) {
      this(locale, theme, sidebar, Map.of());
    }

    public Attributes {
      dataAttributes = Collections.unmodifiableMap(new LinkedHashMap<>(dataAttributes));
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
    attrs
        .dataAttributes()
        .forEach(
            (name, value) ->
                tag.append(" data-")
                    .append(escape(name))
                    .append("=\"")
                    .append(escape(value))
                    .append('"'));
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
