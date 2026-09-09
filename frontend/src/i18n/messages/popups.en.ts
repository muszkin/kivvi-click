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
        edit: "Edytuj",
        showsWhen: "Pokazuje się gdy",
        triggerSummary:
            'klient próbuje opuścić stronę (exit intent) <strong>oraz</strong> spędził <span class="chip">≥ 20s</span> <strong>oraz</strong> nie widział tego popupu w ostatnich <span class="chip">14 dniach</span>',
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
            colorName: "Kolor",
            positionLabel: "Position",
            cornersLabel: "Corners and shadow",
            sharp: "Sharp",
            soft: "Soft",
            full: "Full",
            triggersTitle: "Triggers",
            addTrigger: "Add a trigger",
            audienceTitle: "Who will see it",
            abTestTitle: "A/B test",
            abTestBody: "Wariant B dostaje 50% ruchu i inny nagłówek.",
            variantA: "Variant A",
            variantB: "Variant B",
            newVariant: "+ New",
            desktop: "Desktop",
            mobile: "Mobile",
            animation: "animacja: fade + scale 240ms",
        },
    },
};
