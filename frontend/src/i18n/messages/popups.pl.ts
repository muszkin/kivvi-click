/**
 * Popups index + widget editor page chrome — ported verbatim from translations/messages.pl.yaml's
 * `popups:`/`common:` blocks and the page-level `|trans` calls in pages/popups.html.twig,
 * pages/popup-editor.html.twig and its pages/widgets/{blocks,inspector,stage-meta,type-list}.html.twig
 * and pages/popups/trigger-summary.html.twig partials. Widget type/block-library labels and every
 * card's chip/metric text are NOT here: the old templates never ran any of those through `|trans`
 * either — it is hard-coded Polish fixture text, reproduced as-is by WidgetFixtures/
 * WidgetViewService and rendered verbatim by the SPA's widget components.
 *
 * A handful of keys below (colorName, showsWhen, triggerSummary, editor.abTestBody,
 * editor.animation) have no entry in messages.en.yaml either: Symfony's translator falls back to
 * the message id itself when a translation is missing, so the old panel shows this exact Polish
 * text even with the English toggle on — popups.en.ts mirrors that fallback verbatim rather than
 * inventing an English string the old stack never had. `editor.desktop`/`editor.mobile` were never
 * `|trans`'d at all (segmented.html.twig's own literal labels) — both locales carry the same
 * English word, exactly like the old stack.
 *
 * `triggerSummary` is one v-html-bound string carrying the whole "Pokazuje się gdy ..." sentence
 * (pages/popups/trigger-summary.html.twig) as developer-authored markup — its two `<strong>oraz
 * </strong>` fragments and two `<span class="chip">...</span>` fragments are literal HTML,
 * ported verbatim rather than reassembled from four separate Vue template expressions (which would
 * need explicit `{{ " " }}` whitespace-node guards at every junction — see
 * common-journey-rules.md); `.chip` alone reproduces Chip.vue's own markup for a plain,
 * tone-less, non-mono label exactly.
 */
export default {
    popups: {
        title: "Popupy i widgety",
        sub: "Wszystkie nakładki, które pokazują się odwiedzającym Twoich stron — modale, paski, slide-iny.",
        templates: "Szablony",
        newPopup: "Nowy popup",
        preview: "Podgląd",
        edit: "Edytuj",
        showsWhen: "Pokazuje się gdy",
        triggerSummary:
            'klient próbuje opuścić stronę (exit intent) <strong>oraz</strong> spędził <span class="chip">≥ 20s</span> <strong>oraz</strong> nie widział tego popupu w ostatnich <span class="chip">14 dniach</span>',
        editor: {
            backToList: "← Powrót do listy",
            widgetType: "Typ widgetu",
            testOnSite: "Test na stronie",
            draft: "Szkic",
            publish: "Opublikuj",
            blocksTitle: "Bloki",
            variablesTitle: "Zmienne",
            selectedBlock: "Wybrany blok",
            selectedBlockName: "Nagłówek + opis",
            headingLabel: "Nagłówek",
            descriptionLabel: "Opis",
            buttonTextLabel: "Tekst przycisku",
            accentColorLabel: "Kolor akcentu widgetu",
            colorName: "Kolor",
            positionLabel: "Pozycja",
            cornersLabel: "Zaokrąglenie i cień",
            sharp: "Ostre",
            soft: "Miękkie",
            full: "Pełne",
            triggersTitle: "Wyzwalacze",
            addTrigger: "Dodaj wyzwalacz",
            audienceTitle: "Kto zobaczy",
            abTestTitle: "Test A/B",
            abTestBody: "Wariant B dostaje 50% ruchu i inny nagłówek.",
            variantA: "Wariant A",
            variantB: "Wariant B",
            newVariant: "+ Nowy",
            desktop: "Desktop",
            mobile: "Mobile",
            animation: "animacja: fade + scale 240ms",
        },
    },
};
