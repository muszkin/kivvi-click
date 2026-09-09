import { fileURLToPath, URL } from "node:url";
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vitest/config";

// https://vite.dev/config/
export default defineConfig({
    // Default whitespace mode ("condense") already matches the old stack's Twig output: the
    // oracle's text/a11y comparisons are computed from innerText()/ariaSnapshot(), both of
    // which collapse insignificant inter-tag whitespace themselves, the same effect Twig's
    // {%- -%} trims achieved server-side. No component in this slice needs "preserve".
    plugins: [vue()],
    resolve: {
        alias: {
            "@": fileURLToPath(new URL("./src", import.meta.url)),
        },
    },
    build: {
        outDir: "dist",
        assetsDir: "assets",
    },
    test: {
        environment: "jsdom",
        globals: true,
        setupFiles: ["./test/setup.ts"],
        // Repair-2 (R2-C): must stay false — an unhandled error/rejection during a test is a
        // real bug, not something to swallow. See test/setup.ts for the rest of the
        // fail-on-warning test policy (console.warn/error, Vue runtime warnings).
        dangerouslyIgnoreUnhandledErrors: false,
    },
});
