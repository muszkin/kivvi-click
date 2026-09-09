# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: navigation.spec.ts >> app shell >> sidebar entry "import" opens its page
- Location: specs/navigation.spec.ts:26:13

# Error details

```
Error: expect(locator).toContainText(expected) failed

Locator: locator('.page-title')
Expected substring: "Import klientów"
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toContainText" with timeout 5000ms
  - waiting for locator('.page-title')

```

```yaml
- complementary:
  - text: kivvi click
  - button "AS aureashop.pl Plan Pro · 3 strony"
  - navigation "Nawigacja główna":
    - text: Główne
    - link "Pulpit":
      - /url: /pl/dashboard
    - link "Strumień zdarzeń ·":
      - /url: /pl/events
    - link "Klienci":
      - /url: /pl/customers
    - text: Automatyzacja
    - link "Reguły":
      - /url: /pl/automations
    - link "Kampanie email":
      - /url: /pl/campaigns
    - link "Popupy i widgety":
      - /url: /pl/popups
    - text: Dane
    - link "Feedy produktów":
      - /url: /pl/feeds
    - link "Import klientów":
      - /url: /pl/import
    - text: Konfiguracja
    - link "Ustawienia":
      - /url: /pl/settings
  - text: MK Maciej Kowalczyk maciej@aureashop.pl
- main:
  - button "Zwiń panel boczny"
  - navigation "Ścieżka": aureashop.pl / Import klientów
  - button "Szukaj": Szukaj zdarzeń, klientów, reguł… ⌘K
  - link "PL":
    - /url: /en/import
  - button "Motyw"
  - button "Powiadomienia"
```

# Test source

```ts
  1   | import { expect, test } from "@playwright/test";
  2   | 
  3   | /**
  4   |  * The shell: every navigation entry leads somewhere, and the chrome that surrounds
  5   |  * a page keeps working — collapse, theme, locale, breadcrumb.
  6   |  */
  7   | 
  8   | const SECTIONS = [
  9   |     { route: "dashboard", url: "/pl/dashboard", title: "Co dzieje się teraz" },
  10  |     { route: "events", url: "/pl/events", title: "Strumień zdarzeń" },
  11  |     { route: "customers", url: "/pl/customers", title: "Klienci" },
  12  |     {
  13  |         route: "automations",
  14  |         url: "/pl/automations",
  15  |         title: "Reguły i automatyzacje",
  16  |     },
  17  |     { route: "campaigns", url: "/pl/campaigns", title: "Kampanie email" },
  18  |     { route: "popups", url: "/pl/popups", title: "Popupy i widgety" },
  19  |     { route: "feeds", url: "/pl/feeds", title: "Feedy produktów" },
  20  |     { route: "import", url: "/pl/import", title: "Import klientów" },
  21  |     { route: "settings", url: "/pl/settings", title: "Ustawienia" },
  22  | ];
  23  | 
  24  | test.describe("app shell", () => {
  25  |     for (const section of SECTIONS) {
  26  |         test(`sidebar entry "${section.route}" opens its page`, async ({
  27  |             page,
  28  |         }) => {
  29  |             await page.goto("/pl/dashboard");
  30  |             await page
  31  |                 .locator(`.nav-item[data-route="${section.route}"]`)
  32  |                 .click();
  33  | 
  34  |             await expect(page).toHaveURL(
  35  |                 new RegExp(`${section.url.replace(/\//g, "\\/")}`),
  36  |             );
> 37  |             await expect(page.locator(".page-title")).toContainText(
      |                                                       ^ Error: expect(locator).toContainText(expected) failed
  38  |                 section.title,
  39  |             );
  40  |             await expect(
  41  |                 page.locator(`.nav-item[data-route="${section.route}"]`),
  42  |             ).toHaveAttribute("aria-current", "page");
  43  |         });
  44  |     }
  45  | 
  46  |     test("only .main-scroll scrolls, never the window", async ({ page }) => {
  47  |         await page.goto("/pl/events");
  48  | 
  49  |         const documentOverflows = await page.evaluate(
  50  |             () =>
  51  |                 document.documentElement.scrollHeight > window.innerHeight + 1,
  52  |         );
  53  |         const scrollerOverflows = await page.evaluate(() => {
  54  |             const scroller =
  55  |                 document.querySelector<HTMLElement>(".main-scroll");
  56  |             return !!scroller && scroller.scrollHeight > scroller.clientHeight;
  57  |         });
  58  | 
  59  |         expect(documentOverflows).toBe(false);
  60  |         expect(scrollerOverflows).toBe(true);
  61  |     });
  62  | 
  63  |     test("sidebar collapse survives a reload", async ({ page }) => {
  64  |         await page.goto("/pl/dashboard");
  65  |         await expect(page.locator(".app")).toHaveAttribute(
  66  |             "data-sidebar",
  67  |             "expanded",
  68  |         );
  69  | 
  70  |         await page.locator('[data-action="toggle-sidebar"]').click();
  71  |         await expect(page.locator(".app")).toHaveAttribute(
  72  |             "data-sidebar",
  73  |             "collapsed",
  74  |         );
  75  | 
  76  |         await page.reload();
  77  |         await expect(page.locator(".app")).toHaveAttribute(
  78  |             "data-sidebar",
  79  |             "collapsed",
  80  |         );
  81  | 
  82  |         await page.locator('[data-action="toggle-sidebar"]').click();
  83  |         await expect(page.locator(".app")).toHaveAttribute(
  84  |             "data-sidebar",
  85  |             "expanded",
  86  |         );
  87  |     });
  88  | 
  89  |     test("theme toggle survives a reload", async ({ page }) => {
  90  |         await page.goto("/pl/dashboard");
  91  |         await expect(page.locator("html")).toHaveAttribute(
  92  |             "data-theme",
  93  |             "light",
  94  |         );
  95  | 
  96  |         await page.locator('[data-action="set-theme"]').click();
  97  |         await expect(page.locator("html")).toHaveAttribute(
  98  |             "data-theme",
  99  |             "dark",
  100 |         );
  101 | 
  102 |         await page.reload();
  103 |         await expect(page.locator("html")).toHaveAttribute(
  104 |             "data-theme",
  105 |             "dark",
  106 |         );
  107 | 
  108 |         await page.locator('[data-action="set-theme"]').click();
  109 |         await expect(page.locator("html")).toHaveAttribute(
  110 |             "data-theme",
  111 |             "light",
  112 |         );
  113 |     });
  114 | 
  115 |     test("locale switch keeps the current page", async ({ page }) => {
  116 |         await page.goto("/pl/customers");
  117 |         await page.locator(".tb-btn", { hasText: "PL" }).click();
  118 | 
  119 |         await expect(page).toHaveURL(/\/en\/customers/);
  120 |         await expect(
  121 |             page.locator('.nav-item[data-route="customers"] .nav-label'),
  122 |         ).toHaveText("Customers");
  123 |     });
  124 | 
  125 |     test("breadcrumb names the current section", async ({ page }) => {
  126 |         await page.goto("/pl/feeds");
  127 | 
  128 |         await expect(page.locator(".crumbs .now")).toHaveText(
  129 |             "Feedy produktów",
  130 |         );
  131 |         await expect(page.locator(".crumbs")).toContainText("aureashop.pl");
  132 |     });
  133 | });
  134 | 
```