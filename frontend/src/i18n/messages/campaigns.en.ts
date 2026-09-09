/**
 * Campaigns index + e-mail editor page chrome — ported verbatim from
 * translations/messages.en.yaml. Keys mirror campaigns.pl.ts exactly; a key with no entry in
 * messages.en.yaml keeps campaigns.pl.ts's own Polish text, matching Symfony's translator falling
 * back to the message id when no translation exists (see campaigns.pl.ts's class comment).
 */
export default {
    campaigns: {
        title: "Email campaigns",
        sub: "Triggered by automations, plus one-off broadcasts.",
        templates: "Templates",
        newCampaign: "New campaign",
        filters: "Filters",
        editor: {
            back: "← Back",
            previewDesktop: "Podgląd desktop",
            mobile: "Mobile",
            sendTest: "Send test",
            draft: "Draft",
            publish: "Publish",
            blocksTitle: "Blocks",
            envelopeFrom: "Od:",
            envelopeSubject: "Temat:",
            variablesTitle: "Variables",
            abTestsTitle: "Sekcje testów A/B",
            abTestsBody:
                "Wariant A jest pokazywany. Wariant B otrzymuje 50% ruchu i ma inny temat.",
            selectedBlock: "Selected block",
            titleField: "Tytuł",
            placeholderSupport: "Wsparcie placeholder",
            sectionBackground: "Kolor tła sekcji",
            backgroundAlt: "Tło",
            alignment: "Wyrównanie",
            alignLeft: "Lewo",
            alignCenter: "Środek",
            alignRight: "Prawo",
            sectionPadding: "Padding sekcji",
            visibilityTitle: "Warunki widoczności",
            visibilityLead: "Pokaż blok tylko, gdy:",
            addVisibilityRule: "Dodaj warunek",
        },
    },
};
