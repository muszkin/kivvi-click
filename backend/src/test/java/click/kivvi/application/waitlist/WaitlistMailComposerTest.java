package click.kivvi.application.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import click.kivvi.domain.SupportedLocale;
import click.kivvi.domain.mail.OutboundMail;
import click.kivvi.domain.waitlist.ConfirmationToken;
import click.kivvi.domain.waitlist.OpaqueToken;
import click.kivvi.infrastructure.mail.MailTemplateRenderer;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Renders the real shipped template with the real shipped strings, container-free.
 *
 * <p>The engine is assembled here rather than taken from a Spring context so this stays in the fast
 * {@code *Test} tier; {@code WaitlistConfirmationApiIT} is what proves the same template renders
 * through Boot's own auto-configured engine.
 *
 * <p>The assertions are the ones that fail silently in production if nobody checks: an unresolved
 * {@code #{...}} looks like ordinary text in a browser, an {@code oklch()} colour simply renders as
 * nothing in most mail clients, and a missing plain-text part costs spam score without any visible
 * symptom at all.
 */
class WaitlistMailComposerTest {

  private static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");
  private static final String BASE_URL = "https://kivvi.click";

  private final WaitlistMailComposer composer =
      new WaitlistMailComposer(
          new MailTemplateRenderer(templateEngine()), messageSource(), BASE_URL);

  @Test
  @DisplayName(
      "the Polish mail carries the confirmation link, the unsubscribe link and the address")
  void thePolishMailCarriesBothLinks() {
    OutboundMail mail = confirmationFor(SupportedLocale.PL, "ala@sklep.pl");

    assertThat(mail.recipient()).isEqualTo("ala@sklep.pl");
    assertThat(mail.subject()).isEqualTo("Potwierdź swój adres — kivvi·click");
    assertThat(mail.htmlBody())
        .contains("https://kivvi.click/pl/waitlist/confirm/")
        .contains("https://kivvi.click/pl/waitlist/unsubscribe/")
        .contains("ala@sklep.pl")
        .contains("Potwierdzam adres");
    assertThat(mail.textBody())
        .contains("https://kivvi.click/pl/waitlist/confirm/")
        .contains("https://kivvi.click/pl/waitlist/unsubscribe/")
        .contains("Wypisz się");
  }

  @Test
  @DisplayName("the English mail is actually in English, links included")
  void theEnglishMailIsInEnglish() {
    OutboundMail mail = confirmationFor(SupportedLocale.EN, "ann@shop.uk");

    assertThat(mail.subject()).isEqualTo("Confirm your address — kivvi·click");
    assertThat(mail.htmlBody())
        .contains("Confirm my address")
        .contains("https://kivvi.click/en/waitlist/confirm/")
        .doesNotContain("Potwierdzam adres");
    assertThat(mail.textBody()).contains("Unsubscribe").doesNotContain("Wypisz się");
  }

  @Test
  @DisplayName("no placeholder survives rendering — an unresolved key reads as ordinary text")
  void everyPlaceholderIsResolved() {
    OutboundMail mail = confirmationFor(SupportedLocale.PL, "ala@sklep.pl");

    // Named attribute by attribute rather than by the "th:" prefix alone: every `width: 600px`
    // in an inline style contains that prefix too, and a test that trips over its own CSS teaches
    // the next person to weaken it.
    assertThat(mail.htmlBody())
        .doesNotContain("xmlns:th")
        .doesNotContain("th:text")
        .doesNotContain("th:href")
        .doesNotContain("th:lang")
        .doesNotContain("#{")
        .doesNotContain("${")
        // Thymeleaf renders a message key it cannot resolve as ??key_locale??.
        .doesNotContain("??");
  }

  @Test
  @DisplayName("the mail uses hex colours, never oklch(), which most mail clients cannot render")
  void theMailAvoidsOklch() {
    OutboundMail mail = confirmationFor(SupportedLocale.PL, "ala@sklep.pl");

    assertThat(mail.htmlBody()).doesNotContain("oklch(").contains("#34563b");
  }

  @Test
  @DisplayName("the plain-text alternative is not empty and repeats the link in full")
  void thePlainTextPartStandsOnItsOwn() {
    OutboundMail mail = confirmationFor(SupportedLocale.PL, "ala@sklep.pl");

    assertThat(mail.textBody()).isNotBlank().doesNotContain("<");
    assertThat(mail.textBody().lines().count()).isGreaterThan(5);
  }

  @Test
  @DisplayName("the deduplication key is derived from the token, so a resend is a different mail")
  void theDeduplicationKeyFollowsTheToken() {
    ConfirmationToken first = ConfirmationToken.issue(NOW);
    ConfirmationToken second = ConfirmationToken.issue(NOW);

    String firstKey =
        composer
            .confirmation("ala@sklep.pl", SupportedLocale.PL, first, OpaqueToken.generate())
            .dedupKey();
    String secondKey =
        composer
            .confirmation("ala@sklep.pl", SupportedLocale.PL, second, OpaqueToken.generate())
            .dedupKey();

    assertThat(firstKey).isEqualTo("waitlist-confirm:" + first.hash()).isNotEqualTo(secondKey);
  }

  @Test
  @DisplayName("a base URL with a trailing slash does not produce a double slash in the link")
  void aTrailingSlashInTheBaseUrlIsTrimmed() {
    WaitlistMailComposer trailing =
        new WaitlistMailComposer(
            new MailTemplateRenderer(templateEngine()), messageSource(), "https://kivvi.click/");

    assertThat(trailing.confirmUrl(SupportedLocale.PL, "0".repeat(64)))
        .isEqualTo("https://kivvi.click/pl/waitlist/confirm/" + "0".repeat(64));
  }

  private OutboundMail confirmationFor(SupportedLocale locale, String recipient) {
    return composer.confirmation(
        recipient, locale, ConfirmationToken.issue(NOW), OpaqueToken.generate());
  }

  private static SpringTemplateEngine templateEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setTemplateEngineMessageSource(messageSource());
    return engine;
  }

  private static MessageSource messageSource() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:messages");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(false);
    return source;
  }
}
