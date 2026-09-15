import { expect, test } from "@playwright/test";

/**
 * The landing page's waitlist signup (PIO-70), end to end against the running stack.
 *
 * Every test uses its own address. The per-address allowance is 3 submissions an hour and the
 * per-IP one is 10, both counted in Postgres and therefore shared by everything running against
 * this stack — reusing a literal address across tests, or across two runs within the hour, would
 * make the result depend on what ran before.
 */
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
        await expect(page.locator(".waitlist-thanks")).toContainText(
            "jesteś na liście",
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
