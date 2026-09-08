# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: public.spec.ts >> landing >> English landing keeps the same structure
- Location: specs/public.spec.ts:33:9

# Error details

```
Error: expect(locator).toContainText(expected) failed

Locator: locator('.hero h1')
Expected substring: "See"
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toContainText" with timeout 5000ms
  - waiting for locator('.hero h1')

```

```yaml
- banner:
  - text: kivvi click
  - navigation:
    - link "Features":
      - /url: "#features"
    - link "How it works":
      - /url: "#how"
    - link "Pricing":
      - /url: "#pricing"
    - link "Documentation":
      - /url: "#docs"
    - link "Blog":
      - /url: "#blog"
  - link "Logowanie":
    - /url: /en/login
  - link "Załóż konto →":
    - /url: /en/login
- contentinfo:
  - text: kivvi click · © 2026 ·
  - link "Regulamin":
    - /url: "#"
  - link "Prywatność":
    - /url: "#"
  - link "RODO / DPA":
    - /url: "#"
  - link "Status":
    - /url: "#"
  - text: Zbudowane w Polsce z Symfony, PHP i sporą ilością herbaty
```

# Test source

```ts
  1  | import { expect, test } from "@playwright/test";
  2  | 
  3  | /**
  4  |  * The public surface: landing page and the way into the panel.
  5  |  */
  6  | test.describe("landing", () => {
  7  |     test("hero, preview frame, features, steps and pricing", async ({
  8  |         page,
  9  |     }) => {
  10 |         await page.goto("/pl");
  11 | 
  12 |         await expect(page.locator(".hero h1")).toContainText("Widzisz");
  13 |         await expect(page.locator(".hero-cta a")).toHaveCount(2);
  14 |         await expect(page.locator(".hero-preview .kpi")).toHaveCount(4);
  15 |         // The marketing preview renders the traffic shape server-side, not as the live canvas.
  16 |     await expect(page.locator(".hero-preview svg path")).not.toHaveCount(0);
  17 |         await expect(page.locator("#features .feat")).toHaveCount(6);
  18 |         await expect(page.locator("#how .feat")).toHaveCount(3);
  19 |         await expect(page.locator(".price-card")).toHaveCount(2);
  20 |         await expect(page.locator(".price-card.pro .tier")).toHaveText("Pro");
  21 |     });
  22 | 
  23 |     test("the demo button lands in the panel", async ({ page }) => {
  24 |         await page.goto("/pl");
  25 |         await page.locator(".hero-cta a", { hasText: "demo" }).click();
  26 | 
  27 |         await expect(page).toHaveURL(/\/pl\/dashboard/);
  28 |         await expect(page.locator(".page-title")).toContainText(
  29 |             "Co dzieje się teraz",
  30 |         );
  31 |     });
  32 | 
  33 |     test("English landing keeps the same structure", async ({ page }) => {
  34 |         await page.goto("/en");
  35 | 
> 36 |         await expect(page.locator(".hero h1")).toContainText("See");
     |                                                ^ Error: expect(locator).toContainText(expected) failed
  37 |         await expect(page.locator("#features .feat")).toHaveCount(6);
  38 |     });
  39 | });
  40 | 
  41 | test.describe("login", () => {
  42 |     test("signs in and shows the identity in the sidebar", async ({ page }) => {
  43 |         await page.goto("/pl/login");
  44 | 
  45 |         await expect(page.locator(".auth-side h1")).toContainText("Widzisz");
  46 |         await page.locator("#f-_username").fill("anna@aureashop.pl");
  47 |         await page.locator("#f-_password").fill("hasło-testowe");
  48 |         await page.locator('button[type="submit"]').click();
  49 | 
  50 |         await expect(page).toHaveURL(/\/pl\/dashboard/);
  51 |         await expect(page.locator(".sb-foot")).toContainText(
  52 |             "anna@aureashop.pl",
  53 |         );
  54 |     });
  55 | 
  56 |     test("the browser blocks a malformed address before it is sent", async ({
  57 |         page,
  58 |     }) => {
  59 |         await page.goto("/pl/login");
  60 |         await page.locator("#f-_username").fill("nie-adres");
  61 |         await page.locator("#f-_password").fill("x");
  62 |         await page.locator('button[type="submit"]').click();
  63 | 
  64 |         // type="email" refuses to submit; the server-side check behind it is covered by
  65 |         // App\Tests\Controller\SecurityControllerTest.
  66 |         await expect(page).toHaveURL(/\/pl\/login/);
  67 |         const valid = await page
  68 |             .locator("#f-_username")
  69 |             .evaluate((el: HTMLInputElement) => el.checkValidity());
  70 |         expect(valid).toBe(false);
  71 |     });
  72 | });
  73 | 
```