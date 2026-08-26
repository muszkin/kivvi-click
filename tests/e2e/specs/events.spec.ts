import { expect, test } from "@playwright/test";

/**
 * The event log: 30 server-rendered rows, filter rails, and a live row arriving
 * over Mercure as ready-made HTML.
 */
test.describe("event stream", () => {
    test("renders thirty rows with type, detail and site", async ({ page }) => {
        await page.goto("/pl/events");

        const rows = page.locator("#event-stream .event-row");
        await expect(rows).toHaveCount(30);

        const first = rows.first();
        await expect(first.locator(".event-row__type")).not.toBeEmpty();
        await expect(first.locator(".event-row__site")).not.toBeEmpty();
    });

    test("offers every event type and every tracked site as a filter", async ({
        page,
    }) => {
        await page.goto("/pl/events");

        await expect(
            page.locator(".events-toolbar").first().locator(".filter-chip"),
        ).toHaveCount(9);
        await expect(
            page.locator(".events-toolbar").nth(1).locator(".filter-chip"),
        ).toHaveCount(4);
        await expect(
            page.locator('.seg [data-action="set-range"]'),
        ).toHaveCount(4);
    });

    test("a collected event reaches the stream as a rendered row", async ({
        page,
        request,
    }) => {
        await page.goto("/pl/events");
        const stream = page.locator("#event-stream");
        await expect(stream.locator(".event-row")).toHaveCount(30);

        // Other specs publish to the same topic, so recognise this event by its own detail.
        const marker = `412,00 PLN · zamówienie ${Date.now()}`;
        const response = await request.post("/collect", {
            data: {
                idempotency_id: `e2e-${Date.now()}`,
                type: "purchase",
                detail: marker,
                customer_id: "c_1001",
                customer_name: "Hania Kowalska",
                site: "aureashop.pl",
            },
        });
        expect(response.status()).toBe(202);

        const arrived = stream.locator(".event-row", { hasText: marker });
        await expect(arrived).toHaveCount(1, { timeout: 10_000 });
        await expect(arrived).toHaveClass(/new/);
        await expect(arrived.locator(".event-row__type")).toHaveText("Zakup");
        await expect(arrived.locator(".event-row__customer")).toContainText(
            "Hania Kowalska",
        );
    });

    test("a replayed event is not shown twice", async ({ page, request }) => {
        await page.goto("/pl/events");
        const stream = page.locator("#event-stream");

        // The topic is shared, so identify this test's own event by its detail line
        // rather than by counting every row that flashed in.
        const marker = `iga-${Date.now()}@example.com`;
        const payload = {
            idempotency_id: `e2e-dedup-${Date.now()}`,
            type: "signup",
            detail: marker,
            customer_id: "c_1002",
            customer_name: "Iga Pszczółkowska",
            site: "aureashop.pl",
        };
        const mine = stream.locator(".event-row", { hasText: marker });

        expect(
            (await request.post("/collect", { data: payload })).status(),
        ).toBe(202);
        await expect(mine).toHaveCount(1, { timeout: 10_000 });

        expect(
            (await request.post("/collect", { data: payload })).status(),
        ).toBe(200);
        await page.waitForTimeout(1000);
        await expect(mine).toHaveCount(1);
    });
});
