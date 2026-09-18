package click.kivvi.fixtures;

import click.kivvi.domain.SupportedLocale;
import java.util.List;

/**
 * On-site widget fixture data — ported verbatim from {@code WidgetCatalog}'s private {@code
 * WIDGETS_PL} array, its type/status-chip maps and the composer model behind the popup editor
 * (widget type list, block library, placeholder variables, triggers, audience and accent colours).
 *
 * <p>Widget copy is kept apart from the panel's own tokens on purpose — mirrors {@code
 * WidgetCatalog}'s own class comment: the widget renders on the customer's storefront, so its
 * colours are literal {@code oklch(...)} strings, never a CSS custom property.
 */
public final class WidgetFixtures {

  /** One row of the old stack's private {@code WIDGETS_PL} array — status/type are raw enums. */
  public record Widget(
      String id, String name, String status, String type, int impressions, double conversion) {}

  /** {@code WidgetCatalog::STATUS_CHIP}'s tone/label pair for one status. */
  public record StatusChip(String tone, String label) {}

  /** {@code WidgetCatalog::TYPE_LABEL}'s Polish label for one widget type. */
  public record TypeOption(String id, String label, String icon) {}

  /** One of the ten draggable block sources in {@code block-library.html.twig} — never varies. */
  public record Block(String icon, String label, String type) {}

  /**
   * One popup trigger row. {@code note} accompanies {@code exit intent}; {@code value}/{@code unit}
   * accompany the two numeric triggers — never both on the same row, mirrors {@code
   * WidgetCatalog::triggers()}'s own untyped array shape.
   */
  public record Trigger(String label, String tone, String note, String value, String unit) {}

  /**
   * One "Kto zobaczy" audience rule. {@code label} may carry a literal {@code <span class="mono">}
   * fragment (the "14 dniach" rule) exactly like {@code WidgetCatalog::audience()}'s own {@code
   * |raw}-rendered label.
   */
  public record AudienceRule(String label, boolean checked) {}

  /**
   * One widget shape's rendered content — flat and nullable-by-type rather than a sealed hierarchy,
   * mirrors {@code WidgetCatalog::content()}'s own untyped array shape: {@code banner} has no
   * {@code body}/{@code placeholder}/{@code fine}, {@code toast} has no {@code kicker}/{@code
   * placeholder}/{@code fine}, every other type carries all six. Jackson's {@code non_null}
   * inclusion (application.yml) drops every field a given shape does not use.
   */
  public record Content(
      String type,
      String kicker,
      String title,
      String body,
      String placeholder,
      String cta,
      String fine) {}

  private static final List<Widget> WIDGETS_PL =
      List.of(
          new Widget("p1", "Exit intent — 10% rabatu", "active", "modal", 48210, 7.4),
          new Widget("p2", "Pasek darmowej dostawy od 199 zł", "active", "banner", 182400, 2.1),
          new Widget("p3", "Slide-in: zapisz się do newslettera", "active", "slide-in", 62100, 4.8),
          new Widget("p4", "Social proof — „ktoś właśnie kupił”", "paused", "toast", 0, 0.0),
          new Widget("p5", "Promo wakacyjne — full screen", "draft", "fullscreen", 0, 0.0));

  private static final List<TypeOption> TYPES_PL =
      List.of(
          new TypeOption("modal", "Modal", "layout"),
          new TypeOption("slide-in", "Slide-in", "arrow_right"),
          new TypeOption("banner", "Pasek", "minus"),
          new TypeOption("fullscreen", "Pełny ekran", "grid"),
          new TypeOption("toast", "Toast", "bell"));

  private static final List<Block> BLOCKS_PL =
      List.of(
          new Block("list", "Nagłówek", "heading"),
          new Block("list", "Tekst", "text"),
          new Block("eye", "Obraz", "image"),
          new Block("mail", "Pole e-mail", "email-field"),
          new Block("user", "Pole tekstowe", "text-field"),
          new Block("play", "Przycisk CTA", "cta"),
          new Block("coupon", "Kod kuponu", "coupon"),
          new Block("cart", "Karuzela produktów", "carousel"),
          new Block("check", "Checkbox zgody", "consent"),
          new Block("minus", "Licznik czasu", "countdown"));

