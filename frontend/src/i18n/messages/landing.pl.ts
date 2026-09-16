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
        lead: "Kivvi-click łączy śledzenie zachowania odwiedzających z automatyzacjami: popupami, e-mailami, kuponami i rekomendacjami — wysyłanymi w odpowiednim momencie. Cały kod jest otwarty na licencji MIT: możesz postawić go u siebie.",
        // PIO-121 removed ctaPrimary together with the price cards: their buttons were its only
        // remaining reader once PIO-70 had replaced the hero CTA with the form below.
        ctaSecondary: "Zobacz panel demo",
        waitlist: {
            label: "Adres e-mail",
            // vue-i18n reads a bare "@" as the start of a linked message, so the address
            // placeholder escapes it as a literal.
            placeholder: "twoj{'@'}sklep.pl",
            submit: "Porozmawiajmy o wdrożeniu →",
            // Stored verbatim as the consent proof on every subscriber row, so this must stay
            // word-for-word identical to waitlist.consent.text in
            // backend/src/main/resources/messages_pl.properties. PIO-121 changed the purpose to
            // contact about a deployment; rows collected under the old clause keep it.
            consent:
                "Zgadzam się na kontakt w sprawie wdrożenia kivvi·click na podany adres e-mail.",
            privacyLink: "Co robimy z tymi danymi",
            note: "Odezwiemy się w sprawie wdrożenia. Adres zostaje tylko na tę rozmowę.",
            // PIO-71 made this true: signing up only creates a `pending` row, so nothing is
            // promised until the link in the mail is followed.
            thanks: "Wysłaliśmy link potwierdzający. Potwierdź adres, a odezwiemy się w sprawie wdrożenia.",
            honeypotLabel: "Zostaw to pole puste",
        },
        featuresTitle:
            "Wszystko czego potrzebuje<br>nowoczesny <em>e-commerce</em>.",
        featuresLead:
            "Jeden panel zamiast pięciu narzędzi. Pełny obraz klienta, automatyzacje oparte o realne zachowanie, kanały dostarczania w jednym miejscu.",
        howTitle: "Pięć minut do pierwszego<br><em>„wow”</em>.",
        openSourceTitle: "Postaw sam<br>albo <em>z nami</em>.",
        openSourceLead:
            "Kod jest na licencji MIT — klonujesz, budujesz, uruchamiasz na swojej infrastrukturze i nikomu nic nie płacisz. Jeśli wolisz nie robić tego sam, pomożemy wdrożyć, zintegrować z Twoim sklepem i utrzymać.",
        openSourceRepo: "Zobacz kod na GitHubie →",
    },
};
