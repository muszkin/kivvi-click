package click.kivvi.application.waitlist;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.mail.OutboundMail;
import click.kivvi.domain.waitlist.ConfirmationToken;
import click.kivvi.domain.waitlist.OpaqueToken;
import click.kivvi.infrastructure.mail.MailTemplateRenderer;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 * Turns a pair of freshly issued tokens into the message that carries them.
 *
 * <p>Both halves of the message come from the same keys in {@code messages_pl.properties} / {@code
 * messages_en.properties}: the HTML through Thymeleaf's {@code #{...}}, the plain-text part
 * assembled here. That is not duplication for its own sake — a plain-text alternative is mandatory
 * (spam scoring, text clients), and building it from the same strings is what keeps the two from
 * drifting into saying different things.
 *
 * <p>Links are absolute and built from configuration, not from the incoming request. The message is
 * composed now and sent by a background job later, possibly after a restart, so there is no request
 * to derive a host from; and deriving one from a visitor-controlled {@code Host} header is how a
 * confirmation link ends up pointing at somebody else's server.
 */
@Service
public class WaitlistMailComposer {

  private static final String TEMPLATE = "email/waitlist-confirmation";

  private static final String SUBJECT_KEY = "mail.waitlist.confirm.subject";
  private static final String HEADING_KEY = "mail.waitlist.confirm.heading";
  private static final String INTRO_KEY = "mail.waitlist.confirm.intro";
  private static final String FALLBACK_KEY = "mail.waitlist.confirm.fallback";
  private static final String EXPIRY_KEY = "mail.waitlist.confirm.expiry";
  private static final String IGNORE_KEY = "mail.waitlist.confirm.ignore";
  private static final String UNSUBSCRIBE_KEY = "mail.unsubscribe";

  /**
   * One queued confirmation per issued token. A resend issues a new token, so it gets a new key and
   * goes out; a double submit reuses neither and is collapsed into one message.
   */
  private static final String DEDUP_KEY_PREFIX = "waitlist-confirm:";

  private final MailTemplateRenderer renderer;
  private final MessageSource messageSource;
  private final String baseUrl;

  public WaitlistMailComposer(
      MailTemplateRenderer renderer,
      MessageSource messageSource,
      @Value("${kivvi.mail.base-url}") String baseUrl) {
    this.renderer = renderer;
    this.messageSource = messageSource;
    // A trailing slash here would produce "//pl/..." in every link.
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  public OutboundMail confirmation(
      String recipient,
      SupportedLocale localeCode,
      ConfirmationToken confirmation,
      OpaqueToken unsubscribe) {
    Locale locale = Locale.forLanguageTag(localeCode.code());
    String confirmUrl = confirmUrl(localeCode, confirmation.value());
    String unsubscribeUrl = unsubscribeUrl(localeCode, unsubscribe.value());

    String html =
        renderer.render(
            TEMPLATE,
            locale,
            Map.of(
                "locale", localeCode.code(),
                "recipient", recipient,
                "confirmUrl", confirmUrl,
                "unsubscribeUrl", unsubscribeUrl));

    return new OutboundMail(
        recipient,
        message(SUBJECT_KEY, locale),
        html,
        plainText(locale, recipient, confirmUrl, unsubscribeUrl),
        DEDUP_KEY_PREFIX + confirmation.hash());
  }

  public String confirmUrl(SupportedLocale locale, String token) {
    return baseUrl + "/" + locale.code() + "/waitlist/confirm/" + token;
  }

  public String unsubscribeUrl(SupportedLocale locale, String token) {
    return baseUrl + "/" + locale.code() + "/waitlist/unsubscribe/" + token;
  }

  private String plainText(
      Locale locale, String recipient, String confirmUrl, String unsubscribeUrl) {
    return String.join(
        "\n\n",
        message(HEADING_KEY, locale),
        message(INTRO_KEY, locale),
        message(FALLBACK_KEY, locale) + "\n" + confirmUrl,
        message(EXPIRY_KEY, locale),
        message(IGNORE_KEY, locale),
        "-- \n" + recipient + "\n" + message(UNSUBSCRIBE_KEY, locale) + ": " + unsubscribeUrl);
  }

  private String message(String key, Locale locale) {
    return messageSource.getMessage(key, null, locale);
  }
}
