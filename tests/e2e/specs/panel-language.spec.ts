import { expect, test } from "@playwright/test";

/**
 * PIO-129: the English landing page's "See the demo panel" button led to a panel that mostly spoke
 * Polish. The unit and API tests hold the copy itself to both languages; this spec proves the
 * browser actually renders it — that the locale reaches the page, not just the payload.
 *
 * <p>The panel is being translated one area at a time, so this file grows with each slice. What it
 * must never do is assert that a whole page carries no Polish: the dashboard deliberately shows two
 * lists borrowed from pages not yet translated, and a blanket assertion would have to be weakened
 * later, which is the opposite of a guard.
 */
test.describe("the panel speaks the language of the URL", () => {
    test("the dashboard's KPI tiles and legend are English on /en and Polish on /pl", async ({
        page,
    }) => {
        await page.goto("/en/dashboard");
        const englishLabels = page.locator(".kpi-grid .kpi-label");
        await expect(englishLabels).toHaveCount(4);
        await expect(englishLabels.nth(0)).toHaveText("Events last minute");
        await expect(englishLabels.nth(3)).toHaveText(
            "Revenue attributed (24h)",
        );

        await page.goto("/pl/dashboard");
        const polishLabels = page.locator(".kpi-grid .kpi-label");
        await expect(polishLabels.nth(0)).toHaveText("Zdarzeń ostatnia minuta");
        await expect(polishLabels.nth(3)).toHaveText(
            "Przypisany przychód (24h)",
        );
    });

    test("money is written in euro on /en and in złoty on /pl", async ({
        page,
    }) => {
        await page.goto("/en/dashboard");
        await expect(page.locator(".kpi-grid .kpi-value").nth(3)).toHaveText(
            "€94,200",
        );

        await page.goto("/pl/dashboard");
        await expect(page.locator(".kpi-grid .kpi-value").nth(3)).toHaveText(
            "94 200 zł",
        );
    });

    test("the event stream's sample shop is English on /en", async ({
        page,
    }) => {
        await page.goto("/en/events");
        const rows = page.locator("#event-stream .event-row");
        await expect(rows.first()).toBeVisible();

        const details = (await rows.allTextContents()).join(" ");
        expect(details).toContain("Sencha green tea 100g");
        expect(details).not.toContain("Zielona herbata");
    });

    test("the range buttons above the stream are English on /en", async ({
        page,
    }) => {
        await page.goto("/en/events");
        const ranges = page.locator('.seg [data-action="set-range"]');

        await expect(ranges.nth(1)).toHaveText("1 hr");
        await expect(ranges.nth(3)).toHaveText("7 days");
    });

    test("the shell's icon-only controls are labelled in English on /en", async ({
        page,
    }) => {
        await page.goto("/en/dashboard");

        await expect(page.locator("nav.nav")).toHaveAttribute(
            "aria-label",
            "Main navigation",
        );
        await expect(page.locator("nav.crumbs")).toHaveAttribute(
            "aria-label",
            "Breadcrumb",
        );
        await expect(page.locator("button.kbar")).toHaveAttribute(
            "aria-label",
            "Search",
        );
    });

    test("the workspace card counts sites in English on /en", async ({
        page,
    }) => {
        await page.goto("/en/dashboard");
        await expect(page.locator(".ws-meta")).toHaveText("3 sites");

        await page.goto("/pl/dashboard");
        await expect(page.locator(".ws-meta")).toHaveText("3 strony");
    });
});
