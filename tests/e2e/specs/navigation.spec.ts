import { expect, test } from "@playwright/test";

/**
 * The shell: every navigation entry leads somewhere, and the chrome that surrounds
 * a page keeps working — collapse, theme, locale, breadcrumb.
 */

const SECTIONS = [
    { route: "dashboard", url: "/pl/dashboard", title: "Co dzieje się teraz" },
    { route: "events", url: "/pl/events", title: "Strumień zdarzeń" },
    { route: "customers", url: "/pl/customers", title: "Klienci" },
    {
        route: "automations",
        url: "/pl/automations",
        title: "Reguły i automatyzacje",
    },
    { route: "campaigns", url: "/pl/campaigns", title: "Kampanie email" },
    { route: "popups", url: "/pl/popups", title: "Popupy i widgety" },
    { route: "feeds", url: "/pl/feeds", title: "Feedy produktów" },
    { route: "import", url: "/pl/import", title: "Import klientów" },
    { route: "settings", url: "/pl/settings", title: "Ustawienia" },
];

test.describe("app shell", () => {
    for (const section of SECTIONS) {
        test(`sidebar entry "${section.route}" opens its page`, async ({
            page,
        }) => {
            await page.goto("/pl/dashboard");
            await page
                .locator(`.nav-item[data-route="${section.route}"]`)
                .click();

            await expect(page).toHaveURL(
                new RegExp(`${section.url.replace(/\//g, "\\/")}`),
            );
            await expect(page.locator(".page-title")).toContainText(
                section.title,
            );
            await expect(
                page.locator(`.nav-item[data-route="${section.route}"]`),
            ).toHaveAttribute("aria-current", "page");
        });
    }

    test("only .main-scroll scrolls, never the window", async ({ page }) => {
        await page.goto("/pl/events");

        const documentOverflows = await page.evaluate(
            () =>
                document.documentElement.scrollHeight > window.innerHeight + 1,
        );
        const scrollerOverflows = await page.evaluate(() => {
            const scroller =
                document.querySelector<HTMLElement>(".main-scroll");
            return !!scroller && scroller.scrollHeight > scroller.clientHeight;
        });

        expect(documentOverflows).toBe(false);
        expect(scrollerOverflows).toBe(true);
    });

    test("sidebar collapse survives a reload", async ({ page }) => {
        await page.goto("/pl/dashboard");
        await expect(page.locator(".app")).toHaveAttribute(
            "data-sidebar",
            "expanded",
        );

        await page.locator('[data-action="toggle-sidebar"]').click();
        await expect(page.locator(".app")).toHaveAttribute(
            "data-sidebar",
            "collapsed",
        );

        await page.reload();
        await expect(page.locator(".app")).toHaveAttribute(
            "data-sidebar",
            "collapsed",
        );

        await page.locator('[data-action="toggle-sidebar"]').click();
        await expect(page.locator(".app")).toHaveAttribute(
            "data-sidebar",
            "expanded",
        );
    });

    test("theme toggle survives a reload", async ({ page }) => {
        await page.goto("/pl/dashboard");
        await expect(page.locator("html")).toHaveAttribute(
            "data-theme",
            "light",
        );

        await page.locator('[data-action="set-theme"]').click();
        await expect(page.locator("html")).toHaveAttribute(
            "data-theme",
            "dark",
        );

        await page.reload();
        await expect(page.locator("html")).toHaveAttribute(
            "data-theme",
            "dark",
        );

        await page.locator('[data-action="set-theme"]').click();
        await expect(page.locator("html")).toHaveAttribute(
            "data-theme",
            "light",
        );
    });

    test("locale switch keeps the current page", async ({ page }) => {
        await page.goto("/pl/customers");
        await page.locator(".tb-btn", { hasText: "PL" }).click();

        await expect(page).toHaveURL(/\/en\/customers/);
        await expect(
            page.locator('.nav-item[data-route="customers"] .nav-label'),
        ).toHaveText("Customers");
    });

    // PIO-125: with English the default, the way back to Polish matters as much as the way out.
    test("locale switch leads back to Polish too", async ({ page }) => {
        await page.goto("/en/customers");
        await page.locator(".tb-btn", { hasText: "EN" }).click();

        await expect(page).toHaveURL(/\/pl\/customers/);
        await expect(
            page.locator('.nav-item[data-route="customers"] .nav-label'),
        ).toHaveText("Klienci");
    });

    test("breadcrumb names the current section", async ({ page }) => {
        await page.goto("/pl/feeds");

        await expect(page.locator(".crumbs .now")).toHaveText(
            "Feedy produktów",
        );
        await expect(page.locator(".crumbs")).toContainText("aureashop.pl");
    });
});
