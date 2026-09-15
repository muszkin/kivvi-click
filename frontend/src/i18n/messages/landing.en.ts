/**
 * Landing page copy — ported verbatim from translations/messages.en.yaml's `landing.*` block.
 * See landing.pl.ts for why this lives under `landingPage`, not the existing `landing` key.
 */
export default {
    landingPage: {
        kicker: "Marketing automation for e-commerce",
        headline:
            '<span class="accent">See</span>, <span class="brown">decide</span>,<br>and act <em class="accent">in real time</em>.',
        lead: "Kivvi-click connects visitor tracking with automations: popups, emails, coupons and recommendations — delivered at the right moment.",
        // See landing.pl.ts for why ctaPrimary stays after PIO-70 replaced the hero CTA.
        ctaPrimary: "Create a free account →",
        ctaSecondary: "See the demo panel",
        waitlist: {
            label: "E-mail address",
            // See landing.pl.ts: a bare "@" would read as a linked message.
            placeholder: "you{'@'}shop.com",
            submit: "Notify me →",
            // Must stay word-for-word identical to waitlist.consent.text in
            // backend/src/main/resources/messages_en.properties — see landing.pl.ts.
            consent:
                "I agree to receive a single notification by e-mail when kivvi·click launches.",
            privacyLink: "What we do with this data",
            note: "One e-mail when we launch. No spam, and one click to unsubscribe.",
            thanks: "Thanks — you are on the list. We will write when we launch.",
            honeypotLabel: "Leave this field empty",
        },
        featuresTitle: "Everything a modern<br><em>e-commerce</em> needs.",
        featuresLead:
            "One panel instead of five tools. The full customer picture, automations driven by real behaviour, delivery channels in one place.",
        howTitle: 'Five minutes to the first<br><em>"wow"</em>.',
        pricingTitle: "No hidden costs,<br>no penalty for growing.",
        pricingLead:
            "Start free. Pay when you need more scale or the ML engine.",
    },
};
