package click.kivvi.domain.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import click.kivvi.domain.SupportedLocale;
import org.junit.jupiter.api.Test;

class EventTypeTest {

  @Test
  void feedTypesAreTheEightDisplayTypesInEventFeedOrder() {
    assertThat(EventType.FEED_TYPES)
        .containsExactly(
            EventType.PAGEVIEW,
            EventType.ADD_TO_CART,
            EventType.PURCHASE,
            EventType.LOGIN,
            EventType.SIGNUP,
            EventType.SEARCH,
            EventType.CART_ABANDON,
            EventType.WISHLIST);
  }

  @Test
  void feedTypesCarryTheirOwnIconAndTone() {
    assertThat(EventType.FEED_TYPES)
        .extracting(EventType::icon, EventType::tone)
        .containsExactly(
            tuple("eye", ""),
            tuple("cart", "accent"),
            tuple("money", "good"),
            tuple("user", "info"),
            tuple("user", "brown"),
            tuple("search", ""),
            tuple("cart", "warn"),
            tuple("heart", ""));
  }

  @Test
  void typesOutsideTheFeedSubsetFallBackToTheGenericActivityIconAndNoTone() {
    assertThat(EventType.REMOVE_FROM_CART.icon()).isEqualTo("activity");
    assertThat(EventType.REMOVE_FROM_CART.tone()).isEmpty();
    assertThat(EventType.EMAIL_OPEN.icon()).isEqualTo("activity");
    assertThat(EventType.POPUP_SHOWN.icon()).isEqualTo("activity");
    assertThat(EventType.COUPON_USED.icon()).isEqualTo("activity");
  }

  @Test
  void allThirteenCodesRoundTripThroughFromCode() {
    for (EventType type : EventType.values()) {
      assertThat(EventType.fromCode(type.code())).contains(type);
    }
    assertThat(EventType.values()).hasSize(13);
  }

  @Test
  void anUnknownCodeIsAbsent() {
    assertThat(EventType.fromCode("teleport")).isEmpty();
  }

  @Test
  void labelsAreTranslatedPerLocale() {
    assertThat(EventType.PURCHASE.label(SupportedLocale.PL)).isEqualTo("Zakup");
    assertThat(EventType.PURCHASE.label(SupportedLocale.EN)).isEqualTo("Purchase");
  }
}
