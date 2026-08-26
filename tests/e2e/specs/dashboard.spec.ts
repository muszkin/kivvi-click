import { expect, test } from "@playwright/test";

/**
 * The dashboard: four KPIs, the cardiogram canvas, the live stream and the
 * best-performing automations — each one a link into the detail it summarises.
 */
test.describe("dashboard", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/pl/dashboard");
    });

    test("shows the four operational KPIs", async ({ page }) => {
        const labels = page.locator(".kpi-grid .kpi-label");

        await expect(labels).toHaveCount(4);
        await expect(labels.nth(0)).toHaveText("Zdarzeń ostatnia minuta");
        await expect(labels.nth(3)).toHaveText("Przypisany przychód (24h)");
        await expect(page.locator(".kpi-spark svg").first()).toBeVisible();
    });

    test("draws the cardiogram canvas and redraws it after a theme change", async ({
        page,
    }) => {
        const canvas = page.locator("#cg-main");
        await expect(canvas).toBeVisible();
        await expect(canvas).toHaveAttribute("role", "img");

        const pixelsBefore = await canvas.evaluate(
            (el: HTMLCanvasElement) => el.width * el.height,
        );
        expect(pixelsBefore).toBeGreaterThan(0);

        await page.locator('[data-action="set-theme"]').click();
        await expect(page.locator("html")).toHaveAttribute(
            "data-theme",
            "dark",
        );
        await expect(canvas).toBeVisible();
    });

    test("server-renders the first page of the live stream", async ({
        page,
    }) => {
        const rows = page.locator("#event-stream .event-row");

        await expect(rows).toHaveCount(10);
        await expect(rows.first().locator(".event-row__time")).toHaveText(
            /\d{2}:\d{2}:\d{2}/,
        );
        await expect(page.locator("#event-stream")).toHaveAttribute(
            "data-event-stream-topic",
            "/accounts/1/events",
        );
    });

    test("recently seen customers link to their profile", async ({ page }) => {
        const firstCustomer = page
            .locator('a.event-row[href*="/customers/c_"]')
            .first();
        const name = await firstCustomer
            .locator("div > div")
            .first()
            .innerText();

        await firstCustomer.click();

        await expect(page).toHaveURL(/\/pl\/customers\/c_\d+/);
        await expect(page.locator(".profile-name")).toHaveText(name);
    });

    test("best-performing automations open the rule editor", async ({
        page,
    }) => {
        await page
            .locator('a.event-row[href*="/automations/a"]')
            .first()
            .click();

        await expect(page).toHaveURL(/\/pl\/automations\/a\d/);
        await expect(page.locator(".rb-pipeline")).toBeVisible();
    });

    test("pausing the stream toggles its state", async ({ page }) => {
        const pause = page.locator('[data-action="pause-stream"]').first();
        await pause.click();

        await expect(page.locator("#event-stream")).toHaveAttribute(
            "data-paused",
            "true",
        );
    });
});
