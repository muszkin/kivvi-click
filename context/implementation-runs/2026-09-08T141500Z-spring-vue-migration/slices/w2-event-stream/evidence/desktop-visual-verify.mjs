// Diagnostic-only desktop-viewport full verification for the event-stream journey, working
// around compare.mjs's hardcoded (non-randomized, shared across desktop+mobile) RUN_ID
// substitution, which collides with Mercure's no-replay semantics + this journey's real 24h
// idempotency dedup once step 2 is replayed by the mobile pass within the same invocation.
// Reuses compare.mjs's own comparison functions verbatim (copied, not imported — compare.mjs
// itself is not modified). Not part of the deliverable; scratchpad diagnostic only.
import { createRequire } from "node:module";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { PNG } from "pngjs";
import pixelmatch from "pixelmatch";

const require = createRequire("/home/muszkin/work/kivvi-click-wt/w2-event-stream/tests/e2e/package.json");
const { chromium } = require("playwright");

const BASE = "https://localhost:19041";
const FIXED_TIME = new Date("2026-09-08T12:00:00Z");
const SETTLE_MS = 400;
const VIEWPORT = { width: 1440, height: 900 };
const SCREENSHOT_DIFF_THRESHOLD = 0.005;
const oracleDir = "/home/muszkin/work/kivvi-click/context/migration-oracle/symfony-to-spring-vue";
const oracleJourneyDir = join(oracleDir, "journeys/event-stream");
const outDir = "/tmp/claude-1000/-home-muszkin-work-kivvi-click/10289fb7-a05e-4951-9d8a-97048ccf524b/scratchpad/repro/desktop-out";
const runId = "dv" + Date.now().toString(36); // randomized, unlike compare.mjs's hardcoded "verify"

const normalize = JSON.parse(readFileSync(oracleDir + "/capture/normalize.json", "utf8"));
const scenarios = JSON.parse(readFileSync(oracleDir + "/capture/scenarios.json", "utf8"));
const journey = scenarios["event-stream"];

