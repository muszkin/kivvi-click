import { describe, expect, it } from "vitest";
import { DEFAULT_LOCALE, i18n } from "@/i18n";
import en from "@/i18n/en";
import pl from "@/i18n/pl";

type Catalogue = Record<string, unknown>;

function keys(obj: Catalogue, prefix = ""): string[] {
    return Object.entries(obj).flatMap(([key, value]) =>
        typeof value === "object" && value !== null
            ? keys(value as Catalogue, `${prefix}${key}.`)
            : [`${prefix}${key}`],
    );
}

function leaves(obj: Catalogue, prefix = ""): Map<string, string> {
    return new Map(
        Object.entries(obj).flatMap(([key, value]) =>
            typeof value === "object" && value !== null
                ? [...leaves(value as Catalogue, `${prefix}${key}.`)]
                : [[`${prefix}${key}`, String(value)] as const],
        ),
    );
}

/** What the running app renders from: pl.ts / en.ts merged with every messages/<journey> file. */
function merged(locale: "pl" | "en"): Catalogue {
    return i18n.global.getLocaleMessage(locale) as Catalogue;
}

/**
 * Every key a public page (landing, privacy policy, confirmation and unsubscribe pages, the login
 * page the public header links to, and the header and footer around them) renders. The panel's
 * catalogues are not listed: translating the panel is outside PIO-117 and PIO-125.
 */
const PUBLIC_NAMESPACES = [
    "landing.",
    "landingPage.",
    "waitlistPage.",
    "privacyPage.",
    "auth.",
];
const PUBLIC_COMMON_KEYS = [
    "common.documentation",
    "common.status",
    "common.changeLanguage",
    "common.email",
    "common.password",
    "common.or",
    "common.continueWithGoogle",
    "common.marketingAutomation",
];

/** Keys whose English and Polish really are the same word — every one of them on purpose. */
const IDENTICAL_BY_DESIGN = new Set([
    "landing.openSource",
    "landing.blog",
    "landing.repo",
    "common.status",
    "common.email",
    "common.marketingAutomation",
]);

/** Letters only Polish uses. */
const POLISH_LETTERS = /[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]/;

/** A place name in the controller's registered address, which no translation changes. */
const PROPER_NOUNS = ["Niepołomice"];

function isPublicKey(key: string): boolean {
    return (
        PUBLIC_NAMESPACES.some((namespace) => key.startsWith(namespace)) ||
        PUBLIC_COMMON_KEYS.includes(key)
    );
}

describe("B04 the English catalogue translates every Polish navigation and page-head key", () => {
    it("carries the same key set as the Polish catalogue", () => {
        expect(keys(en).sort()).toEqual(keys(pl).sort());
    });

    it("translates every nav.* label", () => {
        expect(en.nav.dashboard).toBe("Dashboard");
        expect(en.nav.events).toBe("Event stream");
        expect(en.nav.customers).toBe("Customers");
        expect(pl.nav.dashboard).toBe("Pulpit");
    });

    it("translates the dashboard page-head", () => {
        expect(en.dashboard.title).toBe("What's happening now");
        expect(pl.dashboard.title).toBe("Co dzieje się teraz");
    });
});

describe("PIO-117 no key drifts between the Polish and English catalogues", () => {
    // The base catalogues were already compared above; the journey files under messages/ were
    // not, and they are where most of the copy lives. A key missing on one side does not fail —
    // vue-i18n quietly renders the other language's string, which is exactly how "Regulamin" and
    // "Prywatność" ended up on the English footer.
    it("every key in the merged Polish catalogue exists in English", () => {
        const english = new Set(keys(merged("en")));

        expect(keys(merged("pl")).filter((key) => !english.has(key))).toEqual(
            [],
        );
    });

    it("every key in the merged English catalogue exists in Polish", () => {
        const polish = new Set(keys(merged("pl")));

        expect(keys(merged("en")).filter((key) => !polish.has(key))).toEqual(
            [],
        );
    });
});

describe("PIO-117 every string on an English public page is English", () => {
    const polish = leaves(merged("pl"));
    const english = leaves(merged("en"));
    const publicKeys = [...english.keys()].filter(isPublicKey);

    it("finds the public keys it is meant to guard", () => {
        expect(publicKeys).toContain("landing.terms");
        expect(publicKeys).toContain("landingPage.waitlist.consent");
        expect(publicKeys).toContain("privacyPage.translation.notice");
        expect(publicKeys).toContain("auth.deploymentCta");
        expect(publicKeys.length).toBeGreaterThan(50);
    });

    it("no English public string is a copy of its Polish original", () => {
        const untranslated = publicKeys.filter(
            (key) =>
                !IDENTICAL_BY_DESIGN.has(key) &&
                english.get(key) === polish.get(key),
        );

        expect(untranslated).toEqual([]);
    });

    it("no English public string carries a Polish letter", () => {
        const withPolishLetters = publicKeys.filter((key) => {
            const text = PROPER_NOUNS.reduce(
                (remaining, noun) => remaining.replaceAll(noun, ""),
                english.get(key) ?? "",
            );
            return POLISH_LETTERS.test(text);
        });

        expect(withPolishLetters).toEqual([]);
    });
});

describe("PIO-125 English is the default locale", () => {
    it("is what a document without a lang attribute starts in, and what a missing key falls back to", () => {
        expect(DEFAULT_LOCALE).toBe("en");
        expect(i18n.global.fallbackLocale.value).toBe("en");
    });
});
