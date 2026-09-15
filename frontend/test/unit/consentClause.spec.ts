import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import landingEn from "@/i18n/messages/landing.en";
import landingPl from "@/i18n/messages/landing.pl";

/**
 * The waitlist consent clause exists twice: the Vue catalogue renders it to the visitor, and the
 * backend's message bundle supplies the copy stored verbatim on every subscriber row as the GDPR
 * consent proof.
 *
 * Those two must say the same thing. If they drift, the database keeps attesting that someone
 * agreed to wording they were never shown — which is exactly the failure that makes a consent
 * record worthless, and it would happen silently, because nothing else in either stack reads both.
 * Four comments across four files asked for this; this test is what actually enforces it.
 */
function backendMessage(bundle: string, key: string): string {
    // Resolved from the Vitest root (frontend/) rather than through import.meta.url: Vite treats
    // a new URL(...) as a module asset and refuses to load one from outside its own fs root.
    const path = resolve(
        process.cwd(),
        `../backend/src/main/resources/messages_${bundle}.properties`,
    );
    const line = readFileSync(path, "utf8")
        .split("\n")
        .find((candidate) => candidate.startsWith(`${key}=`));
    if (line === undefined) {
        throw new Error(`${key} is missing from messages_${bundle}.properties`);
    }
    return line.slice(`${key}=`.length).trim();
}

describe("PIO-70 the stored consent proof matches the clause the form shows", () => {
    it("Polish: the backend bundle and the Vue catalogue agree word for word", () => {
        expect(landingPl.landingPage.waitlist.consent).toBe(
            backendMessage("pl", "waitlist.consent.text"),
        );
    });

    it("English: the backend bundle and the Vue catalogue agree word for word", () => {
        expect(landingEn.landingPage.waitlist.consent).toBe(
            backendMessage("en", "waitlist.consent.text"),
        );
    });

    it("the clause actually says what is being agreed to, in both languages", () => {
        expect(landingPl.landingPage.waitlist.consent).toContain("e-mail");
        expect(landingEn.landingPage.waitlist.consent).toContain("e-mail");
    });
});
