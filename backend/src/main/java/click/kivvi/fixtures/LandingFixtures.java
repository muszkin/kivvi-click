package click.kivvi.fixtures;

import click.kivvi.domain.Format;
import java.util.ArrayList;
import java.util.List;

/**
 * Copy for the public marketing page: feature grid, the three onboarding steps, and the Free / Pro
 * pricing pair — ported from {@code LandingContent}. None of it is translated (the old stack never
 * ran these arrays through the translator either), so it is the same for every locale.
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

  /** {@code unit} carries its own leading space, mirroring {@code LandingContent::plans()}. */
  public record Plan(
      String tier,
      String price,
      String unit,
      List<String> items,
      String cta,
      boolean featured,
      String badge) {}

  public record PreviewTile(String label, String value, String unit) {}

  private static final List<Feature> FEATURES =
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
              "Collaborative filtering, „kupili też”, „podobne”, „trending”. Plan Pro"
                  + " odblokowuje pełen silnik personalizacji."));

  private static final List<Step> STEPS =
      List.of(
          new Step(
              "01",
              "Wklej snippet",
              "Jeden tag <script> w <head>. Po 30 sekundach zaczynasz widzieć zdarzenia w"
                  + " panelu."),
          new Step(
              "02",
              "Wybierz szablon",
              "Powitanie, porzucony koszyk, win-back, rekomendacje — startuj z gotowca i"
                  + " dopasuj do Twoich tonacji."),
          new Step(
              "03",
              "Publikuj",
              "Najpierw test na danych historycznych. Potem przycisk „Opublikuj” — i"
                  + " automatyzacja działa."));

  private static final List<Plan> PLANS =
      List.of(
          new Plan(
              "Free",
              "0",
              " zł / mies.",
              List.of(
                  "1 strona, do 50 000 zdarzeń / mies.",
                  "1 000 maili / mies.",
                  "Reguły, popupy, kupony",
                  "Wsparcie społeczności"),
              "Zacznij za darmo",
              false,
              null),
          new Plan(
              "Pro",
              "149",
              " zł / mies.",
              List.of(
                  "5 stron, bez limitu zdarzeń",
                  "Bez limitu maili (Twój dostawca)",
                  "Pełny silnik rekomendacji ML",
                  "Webhooks, API, RODO/DPA",
                  "Wsparcie e-mail w 24h"),
              "Zacznij 14-dniowy trial →",
              true,
              "popularne"));

  private static final List<String> TRUST_POINTS =
      List.of("14 dni Pro za darmo", "Bez karty", "Skrypt 2 KB");

  private LandingFixtures() {}

  public static List<Feature> features() {
    return FEATURES;
  }

  public static List<Step> steps() {
    return STEPS;
  }

  public static List<Plan> plans() {
    return PLANS;
  }

  public static List<String> trustPoints() {
    return TRUST_POINTS;
  }

  /**
   * KPI tiles inside the framed product preview — mirrors {@code LandingContent::previewTiles()},
   * pre-formatted with {@link Format#number}.
   */
  public static List<PreviewTile> previewTiles() {
    return List.of(
        new PreviewTile("Zdarzeń / min", "847", null),
        new PreviewTile("Aktywne sesje", "312", null),
        new PreviewTile("Maile (24h)", Format.number(8410), null),
        new PreviewTile("Przychód (24h)", Format.number(94200), "zł"));
  }

  /**
   * Traffic shape drawn inside the framed product preview — mirrors {@code
   * LandingContent::previewSeries()} exactly: the panel's cardiogram is a live canvas, the
   * marketing page has no live account behind it, so the same shape is server-rendered as a
   * sparkline instead of showing a visitor an empty chart.
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

  /** Half-away-from-zero rounding to 2 decimals — every value here is positive. */
  private static double round2(double value) {
    return Math.round(value * 100) / 100.0;
  }
}
