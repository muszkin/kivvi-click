import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { createRouter, createWebHistory } from "vue-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { i18n } from "@/i18n";
import { routes } from "@/router/routes";
import { useShellStore } from "@/stores/shell";
import ImportView from "@/views/ImportView.vue";

const STEPS = [
    { n: 1, label: "Plik" },
    { n: 2, label: "Mapowanie kolumn" },
    { n: 3, label: "Reguły i segmenty" },
    { n: 4, label: "Podgląd i start" },
];

const TARGETS = [
    { value: "", label: "— wybierz pole —" },
    { value: "email", label: "Email (klucz dedup.)" },
];

const RECENT = [
    {
        file: "newsletter-2026-feb.csv",
        rows: "4 280",
        date: "14 lut 2026",
        who: "Maciej K.",
        ok: true,
        note: "OK",
    },
    {
        file: "klienci-shoper.xml",
        rows: "1 284",
        date: "02 sty 2026",
        who: "Anna B.",
        ok: true,
        note: "OK",
    },
    {
        file: "mailerlite-archiwum.csv",
        rows: "8 120",
        date: "18 gru 2025",
        who: "Maciej K.",
        ok: false,
        note: "12 błędów",
    },
];

const COLUMN_NAMES = [
    "Adres e-mail",
    "Imię",
    "Nazwisko",
    "Telefon",
    "Data rejestracji",
    "Status zgody mkt.",
    "Liczba zamówień",
    "Łączna wartość PLN",
    "Język",
    "Tag CRM",
    "Identyfikator Shoper",
];

function makeColumns() {
    return COLUMN_NAMES.map((name, index) => ({
        letter: String.fromCharCode(65 + index),
        name,
        samples: ["a", "b", "c"],
        targets: TARGETS,
        mapped: index === COLUMN_NAMES.length - 1 ? "__skip" : "email",
        confidence: index === COLUMN_NAMES.length - 1 ? 0 : 90,
        skipped: index === COLUMN_NAMES.length - 1,
    }));
}

const DEDUP_STRATEGIES = [
    {
        value: "merge",
        label: "Nadpisz polami z pliku, gdy nie są puste (zalecane)",
        checked: true,
    },
    {
        value: "fill",
        label: "Wypełnij tylko brakujące pola — nie nadpisuj istniejących",
        checked: false,
    },
    {
        value: "skip",
        label: "Pomiń duplikat — nie zmieniaj nic",
        checked: false,
    },
    {
        value: "replace",
        label: "Zastąp wszystkie pola pełną zawartością z pliku",
        checked: false,
    },
];

const SUMMARY = [
    {
        label: "Wierszy łącznie",
        value: "8 420",
        delta: "plik wczytany poprawnie",
        dir: "up",
        deltaIcon: "check",
    },
    {
        label: "Nowi klienci",
        value: "7 124",
        delta: "~84,6% wszystkich",
        dir: "up",
        deltaIcon: null,
    },
    {
        label: "Aktualizacje istniejących",
        value: "1 252",
        delta: "nadpisanie wg reguł z kroku 3",
        dir: "flat",
        deltaIcon: "check",
    },
    {
        label: "Z błędem walidacji",
        value: "44",
        delta: "zostaną pominięte",
        dir: "down",
        deltaIcon: "info",
    },
];

const PREVIEW = [
    {
        status: "new",
        statusTone: "good",
        statusLabel: "Nowy",
        email: "hania.k@aurea.pl",
        name: "Hania Kowalska",
        optIn: "Tak",
        orders: "7",
        ltv: "1 248 zł",
        segments: ["Newsletter", "VIP"],
        error: "",
    },
    {
        status: "update",
        statusTone: "info",
        statusLabel: "Aktualizacja",
        email: "marta.w@example.com",
        name: "Marta Wiśniewska",
        optIn: "Tak",
        orders: "2",
        ltv: "384 zł",
        segments: ["Newsletter"],
        error: "",
    },
    {
        status: "new",
        statusTone: "good",
        statusLabel: "Nowy",
        email: "tomek.s@gmail.com",
        name: "Tomek Sobczak",
        optIn: "Nie",
        orders: "0",
        ltv: "0 zł",
        segments: ["Newsletter"],
        error: "",
    },
    {
        status: "update",
        statusTone: "info",
        statusLabel: "Aktualizacja",
        email: "olek.b@example.pl",
        name: "Olek Borowski",
        optIn: "Tak",
        orders: "14",
        ltv: "3 490 zł",
        segments: ["Newsletter", "VIP"],
        error: "",
    },
    {
        status: "error",
        statusTone: "bad",
        statusLabel: "Błąd",
        email: "invalid@@@bad",
        name: "(nieznane)",
        optIn: "?",
        orders: "?",
        ltv: "?",
        segments: [],
        error: "Email nie przechodzi walidacji",
    },
    {
        status: "new",
        statusTone: "good",
        statusLabel: "Nowy",
        email: "iga.p@example.com",
        name: "Iga Pszczółkowska",
        optIn: "Tak",
        orders: "0",
        ltv: "0 zł",
        segments: ["Newsletter"],
        error: "",
    },
];