const textRules = normalize.text_rules.map((r) => ({ re: new RegExp(r.pattern, "g"), to: r.replace }));
const ignorePaths = normalize.http_rules.find((r) => r.ignore_paths).ignore_paths.map((p) => new RegExp(p));
const stripReq = new Set(normalize.http_rules.find((r) => r.strip_request_headers).strip_request_headers);
const stripRes = new Set(normalize.http_rules.find((r) => r.strip_response_headers).strip_response_headers);
const applyText = (s) => textRules.reduce((acc, r) => acc.replace(r.re, r.to), s);
const sub = (s) => (typeof s === "string" ? s.replaceAll("__RUN__", runId) : s);
const subDeep = (v) => Array.isArray(v) ? v.map(subDeep) : v && typeof v === "object" ? Object.fromEntries(Object.entries(v).map(([k, x]) => [k, subDeep(x)])) : sub(v);
function normalizeUrl(raw) {
  const u = new URL(raw, BASE);
  const params = [...u.searchParams.entries()].sort(([a], [b]) => a.localeCompare(b));
  const qs = params.map(([k, v]) => `${k}=${applyText(v)}`).join("&");
  return applyText(u.pathname) + (qs ? `?${qs}` : "");
}
const sameOrigin = (raw) => raw.startsWith(BASE);
const ignored = (raw) => {
  const path = raw.startsWith(BASE) ? raw.slice(BASE.length) : raw;
  return ignorePaths.some((re) => re.test(path)) || (!sameOrigin(raw) && !/^\/\//.test(raw));
};
const pick = (headers, strip) => Object.fromEntries(Object.entries(headers).filter(([k]) => !strip.has(k.toLowerCase())).sort());
const isApiBaseline = (path) => /^\/api\/v1\//.test(path);
const FULL_PARITY_PATTERNS = [/^\/collect$/, /^\/preferences\/theme$/, /^\/preferences\/sidebar$/, /^\/import\/upload$/, /^\/(pl|en)\/login$/, /^\/(pl|en)\/logout$/];
const isFullParityPath = (path) => FULL_PARITY_PATTERNS.some((re) => re.test(path));

async function locate(page, step) {
  const loc = page.locator(step.click ?? step.fill ?? step.setFile);
  return step.hasText ? loc.filter({ hasText: step.hasText }).first() : loc.first();
}

async function runStep(page, context, step, record) {
  const s = subDeep(step);
  let apiEntry = null;
  if (s.goto) {
    const response = await page.goto(BASE + s.goto, { waitUntil: "domcontentloaded" });
    record.documentStatus = response?.status() ?? null;
  } else if (s.click) {
    await (await locate(page, s)).click();
  } else if (s.post) {
    const options = s.json ? { data: subDeep(s.json), maxRedirects: 0 } : { form: subDeep(s.form ?? {}), maxRedirects: 0 };
    const response = await context.request.post(BASE + s.post, options);
    const body = await response.text();
    apiEntry = {
      kind: "api", method: "POST", path: normalizeUrl(s.post),
      request: s.json ? { json: subDeep(s.json) } : { form: subDeep(s.form ?? {}) },
      status: response.status(), headers: pick(response.headers(), stripRes),
      body: applyText(body.length > 4000 ? body.slice(0, 4000) + "…" : body),
    };
  }
  if (s.waitAttr) await page.locator(s.waitAttr[0]).first().waitFor({ timeout: 15000 }).then(() =>
    page.waitForFunction(([sel, attr, val]) => document.querySelector(sel)?.getAttribute(attr) === val, s.waitAttr, { timeout: 15000 }));
  if (s.waitText) await page.locator(s.waitText[0]).filter({ hasText: s.waitText[1] }).first().waitFor({ timeout: 15000 });
  return apiEntry;
}

function verdict(regression, devIds) {
  if (!regression) return devIds.length > 0 ? `accepted-deviation(${devIds.join(",")})` : "parity";
  return "regression";
}
function compareUrl(oracle, candidate) {
  if (oracle == null) return { verdict: "parity", detail: "no oracle url recorded" };
  const ok = oracle === candidate;
  return { verdict: verdict(!ok, []), detail: ok ? "" : `expected "${oracle}", got "${candidate}"` };
}
function compareTexts(oracle, candidate) {
  if (oracle == null) return { verdict: "parity", detail: "no oracle texts recorded" };
  const equal = JSON.stringify(oracle) === JSON.stringify(candidate);
  return { verdict: verdict(!equal, []), detail: equal ? "" : "text arrays differ" };
}
function compareAria(oracle, candidate) {
  if (oracle == null) return { verdict: "parity", detail: "no oracle a11y recorded" };
  const equal = oracle.aria === candidate.aria;
  return { verdict: verdict(!equal, []), detail: equal ? "" : "a11y trees differ" };
}
function locationPath(headers) {
  const raw = headers?.location;
  return raw ? normalizeUrl(raw) : null;
}
function compareHttpEntry(oracleEntry, candidateEntry) {
  const problems = [];
  if (oracleEntry.method !== candidateEntry.method) problems.push(`method: ${oracleEntry.method} vs ${candidateEntry.method}`);
  if (oracleEntry.path !== candidateEntry.path) problems.push(`path: ${oracleEntry.path} vs ${candidateEntry.path}`);
  if (oracleEntry.status !== candidateEntry.status) problems.push(`status: ${oracleEntry.status} vs ${candidateEntry.status}`);
  if (String(oracleEntry.status).startsWith("3")) {
    const oracleLoc = locationPath(oracleEntry.headers ?? oracleEntry.responseHeaders);
    const candidateLoc = locationPath(candidateEntry.headers ?? candidateEntry.responseHeaders);
    if (oracleLoc !== candidateLoc) problems.push(`location: ${oracleLoc} vs ${candidateLoc}`);
  }
  return problems;
}
function compareHttp(oracleEntries, candidateEntries, devIds) {
  const core = candidateEntries.filter((e) => !isApiBaseline(e.path));
  const apiBaseline = candidateEntries.filter((e) => isApiBaseline(e.path));
  if (core.length !== oracleEntries.length) {
    return { verdict: "regression", detail: `expected ${oracleEntries.length}, candidate made ${core.length} (+${apiBaseline.length} baseline)` };
  }
  const problems = [];
  oracleEntries.forEach((oracleEntry, i) => {
    const found = compareHttpEntry(oracleEntry, core[i]);
    if (found.length > 0) problems.push(`entry ${i} (${oracleEntry.method} ${oracleEntry.path}): ${found.join("; ")}`);
  });
  return { verdict: verdict(problems.length > 0, devIds), detail: problems.join(" | ") };
}
async function compareScreenshots(oraclePath, candidatePath) {
  if (!existsSync(oraclePath)) return { verdict: "parity", detail: "no oracle screenshot recorded" };
  const oracleFinal = PNG.sync.read(readFileSync(oraclePath));
  const candidatePng = PNG.sync.read(readFileSync(candidatePath));
  if (oracleFinal.width !== candidatePng.width || oracleFinal.height !== candidatePng.height) {
    return { verdict: "regression", detail: `dimension mismatch: ${oracleFinal.width}x${oracleFinal.height} vs ${candidatePng.width}x${candidatePng.height}` };
  }
  const { width, height } = oracleFinal;
  const diff = new PNG({ width, height });
  const diffPixels = pixelmatch(oracleFinal.data, candidatePng.data, diff.data, width, height, { threshold: 0.1 });
  const ratio = diffPixels / (width * height);
  writeFileSync(candidatePath.replace(/\.png$/, ".diff.png"), PNG.sync.write(diff));
  return { verdict: verdict(ratio > SCREENSHOT_DIFF_THRESHOLD, []), detail: `${(ratio * 100).toFixed(3)}% pixels differ (${diffPixels}/${width * height})` };
}

(async () => {
  mkdirSync(outDir, { recursive: true });
  const browser = await chromium.launch({ channel: "chrome", headless: true });
  const context = await browser.newContext({ viewport: VIEWPORT, ignoreHTTPSErrors: true, reducedMotion: "reduce", locale: "pl-PL", timezoneId: "Europe/Warsaw" });
  await context.addInitScript(() => {
    const style = document.createElement("style");
    style.textContent = "*,*::before,*::after{transition:none!important;animation:none!important;caret-color:transparent!important}";
    document.addEventListener("DOMContentLoaded", () => document.head.appendChild(style));
  });
  const page = await context.newPage();
  await page.clock.setFixedTime(FIXED_TIME);

  let http = [];
  page.on("response", async (response) => {
    const req = response.request();
    const url = req.url();
    if (ignored(url)) return;
    const entry = {
      kind: req.resourceType() === "document" ? "document" : "xhr", method: req.method(), path: normalizeUrl(url),
      status: response.status(), contentType: response.headers()["content-type"] ?? null,
      requestHeaders: pick(req.headers(), stripReq), responseHeaders: pick(response.headers(), stripRes),
    };
    if (req.postData()) entry.requestBody = applyText(req.postData().slice(0, 2000));
    if (/json/.test(entry.contentType ?? "")) entry.body = applyText((await response.text().catch(() => "")).slice(0, 4000));
    http.push(entry);
  });

  const results = [];
  for (const [i, step] of journey.steps.entries()) {
    const n = i + 1;
    http = [];
    const record = { index: n, step: subDeep(step) };
    const apiEntry = await runStep(page, context, step, record);
    await page.waitForLoadState("load").catch(() => {});
    await page.waitForTimeout(SETTLE_MS);

    const shotPath = join(outDir, `step-${n}.png`);
    const normalizeMasks = normalize.screenshot_masks.map((m) => page.locator(m.selector));
    await page.screenshot({ path: shotPath, fullPage: false, mask: normalizeMasks, maskColor: "#ff00ff", animations: "disabled" });
    const aria = { aria: applyText(await page.locator("body").ariaSnapshot()) };
    const texts = (await page.locator("body").innerText()).split("\n").map((l) => applyText(l.trim())).filter(Boolean);
    const url = normalizeUrl(page.url());
    const candidateHttp = [...(apiEntry ? [apiEntry] : []), ...http];

    // Load oracle
    const oDir = join(oracleJourneyDir, "steps", String(n));
    const oracleUrl = existsSync(join(oDir, "url.txt")) ? readFileSync(join(oDir, "url.txt"), "utf8").trim() : null;
    const oracleTexts = existsSync(join(oDir, "texts.json")) ? JSON.parse(readFileSync(join(oDir, "texts.json"), "utf8")) : null;
    const oracleAria = existsSync(join(oDir, "a11y.json")) ? JSON.parse(readFileSync(join(oDir, "a11y.json"), "utf8")) : null;
    const oracleHttp = existsSync(join(oDir, "http.jsonl"))
      ? readFileSync(join(oDir, "http.jsonl"), "utf8").split("\n").filter(Boolean).map((l) => JSON.parse(l))
      : [];

    const stepResult = {
      step: n,
      url: compareUrl(oracleUrl, url),
      texts: compareTexts(oracleTexts, texts),
      aria: compareAria(oracleAria, aria),
      screenshot: await compareScreenshots(join(oDir, "desktop.png"), shotPath),
      http: oracleHttp.length > 0 ? compareHttp(oracleHttp, candidateHttp, []) : { verdict: "parity", detail: "no oracle http recorded" },
    };
    results.push(stepResult);
    console.log(`step ${n}: url=${stepResult.url.verdict} texts=${stepResult.texts.verdict} aria=${stepResult.aria.verdict} screenshot=${stepResult.screenshot.verdict} (${stepResult.screenshot.detail}) http=${stepResult.http.verdict} ${stepResult.http.detail}`);
  }

  const regressions = results.flatMap((r) => [r.url, r.texts, r.aria, r.screenshot, r.http]).filter((v) => v.verdict === "regression");
  console.log(`\nTOTAL REGRESSIONS: ${regressions.length}`);
  writeFileSync(join(outDir, "results.json"), JSON.stringify(results, null, 2));

  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });
