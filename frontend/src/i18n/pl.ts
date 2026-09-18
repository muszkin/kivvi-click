/**
 * Polish message catalogue. Values are ported verbatim from translations/messages.pl.yaml and the
 * inline Polish strings in the ported Twig templates (the old stack's convention: untranslated
 * Polish text IS the message id). It was the default locale until PIO-125 made English the
 * default; it stays a complete language of its own behind /pl.
 */
export default {
    brand: {
        name: "Kivvi-click",
    },
    nav: {
        main: "Główne",
        automate: "Automatyzacja",
        data: "Dane",
        config: "Konfiguracja",
        dashboard: "Pulpit",
        events: "Strumień zdarzeń",
        customers: "Klienci",
        automations: "Reguły",
        campaigns: "Kampanie email",
        popups: "Popupy i widgety",
        feeds: "Feedy produktów",
        import: "Import klientów",
        settings: "Ustawienia",
    },
    common: {
        search: "Szukaj zdarzeń, klientów, reguł…",
        email: "Email",
        password: "Hasło",
        or: "lub",
        continueWithGoogle: "Kontynuuj z Google",
        status: "Status",
        documentation: "Dokumentacja",
        marketingAutomation: "MARKETING AUTOMATION",
        changeLanguage: "Zmień język",
        theme: "Motyw",
        notifications: "Powiadomienia",
        collapseSidebar: "Zwiń panel boczny",
        mainNavigation: "Nawigacja główna",
        breadcrumb: "Ścieżka",
        openSearch: "Szukaj",
        blocks: "Bloki",
        properties: "Właściwości",
        search_kbd: "⌘K",
    },
    auth: {
        headline:
            'Widzisz.<br><span style="color: var(--accent);">Decydujesz.</span><br><span style="color: var(--brown);">Działasz</span> w czasie rzeczywistym.',
        lead: "Śledź zachowanie odwiedzających i automatycznie uruchamiaj e-maile, popupy, kupony i rekomendacje w odpowiednim momencie ścieżki zakupowej.",
        welcomeBack: "Wróć do Kivvi",
        loginSub: "Zaloguj się do panelu zarządzania automatyzacjami.",
        signIn: "Zaloguj się →",
        // PIO-125: this pair used to read "Nie masz jeszcze konta? Załóż w 2 minuty →" and linked
        // back to this very form, because there is no registration to link to — the same promise
        // PIO-121 took out of the landing header. It leads to the landing page's deployment form
        // now, and the key is no longer called `registerCta`.
        noAccount: "Nie masz konta?",
        deploymentCta: "Porozmawiajmy o wdrożeniu →",
    },
    dashboard: {
        title: "Co dzieje się teraz",
        sub: "Strumień zdarzeń z Twoich stron, na żywo, oraz wpływ uruchomionych automatyzacji.",
    },
    landing: {
        features: "Funkcje",
        how: "Jak to działa",
        openSource: "Open source",
        blog: "Blog",
        login: "Logowanie",
        // PIO-121: this used to read "Załóż konto →" and point at the login form, because there
        // is no registration to point it at. On a page that says "run it yourself", whose privacy
        // policy says accounts are not publicly available, it was the loudest thing left from the
        // old story. It points at the source now. The login button stays: the demo panel is real.
        register: "Kod na GitHubie →",
        terms: "Regulamin",
        privacy: "Prywatność",
        // PIO-117: hard-coded in PublicLayout.vue until the English footer had to stop saying
        // RODO, which is what Polish calls the GDPR.
        dpa: "RODO / DPA",
        // PIO-125: the public header's language switch. It names the language it leads TO, in
        // that language — "English" here, "Polski" in en.ts — because the person who needs the
        // switch is the one who cannot read the page they are on.
        otherLanguage: "English",
        // PIO-121: the footer used to claim Symfony and PHP, hard-coded in PublicLayout.vue —
        // which is why the English site rendered it in Polish. It is a key now, in both
        // catalogues, so the fix cannot regress the same way.
        builtWith: "Zbudowane w Polsce na Javie i Vue 3. Kod na licencji MIT.",
        repo: "GitHub",
    },
};
