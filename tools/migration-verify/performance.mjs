#!/usr/bin/env node
/**
 * performance.mjs — DEV-8 absolute performance budgets: initial JS bytes (gzip) of the
 * built SPA entry, LCP/TTI of /pl/login, and /collect p95 over 50 sequential POSTs with fresh
 * idempotency ids (wave-2 event-stream journey).
 *
 * Usage (repository root):
 *   node tools/migration-verify/performance.mjs --base https://localhost:<port>
 */
import { execSync } from "node:child_process";
import { createRequire } from "node:module";
import { existsSync, readFileSync, readdirSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { gzipSync } from "node:zlib";

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, "../..");
const require = createRequire(join(root, "tests/e2e/package.json"));
const { chromium } = require("playwright");

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i++) {
    if (argv[i] === "--base") args.base = argv[++i];
  }
  if (!args.base) {
    console.error("usage: performance.mjs --base <url>");
    process.exit(2);
  }
  return args;
}

const args = parseArgs(process.argv.slice(2));
const BASE = args.base.replace(/\/$/, "");
const budget = JSON.parse(readFileSync(join(here, "budget.json"), "utf8"));

function initialJsGzipBytes() {
  const distDir = join(root, "frontend/dist/assets");
  if (!existsSync(distDir)) {
    throw new Error(`frontend/dist/assets not found — run "npm run build" in frontend/ first (${distDir})`);
  }
  const jsFiles = readdirSync(distDir).filter((f) => f.endsWith(".js"));
  return jsFiles.reduce((total, f) => total + gzipSync(readFileSync(join(distDir, f))).length, 0);
}

async function measureLoginPage() {
  const browser = await chromium.launch({ channel: "chrome", headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, ignoreHTTPSErrors: true });
  const page = await context.newPage();
  // The LCP entry is only buffered on the PerformanceObserver that was listening while it
  // fired — reading getEntriesByType() after the fact is unreliable in headless Chrome, so
  // the observer has to be registered before navigation via an init script.
  await page.addInitScript(() => {
    window.__lcp = null;
    new PerformanceObserver((list) => {
      const entries = list.getEntries();
      window.__lcp = entries[entries.length - 1].startTime;
    }).observe({ type: "largest-contentful-paint", buffered: true });
  });
  await page.goto(`${BASE}/pl/login`, { waitUntil: "load" });
  await page.waitForTimeout(500);
  const metrics = await page.evaluate(() => {
    const nav = performance.getEntriesByType("navigation")[0];
    // TTI proxy: domInteractive, the closest metric available without a long-tasks polyfill.
    const tti = nav ? nav.domInteractive : null;
    return { lcp: window.__lcp, tti };
  });
  await browser.close();
  return metrics;
}

// wave-2 (event-stream journey): /collect exists now, so this always measures the real
// endpoint — no more existence probe. Each of the 50 requests carries a fresh idempotency id
// and a fully valid payload, so every one takes the accepted (202) path end to end: dedup
// insert, JWT-signed Mercure publish included — not the cheaper 400-rejection path a malformed
// body would exercise instead.
async function collectP95() {
  // The dev/verification stack's HTTPS is self-signed (see compose.next.yaml's mercure
  // service); Playwright's measureLoginPage() above already opts out via
  // `ignoreHTTPSErrors: true`, but plain Node fetch() has no equivalent per-call option
  // without an extra HTTP-client dependency, so this loop opts the whole process out —
  // scoped here (not at module load) since only this function ever talks to an HTTPS
  // endpoint outside a Playwright-controlled browser context.
  process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";
  const runId = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  const durations = [];
  for (let i = 0; i < 50; i++) {
    const payload = {
      idempotency_id: `perf-${runId}-${i}`,
      type: "purchase",
      detail: "412,00 PLN · zamówienie perf",
      customer_id: "c_1001",
      customer_name: "Hania Kowalska",
      site: "aureashop.pl",
    };
    const start = performance.now();
    await fetch(`${BASE}/collect`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    durations.push(performance.now() - start);
  }
  durations.sort((a, b) => a - b);
  const p95 = durations[Math.floor(durations.length * 0.95)];
  return { skipped: false, p95Millis: p95 };
}

(async () => {
  const jsBytes = initialJsGzipBytes();
  const { lcp, tti } = await measureLoginPage();
  const collect = await collectP95();

  const results = {
    initialJsGzipBytes: { value: jsBytes, budget: budget.initialJsGzipBytes, pass: jsBytes <= budget.initialJsGzipBytes },
    lcpMillis: { value: lcp, budget: budget.lcpMillis, pass: lcp !== null && lcp <= budget.lcpMillis },
    ttiMillis: { value: tti, budget: budget.ttiMillis, pass: tti !== null && tti <= budget.ttiMillis },
    collectP95Millis: collect.skipped
      ? { skipped: true, note: collect.note }
      : { value: collect.p95Millis, budget: budget.collectP95Millis, pass: collect.p95Millis <= budget.collectP95Millis },
  };

  console.log(JSON.stringify(results, null, 2));

  const failed = Object.entries(results).filter(([, r]) => r.pass === false);
  if (failed.length > 0) {
    console.error(`performance.mjs: ${failed.length} budget(s) exceeded: ${failed.map(([k]) => k).join(", ")}`);
    process.exit(1);
  }
  process.exit(0);
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
