package click.kivvi.domain.tracking;

import click.kivvi.domain.SupportedLocale;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The tracking script's thirteen event types, ported from {@code TrackedEvent::SUPPORTED_TYPES}
 * (validation) merged with {@code EventFeed::TYPES} (display: icon, tone, and the eight types the
 * sample feed and its filter chips ever show). A type outside the eight-entry {@link #FEED_TYPES}
 * subset falls back to the same {@code icon="activity", tone=""} pair {@code
 * EventIngestion::typeDescriptor()} used for it — reproduced here as the type's own icon/tone
 * rather than as a runtime fallback, since the two are indistinguishable in the wire payload.
 */
public enum EventType {
  PAGEVIEW("pageview", "Wyświetlenie strony", "Pageview", "eye", ""),
  ADD_TO_CART("add_to_cart", "Dodanie do koszyka", "Add to cart", "cart", "accent"),
  REMOVE_FROM_CART("remove_from_cart", "Usunięcie z koszyka", "Remove from cart", "activity", ""),
  PURCHASE("purchase", "Zakup", "Purchase", "money", "good"),
  LOGIN("login", "Zalogowanie", "Login", "user", "info"),
  SIGNUP("signup", "Rejestracja", "Sign up", "user", "brown"),
  SEARCH("search", "Wyszukiwanie", "Search", "search", ""),
  CART_ABANDON("cart_abandon", "Porzucony koszyk", "Cart abandon", "cart", "warn"),
  WISHLIST("wishlist", "Lista życzeń", "Wishlist", "heart", ""),
  EMAIL_OPEN("email_open", "Otwarcie maila", "Email open", "activity", ""),
  EMAIL_CLICK("email_click", "Kliknięcie w mailu", "Email click", "activity", ""),
  POPUP_SHOWN("popup_shown", "Wyświetlenie popupu", "Popup shown", "activity", ""),
  COUPON_USED("coupon_used", "Użycie kuponu", "Coupon used", "activity", "");

  /**
   * {@code EventFeed::TYPES}, in its exact declared order: the subset the sample feed cycles
   * through and the type filter rail offers — {@code rows()}'s {@code $i * 3 + 1} sampling and
   * {@code typeFilters()}'s iteration both depend on this exact eight-entry order.
   */
  public static final List<EventType> FEED_TYPES =
      List.of(PAGEVIEW, ADD_TO_CART, PURCHASE, LOGIN, SIGNUP, SEARCH, CART_ABANDON, WISHLIST);

  private final String code;
  private final String labelPl;
  private final String labelEn;
  private final String icon;
  private final String tone;

  EventType(String code, String labelPl, String labelEn, String icon, String tone) {
    this.code = code;
    this.labelPl = labelPl;
    this.labelEn = labelEn;
    this.icon = icon;
    this.tone = tone;
  }

  public String code() {
    return code;
  }

  public String icon() {
    return icon;
  }

  public String tone() {
    return tone;
  }

  /** The translated label the SPA and the Mercure-published row both display. */
  public String label(SupportedLocale locale) {
    return locale == SupportedLocale.EN ? labelEn : labelPl;
  }

  public static Optional<EventType> fromCode(String candidate) {
    return Arrays.stream(values()).filter(type -> type.code.equals(candidate)).findFirst();
  }
}
