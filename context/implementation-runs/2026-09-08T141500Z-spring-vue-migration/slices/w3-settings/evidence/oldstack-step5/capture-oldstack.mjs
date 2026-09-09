#!/usr/bin/env node
/**
 * Old-stack (Symfony, kivvi-oracle project) reproduction of settings journey step 5
 * (/pl/settings/api), replaying steps 1-5 of scenarios.json exactly, then measuring
 * the "Klucze API" table layout to check for the NAZWA-cell wrap the oracle screenshot
 * shows. Browser launch / settle / screenshot options are copied verbatim from
 * context/migration-oracle/symfony-to-spring-vue/capture/capture.mjs and
 * tools/migration-verify/compare.mjs (both read-only, not imported, not modified).
 */
import { createRequire } from "node:module";
import { writeFileSync, readFileSync, mkdirSync } from "node:fs";
import { join } from "node:path";

const E2E_PKG = "/home/muszkin/work/kivvi-click/tests/e2e/package.json";
const VERIFY_PKG = "/home/muszkin/work/kivvi-click-wt/w3-settings/tools/migration-verify/package.json";
const require = createRequire(E2E_PKG);
const requireVerify = createRequire(VERIFY_PKG);
const { chromium } = require("playwright");
const { PNG } = requireVerify("pngjs");
const pixelmatchMod = requireVerify("pixelmatch");
const pixelmatch = pixelmatchMod.default ?? pixelmatchMod;

const OUT = "/tmp/claude-1000/-home-muszkin-work-kivvi-click/10289fb7-a05e-4951-9d8a-97048ccf524b/scratchpad/oldstack-step5";
const EVIDENCE_OUT = "/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/slices/w3-settings/evidence/oldstack-step5";
const ORACLE_PNG = "/home/muszkin/work/kivvi-click/context/migration-oracle/symfony-to-spring-vue/journeys/settings/steps/5/desktop.png";
const CANDIDATE_PNG = "/home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/slices/w3-settings/evidence/repair-1-gates/compare/settings-visual/settings/steps/5/desktop.png";

const BASE = "https://localhost:18443";
const FIXED_TIME = new Date("2026-09-08T12:00:00Z");
const VIEWPORT = { width: 1440, height: 900 };
const SETTLE_MS = 400;
const SCREENSHOT_DIFF_THRESHOLD = 0.005; // 0.5% — from tools/migration-verify/compare.mjs
const PIXELMATCH_OPTS = { threshold: 0.1 }; // from tools/migration-verify/compare.mjs

// settings journey, steps 1-5, from context/migration-oracle/.../capture/scenarios.json (read-only)
const STEPS = [
  { goto: "/pl/settings/account" },
  { click: ".settings-nav a", hasText: "Śledzone strony", waitUrl: "/pl/settings/sites" },
  { click: ".settings-nav a", hasText: "Zespół", waitUrl: "/pl/settings/team" },
  { click: ".settings-nav a", hasText: "Dostawcy email", waitUrl: "/pl/settings/providers" },
  { click: ".settings-nav a", hasText: "Webhooks i API", waitUrl: "/pl/settings/api" },
];

// normalize.json screenshot_masks (read-only, copied verbatim; inert on this page but kept for fidelity)
const SCREENSHOT_MASKS = [
  { selector: "#cg-main, .cardiogram-canvas" },
  { selector: ".event-row__time" },
];

async function locate(page, step) {
  const loc = page.locator(step.click);
  return step.hasText ? loc.filter({ hasText: step.hasText }).first() : loc.first();
}

async function runStep(page, step) {
  if (step.goto) {
    await page.goto(BASE + step.goto, { waitUntil: "domcontentloaded" });
  } else if (step.click) {
    await (await locate(page, step)).click();
  }
  if (step.waitUrl) await page.waitForURL((u) => u.href.includes(step.waitUrl), { timeout: 15000 });
}

async function settle(page) {
  await page.waitForLoadState("load").catch(() => {});
  await page.evaluate(() => document.fonts?.ready).catch(() => {});
  await page.waitForTimeout(SETTLE_MS);
}

