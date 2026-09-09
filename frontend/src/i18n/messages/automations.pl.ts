/**
 * Automations index + rule-editor page chrome — ported verbatim from translations/messages.pl.yaml
 * (the `automations:` block, the `common.*` keys the two pages reference, and the untranslated but
 * still `|trans`'d literal-Polish-string keys from messages.en.yaml's own "UI strings authored in
 * Polish inside templates" section). The rule-pipeline/flow-graph/simulation content and every
 * index card's chip/metric text are NOT here: the old templates never ran any of that through
 * `|trans` either — it is hard-coded Polish fixture text, reproduced as-is by
 * AutomationsFixtures/AutomationsViewService and rendered verbatim by the SPA's automations
 * components.
 */
export default {
    automations: {
        title: "Reguły i automatyzacje",
        sub: "Skonfiguruj logikę „jeśli to — to tamto” w oparciu o zdarzenia z Twoich stron.",
        templates: "Szablony",
        newAutomation: "Nowa automatyzacja",
        filters: "Filtry",
        backToList: "← Powrót do listy",
        list: "Lista",
        diagram: "Diagram",
        preview: "Podgląd",
        draft: "Szkic",
        publish: "Opublikuj",
        ruleTestTitle: "Test reguły",
        ruleTestSub: "Uruchom symulację na próbce zdarzeń",
        runTest: "Uruchom test",
    },
};
