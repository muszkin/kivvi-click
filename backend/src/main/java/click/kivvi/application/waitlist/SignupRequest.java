package click.kivvi.application.waitlist;

import click.kivvi.domain.SupportedLocale;

/**
 * One submission of the landing page's waitlist form, as it arrived.
 *
 * <p>Deliberately the raw values, not cleaned-up ones: {@link WaitlistSignupService} is what
 * decides how an address is normalized and what an unticked box means, and the controller that
 * fills this in has to be able to echo exactly what was typed back into the form when the
 * submission is refused.
 *
 * @param email the address as typed, before trimming or case folding
 * @param consentGiven whether the GDPR consent box was ticked
 * @param honeypot a field no human ever sees; anything in it means a bot filled the form in
 * @param locale which language the visitor is reading, so the stored consent clause matches it
 * @param clientIp the peer address, part of the consent proof and the per-IP allowance
 * @param userAgent the submitting browser, part of the consent proof; may be absent
 */
public record SignupRequest(
    String email,
    boolean consentGiven,
    String honeypot,
    SupportedLocale locale,
    String clientIp,
    String userAgent) {}
