/**
 * Popups index + widget editor page chrome — ported verbatim from translations/messages.en.yaml.
 * Keys mirror popups.pl.ts exactly; a key with no entry in messages.en.yaml keeps popups.pl.ts's
 * own Polish text, matching Symfony's translator falling back to the message id when no
 * translation exists (see popups.pl.ts's class comment).
 */
export default {
    popups: {
        title: "Popups and widgets",
        sub: "Every overlay your visitors can see — modals, bars, slide-ins.",
        templates: "Templates",
        newPopup: "New popup",
        preview: "Preview",
        edit: "Edit",
        showsWhen: "Shows when",
        triggerSummary:
            'the visitor tries to leave the page (exit intent) <strong>and</strong> has spent <span class="chip">≥ 20s</span> on it <strong>and</strong> has not seen this popup in the last <span class="chip">14 days</span>',
        editor: {
            backToList: "← Back to the list",
            widgetType: "Widget type",
            testOnSite: "Test on the site",
            draft: "Draft",
            publish: "Publish",
            blocksTitle: "Blocks",
            variablesTitle: "Variables",
            selectedBlock: "Selected block",
            selectedBlockName: "Heading + description",
            headingLabel: "Heading",
            descriptionLabel: "Description",
            buttonTextLabel: "Button text",
            accentColorLabel: "Widget accent colour",
            colorName: "Colour",
            positionLabel: "Position",
            cornersLabel: "Corners and shadow",
            sharp: "Sharp",
            soft: "Soft",
            full: "Full",
            triggersTitle: "Triggers",
            addTrigger: "Add a trigger",
            audienceTitle: "Who will see it",
            abTestTitle: "A/B test",
            abTestBody:
                "Variant B gets 50% of the traffic and a different heading.",
            variantA: "Variant A",
            variantB: "Variant B",
            newVariant: "+ New",
            desktop: "Desktop",
            mobile: "Mobile",
            animation: "animation: fade + scale 240ms",
        },
    },
};
