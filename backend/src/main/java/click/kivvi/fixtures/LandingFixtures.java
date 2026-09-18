package click.kivvi.fixtures;

import click.kivvi.domain.Format;
import click.kivvi.domain.SupportedLocale;
import java.util.ArrayList;
import java.util.List;

/**
 * Copy for the public marketing page: feature grid, the four onboarding steps, the trust points and
 * the preview tiles' labels — ported from {@code LandingContent}.
 *
 * <p>PIO-117 made it locale-aware. The old stack never ran these arrays through the translator, so
 * the port served the same Polish copy to {@code /en} as well — tolerable while English was an
 * optional toggle, not once PIO-125 made it the default. Every accessor takes the locale and
 * switches over it exhaustively: a third {@link SupportedLocale} does not compile until it has copy
 * of its own here, rather than silently rendering someone else's.
 *
 * <p>Polish and English carry the same entries in the same order — the same icons, step numbers and
 * values — and differ only in their words. {@code LandingFixturesTest} holds them to that.
 *
 * <p>PIO-121 removed the Free / Pro pricing pair: the software is MIT-licensed and self-hostable,
 * so there is no price list to render.
 *
 * <p>Returns its own record types rather than {@code application.LandingView}'s nested ones —
 * mirrors {@code ShellFixtures.Workspace}: {@code fixtures} is a leaf package, so an application
 * view-model type used here would create the same {@code application -> fixtures -> application}
 * cycle ArchUnit's "free of cycles" rule forbids. {@code LandingViewService} maps these into the
 * wire shape.
 */
public final class LandingFixtures {

  public record Feature(String icon, String title, String body) {}

  public record Step(String number, String title, String body) {}

  public record PreviewTile(String label, String value, String unit) {}

  private static final List<Feature> FEATURES_PL =
      List.of(
          new Feature(
              "activity",
              "Strumień zdarzeń na żywo",
              "Widzisz każde kliknięcie, dodanie do koszyka i zakup — z dokładnością do"
                  + " milisekundy. Mercure-driven, bez polling-u."),
          new Feature(
              "bolt",
              "Reguły bez kodowania",
              "KIEDY → JEŚLI → WTEDY. Skomponuj automatyzację z bloków lub przeciągnij węzły na"
                  + " płótnie. Test reguły na danych historycznych zanim wystartujesz."),
          new Feature(
              "mail",
              "E-maile z prawdziwego zdarzenia",
              "WYSIWYG z blokami i zmiennymi. Wysyłka przez Twojego dostawcę (SMTP / SES /"
                  + " SendGrid). Tracking otwarć i kliknięć w panelu."),
          new Feature(
              "layout",
              "Popupy i web layery",
              "Modale, slide-iny, paski. Pełna kontrola nad triggerami: exit intent, czas na"
                  + " stronie, scroll, segment klienta."),
          new Feature(
              "coupon",
              "Kupony w punkcie konwersji",
              "Generuj unikalne kody, pokazuj wtedy gdy mają znaczenie — przy porzuceniu"
                  + " koszyka, dla VIP-ów, po N zakupach."),
          new Feature(
              "target",
              "Rekomendacje ML",
              "Collaborative filtering, „kupili też”, „podobne”, „trending”."));

  private static final List<Feature> FEATURES_EN =
      List.of(
          new Feature(
              "activity",
              "Live event stream",
              "See every click, add-to-cart and purchase as it happens, down to the millisecond."
                  + " Pushed over Mercure, with no polling."),
          new Feature(
              "bolt",
              "Rules without code",
              "WHEN → IF → THEN. Build an automation from blocks or drag nodes onto the canvas."
                  + " Test a rule against historical data before it goes live."),
          new Feature(
              "mail",
              "Emails done properly",
              "A WYSIWYG editor with blocks and variables. Sent through your own provider (SMTP /"
                  + " SES / SendGrid), with opens and clicks tracked in the panel."),
          new Feature(
              "layout",
              "Popups and web layers",
              "Modals, slide-ins, bars. Full control over what triggers them: exit intent, time"
                  + " on page, scroll depth, customer segment."),
          new Feature(
              "coupon",
              "Coupons at the point of conversion",
              "Generate unique codes and show them at the moment they matter: on cart"
                  + " abandonment, to VIPs, after N purchases."),
          new Feature(
              "target",
              "ML recommendations",
              "Collaborative filtering: “customers also bought”, “similar products”,"
                  + " “trending”."));

  /**
   * PIO-121 put deployment first. The three steps this used to hold described a hosted product a
   * visitor walks into and pastes a snippet on; the landing page now says the software is
   * self-hostable, so the path has to start with getting an instance of your own.
   */
  private static final List<Step> STEPS_PL =
      List.of(
          new Step(
              "01",
              "Postaw u siebie",
              "Klonujesz repozytorium i stawiasz instancję jednym poleceniem. Nie chcesz sam —"
                  + " robimy to za Ciebie."),
          new Step(
              "02",
              "Wklej snippet",
              "Jeden tag <script> w <head>. Po 30 sekundach zaczynasz widzieć zdarzenia w swoim"
                  + " panelu."),
          new Step(
              "03",
              "Wybierz szablon",
              "Powitanie, porzucony koszyk, win-back, rekomendacje — startuj z gotowca i"
                  + " dopasuj do Twoich tonacji."),
          new Step(
              "04",
              "Publikuj",
              "Najpierw test na danych historycznych. Potem przycisk „Opublikuj” — i"
                  + " automatyzacja działa."));

