import type { RouteLocationNormalizedLoaded } from "vue-router";
import {
    DEFAULT_LOCALE,
    SUPPORTED_LOCALES,
    type SupportedLocale,
} from "@/i18n";

function isSupportedLocale(candidate: unknown): candidate is SupportedLocale {
    return (SUPPORTED_LOCALES as readonly unknown[]).includes(candidate);
}

/**
 * The locale a public page is being read in: its URL segment, or the default when the path has
 * none. Only the landing page's bare "/" has no segment — every other route requires one — so in
 * practice this is what makes "/" English rather than Polish (PIO-125). Each public view used to
 * carry its own `: "pl"` fallback; one helper keeps them from disagreeing about the default again.
 */
export function routeLocale(
    route: RouteLocationNormalizedLoaded,
): SupportedLocale {
    const segment = route.params.locale;
    return isSupportedLocale(segment) ? segment : DEFAULT_LOCALE;
}

/** The language the public locale switch offers from `locale`. */
export function otherLocale(locale: SupportedLocale): SupportedLocale {
    return locale === "pl" ? "en" : "pl";
}
