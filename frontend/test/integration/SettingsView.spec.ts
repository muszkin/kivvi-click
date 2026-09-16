import { flushPromises, mount } from "@vue/test-utils";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import SettingsView from "@/views/SettingsView.vue";
import { serveInLocaleOf } from "../support/documentLocale";

const TABS = [
    { id: "account", icon: "user", label: "Konto", href: "/pl/settings" },
    {
        id: "sites",
        icon: "globe",
        label: "Śledzone strony",
        href: "/pl/settings/sites",
    },
    { id: "team", icon: "users", label: "Zespół", href: "/pl/settings/team" },
    {
        id: "providers",
        icon: "mail",
        label: "Dostawcy email",
        href: "/pl/settings/providers",
    },
    {
        id: "api",
        icon: "code",
        label: "Webhooks i API",
        href: "/pl/settings/api",
    },
    {
        id: "notifications",
        icon: "bell",
        label: "Powiadomienia",
        href: "/pl/settings/notifications",
    },
    {
        id: "gdpr",
        icon: "info",
        label: "RODO / DPA",
        href: "/pl/settings/gdpr",
    },
];

const SUBTITLES: Record<string, string> = {
    account: "Dane firmy i preferencje właściciela konta.",
    sites: "Domeny objęte trackingiem oraz instalacja skryptu.",
    team: "Osoby z dostępem do panelu, ich role i zaproszenia.",
    providers:
        "Skąd wychodzą Twoje e-maile i jak radzą sobie z dostarczalnością.",
    api: "Klucze API, webhooks i logi wywołań.",
    notifications: "Kiedy Kivvi ma Cię powiadomić i którym kanałem.",
    gdpr: "Retencja danych, umowa powierzenia i obsługa żądań podmiotów.",
};

const TRACKER_SNIPPET = `<!-- Kivvi-click tracker -->\n<script async src="https://cdn.kivvi-click.io/k.js"></script>`;

