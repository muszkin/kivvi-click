import { readdirSync, readFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { expect, test } from "@playwright/test";

/**
 * The landing page's waitlist signup (PIO-70), end to end against the running stack.
 *
 * Every test uses its own address. The per-address allowance is 3 submissions an hour and the
 * per-IP one is 10, both counted in Postgres and therefore shared by everything running against
 * this stack — reusing a literal address across tests, or across two runs within the hour, would
 * make the result depend on what ran before.
 *
 * The per-IP bucket is the one to watch when re-running: unique addresses do nothing for it, and
 * this file spends 6 of its 10 hourly submissions per run (PIO-71 added two). A second run against
 * the same stack inside the hour will start seeing 429s. Start the dev stack with `down -v` first
 * (the documented flow) and the counter starts empty.
 *
 * PIO-71 closes the loop through a real message. Under the `dev` profile the API writes every
 * message to ./var/mail as an .eml instead of contacting a relay (compose.yaml mounts that
 * directory from the host), so these tests read the confirmation link out of the file the way a
 * subscriber reads it out of their inbox — no mail container, and Postgres stays the stack's only
 * backing service.
 */
const MAILDROP = process.env.E2E_MAILDROP ?? resolve(__dirname, "../../../var/mail");

/**
 * The sender drains the outbox on a fixed delay, so a message is on disk within a tick rather than
 * within the request. Everything below waits for that, which is why these tests get a longer
 * budget than the default 30 seconds.
 */
const MAIL_TIMEOUT_MS = 60_000;

async function waitForMessage(recipient: string): Promise<string> {
    const wanted = recipient.replace(/[^A-Za-z0-9._@-]/g, "_");
    const deadline = Date.now() + MAIL_TIMEOUT_MS;
    while (Date.now() < deadline) {
        const files = readdirSync(MAILDROP, { withFileTypes: true })
            .filter((entry) => entry.isFile() && entry.name.includes(wanted))
            .map((entry) => join(MAILDROP, entry.name));
        if (files.length > 0) {
            return decodeQuotedPrintable(readFileSync(files[0], "utf8"));
        }
        await new Promise((done) => setTimeout(done, 500));
    }
    throw new Error(`No message for ${recipient} appeared in ${MAILDROP} within ${MAIL_TIMEOUT_MS}ms.`);
}

/**
 * Only the soft line breaks matter here. Quoted-printable wraps at 76 characters, which lands in
 * the middle of a 64-character token often enough that a naive regex finds half a link; the
 * encoded diacritics can stay encoded, since nothing below asserts on Polish text from the file.
 */
function decodeQuotedPrintable(raw: string): string {
    return raw.replace(/=\r?\n/g, "");
}

function pathFrom(message: string, kind: "confirm" | "unsubscribe"): string {
    const match = message.match(
        new RegExp(`/(?:pl|en)/waitlist/${kind}/[0-9a-f]{64}`),
    );
    expect(match, `the message should carry a ${kind} link`).not.toBeNull();
    return match![0];
}
function uniqueEmail(prefix: string): string {
    return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1e6)}@sklep.pl`;
}

test.describe("waitlist signup", () => {
    test("leaving an address shows the confirmation", async ({ page }) => {
        await page.goto("/pl");

        await page.locator("#f-email").fill(uniqueEmail("e2e-new"));
        await page.locator(".waitlist-consent input[type=checkbox]").check();
        await page.locator(".waitlist-form button[type=submit]").click();

        // Post/redirect/get: the browser ends up back on the landing page, flagged as done.
        await expect(page).toHaveURL(/\/pl\?waitlist=ok$/);
        // PIO-71: the address is pending until the link is followed, and the copy says so now.
        await expect(page.locator(".waitlist-thanks")).toContainText(
            "link potwierdzający",
        );
        await expect(page.locator("form.waitlist-form")).toHaveCount(0);
    });

    test("signing up twice with the same address looks exactly like the first time", async ({
        page,
    }) => {
        const email = uniqueEmail("e2e-repeat");

        for (const attempt of [1, 2]) {
            await page.goto("/pl");
            await page.locator("#f-email").fill(email);
            await page.locator(".waitlist-consent input[type=checkbox]").check();
            await page.locator(".waitlist-form button[type=submit]").click();

            await expect(
                page,
                `attempt ${attempt} should land on the confirmation`,
            ).toHaveURL(/\/pl\?waitlist=ok$/);
            await expect(page.locator(".waitlist-thanks")).toBeVisible();
        }
    });

    test("submitting without the consent box shows the message and keeps the address", async ({
        page,
    }) => {
        const email = uniqueEmail("e2e-no-consent");
        await page.goto("/pl");

        await page.locator("#f-email").fill(email);
        // The box is deliberately not `required`, so the browser lets this through and the
        // server-side check is the one that answers — which is the point of the test.
        await page.locator(".waitlist-form button[type=submit]").click();

        await expect(page.locator(".waitlist-row")).toContainText(
            "Zaznacz zgodę",
        );
        await expect(page.locator("#f-email")).toHaveValue(email);
        await expect(
            page.locator(".waitlist-consent input[type=checkbox]"),
        ).not.toBeChecked();
    });

    test("the consent clause links to the privacy policy", async ({ page }) => {
        await page.goto("/pl");

        await page.locator(".waitlist-consent a").click();

        await expect(page).toHaveURL(/\/pl\/privacy$/);
        await expect(page.locator("h1")).toContainText("Polityka prywatności");
    });
});

test.describe("privacy policy", () => {
    test("is reachable from the public footer", async ({ page }) => {
        await page.goto("/pl");

        await page.locator(".landing-foot a", { hasText: "Prywatność" }).click();

        await expect(page).toHaveURL(/\/pl\/privacy$/);
        await expect(page.locator("h1")).toContainText("Polityka prywatności");
        await expect(page.locator(".legal-section h2").first()).toContainText(
            "Kto jest administratorem",
        );
    });

    test("opens directly in English too", async ({ page }) => {
        await page.goto("/en/privacy");

        await expect(page.locator("h1")).toContainText("Privacy policy");
    });
});

test.describe("waitlist double opt-in", () => {
    test.describe.configure({ timeout: MAIL_TIMEOUT_MS + 30_000 });

    test("the confirmation link in the message confirms the address, once", async ({
        page,
    }) => {
        const email = uniqueEmail("e2e-confirm");
        await page.goto("/pl");
        await page.locator("#f-email").fill(email);
        await page.locator(".waitlist-consent input[type=checkbox]").check();
        await page.locator(".waitlist-form button[type=submit]").click();
        await expect(page).toHaveURL(/\/pl\?waitlist=ok$/);

        const message = await waitForMessage(email);
        expect(message).toContain("Potwier");
        // The plain-text alternative is mandatory; a message without one scores worse with spam
        // filters and is unreadable in a text client.
        expect(message).toContain("text/plain");
        expect(message).not.toContain("oklch(");

        await page.goto(pathFrom(message, "confirm"));
        await expect(page.locator(".confirm-page")).toHaveAttribute(
            "data-state",
            "ok",
        );
        await expect(page.locator("h1")).toContainText("potwierdziliśmy adres");

        // Following it again is reassurance, not an error and not a second confirmation.
        await page.reload();
        await expect(page.locator(".confirm-page")).toHaveAttribute(
            "data-state",
            "already",
        );
    });

    test("the unsubscribe link in the message works without a login", async ({
        page,
    }) => {
        const email = uniqueEmail("e2e-unsub");
        await page.goto("/pl");
        await page.locator("#f-email").fill(email);
        await page.locator(".waitlist-consent input[type=checkbox]").check();
        await page.locator(".waitlist-form button[type=submit]").click();
        await expect(page).toHaveURL(/\/pl\?waitlist=ok$/);

        const message = await waitForMessage(email);

        await page.goto(pathFrom(message, "unsubscribe"));
        await expect(page.locator(".confirm-page")).toHaveAttribute(
            "data-state",
            "ok",
        );
        await expect(page.locator("h1")).toContainText("Wypisaliśmy Cię");

        // Idempotent: a second click says the same thing rather than falling over.
        await page.reload();
        await expect(page.locator(".confirm-page")).toHaveAttribute(
            "data-state",
            "ok",
        );
    });

    test("a link nobody issued is answered, not crashed into", async ({ page }) => {
        await page.goto(`/pl/waitlist/confirm/${"f".repeat(64)}`);

        await expect(page.locator(".confirm-page")).toHaveAttribute(
            "data-state",
            "unknown",
        );
        await expect(page.locator("h1")).toContainText("Nie znamy tego linku");
    });
});
