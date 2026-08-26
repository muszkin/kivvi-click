import { expect, test } from "@playwright/test";

/**
 * Automation index and the two representations of one rule model.
 */
test.describe("automations", () => {
    test("lists rules with status, trigger and performance", async ({
        page,
    }) => {
        await page.goto("/pl/automations");

        await expect(page.locator(".auto-card")).toHaveCount(6);
        const first = page.locator(".auto-card").first();
        await expect(first.locator(".auto-card__title")).toHaveText(
            "Powrót do porzuconego koszyka",
        );
        await expect(first.locator(".chip").first()).toHaveText("Aktywna");
        await expect(first.locator(".auto-card__num").last()).toContainText(
            "zł",
        );
        await expect(page.locator(".filter-chip")).toHaveCount(4);
    });

    test("opens the rule editor in list form", async ({ page }) => {
        await page.goto("/pl/automations");
        await page.locator(".auto-card").first().click();

        await expect(page).toHaveURL(/\/pl\/automations\/a1/);
        await expect(page.locator(".rb-step")).toHaveCount(3);
        await expect(page.locator(".rb-step__kicker").first()).toContainText(
            "KIEDY",
        );
        await expect(
            page.locator(".rb-step").nth(1).locator(".rb-block"),
        ).toHaveCount(3);
    });

    test("switches to the diagram and back without changing the rule", async ({
        page,
    }) => {
        await page.goto("/pl/automations/a1");

        await page
            .locator('.seg [data-action="navigate"]', { hasText: "Diagram" })
            .click();
        await expect(page).toHaveURL(/view=flow/);
        await expect(page.locator(".flow-canvas .flow-node")).toHaveCount(6);
        await expect(page.locator(".flow-canvas .flow-svg path")).toHaveCount(
            5,
        );

        await page
            .locator('.seg [data-action="navigate"]', { hasText: "Lista" })
            .click();
        await expect(page).toHaveURL(/view=list/);
        await expect(page.locator(".rb-pipeline")).toBeVisible();
    });

    test("shows the simulation before publishing", async ({ page }) => {
        await page.goto("/pl/automations/a1");
        const card = page.locator(".card", { hasText: "Test reguły" });

        await expect(card.locator(".mono").first()).toContainText("2");
        await expect(card).toContainText("Estymowany przychód");
        await expect(page.locator('[data-action="publish"]')).toBeVisible();
    });
});
