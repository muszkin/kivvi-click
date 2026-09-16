import { expect, test } from "@playwright/test";

/**
 * The public surface: landing page and the way into the panel.
 */
test.describe("landing", () => {
    test("hero, preview frame, features, steps and the open-source section", async ({
        page,
    }) => {
        await page.goto("/pl");

        await expect(page.locator(".hero h1")).toContainText("Widzisz");
        // Was 2 until PIO-70. The waitlist form replaced the primary CTA
        // ("Załóż darmowe konto") in the hero, so "Zobacz panel demo" is the
        // only link left in .hero-cta. Lowered deliberately rather than
        // loosened: the form that took its place is asserted on the next line,
        // and waitlist.spec.ts covers what it does.
        await expect(page.locator(".hero-cta a")).toHaveCount(1);
        await expect(page.locator("form.waitlist-form")).toBeVisible();
        await expect(page.locator(".hero-preview .kpi")).toHaveCount(4);
        // The marketing preview renders the traffic shape server-side, not as the live canvas.
    await expect(page.locator(".hero-preview svg path")).not.toHaveCount(0);
        await expect(page.locator("#features .feat")).toHaveCount(6);
        await expect(page.locator("#how .feat")).toHaveCount(3);
        // PIO-121 removed the price list: the software is MIT-licensed and self-hostable, so
        // there is nothing to price. The section in its place explains that instead.
        await expect(page.locator(".price-card")).toHaveCount(0);
        await expect(page.locator("#pricing")).toHaveCount(0);
        await expect(page.locator("#open-source")).toBeVisible();
        await expect(page.locator("#open-source")).toContainText("licencji MIT");
    });

    test("no page promises a launch, a trial or a price", async ({ page }) => {
        await page.goto("/pl");

        const body = page.locator("body");
        await expect(body).not.toContainText("14 dni Pro");
        await expect(body).not.toContainText("trial");
        await expect(body).not.toContainText("149");
        await expect(body).not.toContainText("powiadomienie o starcie");
    });

    test("the footer names the current stack and links the repository", async ({
        page,
    }) => {
        await page.goto("/pl");

        const footer = page.locator(".landing-foot");
        await expect(footer).toContainText("Javie i Vue 3");
        await expect(footer).not.toContainText("Symfony");
        await expect(footer).not.toContainText("PHP");
        await expect(footer.locator(".landing-foot-repo")).toHaveAttribute(
            "href",
            "https://github.com/muszkin/kivvi-click",
        );
    });

    test("the nav open-source entry scrolls to a section that exists", async ({
        page,
    }) => {
        await page.goto("/pl");

        const entry = page.locator('.nav-links a[href="#open-source"]');
        await expect(entry).toBeVisible();
        await entry.click();
        await expect(page.locator("#open-source")).toBeInViewport();
    });

    test("the demo button lands in the panel", async ({ page }) => {
        await page.goto("/pl");
        await page.locator(".hero-cta a", { hasText: "demo" }).click();

        await expect(page).toHaveURL(/\/pl\/dashboard/);
        await expect(page.locator(".page-title")).toContainText(
            "Co dzieje się teraz",
        );
    });

    test("English landing keeps the same structure", async ({ page }) => {
        await page.goto("/en");

        await expect(page.locator(".hero h1")).toContainText("See");
        await expect(page.locator("#features .feat")).toHaveCount(6);
    });
});

test.describe("login", () => {
    test("signs in and shows the identity in the sidebar", async ({ page }) => {
        await page.goto("/pl/login");

        await expect(page.locator(".auth-side h1")).toContainText("Widzisz");
        await page.locator("#f-_username").fill("anna@aureashop.pl");
        await page.locator("#f-_password").fill("hasło-testowe");
        await page.locator('button[type="submit"]').click();

        await expect(page).toHaveURL(/\/pl\/dashboard/);
        await expect(page.locator(".sb-foot")).toContainText(
            "anna@aureashop.pl",
        );
    });

    test("the browser blocks a malformed address before it is sent", async ({
        page,
    }) => {
        await page.goto("/pl/login");
        await page.locator("#f-_username").fill("nie-adres");
        await page.locator("#f-_password").fill("x");
        await page.locator('button[type="submit"]').click();

        // type="email" refuses to submit; the server-side check behind it is covered by
        // App\Tests\Controller\SecurityControllerTest.
        await expect(page).toHaveURL(/\/pl\/login/);
        const valid = await page
            .locator("#f-_username")
            .evaluate((el: HTMLInputElement) => el.checkValidity());
        expect(valid).toBe(false);
    });
});
