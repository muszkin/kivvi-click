/**
 * Privacy policy copy, English (PIO-70, decision D3).
 *
 * DRAFT — a translation of the Polish text, which is the authoritative version. Like it, this
 * needs the owner's approval before it goes live. See privacy.pl.ts for why the catalogue lives
 * under `privacyPage` rather than `privacy`.
 */
export default {
    privacyPage: {
        title: "Privacy policy",
        updated: "Last updated: 15 September 2026",
        intro: "This policy describes what happens to the data you leave when you join the kivvi·click waiting list. It covers that signup only — the panel and user accounts are not publicly available yet.",
        sections: [
            {
                heading: "Who the controller is",
                body: 'The controller of your data is the owner of kivvi·click, available at kivvi.click. For anything concerning personal data, write to the address given below, under "Contact".',
            },
            {
                heading: "What we collect",
                body: "When you join the waiting list we store: the e-mail address you gave, the date and time you consented, the IP address the form was sent from, your browser identifier (user agent) and the exact wording of the consent clause you saw at that moment. The last three exist only so we can show what you agreed to, and when.",
            },
            {
                heading: "What for",
                body: "Only to send you a single notification when kivvi·click launches. We do not run a newsletter, we do not profile you, and we do not pass your address to anyone for marketing.",
            },
            {
                heading: "On what basis",
                body: "On your consent — Article 6(1)(a) GDPR. Consent is voluntary; without it we simply do not store the address.",
            },
            {
                heading: "How long we keep it",
                body: "Until the launch notification has been sent and for at most 12 months after that, or until you withdraw your consent, whichever comes first. We then delete the address together with the consent record.",
            },
            {
                heading: "How to withdraw consent",
                body: 'Write to the address under "Contact" and we will remove your address from the list. Withdrawing consent does not affect the lawfulness of what we did before you withdrew it.',
            },
            {
                heading: "Your rights",
                body: "You have the right to access your data, to have it corrected or erased, to restrict its processing and to have it ported. You also have the right to lodge a complaint with the President of the Personal Data Protection Office if you believe we are processing your data unlawfully.",
            },
            {
                heading: "Contact",
                // TO BE FILLED IN BEFORE PUBLICATION — see privacy.pl.ts. This is the only route
                // for withdrawing consent the document names.
                body: "[controller's e-mail address — to be filled in before publication]",
            },
            {
                heading: "Who else handles it",
                body: "The data is held on the server that runs kivvi·click. Our infrastructure provider operates within the European Union. We do not transfer data outside the European Economic Area.",
            },
        ],
    },
};
