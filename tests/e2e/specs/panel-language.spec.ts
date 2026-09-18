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

    test("the automations index is English on /en and Polish on /pl", async ({
        page,
    }) => {
        await page.goto("/en/automations");
        const englishCards = page.locator(".auto-card .auto-card__title");
        await expect(englishCards.first()).toHaveText("Abandoned cart recovery");

        await page.goto("/pl/automations");
        const polishCards = page.locator(".auto-card .auto-card__title");
        await expect(polishCards.first()).toHaveText(
            "Powrót do porzuconego koszyka",
        );
    });

    test("the rule builder's WHEN/IF/THEN steps are English on /en", async ({
        page,
    }) => {
        await page.goto("/en/automations/a1");
        const kickers = page.locator(".rb-step .rb-step__kicker");

        await expect(kickers.nth(0)).toContainText("WHEN");
        await expect(kickers.nth(1)).toContainText("IF");
        await expect(kickers.nth(2)).toContainText("THEN");
    });

    test("the flow canvas labels its buttons and counts in English on /en", async ({
        page,
    }) => {
        await page.goto("/en/automations/a1?view=flow");

        await expect(page.locator('[data-action="add-node"]')).toContainText(
            "Node",
        );
        await expect(page.locator('[data-action="autolayout"]')).toContainText(
            "Auto-layout",
        );
        await expect(page.locator(".flow-canvas")).toContainText(
            "6 nodes · 5 connections",
        );
    });

    test("the campaigns table is English on /en and Polish on /pl", async ({
        page,
    }) => {
        await page.goto("/en/campaigns");
        await expect(page.locator(".table thead th").first()).toHaveText(
            "Campaign",
        );
        await expect(
            page.locator(".table tbody tr").first(),
        ).toContainText("Triggered");
        await expect(page.locator(".kpi-grid .kpi-value").last()).toHaveText(
            "€184,230",
        );

        await page.goto("/pl/campaigns");
        await expect(page.locator(".table thead th").first()).toHaveText(
            "Kampania",
        );
        await expect(
            page.locator(".table tbody tr").first(),
        ).toContainText("Wyzwalana");
    });

    test("the sample e-mail in the editor is written in English on /en", async ({
        page,
    }) => {
        await page.goto("/en/emails/k1");

        await expect(page.locator(".ee-doc")).toContainText(
            "Hannah, your basket is waiting.",
        );
        await expect(page.locator(".ee-doc")).toContainText("€38.90");
        await expect(page.locator(".email-right")).toContainText(
            "Visibility conditions",
        );
    });

    test("the popup preview and its type list are English on /en", async ({
        page,
    }) => {
        await page.goto("/en/popups");

        await expect(page.locator(".auto-card__title").first()).toHaveText(
            "Exit intent — 10% off",
        );

        await page.goto("/en/popups/p2");
        await expect(page.locator(".pw-banner-row .pw-title")).toContainText(
            "Over €199 we cover the postage",
        );
    });

    test("the customers index and profile are English on /en", async ({
        page,
    }) => {
        await page.goto("/en/customers");
        await expect(page.locator(".page-sub")).toContainText(
            "identified customers",
        );
        await expect(
            page.locator(".table tbody tr").first().locator("td").nth(4),
        ).toContainText("€");

        await page.goto("/en/customers/c_1001");
        await expect(page.locator(".profile-card .profile-fact").first()).toContainText(
            "Orders",
        );
        await expect(page.locator(".tab-strip .tab").first()).toHaveText(
            "Activity",
        );
        await expect(page.locator(".timeline")).toContainText("Today · 14:42");
    });

    test("the feeds page reports its sync state in English on /en", async ({
        page,
    }) => {
        await page.goto("/en/feeds");

        await expect(page.locator(".feed-card .chip").first()).toHaveText(
            "Synchronised",
        );
        await expect(page.locator(".kpi-grid .kpi-label").first()).toHaveText(
            "Active feeds",
        );
    });

    test("the import wizard's steps and columns are English on /en", async ({
        page,
    }) => {
        await page.goto("/en/import");
        await expect(
            page.locator('.step[data-state="cur"] .step__title'),
        ).toHaveText("File");

        await page.goto("/en/import/2");
        await expect(page.locator(".map-table .map-row--head")).toContainText(
            "Column in the file",
        );

        await page.goto("/en/import/3");
        await expect(page.locator(".cond-rule").first()).toContainText("IF");
    });

    test("the settings tabs and role descriptions are English on /en", async ({
        page,
    }) => {
        await page.goto("/en/settings");
        await expect(page.locator(".settings-nav")).toHaveAttribute(
            "aria-label",
            "Settings sections",
        );
        await expect(page.locator(".settings-nav a").first()).toContainText(
            "Account",
        );

        await page.goto("/en/settings/team");
        await expect(page.locator(".page-sub")).toContainText(
            "Who can reach the panel",
        );
        await expect(page.locator(".settings-grid")).toContainText("Owner");
        await expect(page.locator(".settings-grid")).toContainText(
            "Everything except deleting the account.",
        );
    });

    test("PIO-128 no role description mentions billing, in either language", async ({
        page,
    }) => {
        for (const path of ["/en/settings/team", "/pl/settings/team"]) {
            await page.goto(path);
            const text = await page.locator(".page").innerText();
            expect(text).not.toMatch(/rozliczeni|billing|invoice|faktur/i);
        }
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
