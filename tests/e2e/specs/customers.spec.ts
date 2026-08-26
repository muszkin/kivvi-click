import { expect, test } from "@playwright/test";

/**
 * Customer index and the 360 profile.
 */
test.describe("customers", () => {
    test("lists customers with segments and money, and paginates", async ({
        page,
    }) => {
        await page.goto("/pl/customers");

        await expect(page.locator(".page-sub")).toContainText(
            "zidentyfikowanych klientów",
        );
        await expect(page.locator(".table tbody tr")).toHaveCount(24);
        await expect(page.locator(".table thead th")).toHaveCount(7);
        await expect(
            page.locator(".table tbody tr").first().locator("td").nth(4),
        ).toContainText("zł");

        await expect(page.locator(".row .mono.muted")).toContainText(
            "Strona 1 z 192",
        );
        await expect(
            page.locator('button[data-action="go-page"]').first(),
        ).toBeDisabled();
    });

    test("a row opens the profile it belongs to", async ({ page }) => {
        await page.goto("/pl/customers");
        const firstRow = page.locator(".table tbody tr").first();
        // The first cell is avatar + name; the name is the last span in it.
        const name = await firstRow
            .locator("td")
            .first()
            .locator("span")
            .last()
            .innerText();

        await firstRow.click();

        await expect(page).toHaveURL(/\/pl\/customers\/c_\d+/);
        await expect(page.locator(".profile-name")).toHaveText(name.trim());
    });

    test("the profile shows facts, ML scores and the activity axis", async ({
        page,
    }) => {
        await page.goto("/pl/customers/c_1001");

        await expect(page.locator(".profile-card .profile-fact")).toHaveCount(
            7,
        );
        await expect(page.locator(".profile-card .chip").first()).toHaveText(
            "VIP",
        );
        await expect(page.locator(".kpi-grid .kpi")).toHaveCount(3);
        await expect(page.locator(".timeline .tl-item")).toHaveCount(9);
        await expect(page.locator(".tab-strip .tab")).toHaveCount(5);
        await expect(page.locator(".tab-strip .tab").first()).toHaveAttribute(
            "aria-selected",
            "true",
        );
    });

    test("back link returns to the index", async ({ page }) => {
        await page.goto("/pl/customers/c_1001");
        await page.locator(".page-head a.btn.ghost").first().click();

        await expect(page).toHaveURL(/\/pl\/customers$/);
    });
});
