import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end suite for the panel.
 *
 * Runs headless against the Docker stack (FrankenPHP + Caddy) over its self-signed
 * certificate, driving the system Chrome so no browser download is needed on hosts
 * that already ship one.
 */
export default defineConfig({
    testDir: "./specs",
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 1 : 0,
    reporter: process.env.CI ? [["github"], ["list"]] : [["list"]],
    use: {
        baseURL: process.env.E2E_BASE_URL ?? "https://localhost:8543",
        ignoreHTTPSErrors: true,
        trace: "retain-on-failure",
        screenshot: "only-on-failure",
    },
    projects: [
        {
            name: "chrome",
            use: {
                ...devices["Desktop Chrome"],
                channel: "chrome",
                viewport: { width: 1440, height: 900 },
            },
        },
    ],
});
