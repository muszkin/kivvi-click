/**
 * Campaigns index + e-mail editor page chrome — ported verbatim from
 * translations/messages.pl.yaml's `campaigns:`/`common:` blocks and the page-level `|trans` calls
 * in pages/campaigns.html.twig, pages/email-editor.html.twig and its
 * pages/emails/{envelope,inspector,variables}.html.twig partials. Table column labels, campaign
 * row content, block-library labels and the document/inspector copy are NOT here: the old
 * templates never ran any of those through `|trans` either (`CampaignCatalog::columns()`/
 * `::rows()`/`::blocks()`/`::sections()`/`::selectedBlock()` are hard-coded Polish fixture text,
 * reproduced as-is by CampaignsFixtures/CampaignsViewService and rendered verbatim by the
 * campaigns-email-editor components).
 *
 * A handful of keys below (envelopeFrom/envelopeSubject and every `editor.*` inspector label
 * except blocksTitle/sendTest) have no entry in messages.en.yaml either: Symfony's translator
 * falls back to the message id itself when a translation is missing, so the old panel shows this
 * exact Polish text even with the English toggle on — campaigns.en.ts mirrors that fallback
 * verbatim rather than inventing an English string the old stack never had.
 */
export default {
    campaigns: {
        title: "Kampanie email",
        sub: "Wyzwalane przez automatyzacje oraz jednorazowe wysyłki masowe.",
        templates: "Szablony",
        newCampaign: "Nowa kampania",
        filters: "Filtry",
        editor: {
            back: "← Powrót",
            previewDesktop: "Podgląd desktop",
            mobile: "Mobile",
            sendTest: "Wyślij test",
            draft: "Szkic",
            publish: "Opublikuj",
            blocksTitle: "Bloki",
            envelopeFrom: "Od:",
            envelopeSubject: "Temat:",
            variablesTitle: "Zmienne",
            abTestsTitle: "Sekcje testów A/B",
            abTestsBody:
                "Wariant A jest pokazywany. Wariant B otrzymuje 50% ruchu i ma inny temat.",
            selectedBlock: "Wybrany blok",
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
