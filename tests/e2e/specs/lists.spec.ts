import { expect, test } from "@playwright/test";

/**
 * Campaigns, widgets and product feeds — the three index screens that carry
 * their own KPI or preview surface.
 */
test.describe("campaigns", () => {
    test("KPI strip, filters and the performance table", async ({ page }) => {
        await page.goto("/pl/campaigns");

        await expect(page.locator(".kpi-grid .kpi")).toHaveCount(4);
        await expect(page.locator(".filter-chip")).toHaveCount(4);
        await expect(page.locator(".table tbody tr")).toHaveCount(5);
        await expect(page.locator(".table tbody tr").first()).toContainText(
            "Wyzwalana",
        );
        await expect(
            page.locator(".table tbody tr").last().locator("td").nth(3),
        ).toHaveText("—");
    });
});

test.describe("widgets", () => {
    test("list with a live preview of the selected widget", async ({
        page,
    }) => {
        await page.goto("/pl/popups");

        await expect(page.locator(".auto-card")).toHaveCount(5);
        await expect(page.locator(".pw-stage")).toHaveAttribute(
            "data-type",
            "modal",
        );
        await expect(page.locator(".section-title")).toContainText(
            "Exit intent — 10% rabatu",
        );
    });

    test("selecting another widget updates the preview", async ({ page }) => {
        await page.goto("/pl/popups");
        await page
            .locator(".auto-card", { hasText: "Pasek darmowej dostawy" })
            .click();

        await expect(page).toHaveURL(/preview=p2/);
        await expect(page.locator(".pw-stage")).toHaveAttribute(
            "data-type",
            "banner",
        );
        await expect(page.locator(".pw--banner")).toBeVisible();
    });
});

test.describe("product feeds", () => {
    test("sources, connected feeds and the failing one", async ({ page }) => {
        await page.goto("/pl/feeds");

        await expect(page.locator(".feed-source")).toHaveCount(4);
        await expect(page.locator(".feed-card")).toHaveCount(4);
        await expect(page.locator(".feed-card__err")).toHaveCount(1);
        await expect(page.locator(".feed-card__err")).toContainText("HTTP 503");
        await expect(
            page.locator(".feed-card .chip.info .dot.live"),
        ).toHaveCount(1);
    });

    test("the matching diagnostic explains the coverage", async ({ page }) => {
        await page.goto("/pl/feeds");
        const card = page.locator(".card", {
            hasText: "Dopasowanie cen do zdarzeń",
        });

        await expect(card.locator(".bar-row")).toHaveCount(4);
        await expect(card).toContainText("event.product_id");
        await expect(card).toContainText("feed.id");
        await expect(
            card.locator('[data-action="show-mismatched"]'),
        ).toContainText("142");
    });
});
