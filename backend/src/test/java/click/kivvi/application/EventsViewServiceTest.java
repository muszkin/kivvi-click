package click.kivvi.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import click.kivvi.domain.SupportedLocale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventsViewServiceTest {

  private final EventsViewService service = new EventsViewService();

  @Test
  @DisplayName("B24 30 rows, 9 type filters (8 + all) and 4 site filters (3 + all)")
  void thirtyRowsAndTheFilterCounts() {
    EventsViewService.Payload payload = service.build(SupportedLocale.PL, null, null, null);

    assertThat(payload.events()).hasSize(30);
    assertThat(payload.typeFilters()).hasSize(9);
    assertThat(payload.siteFilters()).hasSize(4);
    assertThat(payload.ranges()).hasSize(4);
    assertThat(payload.total()).isEqualTo("9 360");
    assertThat(payload.shown()).isEqualTo(30);
    assertThat(payload.mercureTopic()).isEqualTo("/accounts/1/events");
  }

  @Test
  @DisplayName(
      "the type/site/range query parameters mark a filter active but never change the rows")
  void filterParametersOnlyMarkAChipActiveNeverFilterTheRows() {
    EventsViewService.Payload unfiltered = service.build(SupportedLocale.PL, null, null, null);
    EventsViewService.Payload filtered =
        service.build(SupportedLocale.PL, "purchase", "aurea", "24h");

    // `time` is excluded: it is derived from the wall clock at each build() call and may tick
    // over a second between the two calls above — DEV-1 masks it for exactly this reason.
    assertThat(filtered.events())
        .extracting(
            EventsViewService.RowView::type,
            EventsViewService.RowView::detail,
            EventsViewService.RowView::customerName,
            EventsViewService.RowView::siteName)
        .isEqualTo(
            unfiltered.events().stream()
                .map(row -> tuple(row.type(), row.detail(), row.customerName(), row.siteName()))
                .toList());
    assertThat(filtered.total()).isEqualTo(unfiltered.total());
    assertThat(filtered.shown()).isEqualTo(unfiltered.shown());

    assertThat(filtered.typeFilters())
        .filteredOn(EventsViewService.FilterView::active)
        .extracting(EventsViewService.FilterView::payload)
        .containsExactly("purchase");
    assertThat(filtered.siteFilters())
        .filteredOn(EventsViewService.FilterView::active)
        .extracting(EventsViewService.FilterView::payload)
        .containsExactly("aurea");
    assertThat(filtered.ranges())
        .filteredOn(EventsViewService.RangeView::active)
        .extracting(EventsViewService.RangeView::value)
        .containsExactly("24h");
  }

  @Test
  @DisplayName("with no filter given, 'all' and the '1h' range are active by default")
  void defaultsToAllAndOneHour() {
    EventsViewService.Payload payload = service.build(SupportedLocale.PL, null, null, null);

    assertThat(payload.typeFilters().get(0).active()).isTrue();
    assertThat(payload.typeFilters().get(0).payload()).isEqualTo("all");
    assertThat(payload.siteFilters().get(0).active()).isTrue();
    assertThat(payload.siteFilters().get(0).payload()).isEqualTo("all");
    assertThat(payload.ranges())
        .extracting(EventsViewService.RangeView::value, EventsViewService.RangeView::active)
        .containsExactly(
            tuple("5m", false), tuple("1h", true), tuple("24h", false), tuple("7d", false));
  }

  @Test
  @DisplayName("type labels and filter chip icons are translated per locale")
  void typeLabelsAreTranslatedPerLocale() {
    EventsViewService.Payload pl = service.build(SupportedLocale.PL, null, null, null);
    EventsViewService.Payload en = service.build(SupportedLocale.EN, null, null, null);

    assertThat(pl.typeFilters()).extracting(EventsViewService.FilterView::label).contains("Zakup");
    assertThat(en.typeFilters())
        .extracting(EventsViewService.FilterView::label)
        .contains("Purchase");
    assertThat(pl.typeFilters().get(0).label()).isEqualTo("Wszystkie");
    assertThat(en.typeFilters().get(0).label()).isEqualTo("All");
  }

  @Test
  @DisplayName("the 'all' type filter carries no icon; each type filter carries its own icon")
  void typeFilterChipShape() {
    EventsViewService.Payload payload = service.build(SupportedLocale.PL, null, null, null);

    assertThat(payload.typeFilters().get(0).icon()).isNull();
    assertThat(payload.typeFilters().get(1).icon()).isEqualTo("eye");
  }

  @Test
  @DisplayName(
      "the 'all' site filter carries the globe icon; each site filter carries its dot colour")
  void siteFilterChipShape() {
    EventsViewService.Payload payload = service.build(SupportedLocale.PL, null, null, null);

    assertThat(payload.siteFilters().get(0).icon()).isEqualTo("globe");
    assertThat(payload.siteFilters().get(0).dotColor()).isNull();
    assertThat(payload.siteFilters().get(1).dotColor()).isEqualTo("#7a8763");
    assertThat(payload.siteFilters().get(1).icon()).isNull();
  }

  @Test
  @DisplayName("row generation is a deterministic function of position, not real time")
  void rowGenerationIsDeterministic() {
    EventsViewService.Payload first = service.build(SupportedLocale.PL, null, null, null);
    EventsViewService.Payload second = service.build(SupportedLocale.PL, null, null, null);

    assertThat(first.events())
        .extracting(
            EventsViewService.RowView::type,
            EventsViewService.RowView::detail,
            EventsViewService.RowView::customerName,
            EventsViewService.RowView::siteName)
        .isEqualTo(
            second.events().stream()
                .map(row -> tuple(row.type(), row.detail(), row.customerName(), row.siteName()))
                .toList());
  }

  @Test
  @DisplayName("the first row (login-derived seed 0) matches the oracle's cycling pattern")
  void firstRowMatchesTheOraclePattern() {
    EventsViewService.RowView first =
        service.build(SupportedLocale.PL, null, null, null).events().get(0);

    assertThat(first.type()).isEqualTo("Dodanie do koszyka");
    assertThat(first.detail()).isEqualTo("Zielona herbata Sencha 100g");
    assertThat(first.customerName()).isEqualTo("Anna K.");
    assertThat(first.siteName()).isEqualTo("aureashop.pl");
  }

  @Test
  @DisplayName("a login row's detail is the customer's e-mail, matching the seventh oracle row")
  void loginRowDetailIsCustomerEmail() {
    EventsViewService.RowView seventh =
        service.build(SupportedLocale.PL, null, null, null).events().get(6);

    assertThat(seventh.type()).isEqualTo("Zalogowanie");
    assertThat(seventh.detail()).isEqualTo("olek.c@example.com");
    assertThat(seventh.customerName()).isEqualTo("Olek C.");
    assertThat(seventh.siteName()).isEqualTo("aureashop.pl");
  }
}