async function measure(page) {
  return page.evaluate(() => {
    const table = document.querySelector(".table");
    const theadRow = table?.querySelector("thead tr");
    const ths = table ? [...table.querySelectorAll("th")].map((th) => {
      const r = th.getBoundingClientRect();
      return { text: th.textContent.trim(), x: r.x, y: r.y, width: r.width, height: r.height };
    }) : [];
    const rows = table ? [...table.querySelectorAll("tbody tr")] : [];
    const nameRow = rows.find((tr) => tr.textContent.includes("Produkcja"));
    const nameTd = nameRow ? nameRow.querySelector("td") : null;
    const nameSpan = nameTd ? nameTd.querySelector("span") : null;
    const actionsCell = nameRow ? nameRow.querySelectorAll("td")[nameRow.querySelectorAll("td").length - 1] : null;
    const actionButtons = actionsCell ? [...actionsCell.querySelectorAll("button, a")].map((b) => b.getBoundingClientRect()).map((r) => ({ x: r.x, y: r.y, width: r.width, height: r.height })) : [];
    const stacked = actionButtons.length >= 2 ? Math.abs(actionButtons[0].y - actionButtons[1].y) > 2 : null;

    return {
      headerRowHeight: theadRow ? theadRow.getBoundingClientRect().height : null,
      nameCellHeight: nameTd ? nameTd.getBoundingClientRect().height : null,
      nameSpanHeight: nameSpan ? nameSpan.getBoundingClientRect().height : null,
      nameSpanClientRectsCount: nameSpan ? nameSpan.getClientRects().length : null,
      nameSpanText: nameSpan ? nameSpan.textContent : null,
      wrapped: nameSpan ? nameSpan.getClientRects().length > 1 : null,
      actionButtonsStacked: stacked,
      actionButtonRects: actionButtons,
      allThRects: ths,
      documentFontsStatus: document.fonts.status,
      fontsCheckGeist500_14: document.fonts.check("500 14px Geist"),
      fontsCheckGeist400_13: document.fonts.check("400 13px Geist"),
      devicePixelRatio: window.devicePixelRatio,
      innerWidth: window.innerWidth,
      innerHeight: window.innerHeight,
    };
  });
}

function pixelDiff(oraclePath, candidatePath, outDiffPath) {
  const oraclePng = PNG.sync.read(readFileSync(oraclePath));
  const candidatePng = PNG.sync.read(readFileSync(candidatePath));
  if (oraclePng.width !== candidatePng.width || oraclePng.height !== candidatePng.height) {
    return { verdict: "dimension-mismatch", detail: `${oraclePng.width}x${oraclePng.height} vs ${candidatePng.width}x${candidatePng.height}` };
  }
  const { width, height } = oraclePng;
  const diff = new PNG({ width, height });
  const diffPixels = pixelmatch(oraclePng.data, candidatePng.data, diff.data, width, height, PIXELMATCH_OPTS);
  const ratio = diffPixels / (width * height);
  writeFileSync(outDiffPath, PNG.sync.write(diff));
  return { ratioPct: ratio * 100, diffPixels, totalPixels: width * height, regression: ratio > SCREENSHOT_DIFF_THRESHOLD };
}

async function runOnce(label, { extraFontsReadyBeforeShot = false } = {}) {
  const browser = await chromium.launch({ channel: "chrome", headless: true });
  const context = await browser.newContext({
    viewport: VIEWPORT,
    ignoreHTTPSErrors: true,
    reducedMotion: "reduce",
    locale: "pl-PL",
    timezoneId: "Europe/Warsaw",
  });
  await context.addInitScript(() => {
    const style = document.createElement("style");
    style.textContent = "*,*::before,*::after{transition:none!important;animation:none!important;caret-color:transparent!important}";
    document.addEventListener("DOMContentLoaded", () => document.head.appendChild(style));
  });
  const page = await context.newPage();
  await page.clock.setFixedTime(FIXED_TIME);

  for (const step of STEPS) {
    await runStep(page, step);
  }
  await settle(page);
  if (extraFontsReadyBeforeShot) {
    await page.evaluate(() => document.fonts.ready).catch(() => {});
  }

  const metrics = await measure(page);
  const shotPath = join(OUT, `${label}-desktop.png`);
  const masks = SCREENSHOT_MASKS.map((m) => page.locator(m.selector));
  await page.screenshot({ path: shotPath, fullPage: false, mask: masks, maskColor: "#ff00ff", animations: "disabled" });

  await browser.close();

  const vsOracle = pixelDiff(ORACLE_PNG, shotPath, join(OUT, `${label}-vs-oracle.diff.png`));
  const vsCandidate = pixelDiff(CANDIDATE_PNG, shotPath, join(OUT, `${label}-vs-candidate.diff.png`));

  return { label, url: page.url(), metrics, vsOracle, vsCandidate, screenshot: shotPath };
}

(async () => {
  mkdirSync(OUT, { recursive: true });
  mkdirSync(EVIDENCE_OUT, { recursive: true });
  const results = [];
  for (let i = 1; i <= 5; i++) {
    const r = await runOnce(`run-${i}`);
    console.log(`run-${i}: wrapped=${r.metrics.wrapped} vsOracle=${r.vsOracle.ratioPct?.toFixed(3)}% vsCandidate=${r.vsCandidate.ratioPct?.toFixed(3)}% fontsCheck=${r.metrics.fontsCheckGeist500_14}`);
    writeFileSync(join(OUT, `run-${i}.json`), JSON.stringify(r, null, 2));
    results.push(r);
  }
  const extra = await runOnce("run-6-fonts-ready", { extraFontsReadyBeforeShot: true });
  console.log(`run-6-fonts-ready: wrapped=${extra.metrics.wrapped} vsOracle=${extra.vsOracle.ratioPct?.toFixed(3)}% vsCandidate=${extra.vsCandidate.ratioPct?.toFixed(3)}% fontsCheck=${extra.metrics.fontsCheckGeist500_14}`);
  writeFileSync(join(OUT, `run-6-fonts-ready.json`), JSON.stringify(extra, null, 2));
  results.push(extra);

  writeFileSync(join(OUT, "all-results.json"), JSON.stringify(results, null, 2));
  console.log("done");
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