  private static final List<String> VARIABLES =
      List.of(
          "{{customer.first_name}}",
          "{{cart.value}}",
          "{{coupon.code}}",
          "{{product.last_viewed}}");

  private static final List<Trigger> TRIGGERS_PL =
      List.of(
          new Trigger("exit intent", "accent", "kursor opuszcza okno", null, null),
          new Trigger("czas na stronie", null, null, "20", "sek."),
          new Trigger("scroll", null, null, "60", "% strony"));

  private static final List<AudienceRule> AUDIENCE_PL =
      List.of(
          new AudienceRule("Tylko niezalogowani", true),
          new AudienceRule("Nie widzieli w ostatnich <span class=\"mono\">14 dniach</span>", true),
          new AudienceRule("Tylko ruch z kampanii płatnych", false),
          new AudienceRule("Pomiń, jeśli koszyk jest pusty", true));

  private static final List<String> ACCENT_COLORS =
      List.of(
          "oklch(0.42 0.06 150)",
          "oklch(0.55 0.07 55)",
          "oklch(0.52 0.12 32)",
          "oklch(0.22 0.02 150)");

  private static final List<Widget> WIDGETS_EN =
      List.of(
          new Widget("p1", "Exit intent — 10% off", "active", "modal", 48210, 7.4),
          new Widget("p2", "Free delivery bar over €199", "active", "banner", 182400, 2.1),
          new Widget("p3", "Slide-in: sign up to the newsletter", "active", "slide-in", 62100, 4.8),
          new Widget("p4", "Social proof — “someone just bought”", "paused", "toast", 0, 0.0),
          new Widget("p5", "Summer promo — full screen", "draft", "fullscreen", 0, 0.0));

  private static final List<TypeOption> TYPES_EN =
      List.of(
          new TypeOption("modal", "Modal", "layout"),
          new TypeOption("slide-in", "Slide-in", "arrow_right"),
          new TypeOption("banner", "Bar", "minus"),
          new TypeOption("fullscreen", "Full screen", "grid"),
          new TypeOption("toast", "Toast", "bell"));

  private static final List<Block> BLOCKS_EN =
      List.of(
          new Block("list", "Heading", "heading"),
          new Block("list", "Text", "text"),
          new Block("eye", "Image", "image"),
          new Block("mail", "E-mail field", "email-field"),
          new Block("user", "Text field", "text-field"),
          new Block("play", "CTA button", "cta"),
          new Block("coupon", "Coupon code", "coupon"),
          new Block("cart", "Product carousel", "carousel"),
          new Block("check", "Consent checkbox", "consent"),
          new Block("minus", "Countdown", "countdown"));

  private static final List<Trigger> TRIGGERS_EN =
      List.of(
          new Trigger("exit intent", "accent", "the cursor leaves the window", null, null),
          new Trigger("time on page", null, null, "20", "sec."),
          new Trigger("scroll", null, null, "60", "% of the page"));

  private static final List<AudienceRule> AUDIENCE_EN =
      List.of(
          new AudienceRule("Signed-out visitors only", true),
          new AudienceRule(
              "Have not seen it in the last <span class=\"mono\">14 days</span>", true),
          new AudienceRule("Traffic from paid campaigns only", false),
          new AudienceRule("Skip when the basket is empty", true));

  private WidgetFixtures() {}

  public static List<Widget> all(SupportedLocale locale) {
    return switch (locale) {
      case PL -> WIDGETS_PL;
      case EN -> WIDGETS_EN;
    };
  }

  public static StatusChip statusChip(SupportedLocale locale, String status) {
    return switch (locale) {
      case PL ->
          switch (status) {
            case "active" -> new StatusChip("good", "Aktywny");
            case "paused" -> new StatusChip("warn", "Wstrzymany");
            default -> new StatusChip("neutral", "Szkic");
          };
      case EN ->
          switch (status) {
            case "active" -> new StatusChip("good", "Active");
            case "paused" -> new StatusChip("warn", "Paused");
            default -> new StatusChip("neutral", "Draft");
          };
    };
  }