const SETTINGS = {
    trackedSites: [
        {
            name: "aureashop.pl",
            color: "#7a8763",
            events: "28 410",
            key: "pk_live_8a4f2c…",
        },
        {
            name: "mlot-narzedzia.pl",
            color: "#a3825b",
            events: "14 820",
            key: "pk_live_3c91b7…",
        },
        {
            name: "polna-bistro.pl",
            color: "#8b6f53",
            events: "12 240",
            key: "pk_live_be22a0…",
        },
    ],
    automaticEvents: ["pageview", "login", "signup"],
    team: [
        {
            name: "Maciej Kowalczyk",
            email: "maciej@aureashop.pl",
            role: "Właściciel",
            roleTone: "accent",
            last: "teraz",
            invited: false,
            mfa: true,
        },
        {
            name: "—",
            email: "kamil@aureashop.pl",
            role: "Analityk",
            roleTone: "neutral",
            last: "—",
            invited: true,
            mfa: false,
        },
    ],
    roles: [{ name: "Właściciel", description: "Pełny dostęp.", count: 1 }],
    emailProviders: [
        {
            name: "Amazon SES",
            region: "eu-central-1",
            statusTone: "good",
            statusLabel: "Główny",
            sent: "118 420",
            bounce: "0,24%",
            bounceWarn: false,
            complaint: "0,01%",
            verified: true,
        },
    ],
    dnsRecords: [
        { record: "SPF", value: "v=spf1 include:amazonses.com ~all", ok: true },
        { record: "DKIM", value: "kivvi1._domainkey.aureashop.pl", ok: true },
        { record: "DMARC", value: "v=DMARC1; p=quarantine", ok: true },
        { record: "BIMI", value: "nie skonfigurowane", ok: false },
    ],
    apiKeys: [
        {
            name: "Produkcja — backend",
            prefix: "sk_live_8a4f2c",
            created: "14 sty 2024",
            last: "3 min temu",
            scopes: ["events:write", "customers:read"],
        },
    ],
    webhooks: [
        {
            url: "https://aureashop.pl/hooks/kivvi",
            events: ["purchase", "cart_abandon"],
            code: 200,
            last: "2 min temu",
        },
        {
            url: "https://erp.aureashop.pl/api/kivvi",
            events: ["customer.created"],
            code: 200,
            last: "11 min temu",
        },
        {
            url: "https://hooks.slack.com/services/T0…",
            events: ["automation.failed"],
            code: 410,
            last: "3 godz. temu",
        },
    ],
    apiLimits: [
        {
            label: "Ingest zdarzeń (na sek.)",
            pct: 42,
            value: "42 / 100",
            tone: "accent",
        },
    ],
    notificationMatrix: [
        {
            label: "Automatyzacja przestała działać",
            email: true,
            slack: true,
            sms: true,
        },
        {
            label: "Feed produktów zwrócił błąd",
            email: true,
            slack: true,
            sms: false,
        },
        {
            label: "Bounce rate przekroczył próg",
            email: true,
            slack: true,
            sms: true,
        },
        {
            label: "Limit wysyłek na wyczerpaniu (80%)",
            email: true,
            slack: false,
            sms: false,
        },
        {
            label: "Import klientów zakończony",
            email: true,
            slack: false,
            sms: false,
        },
        {
            label: "Nowa osoba dołączyła do zespołu",
            email: true,
            slack: false,
            sms: false,
        },
        {
            label: "Tygodniowe podsumowanie wyników",
            email: true,
            slack: false,
            sms: false,
        },
        {
            label: "Tracker przestał odbierać zdarzenia",
            email: true,
            slack: true,
            sms: true,
        },
    ],
    dataSubjectRequests: [
        {
            id: "DSR-0142",
            person: "hania.k@aurea.pl",
            type: "Dostęp do danych",
            status: "zakończone",
            done: true,
            due: "22 sie 2026",
        },
    ],
    retentionPolicies: [
        {
            label: "Surowe zdarzenia",
            options: ["90 dni", "13 miesięcy", "24 miesiące"],
            selected: "13 miesięcy",
        },
    ],
};

function payloadFor(tab: string) {
    return {
        tab,
        tabs: TABS.map((t) => ({ ...t, active: t.id === tab })),
        tabSubtitle: SUBTITLES[tab],
        settings: SETTINGS,
        trackerSnippet: TRACKER_SNIPPET,
    };
}

async function mountAt(path: string) {
    serveInLocaleOf(path);
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(SettingsView, {
        global: { plugins: [router, i18n] },
    });
    await flushPromises();
    return wrapper;
}

