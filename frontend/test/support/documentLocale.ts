import {
    DEFAULT_LOCALE,
    i18n,
    SUPPORTED_LOCALES,
    type SupportedLocale,
} from "@/i18n";

const LOCALE_SEGMENT = /[/?#]/;

/**
 * Puts the shared i18n instance into the language the server would serve `path` in.
 *
 * In the browser the SPA never picks its own language: the server stamps `<html lang>` from the
 * URL's locale segment and src/i18n/index.ts reads it once at start-up. jsdom has no server, so
 * every test file starts in DEFAULT_LOCALE — and the suites that mount `/pl/...` and assert Polish
 * copy only passed while Polish was the default. PIO-125 made English the default, which is what
 * exposed that. Mount helpers call this so a test's language follows its URL, the way a real
 * page's does, instead of following whichever test happened to run before it.
 */
export function serveInLocaleOf(path: string): SupportedLocale {
    const segment = path.split(LOCALE_SEGMENT)[1] ?? "";
    const locale = (SUPPORTED_LOCALES as readonly string[]).includes(segment)
        ? (segment as SupportedLocale)
        : DEFAULT_LOCALE;
    i18n.global.locale.value = locale;
    return locale;
}
