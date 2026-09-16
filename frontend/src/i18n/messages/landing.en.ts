/**
 * Landing page copy — ported verbatim from translations/messages.en.yaml's `landing.*` block.
 * See landing.pl.ts for why this lives under `landingPage`, not the existing `landing` key.
 */
export default {
    landingPage: {
        kicker: "Marketing automation for e-commerce",
        headline:
            '<span class="accent">See</span>, <span class="brown">decide</span>,<br>and act <em class="accent">in real time</em>.',
        lead: "Kivvi-click connects visitor tracking with automations: popups, emails, coupons and recommendations — delivered at the right moment. The whole codebase is open under the MIT licence: you can run it yourself.",
        // See landing.pl.ts for why PIO-121 removed ctaPrimary along with the price cards.
        ctaSecondary: "See the demo panel",
        waitlist: {
            label: "E-mail address",
            // See landing.pl.ts: a bare "@" would read as a linked message.
            placeholder: "you{'@'}shop.com",
            submit: "Let's talk about your deployment →",
            // Must stay word-for-word identical to waitlist.consent.text in
            // backend/src/main/resources/messages_en.properties — see landing.pl.ts.
            consent:
                "I agree to be contacted about deploying kivvi·click at the e-mail address given.",
            privacyLink: "What we do with this data",
            note: "We will be in touch about your deployment. No newsletter, and one click to unsubscribe.",
            // PIO-71 made this true: nothing is promised until the link in the mail is followed.
            thanks: "We have sent you a confirmation link. Confirm your address and we will be in touch about your deployment.",
            honeypotLabel: "Leave this field empty",
        },
        featuresTitle: "Everything a modern<br><em>e-commerce</em> needs.",
        featuresLead:
            "One panel instead of five tools. The full customer picture, automations driven by real behaviour, delivery channels in one place.",
        howTitle: 'Five minutes to the first<br><em>"wow"</em>.',
        openSourceTitle: "Run it yourself,<br>or <em>with us</em>.",
        openSourceLead:
            "The code is MIT-licensed — clone it, build it, run it on your own infrastructure and pay nobody. If you would rather not do that yourself, we will help you deploy it, integrate it with your shop and keep it running.",
        openSourceRepo: "See the code on GitHub →",
    },
};
