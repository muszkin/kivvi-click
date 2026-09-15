/**
 * Landing page copy — ported verbatim from translations/messages.pl.yaml's `landing.*` block.
 * Kept under the `landingPage` top-level key rather than the existing `landing` key (already
 * used by PublicLayout.vue for the nav/footer labels it shares with this page's section
 * eyebrows), per the i18n merge rule in common-journey-rules.md: keys must not collide with an
 * existing top-level key.
 */
export default {
    landingPage: {
        kicker: "Marketing automation dla e-commerce",
        headline:
            '<span class="accent">Widzisz</span>, <span class="brown">decydujesz</span>,<br>i działasz <em class="accent">w czasie rzeczywistym</em>.',
        lead: "Kivvi-click łączy śledzenie zachowania odwiedzających z automatyzacjami: popupami, e-mailami, kuponami i rekomendacjami — wysyłanymi w odpowiednim momencie.",
        // PIO-70 replaced the primary CTA with the waitlist form; ctaPrimary stays in the
        // catalogue because the pricing cards still use the same wording on their own buttons.
        ctaPrimary: "Załóż darmowe konto →",
        ctaSecondary: "Zobacz panel demo",
        waitlist: {
            label: "Adres e-mail",
            // vue-i18n reads a bare "@" as the start of a linked message, so the address
            // placeholder escapes it as a literal.
            placeholder: "twoj{'@'}sklep.pl",
            submit: "Powiadom mnie →",
            // Stored verbatim as the consent proof on every subscriber row, so this must stay
            // word-for-word identical to waitlist.consent.text in
            // backend/src/main/resources/messages_pl.properties.
            consent:
                "Zgadzam się na otrzymanie jednorazowego powiadomienia o starcie kivvi·click na podany adres e-mail.",
            privacyLink: "Co robimy z tymi danymi",
            note: "Jeden e-mail, kiedy ruszamy. Żadnego spamu, wypisujesz się jednym kliknięciem.",
            thanks: "Dzięki — jesteś na liście. Odezwiemy się, kiedy ruszamy.",
            honeypotLabel: "Zostaw to pole puste",
        },
        featuresTitle:
            "Wszystko czego potrzebuje<br>nowoczesny <em>e-commerce</em>.",
        featuresLead:
            "Jeden panel zamiast pięciu narzędzi. Pełny obraz klienta, automatyzacje oparte o realne zachowanie, kanały dostarczania w jednym miejscu.",
        howTitle: "Pięć minut do pierwszego<br><em>„wow”</em>.",
        pricingTitle: "Bez ukrytych kosztów,<br>bez kar za sukces.",
        pricingLead:
            "Zaczynasz za darmo. Płacisz dopiero gdy potrzebujesz większej skali albo silnika ML.",
    },
};
