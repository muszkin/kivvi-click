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
        // Was 3 until PIO-121. "Postaw u siebie" became step 01: the landing page now says
        // the software is self-hostable, so the path starts with getting your own instance
        // rather than with pasting a snippet into a product someone else runs for you.
        await expect(page.locator("#how .feat")).toHaveCount(4);
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
        // The header's primary button used to read "Załóż konto →" and lead to the login form,
        // on a page whose privacy policy says accounts are not publicly available.
        await expect(body).not.toContainText("Załóż konto");
    });

    test("the header's primary button offers the source, not an account", async ({
        page,
    }) => {
        await page.goto("/pl");

        const cta = page.locator(".landing-nav .landing-nav-repo");
        await expect(cta).toHaveText("Kod na GitHubie →");
        await expect(cta).toHaveAttribute(
            "href",
            "https://github.com/muszkin/kivvi-click",
        );
        await expect(cta).toHaveAttribute("target", "_blank");
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
        // PIO-117: the structure was never the problem — the words inside it were. The trust
        // points, cards, steps and tiles come from the API, the footer from the catalogue, and
        // all of them used to reach /en in Polish.
        await expect(page.locator(".hero .row > span")).toHaveText([
            "MIT licence",
            "Host it yourself",
            "2 KB script",
        ]);
        await expect(page.locator("#features .feat h3").first()).toHaveText(
            "Live event stream",
        );
        await expect(page.locator("#how .feat h3").first()).toHaveText(
            "Run it yourself",
        );
        await expect(page.locator(".hero-preview .kpi-label").first()).toHaveText(
            "Events / min",
        );
        const footer = page.locator(".landing-foot");
        await expect(footer).toContainText("Terms");
        await expect(footer).toContainText("Privacy");
        await expect(footer).toContainText("GDPR / DPA");
        await expect(footer).not.toContainText("Regulamin");
        await expect(footer).not.toContainText("Prywatność");
    });
});

/**
 * PIO-125 — the project is open source, and its first contact is in English.
 *
 * Every spec above visits /pl or /en explicitly, so none of them noticed which language "/" was.
 * These do.
 */
test.describe("English by default", () => {
    test("'/' is the English landing page, not a redirect to either prefix", async ({
        page,
    }) => {
        await page.goto("/");

        await expect(page).toHaveURL(/\/$/);
        await expect(page.locator("html")).toHaveAttribute("lang", "en");
        await expect(page.locator(".hero h1")).toContainText("See");
        await expect(page.locator("#features .feat h3").first()).toHaveText(
            "Live event stream",
        );
        await expect(page.locator("form.waitlist-form")).toHaveAttribute(
            "action",
            "/en/waitlist",
        );
    });

    test("the language switch goes to Polish and back, keeping the page", async ({
        page,
    }) => {
        await page.goto("/");

        await page.locator(".landing-nav-locale", { hasText: "Polski" }).click();
        await expect(page).toHaveURL(/\/pl$/);
        await expect(page.locator("html")).toHaveAttribute("lang", "pl");
        await expect(page.locator(".hero h1")).toContainText("Widzisz");
        await expect(page.locator("#how .feat h3").first()).toHaveText(
            "Postaw u siebie",
        );

        await page.locator(".landing-nav-locale", { hasText: "English" }).click();
        await expect(page).toHaveURL(/\/en$/);
        await expect(page.locator("html")).toHaveAttribute("lang", "en");
        await expect(page.locator(".hero h1")).toContainText("See");
    });

    test("/pl stays complete: header, cards, steps, trust points, tiles and footer all Polish", async ({
        page,
    }) => {
        await page.goto("/pl");

        await expect(page.locator("html")).toHaveAttribute("lang", "pl");
        await expect(page.locator(".hero .row > span")).toHaveText([
            "Licencja MIT",
            "Postawisz u siebie",
            "Skrypt 2 KB",
        ]);
        await expect(page.locator("#features .feat h3").first()).toHaveText(
            "Strumień zdarzeń na żywo",
        );
        await expect(page.locator(".hero-preview .kpi-label").first()).toHaveText(
            "Zdarzeń / min",
        );
        await expect(page.locator(".landing-nav")).toContainText("Logowanie");
        const footer = page.locator(".landing-foot");
        await expect(footer).toContainText("Regulamin");
        await expect(footer).toContainText("RODO / DPA");
        await expect(footer).toContainText("Zbudowane w Polsce");
    });
});

/**
 * PIO-117's acceptance test, widened by PIO-125 to every public page a visitor can reach in
 * English: the landing page (bare and prefixed), the privacy policy, the three pages a mail links
 * to, and the 404 a mistyped address gets.
 *
 * Two checks, because each misses what the other catches. A Polish letter is proof of a leak, but
 * plenty of Polish has none ("Licencja MIT", "Wklej snippet", "Publikuj"), so the words that did
 * leak — or sit one careless edit away from it — are named as well.
 */
