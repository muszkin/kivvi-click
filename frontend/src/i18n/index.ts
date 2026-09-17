import { createI18n } from "vue-i18n";
import en from "./en";
import pl from "./pl";

export const SUPPORTED_LOCALES = ["pl", "en"] as const;
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number];
// PIO-125: English, matching SupportedLocale.DEFAULT on the server. Polish stays a full language
// behind /pl, not a fallback.
export const DEFAULT_LOCALE: SupportedLocale = "en";

function initialLocale(): SupportedLocale {
    const fromDocument =
        typeof document !== "undefined"
            ? document.documentElement.lang
            : undefined;
    return (SUPPORTED_LOCALES as readonly string[]).includes(fromDocument ?? "")
        ? (fromDocument as SupportedLocale)
        : DEFAULT_LOCALE;
}

// Each journey ships its own src/i18n/messages/<journey>.pl.ts / <journey>.en.ts, merged into
// the base catalogues below by their filename's locale suffix — see common-journey-rules.md.
const journeyMessages = import.meta.glob("./messages/*.{pl,en}.ts", {
    eager: true,
    import: "default",
}) as Record<string, Record<string, unknown>>;
const plMessages: Record<string, unknown> = { ...pl };
const enMessages: Record<string, unknown> = { ...en };
for (const [path, messages] of Object.entries(journeyMessages)) {
    if (path.endsWith(".pl.ts")) {
        Object.assign(plMessages, messages);
    } else if (path.endsWith(".en.ts")) {
        Object.assign(enMessages, messages);
    }
}
export const i18n = createI18n({
    legacy: false,
    locale: initialLocale(),
    fallbackLocale: DEFAULT_LOCALE,
    // The merge above widens both catalogues to Record<string, unknown> so createI18n's schema
    // inference can no longer see them as the literal shape pl.ts/en.ts themselves have — cast
    // back to that shape: the merged objects are a strict superset of it (pl.ts/en.ts's own keys
    // plus whatever each journey's messages/*.ts added), so nothing here is actually a lie.
    messages: { pl: plMessages, en: enMessages } as unknown as {
        pl: typeof pl;
        en: typeof en;
    },
    // Catalogue strings ported from Twig `|raw` are developer-authored markup bound only with
    // v-html (see eslint.config.js no-v-html exemption); vue-i18n would otherwise warn on each
    // and the fail-on-warning test policy (test/setup.ts) would turn that into a false failure.
    warnHtmlMessage: false,
});
