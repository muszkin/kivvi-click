import { expect, test } from "@playwright/test";

/**
 * Settings: seven tabs, each addressable, each rendering its own surface. PIO-123 retired the
 * eighth ("Plan i płatności") — the software is open source, so the panel describes no
 * subscription; `/pl/settings/billing` now 404s like any other unknown tab.
 */
const TABS = [
    { id: "account", label: "Konto", marker: "Dane konta" },
    { id: "sites", label: "Śledzone strony", marker: "Instalacja skryptu" },
    { id: "team", label: "Zespół", marker: "Członkowie zespołu" },
    {
        id: "providers",
        label: "Dostawcy email",
        marker: "Uwierzytelnianie domeny",
    },
    { id: "api", label: "Webhooks i API", marker: "Klucze API" },
    {
        id: "notifications",
        label: "Powiadomienia",
        marker: "Kanały powiadomień",
    },
    { id: "gdpr", label: "RODO / DPA", marker: "Retencja danych" },
];

test.describe("settings", () => {
    for (const tab of TABS) {
        test(`tab "${tab.id}" has its own URL and content`, async ({
            page,
        }) => {
            await page.goto("/pl/settings/account");
            await page
                .locator(".settings-nav a", { hasText: tab.label })
                .click();

            // The default tab renders at /pl/settings; the others carry their id.
            await expect(page).toHaveURL(
                new RegExp(`/pl/settings(/${tab.id})?$`),
            );
            await expect(
                page.locator(".settings-nav a", { hasText: tab.label }),
            ).toHaveAttribute("aria-current", "true");
            await expect(page.locator(".settings-grid")).toContainText(
                tab.marker,
            );
        });
    }

    test("the settings nav offers seven tabs and none of them is billing", async ({
        page,
    }) => {
        await page.goto("/pl/settings/account");

        await expect(page.locator(".settings-nav a")).toHaveCount(7);
        await expect(page.locator(".settings-nav")).not.toContainText(
            "Plan i płatności",
        );
        await expect(page.locator(".settings-grid")).not.toContainText(
            "149,00 zł",
        );
    });

    test("the retired billing tab is a 404 document, not an empty panel page", async ({
        page,
    }) => {
        const response = await page.goto("/pl/settings/billing");

        expect(response?.status()).toBe(404);
    });

    test("the workspace switcher states the site count, not a plan", async ({
        page,
    }) => {
        await page.goto("/pl/settings/account");

        await expect(page.locator(".ws-card")).toContainText("3 strony");
        await expect(page.locator(".ws-card")).not.toContainText("Plan Pro");
    });

    test("the tracker snippet is highlighted server-side", async ({ page }) => {
        await page.goto("/pl/settings/sites");
        const snippet = page.locator(".code-block");

        await expect(snippet).toContainText("cdn.kivvi-click.io/k.js");
        await expect(snippet.locator("span.s").first()).toBeVisible();
        await expect(snippet.locator("span.c").first()).toBeVisible();
    });

    test("deliverability records show their verification state", async ({
        page,
    }) => {
        await page.goto("/pl/settings/providers");

        await expect(page.locator(".dns-row")).toHaveCount(4);
        await expect(page.locator(".dns-row .chip.good")).toHaveCount(3);
        await expect(page.locator(".dns-row .chip.warn")).toHaveCount(1);
    });

    test("a failing webhook is reported in place", async ({ page }) => {
        await page.goto("/pl/settings/api");

        await expect(page.locator(".hook-row")).toHaveCount(3);
        await expect(page.locator(".hook-row .chip.bad")).toHaveText("410");
        await expect(page.locator(".callout")).toContainText("410 od 3 godzin");
    });

    test("the notification matrix covers every channel", async ({ page }) => {
        await page.goto("/pl/settings/notifications");

        await expect(page.locator(".table tbody tr")).toHaveCount(8);
        await expect(
            page.locator('.table tbody input[type="checkbox"]'),
        ).toHaveCount(24);
        await expect(
            page.locator('.table tbody input[type="checkbox"]:checked'),
        ).toHaveCount(15);
    });
});