test.describe("no Polish on an English public page", () => {
    const POLISH_LETTERS = /[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]/;
    // The controller's registered address is a place name, and stays one in every language.
    const PROPER_NOUNS = ["Niepołomice"];
    const POLISH_WORDS = [
        "Regulamin",
        "RODO",
        "Logowanie",
        "Licencja",
        "Postaw",
        "Wklej",
        "Wybierz",
        "Publikuj",
        "Zdarzeń",
        "Przychód",
        "Zbudowane",
        "Nie znaleziono",
    ];
    const UNKNOWN_TOKEN = "f".repeat(64);

    for (const { path, ready } of [
        { path: "/", ready: ".hero h1" },
        { path: "/en", ready: ".hero h1" },
        { path: "/en/privacy", ready: ".legal-page h1" },
        { path: "/en/waitlist/confirm/sent", ready: ".confirm-page h1" },
        { path: `/en/waitlist/confirm/${UNKNOWN_TOKEN}`, ready: ".confirm-page h1" },
        {
            path: `/en/waitlist/unsubscribe/${UNKNOWN_TOKEN}`,
            ready: ".confirm-page h1",
        },
        { path: "/en/no-such-page", ready: "h1" },
    ]) {
        test(`${path} renders no Polish text`, async ({ page }) => {
            await page.goto(path);
            await expect(page.locator(ready)).toBeVisible();

            const text = PROPER_NOUNS.reduce(
                (remaining, noun) => remaining.replaceAll(noun, ""),
                await page.locator("body").innerText(),
            );
            expect(text).not.toMatch(POLISH_LETTERS);
            for (const word of POLISH_WORDS) {
                expect(text, `"${word}" on ${path}`).not.toContain(word);
            }
        });
    }
});

/**
 * PIO-118 — nothing a public page loads comes from anywhere but this origin.
 *
 * index.html used to pull Geist, Geist Mono and Instrument Serif from Google's CDN, so every
 * visitor disclosed their IP address to Google simply by opening the page. The privacy policy
 * had to say so. The typefaces are served from this origin now, the policy says we transfer
 * nothing, and these tests are what make that statement checkable rather than aspirational.
 *
 * frontend/test/unit/fonts.spec.ts covers the same ground statically, against the source. This
 * covers what the static test cannot: what a real browser actually goes and fetches.
 */
test.describe("no third-party requests", () => {
    // "/" joined the list with PIO-125: it is the page most visitors now open first.
    for (const path of ["/", "/pl", "/en"]) {
        test(`loading ${path} issues no request to any host but our own`, async ({
            page,
            baseURL,
        }) => {
            const ourHost = new URL(baseURL as string).host;
            const offHost: string[] = [];

            page.on("request", (request) => {
                const url = new URL(request.url());
                // data: and blob: cost no network request; only real schemes can leak.
                if (url.protocol !== "http:" && url.protocol !== "https:") {
                    return;
                }
                if (url.host !== ourHost) {
                    offHost.push(`${request.resourceType()} ${request.url()}`);
                }
            });

            await page.goto(path);
            // Requests for fonts are issued during layout, after load — waiting only for
            // "load" would let exactly the requests this test exists for slip past it.
            await page.evaluate(async () => {
                await document.fonts.ready;
            });
            await page.waitForLoadState("networkidle");

            expect(offHost).toEqual([]);
        });
    }

    test("the typefaces come from this origin and are really used", async ({
        page,
        baseURL,
    }) => {
        const ourHost = new URL(baseURL as string).host;
        const fontRequests: string[] = [];

        page.on("request", (request) => {
            if (request.resourceType() === "font") {
                fontRequests.push(request.url());
            }
        });

        await page.goto("/pl");
        await page.evaluate(async () => {
            await document.fonts.ready;
        });

        // Not just "no off-host requests": a page that loaded no webfont at all would pass
        // that. The page has to be fetching our files and rendering in them.
        expect(fontRequests.length).toBeGreaterThan(0);
        for (const url of fontRequests) {
            expect(new URL(url).host).toBe(ourHost);
            expect(url).toMatch(/\.woff2(\?|$)/);
        }
        expect(
            await page.evaluate(() =>
                getComputedStyle(document.body).fontFamily.includes("Geist"),
            ),
        ).toBe(true);
    });
});

/**
 * PIO-118 — the risk the ticket names: an over-eager subset drops the Polish diacritics, and it
 * shows up in production rather than in CI, because nothing else in the suite happens to contain
 * `ąćęłńóśźż`.
 *
 * Google splits each family by unicode-range: `ó` comes from the `latin` file, the other eight
 * accented letters from `latin-ext`. So the browser, asked to render a string with all nine, must
 * match and successfully download TWO faces per family. One means the latin-ext subset is gone
 * and half of every Polish word is silently rendering in the fallback face.
 */
test.describe("Polish diacritics in all three typefaces", () => {
    const POLISH = "ąćęłńóśźż";

    for (const family of ["Geist", "Geist Mono", "Instrument Serif"]) {
        test(`${family} renders ${POLISH}`, async ({ page }) => {
            await page.goto("/pl");

            const coverage = await page.evaluate(
                async ([name, text]) => {
                    const spec = `400 16px "${name}"`;
                    // Per letter, not per string: document.fonts.load() returns the faces whose
                    // unicode-range matches the text, so a letter no @font-face covers comes back
                    // as an empty list — the browser would silently render it in the fallback.
                    // Asserting letter by letter names which one broke instead of "something".
                    const perLetter: Record<string, string[]> = {};
                    for (const letter of text) {
                        const matched = await document.fonts.load(spec, letter);
                        perLetter[letter] = matched.map((face) => face.status);
                    }
                    const whole = await document.fonts.load(spec, text);
                    return {
                        perLetter,
                        facesForWholeString: whole.length,
                        check: document.fonts.check(spec, text),
                    };
                },
                [family, POLISH] as const,
            );

            for (const letter of POLISH) {
                expect(
                    coverage.perLetter[letter],
                    `${family} has no loaded @font-face covering "${letter}"`,
                ).toEqual(["loaded"]);
            }
            // Two, because the nine letters straddle the latin/latin-ext split: `ó` comes from
            // one file and the other eight from the other. One face here means a subset was
            // dropped; the per-letter assertions above say which letters went with it.
            expect(coverage.facesForWholeString).toBe(2);
            expect(coverage.check).toBe(true);
        });
    }
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