function payloadFor(step: number) {
    return {
        step,
        steps: STEPS,
        file: {
            name: step === 2 ? "klienci-oracle.csv" : "klienci.csv",
            meta: "8 420 wierszy · CSV UTF-8 · ; jako separator",
        },
        columns: makeColumns(),
        detection: {
            recognised: 10,
            total: 11,
            sure: 9,
            unsure: 1,
            skipped: 1,
        },
        validations: [
            { label: "Format email (RFC 5322)", tone: "ok" },
            {
                label: "Wykryte 4 duplikaty po email — patrz krok 3",
                tone: "info",
            },
        ],
        dedupStrategies: DEDUP_STRATEGIES,
        summary: SUMMARY,
        preview: PREVIEW,
        rowCount: 8420,
        rowCountLabel: "8 420",
        errorCount: 44,
        recent: RECENT,
    };
}

async function mountAt(path: string, step: number) {
    const router = createRouter({ history: createWebHistory(), routes });
    await router.push(path);
    await router.isReady();
    const wrapper = mount(ImportView, {
        global: { plugins: [router, i18n] },
    });
    await flushPromises();
    void step;
    return wrapper;
}

describe("B31/B01 the import wizard matches the oracle: 4 steps, one URL each", () => {
    beforeEach(() => {
        setActivePinia(createPinia());
        useShellStore().$patch({
            user: { name: "Maciej Kowalczyk", email: "maciej@aureashop.pl" },
        });
        vi.stubGlobal(
            "fetch",
            vi.fn(async (input: RequestInfo | URL) => {
                const url = String(input);
                const match = /\/import\/(\d)$/.exec(url);
                const step = match ? Number(match[1]) : 1;
                return new Response(JSON.stringify(payloadFor(step)), {
                    status: 200,
                    headers: { "Content-Type": "application/json" },
                });
            }),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it("fetches the locale- and step-scoped payload", async () => {
        await mountAt("/pl/import/1", 1);

        expect(fetch).toHaveBeenCalledWith(
            "/api/v1/pl/import/1",
            expect.objectContaining({
                headers: { Accept: "application/json" },
            }),
        );
    });

    it("B31 step 1 offers the dropzone, the template and the API, plus the sticky recent-imports card", async () => {
        const wrapper = await mountAt("/pl/import/1", 1);

        expect(wrapper.findAll(".stepper .step")).toHaveLength(4);
        expect(
            wrapper.find('.step[data-state="cur"] .step__title').text(),
        ).toBe("Plik");
        expect(wrapper.find(".dropzone").exists()).toBe(true);
        const cards = wrapper.findAll(".wiz-layout .card");
        expect(cards[cards.length - 1]?.text()).toContain("Ostatnie importy");
    });

    it("B31 step 2 maps every column with a confidence read-out, and shows the uploaded file's name", async () => {
        const wrapper = await mountAt("/pl/import/2", 2);

        expect(
            wrapper.findAll(".map-table .map-row:not(.map-row--head)"),
        ).toHaveLength(11);
        expect(wrapper.find(".auto-detect").text()).toContain(
            "rozpoznał 10 z 11 kolumn",
        );
        expect(wrapper.findAll('.map-row[data-skipped="true"]')).toHaveLength(
            1,
        );
        expect(wrapper.find(".file-pill").text()).toContain(
            "klienci-oracle.csv",
        );
    });

    it("B31 step 3 asks for a dedup strategy and GDPR consent", async () => {
        const wrapper = await mountAt("/pl/import/3", 3);

        expect(wrapper.findAll('input[name="dedup"]')).toHaveLength(4);
        expect(
            (wrapper.find('input[name="dedup"]').element as HTMLInputElement)
                .checked,
        ).toBe(true);
        expect(wrapper.findAll(".cond-rule")).toHaveLength(2);
        expect(wrapper.text()).toContain("Zgody i prywatność (RODO)");
    });

    it("B31 step 4 summarises the run and flags invalid rows", async () => {
        const wrapper = await mountAt("/pl/import/4", 4);

        expect(wrapper.findAll(".kpi-grid .kpi")).toHaveLength(4);
        expect(wrapper.findAll(".table tbody tr")).toHaveLength(6);
        expect(wrapper.findAll(".table tbody tr .chip.bad")).toHaveLength(1);
        expect(wrapper.find('[data-action="run-import"]').text()).toContain(
            "8 420",
        );
    });
});
