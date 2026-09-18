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
            previewDesktop: "Desktop preview",
            mobile: "Mobile",
            sendTest: "Send test",
            draft: "Draft",
            publish: "Publish",
            blocksTitle: "Blocks",
            envelopeFrom: "From:",
            envelopeSubject: "Subject:",
            variablesTitle: "Variables",
            abTestsTitle: "A/B test sections",
            abTestsBody:
                "Variant A is the one shown. Variant B gets 50% of the traffic and a different subject.",
            selectedBlock: "Selected block",
            titleField: "Title",
            placeholderSupport: "Placeholder support",
            sectionBackground: "Section background colour",
            backgroundAlt: "Background",
            alignment: "Alignment",
            alignLeft: "Left",
            alignCenter: "Centre",
            alignRight: "Right",
            sectionPadding: "Section padding",
            visibilityTitle: "Visibility conditions",
            visibilityLead: "Show the block only when:",
            addVisibilityRule: "Add a condition",
        },
    },
};
