/**
 * English message catalogue — ported verbatim from translations/messages.en.yaml. Keys
 * mirror pl.ts exactly.
 */
export default {
    brand: {
        name: "Kivvi-click",
    },
    nav: {
        main: "Main",
        automate: "Automation",
        data: "Data",
        config: "Configuration",
        dashboard: "Dashboard",
        events: "Event stream",
        customers: "Customers",
        automations: "Rules",
        campaigns: "Email campaigns",
        popups: "Popups & widgets",
        feeds: "Product feeds",
        import: "Customer import",
        settings: "Settings",
    },
    common: {
        search: "Search events, customers, rules…",
        email: "Email",
        password: "Password",
        or: "or",
        continueWithGoogle: "Continue with Google",
        status: "Status",
        documentation: "Documentation",
        marketingAutomation: "MARKETING AUTOMATION",
        changeLanguage: "Change language",
        theme: "Theme",
        notifications: "Notifications",
        // Absent from messages.en.yaml in the old stack, so Symfony's translator falls back to
        // the Polish message id itself — this button's title/aria-label is Polish even on /en/…
        collapseSidebar: "Zwiń panel boczny",
        search_kbd: "⌘K",
    },
    auth: {
        headline:
            'See.<br><span style="color: var(--accent);">Decide.</span><br><span style="color: var(--brown);">Act</span> in real time.',
        lead: "Track visitor behaviour and trigger emails, popups, coupons and recommendations at the right moment of the customer journey.",
        welcomeBack: "Welcome back to Kivvi",
        loginSub: "Sign in to your automation panel.",
        signIn: "Sign in →",
        noAccount: "Don't have an account yet?",
        registerCta: "Create one in 2 minutes →",
    },
    dashboard: {
        title: "What's happening now",
        sub: "Real-time event stream from your sites and the impact of your live automations.",
    },
    landing: {
        features: "Features",
        how: "How it works",
        openSource: "Open source",
        // "Blog" reads the same either way. `terms` and `privacy` still carry the Polish string
        // for want of an entry in messages.en.yaml — same PL fallback as common.collapseSidebar
        // above, and PIO-117's to finish.
        blog: "Blog",
        // PIO-121 translated these two: see pl.ts for why `register` stopped selling an account
        // and now points at the repository.
        login: "Sign in",
        register: "Source on GitHub →",
        terms: "Regulamin",
        privacy: "Prywatność",
        // PIO-121: see pl.ts — the footer claim is a key now, translated on both sides.
        builtWith: "Built in Poland with Java and Vue 3. MIT-licensed code.",
        repo: "GitHub",
    },
};
