import { expect, test } from "@playwright/test";

/**
 * The two composers: e-mail template and on-site widget.
 */
test.describe("email editor", () => {
    test("shows library, envelope, document and inspector", async ({
        page,
    }) => {
        await page.goto("/pl/campaigns");
        await page.locator(".table tbody tr").first().click();

        await expect(page).toHaveURL(/\/pl\/emails\/k1/);
        await expect(page.locator(".ee-block-lib button")).toHaveCount(10);
        await expect(page.locator(".ee-canvas-wrap").first()).toContainText(
            "Hania, Twój koszyk czeka",
        );
        await expect(page.locator(".ee-doc .doc-hero")).toBeVisible();
        await expect(page.locator(".ee-doc .coupon-code")).toHaveText(
            "WROCMY-A8F2",
        );
        await expect(page.locator(".ee-doc .doc-prod .item")).toHaveCount(4);
        await expect(page.locator(".ee-doc .doc-foot")).toContainText(
            "Wypisz się",
        );
        await expect(page.locator(".email-right")).toContainText(
            "Warunki widoczności",
        );
        await expect(page.locator('input[name="hero_title"]')).toHaveValue(
            "Hania, Twój koszyk czeka.",
        );
    });

    test("the document keeps literal colours so mail clients never render it dark", async ({
        page,
    }) => {
        await page.goto("/pl/emails/k1");
        await page.locator('[data-action="set-theme"]').click();
        await expect(page.locator("html")).toHaveAttribute(
            "data-theme",
            "dark",
        );

        const background = await page
            .locator(".ee-doc")
            .evaluate((el) => getComputedStyle(el).backgroundColor);

        expect(background).toBe("rgb(255, 255, 255)");
    });
});

test.describe("popup editor", () => {
    test("previews the widget on a mock storefront", async ({ page }) => {
        await page.goto("/pl/popups/p1");

        await expect(page.locator(".pw-stage")).toHaveAttribute(
            "data-type",
            "modal",
        );
        await expect(page.locator(".pw-stage")).toHaveAttribute(
            "data-device",
            "desktop",
        );
        await expect(page.locator(".pw.pw--modal .pw-title")).toHaveText(
            "Zostań na 10% taniej",
        );
        await expect(page.locator(".shop-mock")).toBeVisible();
        await expect(page.locator(".pw-type")).toHaveCount(5);
    });

    test("switching widget type reshapes the preview", async ({ page }) => {
        await page.goto("/pl/popups/p1");
        await page.locator(".pw-type", { hasText: "Pasek" }).click();

        await expect(page).toHaveURL(/type=banner/);
        await expect(page.locator(".pw.pw--banner")).toBeVisible();
        await expect(page.locator(".pw-banner-row .pw-title")).toContainText(
            "Od 199 zł",
        );
    });

    test("switching device changes the viewport readout", async ({ page }) => {
        await page.goto("/pl/popups/p1");
        await expect(page.locator(".chip.mono").nth(1)).toHaveText(
            "1440 × 900",
        );

        await page
            .locator('.seg [data-action="navigate"]', { hasText: "Mobile" })
            .click();

        await expect(page).toHaveURL(/device=mobile/);
        await expect(page.locator(".pw-stage")).toHaveAttribute(
            "data-device",
            "mobile",
        );
        await expect(page.locator(".chip.mono").nth(1)).toHaveText("390 × 844");
    });

    test("the inspector carries triggers, audience and the position grid", async ({
        page,
    }) => {
        await page.goto("/pl/popups/p1");
        const inspector = page.locator(".email-right");

        await expect(inspector.locator(".trig-row")).toHaveCount(3);
        await expect(inspector.locator(".pos-grid .pos-cell")).toHaveCount(9);
        await expect(
            inspector.locator('.pos-cell[aria-checked="true"]'),
        ).toHaveCount(1);
        await expect(inspector.locator(".checkbox-row")).toHaveCount(4);
    });
});