  /**
   * An unmatched type falls through to its own raw value, exactly like the old stack's {@code
   * match} expression, which never validated the type either.
   */
  public static String typeLabel(SupportedLocale locale, String type) {
    return switch (locale) {
      case PL ->
          switch (type) {
            case "modal" -> "Modal";
            case "slide-in" -> "Slide-in";
            case "banner" -> "Pasek banner";
            case "fullscreen" -> "Pełny ekran";
            case "toast" -> "Toast";
            default -> type;
          };
      case EN ->
          switch (type) {
            case "modal" -> "Modal";
            case "slide-in" -> "Slide-in";
            case "banner" -> "Banner bar";
            case "fullscreen" -> "Full screen";
            case "toast" -> "Toast";
            default -> type;
          };
    };
  }

  public static List<TypeOption> types(SupportedLocale locale) {
    return switch (locale) {
      case PL -> TYPES_PL;
      case EN -> TYPES_EN;
    };
  }

  public static List<Block> blocks(SupportedLocale locale) {
    return switch (locale) {
      case PL -> BLOCKS_PL;
      case EN -> BLOCKS_EN;
    };
  }

  /** Template placeholders — the same tokens whatever the page is read in. */
  public static List<String> variables() {
    return VARIABLES;
  }

  public static List<Trigger> triggers(SupportedLocale locale) {
    return switch (locale) {
      case PL -> TRIGGERS_PL;
      case EN -> TRIGGERS_EN;
    };
  }

  public static List<AudienceRule> audience(SupportedLocale locale) {
    return switch (locale) {
      case PL -> AUDIENCE_PL;
      case EN -> AUDIENCE_EN;
    };
  }

  /** Colour values only — no words. */
  public static List<String> accentColors() {
    return ACCENT_COLORS;
  }

  /**
   * {@code WidgetCatalog::content()}'s three shapes: {@code banner} (kicker/title/cta only), {@code
   * toast} (title/body/cta only), and every other type (the full six-field shape) — an unmatched
   * type falls into the default (six-field) shape with its own raw value in {@code type}, exactly
   * like the old stack's {@code match} expression's own {@code default} arm never validated the
   * type either.
   */
  public static Content content(SupportedLocale locale, String type) {
    return switch (locale) {
      case PL -> polishContent(type);
      case EN -> englishContent(type);
    };
  }

  private static Content polishContent(String type) {
    return switch (type) {
      case "banner" ->
          new Content(
              "banner",
              "DARMOWA DOSTAWA",
              "Od 199 zł wysyłamy na nasz koszt",
              null,
              null,
              "Do zakupów →",
              null);
      case "toast" ->
          new Content(
              "toast",
              null,
              "Ktoś właśnie kupił",
              "Zielona herbata Sencha 100g · Kraków, 4 min temu",
              null,
              "Zobacz",
              null);
      default ->
          new Content(
              type,
              "CZEKAJ —",
              "Zostań na 10% taniej",
              "Zapisz się do newslettera i odbierz kupon na pierwsze zamówienie. Trwa to 30"
                  + " sekund.",
              "twoj@email.pl",
              "Wyślij mi kupon →",
              "Bez spamu. Wypisujesz się w 1 kliknięciu.");
    };
  }

  private static Content englishContent(String type) {
    return switch (type) {
      case "banner" ->
          new Content(
              "banner",
              "FREE DELIVERY",
              "Over €199 we cover the postage",
              null,
              null,
              "Start shopping →",
              null);
      case "toast" ->
          new Content(
              "toast",
              null,
              "Someone just bought",
              "Sencha green tea 100g · Manchester, 4 min ago",
              null,
              "Take a look",
              null);
      default ->
          new Content(
              type,
              "WAIT —",
              "Stay for 10% off",
              "Sign up to the newsletter and get a coupon for your first order. It takes 30"
                  + " seconds.",
              "you@email.com",
              "Send me the coupon →",
              "No spam. One click to unsubscribe.");
    };
  }
}
