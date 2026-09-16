package click.kivvi.fixtures;

import click.kivvi.domain.Format;
import java.util.ArrayList;
import java.util.List;

/**
 * Copy for the public marketing page: feature grid, the three onboarding steps and the trust points
 * — ported from {@code LandingContent}. None of it is translated (the old stack never ran these
 * arrays through the translator either), so it is the same for every locale.
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
              "Collaborative filtering, „kupili też”, „podobne”, „trending”."));

  /**
   * PIO-121 put deployment first. The three steps this used to hold described a hosted product a
   * visitor walks into and pastes a snippet on; the landing page now says the software is
   * self-hostable, so the path has to start with getting an instance of your own.
   *
   * <p>Polish only, like every other array here — the old stack never ran these through the
   * translator either. The English wording agreed for the new step is "Run it yourself" / "Clone
   * the repository and bring an instance up with one command. Would rather not — we will do it for
   * you."; it lands when PIO-117 makes these fixtures locale-aware.
   */
  private static final List<Step> STEPS =
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

  private static final List<String> TRUST_POINTS =
      List.of("Licencja MIT", "Postawisz u siebie", "Skrypt 2 KB");

  private LandingFixtures() {}

  public static List<Feature> features() {
    return FEATURES;
  }

  public static List<Step> steps() {
    return STEPS;
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
