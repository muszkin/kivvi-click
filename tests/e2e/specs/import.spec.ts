import { expect, test } from "@playwright/test";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

/**
 * The import wizard: four steps, each of them a URL, plus a real file upload.
 */
test.describe("customer import", () => {
    test("step 1 offers the dropzone, the template and the API", async ({
        page,
    }) => {
        await page.goto("/pl/import/1");

        await expect(page.locator(".stepper .step")).toHaveCount(4);
        await expect(
            page.locator('.step[data-state="cur"] .step__title'),
        ).toHaveText("Plik");
        await expect(page.locator(".dropzone")).toBeVisible();
        await expect(page.locator(".wiz-layout .card").last()).toContainText(
            "Ostatnie importy",
        );
    });

    test("step 2 maps every column with a confidence read-out", async ({
        page,
    }) => {
        await page.goto("/pl/import/2");

        await expect(
            page.locator(".map-table .map-row:not(.map-row--head)"),
        ).toHaveCount(11);
        await expect(page.locator(".auto-detect")).toContainText(
            "rozpoznał 10 z 11 kolumn",
        );
        await expect(page.locator('.map-row[data-skipped="true"]')).toHaveCount(
            1,
        );
        await expect(
            page.locator(".map-row").nth(1).locator("select"),
        ).toHaveValue("email");
    });

    test("step 3 asks for a dedup strategy and GDPR consent", async ({
        page,
    }) => {
        await page.goto("/pl/import/3");

        await expect(page.locator('input[name="dedup"]')).toHaveCount(4);
        await expect(page.locator('input[name="dedup"]').first()).toBeChecked();
        await expect(page.locator(".cond-rule")).toHaveCount(2);
        await expect(page.locator(".rules-grid")).toContainText(
            "Zgody i prywatność (RODO)",
        );
    });

    test("step 4 summarises the run and flags invalid rows", async ({
        page,
    }) => {
        await page.goto("/pl/import/4");

        await expect(page.locator(".kpi-grid .kpi")).toHaveCount(4);
        await expect(page.locator(".table tbody tr")).toHaveCount(6);
        await expect(page.locator(".table tbody tr .chip.bad")).toHaveCount(1);
        await expect(page.locator('[data-action="run-import"]')).toContainText(
            "8 420",
        );
    });

    test("the stepper walks forward and back", async ({ page }) => {
        await page.goto("/pl/import/1");

        await page.locator('.step[data-payload="3"]').click();
        await expect(page).toHaveURL(/\/pl\/import\/3/);
        await expect(page.locator('.step[data-state="done"]')).toHaveCount(2);

        await page.locator("a.btn", { hasText: "← Wstecz" }).click();
        await expect(page).toHaveURL(/\/pl\/import\/2/);
    });

    test("uploading a file advances to the mapping step", async ({ page }) => {
        const directory = mkdtempSync(join(tmpdir(), "kivvi-import-"));
        const file = join(directory, "klienci-e2e.csv");
        writeFileSync(file, "email;imie\nhania.k@aurea.pl;Hania\n", "utf8");

        await page.goto("/pl/import/1");
        await page.locator('.dropzone input[type="file"]').setInputFiles(file);

        await page.waitForURL(/\/pl\/import\/2/);
        await expect(page.locator(".file-pill")).toContainText(
            "klienci-e2e.csv",
        );
    });
});
