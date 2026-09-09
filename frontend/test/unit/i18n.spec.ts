import { describe, expect, it } from "vitest";
import en from "@/i18n/en";
import pl from "@/i18n/pl";

function keys(obj: Record<string, unknown>, prefix = ""): string[] {
    return Object.entries(obj).flatMap(([key, value]) =>
        typeof value === "object" && value !== null
            ? keys(value as Record<string, unknown>, `${prefix}${key}.`)
            : [`${prefix}${key}`],
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
