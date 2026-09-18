package click.kivvi.fixtures;

import click.kivvi.domain.Format;
import click.kivvi.domain.SupportedLocale;
import java.util.List;

/**
 * Dashboard-only fixture data: the four KPI tiles' literal label/value/delta copy plus the
 * sparkline generator's {@code (base, variance)} pair, and the cardiogram's three legend entries —
 * ported from {@code DashboardMetrics}'s private constants. The cardiogram's three range-button
 * labels are not here: {@code dashboard.html.twig} passed them to the {@code cardiogram} include as
 * an inline literal, not through {@code DashboardController}'s view-model, so the SPA port keeps
 * them the same way — a literal in {@code DashboardView.vue}, not API data.
 *
 * <p>PIO-129 made it locale-aware, the same way {@link LandingFixtures} already was: the two lists
 * carry the same entries in the same order, with the same sparkline seeds and the same underlying
 * figures, and differ only in their words and their number/currency convention.
 *
 * <p>The sparkline math itself ({@code DashboardViewService.series}) is a computation, not fixture
 * data, so it stays in the view service — mirrors how {@link CustomersFixtures} carries a raw
 * {@code lastSeenMinutes} and {@link click.kivvi.application.CustomersViewService} computes the
 * displayed "ago" string from it, rather than the fixture doing it.
 */
public final class DashboardFixtures {

  /**
   * One KPI tile's literal copy, before {@link click.kivvi.application.DashboardViewService}
   * attaches its computed sparkline. {@code value} already carries whatever formatting {@code
   * DashboardMetrics::kpis()} applied on the old stack — a literal string for the two tiles that
   * needed none, {@link Format#number} / {@link Format#money} for the two that did.
   */
  public record KpiSeed(
      String label,
      String value,
      String delta,
      String dir,
      double seriesBase,
      double seriesVariance) {}

  /** One cardiogram legend entry — label and pre-formatted value, rendered side by side. */
  public record Legend(String label, String value) {}

  private static final int EMAILS_DELIVERED_24H = 8410;
  private static final int REVENUE_ATTRIBUTED_24H = 94200;

  private static final List<KpiSeed> KPIS_PL =
      List.of(
          new KpiSeed("Zdarzeń ostatnia minuta", "847", "+12,4% vs śr.", "up", 60, 0.4),
          new KpiSeed("Aktywne sesje", "312", "+4,1%", "up", 30, 0.3),
          new KpiSeed(
              "Maile dostarczone (24h)",
              Format.number(EMAILS_DELIVERED_24H, SupportedLocale.PL),
              "−2,0%",
              "down",
              90,
              0.25),
          new KpiSeed(
              "Przypisany przychód (24h)",
              Format.money(REVENUE_ATTRIBUTED_24H, SupportedLocale.PL),
              "+22,4%",
              "up",
              100,
              0.5));

  private static final List<KpiSeed> KPIS_EN =
      List.of(
          new KpiSeed("Events last minute", "847", "+12.4% vs avg.", "up", 60, 0.4),
          new KpiSeed("Active sessions", "312", "+4.1%", "up", 30, 0.3),
          new KpiSeed(
              "Emails delivered (24h)",
              Format.number(EMAILS_DELIVERED_24H, SupportedLocale.EN),
              "−2.0%",
              "down",
              90,
              0.25),
          new KpiSeed(
              "Revenue attributed (24h)",
              Format.money(REVENUE_ATTRIBUTED_24H, SupportedLocale.EN),
              "+22.4%",
              "up",
              100,
              0.5));

  private static final List<Legend> LEGEND_PL =
      List.of(
          new Legend("Średnia 5 min:", "14,2 ev/s"),
          new Legend("Pik:", "28 ev/s · 12:42:18"),
          new Legend("Aktualizacja:", "co 1 s"));

  private static final List<Legend> LEGEND_EN =
      List.of(
          new Legend("5-minute average:", "14.2 ev/s"),
          new Legend("Peak:", "28 ev/s · 12:42:18"),
          new Legend("Refresh:", "every 1 s"));

  private DashboardFixtures() {}

  public static List<KpiSeed> kpis(SupportedLocale locale) {
    return switch (locale) {
      case PL -> KPIS_PL;
      case EN -> KPIS_EN;
    };
  }

  public static List<Legend> legend(SupportedLocale locale) {
    return switch (locale) {
      case PL -> LEGEND_PL;
      case EN -> LEGEND_EN;
    };
  }
}