describe("B32 7 tabs at own URLs with markers, highlighted tracker snippet, DNS states, failing webhook, notification matrix", () => {
    beforeEach(() => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async (input: RequestInfo | URL) => {
                const url = String(input);
                const tab = url.split("/").pop() ?? "account";
                return Response.json(payloadFor(tab));
            }),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("fetches the locale- and tab-scoped payload, defaulting the bare route to account", async () => {
        await mountAt("/pl/settings");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/settings/account",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("fetches the explicit tab from the URL", async () => {
        await mountAt("/pl/settings/gdpr");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/settings/gdpr",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("renders all 7 tabs, with the current one aria-current=true", async () => {
        const wrapper = await mountAt("/pl/settings/sites");

        const links = wrapper.findAll(".settings-nav a");
        expect(links).toHaveLength(7);
        expect(
            links.filter((link) => link.attributes("aria-current") === "true"),
        ).toHaveLength(1);
        expect(
            wrapper
                .findAll(".settings-nav a")
                .find((link) => link.text().includes("Śledzone strony"))
                ?.attributes("aria-current"),
        ).toBe("true");
    });

    it('B01 the account tab shows its marker text (mirrors PanelPagesTest\'s .card-title "Dane konta")', async () => {
        const wrapper = await mountAt("/pl/settings/account");

        expect(wrapper.text()).toContain("Dane konta");
    });

    it("the sites tab highlights the tracker snippet server-... client-side", async () => {
        const wrapper = await mountAt("/pl/settings/sites");

        const snippet = wrapper.find(".code-block");
        expect(snippet.exists()).toBe(true);
        expect(snippet.text()).toContain("cdn.kivvi-click.io/k.js");
        expect(snippet.find("span.k").exists()).toBe(true);
    });

    it('B01 the team tab shows its marker text (mirrors PanelPagesTest\'s .card-title "Członkowie zespołu")', async () => {
        const wrapper = await mountAt("/pl/settings/team");

        expect(wrapper.text()).toContain("Członkowie zespołu");
    });

    it("the providers tab shows 4 DNS rows: 3 good, 1 warn", async () => {
        const wrapper = await mountAt("/pl/settings/providers");

        expect(wrapper.findAll(".dns-row")).toHaveLength(4);
        expect(wrapper.findAll(".dns-row .chip.good")).toHaveLength(3);
        expect(wrapper.findAll(".dns-row .chip.warn")).toHaveLength(1);
    });

    it("the api tab shows 3 webhooks, the Slack one failing with a 410 chip and a callout", async () => {
        const wrapper = await mountAt("/pl/settings/api");

        expect(wrapper.findAll(".hook-row")).toHaveLength(3);
        expect(wrapper.find(".hook-row .chip.bad").text()).toBe("410");
        expect(wrapper.find(".callout").text()).toContain("410 od 3 godzin");
    });

    it("the notifications tab renders an 8x3 checkbox matrix with 15 checked", async () => {
        const wrapper = await mountAt("/pl/settings/notifications");

        expect(wrapper.findAll(".table tbody tr")).toHaveLength(8);
        const checkboxes = wrapper.findAll(
            '.table tbody input[type="checkbox"]',
        );
        expect(checkboxes).toHaveLength(24);
        const checked = checkboxes.filter(
            (box) => (box.element as HTMLInputElement).checked,
        );
        expect(checked).toHaveLength(15);
    });

    it("PIO-123 no tab links to the retired billing tab, and the nav names no plan, price or invoice", async () => {
        const wrapper = await mountAt("/pl/settings/account");

        const hrefs = wrapper
            .findAll(".settings-nav a")
            .map((link) => link.attributes("href"));
        expect(hrefs).not.toContain("/pl/settings/billing");
        const nav = wrapper.find(".settings-nav").text();
        expect(nav).not.toContain("Plan i płatności");
        expect(nav).not.toContain("Faktury");
    });

    it('B01 the gdpr tab shows its marker text ("Retencja danych" — a second, also-unique card title on this tab; PanelPagesTest\'s own .card-title marker for this row is "Umowa powierzenia (DPA)", the first card)', async () => {
        const wrapper = await mountAt("/pl/settings/gdpr");

        expect(wrapper.text()).toContain("Retencja danych");
    });

    it("B01 GET /pl/settings/team renders and shows its own page title and subtitle marker", async () => {
        const wrapper = await mountAt("/pl/settings/team");

        expect(wrapper.find(".page-title").text()).toBe("Ustawienia");
        expect(wrapper.find(".page-sub").text()).toBe(SUBTITLES.team);
    });
});

describe("B06 an unknown tab never renders a page body", () => {
    beforeEach(() => {
        vi.stubGlobal(
            "fetch",
            vi.fn(async () => new Response(null, { status: 404 })),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("stays empty when the API 404s", async () => {
        const wrapper = await mountAt("/pl/settings/nonexistent");

        expect(wrapper.find(".page").exists()).toBe(false);
    });

    it("PIO-123 the retired billing tab is one of those unknown tabs — the API 404s and the view renders nothing", async () => {
        const wrapper = await mountAt("/pl/settings/billing");

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/settings/billing",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
        expect(wrapper.find(".page").exists()).toBe(false);
    });
});