  /**
   * Step 01's wording is the one agreed when PIO-121 added it and parked beside the Polish step
   * until these fixtures could carry it.
   */
  private static final List<Step> STEPS_EN =
      List.of(
          new Step(
              "01",
              "Run it yourself",
              "Clone the repository and bring an instance up with one command. Would rather not"
                  + " — we will do it for you."),
          new Step(
              "02",
              "Paste the snippet",
              "One <script> tag in your <head>. Within 30 seconds, events start showing up in"
                  + " your panel."),
          new Step(
              "03",
              "Pick a template",
              "Welcome, abandoned cart, win-back, recommendations — start from a ready-made"
                  + " template and adjust it to your tone of voice."),
          new Step(
              "04",
              "Publish",
              "First, a test run on historical data. Then press “Publish” and the automation is"
                  + " live."));

  private static final List<String> TRUST_POINTS_PL =
      List.of("Licencja MIT", "Postawisz u siebie", "Skrypt 2 KB");

  private static final List<String> TRUST_POINTS_EN =
      List.of("MIT licence", "Host it yourself", "2 KB script");

  private static final int PREVIEW_EVENTS_PER_MINUTE = 847;
  private static final int PREVIEW_ACTIVE_SESSIONS = 312;
  private static final int PREVIEW_EMAILS_PER_DAY = 8410;
  private static final int PREVIEW_REVENUE_PER_DAY = 94200;

  private LandingFixtures() {}

  public static List<Feature> features(SupportedLocale locale) {
    return switch (locale) {
      case PL -> FEATURES_PL;
      case EN -> FEATURES_EN;
    };
  }

  public static List<Step> steps(SupportedLocale locale) {
    return switch (locale) {
      case PL -> STEPS_PL;
      case EN -> STEPS_EN;
    };
  }

  public static List<String> trustPoints(SupportedLocale locale) {
    return switch (locale) {
      case PL -> TRUST_POINTS_PL;
      case EN -> TRUST_POINTS_EN;
    };
  }

  /**
   * KPI tiles inside the framed product preview — mirrors {@code LandingContent::previewTiles()},
   * pre-formatted with {@link Format#number}.
   *
   * <p>The numbers are the same in both languages; the labels and the currency are not. The digit
   * grouping stays Format's narrow no-break space in English too: it is the SI convention, and it
   * reads the same whichever language surrounds it.
   */
  public static List<PreviewTile> previewTiles(SupportedLocale locale) {
    return switch (locale) {
      case PL ->
          previewTiles(
              locale, "Zdarzeń / min", "Aktywne sesje", "Maile (24h)", "Przychód (24h)", "zł");
      // PIO-129: the English tile used to say "PLN" while the panel it previews now trades in
      // euro for English readers. One preview, one currency.
      case EN ->
          previewTiles(
              locale, "Events / min", "Active sessions", "Emails (24h)", "Revenue (24h)", "EUR");
    };
  }

  /**
   * Traffic shape drawn inside the framed product preview — mirrors {@code
   * LandingContent::previewSeries()} exactly: the panel's cardiogram is a live canvas, the
   * marketing page has no live account behind it, so the same shape is server-rendered as a
   * sparkline instead of showing a visitor an empty chart. Numbers only, so it has no locale.
   */
  public static List<Double> previewSeries() {
    List<Double> values = new ArrayList<>(60);
    for (int i = 0; i < 60; i++) {
      double pulse = Math.sin(i / 2.2) + 0.6 * Math.sin(i / 0.9) + 0.35 * Math.sin(i / 5.5);
      double ramp = i > 44 ? (i - 44) * 0.9 : 0;
      values.add(round2(12 + 5 * pulse + ramp));
    }
    return List.copyOf(values);
  }

  private static List<PreviewTile> previewTiles(
      SupportedLocale locale,
      String eventsLabel,
      String sessionsLabel,
      String emailsLabel,
      String revenueLabel,
      String currency) {
    return List.of(
        new PreviewTile(eventsLabel, Format.number(PREVIEW_EVENTS_PER_MINUTE, locale), null),
        new PreviewTile(sessionsLabel, Format.number(PREVIEW_ACTIVE_SESSIONS, locale), null),
        new PreviewTile(emailsLabel, Format.number(PREVIEW_EMAILS_PER_DAY, locale), null),
        new PreviewTile(revenueLabel, Format.number(PREVIEW_REVENUE_PER_DAY, locale), currency));
  }

  /** Half-away-from-zero rounding to 2 decimals — every value here is positive. */
  private static double round2(double value) {
    return Math.round(value * 100) / 100.0;
  }
}
