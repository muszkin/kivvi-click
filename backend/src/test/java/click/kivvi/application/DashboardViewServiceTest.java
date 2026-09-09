package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import click.kivvi.domain.SupportedLocale;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DashboardViewServiceTest {

  private final DashboardViewService service = new DashboardViewService();

  @Test
  @DisplayName("B23 four KPIs, each with a 40-point sparkline, matching the oracle exactly")
  void fourKpisWithSparklines() {
    DashboardViewService.Payload payload = service.build(SupportedLocale.PL, Instant.now());

    assertThat(payload.kpis()).hasSize(4);
    assertThat(payload.kpis())
        .extracting(
            DashboardViewService.KpiView::label,
            DashboardViewService.KpiView::value,
            DashboardViewService.KpiView::delta,
            DashboardViewService.KpiView::dir)
        .containsExactly(
            tuple("Zdarzeń ostatnia minuta", "847", "+12,4% vs śr.", "up"),
            tuple("Aktywne sesje", "312", "+4,1%", "up"),
            tuple("Maile dostarczone (24h)", "8 410", "−2,0%", "down"),
            tuple("Przypisany przychód (24h)", "94 200 zł", "+22,4%", "up"));
    assertThat(payload.kpis()).allSatisfy(kpi -> assertThat(kpi.series()).hasSize(40));
  }

  @Test
  @DisplayName("B23 the sparkline series is deterministic between two builds")
  void seriesIsDeterministic() {
    DashboardViewService.Payload first = service.build(SupportedLocale.PL, Instant.now());
    DashboardViewService.Payload second = service.build(SupportedLocale.PL, Instant.now());

    assertThat(first.kpis().get(0).series()).isEqualTo(second.kpis().get(0).series());
  }

  @Test
  @DisplayName("B23 the cardiogram legend carries the three oracle entries")
  void cardiogramLegend() {
    DashboardViewService.Payload payload = service.build(SupportedLocale.PL, Instant.now());

    assertThat(payload.legend())
        .extracting(DashboardViewService.LegendView::label, DashboardViewService.LegendView::value)
        .containsExactly(
            tuple("Średnia 5 min:", "14,2 ev/s"),
            tuple("Pik:", "28 ev/s · 12:42:18"),
            tuple("Aktualizacja:", "co 1 s"));
  }

  @Test
  @DisplayName("B23 ten live rows and the account's Mercure topic, matching the oracle's first row")
  void tenLiveRowsAndMercureTopic() {
    DashboardViewService.Payload payload = service.build(SupportedLocale.PL, Instant.now());

    assertThat(payload.events()).hasSize(10);
    assertThat(payload.mercureTopic()).isEqualTo("/accounts/1/events");
    DashboardViewService.RowView first = payload.events().get(0);
    assertThat(first.type()).isEqualTo("Dodanie do koszyka");
    assertThat(first.detail()).isEqualTo("Zielona herbata Sencha 100g");
    assertThat(first.customerName()).isEqualTo("Anna K.");
    assertThat(first.siteName()).isEqualTo("aureashop.pl");
  }

  @Test
  @DisplayName("B23 live rows translate their type label to English")
  void liveRowsTranslateToEnglish() {
    DashboardViewService.Payload payload = service.build(SupportedLocale.EN, Instant.now());

    assertThat(payload.events().get(0).type()).isEqualTo("Add to cart");
  }

  @Test
  @DisplayName("B23 six recently seen customers, orders and lastSeen matching the oracle exactly")
  void sixRecentlySeenCustomers() {
    Instant now = Instant.now();
    DashboardViewService.Payload payload = service.build(SupportedLocale.PL, now);

    assertThat(payload.recentCustomers()).hasSize(6);
    assertThat(payload.recentCustomers())
        .extracting(
            DashboardViewService.CustomerView::id,
            DashboardViewService.CustomerView::name,
            DashboardViewService.CustomerView::email,
            DashboardViewService.CustomerView::orders,
            DashboardViewService.CustomerView::lastSeen)
        .containsExactly(
            tuple("c_1000", "Anna K.", "anna.k@example.com", 0, "teraz"),
            tuple("c_1001", "Kasia N.", "kasia.n@example.com", 1, "1 min temu"),
            tuple("c_1002", "Marta C.", "marta.c@example.com", 2, "2 min temu"),
            tuple("c_1003", "Tomek P.", "tomek.p@example.com", 3, "3 min temu"),
            tuple("c_1004", "Piotr K.", "piotr.k@example.com", 4, "4 min temu"),
            tuple("c_1005", "Łukasz N.", "lukasz.n@example.com", 5, "5 min temu"));
  }

  @Test
  @DisplayName(
      "B23 the four best-performing automations, active only, highest revenue first, matching the"
          + " oracle exactly")
  void fourBestPerformingAutomations() {
    DashboardViewService.Payload payload = service.build(SupportedLocale.PL, Instant.now());

    assertThat(payload.topAutomations()).hasSize(4);
    assertThat(payload.topAutomations())
        .extracting(
            DashboardViewService.AutomationView::id,
            DashboardViewService.AutomationView::name,
            DashboardViewService.AutomationView::channels,
            DashboardViewService.AutomationView::runs,
            DashboardViewService.AutomationView::conversion,
            DashboardViewService.AutomationView::revenue)
        .containsExactly(
            tuple(
                "a3",
                "Rekomendacje „podobne produkty”",
                List.of("widget"),
                "18 420",
                "7,9%",
                "41 200 zł"),
            tuple(
                "a1",
                "Powrót do porzuconego koszyka",
                List.of("email", "popup"),
                "1 287",
                "18,4%",
                "24 800 zł"),
            tuple(
                "a4",
                "Kupon dla VIP po 5 zamówieniach",
                List.of("coupon", "email"),
                "89",
                "62,1%",
                "12 400 zł"),
            tuple("a2", "Powitanie po rejestracji", List.of("email"), "412", "41,2%", "8 930 zł"));
  }
}
