#!/usr/bin/env node
/**
 * compare.mjs — replays an oracle journey against a candidate stack and reports parity
 * across the contract and visual dimensions (performance is performance.mjs).
 *
 * Reuses the DSL and normalization logic of
 * context/migration-oracle/symfony-to-spring-vue/capture/capture.mjs (copied here, not
 * imported — the oracle directory itself is never modified) and layers the DEV-1..12
 * handling from ./deviations.json on top.
 *
 * Usage (repository root):
 *   node tools/migration-verify/compare.mjs --journey <id> --base https://localhost:<port>
 *     [--out <dir>] [--dimension contract|visual|performance|all]
 *
 * Env:
 *   VERIFY_COMPOSE — docker compose invocation used for the db.json counts (default: the
 *     wave-0 resource lease, "docker compose -p kivvi-w-login -f compose.next.yaml").
 */
import { execSync } from "node:child_process";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { PNG } from "pngjs";
import pixelmatch from "pixelmatch";

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, "../..");
const oracleDir = join(root, "context/migration-oracle/symfony-to-spring-vue");
const require = createRequire(join(root, "tests/e2e/package.json"));
const { chromium } = require("playwright");

// ---------------------------------------------------------------------------------------
// CLI
// ---------------------------------------------------------------------------------------
function parseArgs(argv) {
  const args = { dimension: "all" };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--journey") args.journey = argv[++i];
    else if (a === "--base") args.base = argv[++i];
    else if (a === "--out") args.out = argv[++i];
    else if (a === "--dimension") args.dimension = argv[++i];
  }
  if (!args.journey || !args.base) {
    console.error(
      "usage: compare.mjs --journey <id> --base <url> [--out <dir>] [--dimension contract|visual|performance|all]",
    );
    process.exit(2);
  }
  args.out = resolve(args.out ?? join(here, ".out"));
  return args;
}

const args = parseArgs(process.argv.slice(2));
const BASE = args.base.replace(/\/$/, "");
const DIMENSIONS = new Set(args.dimension === "all" ? ["contract", "visual"] : [args.dimension]);
const FIXED_TIME = new Date("2026-09-08T12:00:00Z");
const VIEWPORTS = { desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } };
const SETTLE_MS = 400;
const COMPOSE = process.env.VERIFY_COMPOSE ?? "docker compose -p kivvi-w-login -f compose.next.yaml";
// --sidebar-w (248px) x --topbar-h (56px) from assets/styles/01-tokens.css — see DEV-11 in
// deviations.json for why this fixed rectangle stands in for a live-page selector mask.
const MAIN_SCROLL_ORIGIN = { x: 248, y: 56 };
const SCREENSHOT_DIFF_THRESHOLD = 0.005; // 0.5%

// ---------------------------------------------------------------------------------------
// Oracle inputs (read-only)
// ---------------------------------------------------------------------------------------
const scenarios = JSON.parse(readFileSync(join(oracleDir, "capture/scenarios.json"), "utf8"));
const normalize = JSON.parse(readFileSync(join(oracleDir, "capture/normalize.json"), "utf8"));
const deviationsDoc = JSON.parse(readFileSync(join(here, "deviations.json"), "utf8"));

const journey = scenarios[args.journey];
if (!journey) {
  console.error(`unknown journey "${args.journey}" — not present in capture/scenarios.json`);
  process.exit(2);
}
const oracleJourneyDir = join(oracleDir, "journeys", args.journey);

// ---------------------------------------------------------------------------------------
// Normalization — copied from capture.mjs so the oracle directory stays untouched
// ---------------------------------------------------------------------------------------
const textRules = normalize.text_rules.map((r) => ({ re: new RegExp(r.pattern, "g"), to: r.replace }));
const ignorePaths = normalize.http_rules.find((r) => r.ignore_paths).ignore_paths.map((p) => new RegExp(p));
const stripReq = new Set(normalize.http_rules.find((r) => r.strip_request_headers).strip_request_headers);
const stripRes = new Set(normalize.http_rules.find((r) => r.strip_response_headers).strip_response_headers);

const applyText = (s) => textRules.reduce((acc, r) => acc.replace(r.re, r.to), s);
// Randomized per invocation (mirrors capture.mjs's own RUN_ID) and re-suffixed per viewport
// pass inside runJourney below (also mirroring capture.mjs): a journey with a real,
// non-idempotent POST (e.g. event-stream's /collect + its 24h dedup) would otherwise have its
// mobile pass replay the desktop pass's exact idempotency id within the same invocation,
// turning a fresh 202+published-row into a 200 duplicate that never republishes — Mercure has
// no replay, so the mobile pass's own (freshly opened) subscription then waits forever for a
// row that was only ever published once, to the desktop pass's subscription.
const RUN_ID = (process.env.VERIFY_RUN_ID ?? process.env.ORACLE_RUN_ID ?? Date.now().toString(36)).toLowerCase();
let runId = RUN_ID;
const sub = (s) => (typeof s === "string" ? s.replaceAll("__RUN__", runId) : s);
const subDeep = (v) =>
  Array.isArray(v)
    ? v.map(subDeep)
    : v && typeof v === "object"
      ? Object.fromEntries(Object.entries(v).map(([k, x]) => [k, subDeep(x)]))
      : sub(v);

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
const pick = (headers, strip) =>
  Object.fromEntries(Object.entries(headers).filter(([k]) => !strip.has(k.toLowerCase())).sort());
const isApiBaseline = (path) => /^\/api\/v1\//.test(path);

// DEV-4's full-parity endpoint list.
const FULL_PARITY_PATTERNS = [
  /^\/collect$/,
  /^\/preferences\/theme$/,
  /^\/preferences\/sidebar$/,
  /^\/import\/upload$/,
  /^\/(pl|en)\/login$/,
  /^\/(pl|en)\/logout$/,
];
const isFullParityPath = (path) => FULL_PARITY_PATTERNS.some((re) => re.test(path));

// ---------------------------------------------------------------------------------------
// Deviations for this journey
// ---------------------------------------------------------------------------------------
function devAppliesToStep(dev, journeyId, stepIndex) {
  if (dev.steps === undefined || dev.steps === "all") return true;
  if (Array.isArray(dev.steps)) return dev.steps.includes(stepIndex);
  if (typeof dev.steps === "object") {
    const list = dev.steps[journeyId];
    return Array.isArray(list) ? list.includes(stepIndex) : false;
  }
  return false;
}
const journeyDevs = deviationsDoc.deviations.filter(
  (d) => d.journeys.includes("*") || d.journeys.includes(args.journey),
);
function activeDeviations(stepIndex, dimension) {
  return journeyDevs.filter((d) => d.dimension === dimension && devAppliesToStep(d, args.journey, stepIndex));
}

// ---------------------------------------------------------------------------------------
// Candidate capture (adapted from capture.mjs's runStep/runJourney)
// ---------------------------------------------------------------------------------------
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
  } else if (s.reload) {
    const response = await page.reload({ waitUntil: "domcontentloaded" });
    record.documentStatus = response?.status() ?? null;
  } else if (s.back) {
    await page.goBack({ waitUntil: "domcontentloaded" });
  } else if (s.click) {
    await (await locate(page, s)).click();
  } else if (s.fill) {
    await (await locate(page, s)).fill(s.value);
  } else if (s.setFile) {
    await (await locate(page, s)).setInputFiles({ name: s.name, mimeType: "text/csv", buffer: Buffer.from(s.content, "utf8") });
  } else if (s.post) {
    const options = s.json ? { data: subDeep(s.json), maxRedirects: 0 } : { form: subDeep(s.form ?? {}), maxRedirects: 0 };
    const response = await context.request.post(BASE + s.post, options);
    const body = await response.text();
    apiEntry = {
      kind: "api",
      method: "POST",
      path: normalizeUrl(s.post),
      request: s.json ? { json: subDeep(s.json) } : { form: subDeep(s.form ?? {}) },
      status: response.status(),
      headers: pick(response.headers(), stripRes),
      body: applyText(body.length > 4000 ? body.slice(0, 4000) + "…" : body),
    };
  }
  if (s.waitUrl) await page.waitForURL((u) => u.href.includes(s.waitUrl), { timeout: 15000 });
  if (s.waitAttr)
    await page
      .locator(s.waitAttr[0])
      .first()
      .waitFor({ timeout: 15000 })
      .then(() =>
        page.waitForFunction(
          ([sel, attr, val]) => document.querySelector(sel)?.getAttribute(attr) === val,
          s.waitAttr,
          { timeout: 15000 },
        ),
      );
  if (s.waitText) await page.locator(s.waitText[0]).filter({ hasText: s.waitText[1] }).first().waitFor({ timeout: 15000 });
  return apiEntry;
}

function dbCounts() {
  const sql =
    "select 'spring_session', count(*) from spring_session union all select 'event_dedup', count(*) from event_dedup union all select 'shedlock', count(*) from shedlock";
  const out = execSync(`${COMPOSE} exec -T database psql -U app -d app -tAc "${sql}"`, { cwd: root, encoding: "utf8" });
  const counts = Object.fromEntries(
    out
      .trim()
      .split("\n")
      .filter(Boolean)
      .map((l) => l.split("|"))
      .map(([k, v]) => [k, Number(v)]),
  );
  // import-wizard (DEV-5): the old stack's oracle db.json also tracks a filesystem count
  // ("var/import files"), not only Postgres rows — counted inside the api container, where
  // ImportUploadStorage's configured directory (kivvi.import.upload-directory, default
  // ./var/import) resolves relative to the image's WORKDIR (/app). Harmless for every other
  // journey: they never write there, so this count's delta is always 0 = 0 against an oracle
  // db.json that (for those journeys) has no such key either (compareDb defaults a missing
  // oracle key to 0).
  counts["var/import files"] = importFileCount();
  return counts;
}

function importFileCount() {
  const out = execSync(`${COMPOSE} exec -T api sh -c "find var/import -type f 2>/dev/null | wc -l"`, {
    cwd: root,
    encoding: "utf8",
  });
  return Number(out.trim()) || 0;
}

// DEV-11: remove the masked subtree from the candidate DOM before capturing aria/text, so
// the shell chrome is compared but the not-yet-built page body never is.
async function applyDomMasks(page, stepIndex) {
  const masks = activeDeviations(stepIndex, "visual").filter(
    (d) => d.mechanism === "mask-selector" && d.appliesTo?.includes("aria"),
  );
  for (const mask of masks) {
    await page.evaluate((selector) => {
      document.querySelectorAll(selector).forEach((el) => el.remove());
    }, mask.selector);
  }
}

async function captureScreenshot(page, path, stepIndex, viewportName) {
  const normalizeMasks = normalize.screenshot_masks.map((m) => page.locator(m.selector));
  await page.screenshot({ path, fullPage: false, mask: normalizeMasks, maskColor: "#ff00ff", animations: "disabled" });
  const rectMasks = activeDeviations(stepIndex, "visual").filter(
    (d) => d.mechanism === "mask-selector" && d.appliesTo?.includes("screenshot") && d.selector === ".main-scroll",
  );
  if (rectMasks.length > 0) {
    const { width, height } = VIEWPORTS[viewportName];
    paintRect(path, MAIN_SCROLL_ORIGIN.x, MAIN_SCROLL_ORIGIN.y, width - MAIN_SCROLL_ORIGIN.x, height - MAIN_SCROLL_ORIGIN.y);
  }
}

function paintRect(pngPath, x, y, w, h) {
  const png = PNG.sync.read(readFileSync(pngPath));
  const x1 = Math.max(0, Math.min(png.width, x));
  const y1 = Math.max(0, Math.min(png.height, y));
  const x2 = Math.max(0, Math.min(png.width, x + w));
  const y2 = Math.max(0, Math.min(png.height, y + h));
  for (let py = y1; py < y2; py++) {
    for (let px = x1; px < x2; px++) {
      const idx = (png.width * py + px) << 2;
      png.data[idx] = 0xff;
      png.data[idx + 1] = 0x00;
      png.data[idx + 2] = 0xff;
      png.data[idx + 3] = 0xff;
    }
  }
  writeFileSync(pngPath, PNG.sync.write(png));
}

async function runJourney(id, journeyDef, viewportName) {
  runId = RUN_ID + (viewportName === "mobile" ? "m" : "d");
  const browser = await chromium.launch({ channel: "chrome", headless: true });
  const context = await browser.newContext({
    viewport: VIEWPORTS[viewportName],
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

  let http = [];
  page.on("response", async (response) => {
    const req = response.request();
    const url = req.url();
    if (ignored(url)) return;
    const entry = {
      kind: req.resourceType() === "document" ? "document" : "xhr",
      method: req.method(),
      path: normalizeUrl(url),
      status: response.status(),
      contentType: response.headers()["content-type"] ?? null,
      requestHeaders: pick(req.headers(), stripReq),
      responseHeaders: pick(response.headers(), stripRes),
    };
    if (req.postData()) entry.requestBody = applyText(req.postData().slice(0, 2000));
    // compareHttpEntry's DEV-4 full-parity deep-check (/collect, /preferences/*, /login) reads
    // `entry.body` (the response body) — previously only ever set by the explicit `s.post` step
    // branch above, never by this passive listener, so any full-parity endpoint reached via a
    // `click` step (e.g. the theme toggle's `data-action="set-theme"`, which POSTs
    // /preferences/theme) always recorded `body: undefined` and false-flagged as a regression
    // against the oracle's own non-empty `body` field. Scoped to full-parity paths only — other
    // responses (JS/CSS/image assets, `/api/v1/**`) never need this and some response types
    // cannot be read as text, hence the try/catch.
    if (isFullParityPath(entry.path)) {
      try {
        const text = await response.text();
        entry.body = applyText(text.length > 4000 ? text.slice(0, 4000) + "…" : text);
      } catch {
        // Body already consumed or not text-readable — leave entry.body unset, same as before.
      }
    }
    http.push(entry);
  });

  const dir = join(args.out, id);
  // Static journeys (scenarios.json entry has `steps: []`, e.g. scheduler-heartbeat) never
  // enter the per-step loop below, so its own `mkdirSync(stepDir, ...)` never runs — without
  // this, the db.json write at the end of this function fails with ENOENT for a journey that
  // has no browser steps at all.
  mkdirSync(dir, { recursive: true });
  const isDesktop = viewportName === "desktop";
  const before = isDesktop && DIMENSIONS.has("contract") ? dbCounts() : null;
  const stepRecords = [];
  for (const [i, step] of journeyDef.steps.entries()) {
    const n = i + 1;
    const stepDir = join(dir, "steps", String(n));
    mkdirSync(stepDir, { recursive: true });
    http = [];
    const record = { index: n, step: subDeep(step) };
    const apiEntry = await runStep(page, context, step, record);
    await page.waitForLoadState("load").catch(() => {});
    await page.evaluate(() => document.fonts?.ready).catch(() => {});
    await page.waitForTimeout(SETTLE_MS);

    if (DIMENSIONS.has("visual")) {
      await captureScreenshot(page, join(stepDir, `${viewportName}.png`), n, viewportName);
      if (isDesktop) {
        await applyDomMasks(page, n);
        const aria = await page.locator("body").ariaSnapshot();
        const texts = (await page.locator("body").innerText()).split("\n").map((l) => applyText(l.trim())).filter(Boolean);
        writeFileSync(join(stepDir, "a11y.json"), JSON.stringify({ aria: applyText(aria) }, null, 2));
        writeFileSync(join(stepDir, "texts.json"), JSON.stringify(texts, null, 2));
      }
    }
    if (isDesktop) {
      writeFileSync(join(stepDir, "url.txt"), normalizeUrl(page.url()) + "\n");
      const lines = [...(apiEntry ? [apiEntry] : []), ...http].map((e) => JSON.stringify(e)).join("\n");
      writeFileSync(join(stepDir, "http.jsonl"), lines + (lines ? "\n" : ""));
      writeFileSync(join(stepDir, "step.json"), JSON.stringify({ ...record, title: applyText(await page.title()) }, null, 2));
    }
    stepRecords.push(record);
  }
  let delta = null;
  if (isDesktop && DIMENSIONS.has("contract")) {
    const after = dbCounts();
    delta = Object.fromEntries(Object.keys(after).map((k) => [k, after[k] - before[k]]));
    writeFileSync(join(dir, "db.json"), JSON.stringify({ before, after, delta }, null, 2));
  }
  await browser.close();
  return { stepRecords, delta };
}

// ---------------------------------------------------------------------------------------
// Comparison
// ---------------------------------------------------------------------------------------
function readOracleStep(stepIndex) {
  const dir = join(oracleJourneyDir, "steps", String(stepIndex));
  const read = (name, parse = true) => {
    const path = join(dir, name);
    if (!existsSync(path)) return null;
    const raw = readFileSync(path, "utf8");
    return parse ? JSON.parse(raw) : raw;
  };
  return {
    step: read("step.json"),
    url: read("url.txt", false)?.trim(),
    texts: read("texts.json"),
    aria: read("a11y.json"),
    http: (read("http.jsonl", false) ?? "")
      .split("\n")
      .filter(Boolean)
      .map((l) => JSON.parse(l)),
    screenshotDesktop: join(dir, "desktop.png"),
    screenshotMobile: join(dir, "mobile.png"),
  };
}

function readCandidateStep(stepIndex) {
  const dir = join(args.out, args.journey, "steps", String(stepIndex));
  const read = (name, parse = true) => {
    const path = join(dir, name);
    if (!existsSync(path)) return null;
    const raw = readFileSync(path, "utf8");
    return parse ? JSON.parse(raw) : raw;
  };
  return {
    step: read("step.json"),
    url: read("url.txt", false)?.trim(),
    texts: read("texts.json"),
    aria: read("a11y.json"),
    http: (read("http.jsonl", false) ?? "")
      .split("\n")
      .filter(Boolean)
      .map((l) => JSON.parse(l)),
    screenshotDesktop: join(dir, "desktop.png"),
    screenshotMobile: join(dir, "mobile.png"),
  };
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

function compareTexts(oracle, candidate, masked) {
  if (oracle == null) return { verdict: "parity", detail: "no oracle texts recorded" };
  if (masked) {
    const prefixOk = candidate.length <= oracle.length && candidate.every((line, i) => line === oracle[i]);
    return {
      verdict: verdict(!prefixOk, ["DEV-11"]),
      detail: prefixOk ? "" : "candidate shell text is not a prefix of the oracle's recorded text",
    };
  }
  const equal = JSON.stringify(oracle) === JSON.stringify(candidate);
  return { verdict: verdict(!equal, []), detail: equal ? "" : "text arrays differ" };
}

function compareAria(oracle, candidate, masked) {
  if (oracle == null) return { verdict: "parity", detail: "no oracle a11y recorded" };
  const oracleLines = oracle.aria.split("\n");
  const candidateLines = candidate.aria.split("\n");
  if (masked) {
    const prefixOk =
      candidateLines.length <= oracleLines.length && candidateLines.every((line, i) => line === oracleLines[i]);
    return {
      verdict: verdict(!prefixOk, ["DEV-11"]),
      detail: prefixOk ? "" : "candidate shell a11y tree is not a prefix of the oracle's recorded tree",
    };
  }
  const equal = oracle.aria === candidate.aria;
  return { verdict: verdict(!equal, []), detail: equal ? "" : "a11y trees differ" };
}

function locationPath(headers) {
  const raw = headers?.location;
  return raw ? normalizeUrl(raw) : null;
}

function extractLoginErrorAndUsername(body) {
  const errorMatch = /data-login-error="([^"]*)"/.exec(body) ?? /feed-card__err">[\s\S]*?<span>([^<]+)<\/span>/.exec(body);
  const usernameMatch = /data-last-username="([^"]*)"/.exec(body) ?? /id="f-_username"[^>]*value="([^"]*)"/.exec(body);
  return {
    error: errorMatch ? errorMatch[1] : null,
    username: usernameMatch ? usernameMatch[1] : null,
  };
}

// JSON.parse decodes `„`-style escapes into the actual character and JSON.stringify
// re-serializes both sides the same way, so this doubles as canonicalization: an oracle body
// escaped as `„` and a candidate body carrying the raw UTF-8 „ character compare equal.
function compareJsonBodies(oracleBody, candidateBody) {
  try {
    return JSON.stringify(JSON.parse(oracleBody)) === JSON.stringify(JSON.parse(candidateBody));
  } catch {
    return false;
  }
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
  if (problems.length === 0 && isFullParityPath(oracleEntry.path)) {
    if (/\/(pl|en)\/login$/.test(oracleEntry.path) && oracleEntry.status === 200) {
      const oracleExtract = extractLoginErrorAndUsername(oracleEntry.body ?? "");
      const candidateExtract = extractLoginErrorAndUsername(candidateEntry.body ?? "");
      if (oracleExtract.error !== candidateExtract.error) {
        problems.push(`login error message: "${oracleExtract.error}" vs "${candidateExtract.error}"`);
      }
      if (oracleExtract.username !== candidateExtract.username) {
        problems.push(`last username: "${oracleExtract.username}" vs "${candidateExtract.username}"`);
      }
    } else if (/^\/preferences\//.test(oracleEntry.path) && oracleEntry.status === 200) {
      if (!compareJsonBodies(oracleEntry.body ?? "{}", candidateEntry.body ?? "{}")) {
        problems.push(`body: ${oracleEntry.body} vs ${candidateEntry.body}`);
      }
    } else if (oracleEntry.path === "/collect") {
      // DEV-4's full-parity list names /collect for every status it can answer (202
      // accepted, 200 duplicate, 400 invalid payload) — the old guard above only ever ran
      // this deep check for a 200, so /collect's 202/400 bodies (the four exact Polish
      // error messages, {"status":"accepted"}) were never actually diffed.
      if (!compareJsonBodies(oracleEntry.body ?? "", candidateEntry.body ?? "")) {
        problems.push(`body: ${oracleEntry.body} vs ${candidateEntry.body}`);
      }
    }
  }
  return problems;
}

function compareHttp(oracleEntries, candidateEntries, devIds) {
  const core = candidateEntries.filter((e) => !isApiBaseline(e.path));
  const apiBaseline = candidateEntries.filter((e) => isApiBaseline(e.path));
  if (core.length !== oracleEntries.length) {
    return {
      verdict: "regression",
      detail: `expected ${oracleEntries.length} document/api request(s), candidate made ${core.length} (plus ${apiBaseline.length} /api/v1/* baseline call(s))`,
    };
  }
  const problems = [];
  oracleEntries.forEach((oracleEntry, i) => {
    const found = compareHttpEntry(oracleEntry, core[i]);
    if (found.length > 0) problems.push(`entry ${i} (${oracleEntry.method} ${oracleEntry.path}): ${found.join("; ")}`);
  });
  return {
    verdict: verdict(problems.length > 0, devIds),
    detail: problems.join(" | "),
    apiBaselineCount: apiBaseline.length,
  };
}

async function compareScreenshots(oraclePath, candidatePath, stepIndex, viewportName) {
  if (!existsSync(oraclePath)) return { verdict: "parity", detail: "no oracle screenshot recorded" };
  const oracleMasks = activeDeviations(stepIndex, "visual").filter(
    (d) => d.mechanism === "mask-selector" && d.appliesTo?.includes("screenshot") && d.selector === ".main-scroll",
  );
  const oraclePng = PNG.sync.read(readFileSync(oraclePath));
  if (oracleMasks.length > 0) {
    const { width, height } = VIEWPORTS[viewportName];
    const tmp = join(args.out, `.oracle-mask-${viewportName}-${stepIndex}.png`);
    writeFileSync(tmp, PNG.sync.write(oraclePng));
    paintRect(tmp, MAIN_SCROLL_ORIGIN.x, MAIN_SCROLL_ORIGIN.y, width - MAIN_SCROLL_ORIGIN.x, height - MAIN_SCROLL_ORIGIN.y);
  }
  const oracleFinal = PNG.sync.read(
    readFileSync(oracleMasks.length > 0 ? join(args.out, `.oracle-mask-${viewportName}-${stepIndex}.png`) : oraclePath),
  );
  const candidatePng = PNG.sync.read(readFileSync(candidatePath));
  if (oracleFinal.width !== candidatePng.width || oracleFinal.height !== candidatePng.height) {
    return {
      verdict: "regression",
      detail: `dimension mismatch: oracle ${oracleFinal.width}x${oracleFinal.height} vs candidate ${candidatePng.width}x${candidatePng.height}`,
    };
  }
  const { width, height } = oracleFinal;
  const diff = new PNG({ width, height });
  const diffPixels = pixelmatch(oracleFinal.data, candidatePng.data, diff.data, width, height, { threshold: 0.1 });
  const ratio = diffPixels / (width * height);
  const diffPath = candidatePath.replace(/\.png$/, ".diff.png");
  writeFileSync(diffPath, PNG.sync.write(diff));
  const devIds = oracleMasks.length > 0 ? ["DEV-11"] : [];
  return {
    verdict: verdict(ratio > SCREENSHOT_DIFF_THRESHOLD, devIds),
    detail: `${(ratio * 100).toFixed(3)}% pixels differ (${diffPixels}/${width * height})`,
  };
}

function compareDb(oracleDb, candidateDelta) {
  if (!oracleDb || !candidateDelta) return [];
  const mapping = deviationsDoc.deviations.find((d) => d.id === "DEV-5")?.mapping ?? {};
  const results = [];
  for (const [oracleKey, targetKeys] of Object.entries(mapping)) {
    if (targetKeys.length === 0) continue; // messenger_messages: no counterpart, ignored.
    const oracleDelta = oracleDb.delta[oracleKey] ?? 0;
    const candidateSum = targetKeys.reduce((sum, k) => sum + (candidateDelta[k] ?? 0), 0);
    // DEV-9: sessions compared by count only, and Spring Session persists eagerly on any
    // getSession(true) call (a sign-in), unlike the old stack's lazier write-triggered
    // persistence — see worker-report.md for the full reasoning. One extra row from the
    // journey's own sign-in is accepted parity, not a regression.
    const tolerance = oracleKey === "sessions" ? 1 : 0;
    const ok = Math.abs(candidateSum - oracleDelta) <= tolerance;
    results.push({
      table: oracleKey,
      verdict: verdict(!ok, oracleKey === "sessions" ? ["DEV-9"] : ["DEV-5"]),
      detail: `oracle delta ${oracleDelta}, candidate delta ${candidateSum} (mapped to ${targetKeys.join("+")})`,
    });
  }
  return results;
}

// ---------------------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------------------
(async () => {
  mkdirSync(args.out, { recursive: true });

  console.log(`compare.mjs: replaying journey "${args.journey}" against ${BASE} (dimensions: ${[...DIMENSIONS].join(",")})`);
  const desktopResult = await runJourney(args.journey, journey, "desktop");
  if (DIMENSIONS.has("visual")) {
    await runJourney(args.journey, journey, "mobile");
  }

  const report = { journey: args.journey, base: BASE, steps: [], regressions: 0 };
  for (const [i] of journey.steps.entries()) {
    const n = i + 1;
    const oracleStep = readOracleStep(n);
    const candidateStep = readCandidateStep(n);
    const stepReport = { index: n, dimensions: {} };

    const is404 = oracleStep.step?.documentStatus === 404;
    const dev12Active = activeDeviations(n, "visual").some((d) => d.mechanism === "skip-on-404");

    if (DIMENSIONS.has("contract") && oracleStep.http.length > 0) {
      const devs = journeyDevs.filter((d) => d.dimension === "contract" && devAppliesToStep(d, args.journey, n)).map((d) => d.id);
      stepReport.dimensions.contract = compareHttp(oracleStep.http, candidateStep.http, devs);
    }

    if (DIMENSIONS.has("visual")) {
      if (is404 && dev12Active) {
        stepReport.dimensions.visual = { verdict: "accepted-deviation(DEV-12)", detail: "404 document — visual/a11y/text skipped" };
      } else {
        const main11 = activeDeviations(n, "visual").some(
          (d) => d.mechanism === "mask-selector" && d.selector === ".main-scroll",
        );
        const urlR = compareUrl(oracleStep.url, candidateStep.url);
        const textsR = compareTexts(oracleStep.texts, candidateStep.texts, main11);
        const ariaR = compareAria(oracleStep.aria, candidateStep.aria, main11);
        const desktopShotR = await compareScreenshots(oracleStep.screenshotDesktop, candidateStep.screenshotDesktop, n, "desktop");
        const mobileShotR = await compareScreenshots(oracleStep.screenshotMobile, candidateStep.screenshotMobile, n, "mobile");
        stepReport.dimensions.visual = { url: urlR, texts: textsR, aria: ariaR, screenshotDesktop: desktopShotR, screenshotMobile: mobileShotR };
      }
    }

    report.steps.push(stepReport);
  }

  if (DIMENSIONS.has("contract") && desktopResult.delta) {
    const oracleDbPath = join(oracleJourneyDir, "db.json");
    const oracleDb = existsSync(oracleDbPath) ? JSON.parse(readFileSync(oracleDbPath, "utf8")) : null;
    report.db = compareDb(oracleDb, desktopResult.delta);
  }

  const allVerdicts = [];
  for (const step of report.steps) {
    for (const dim of Object.values(step.dimensions)) {
      if (dim.verdict) allVerdicts.push(dim.verdict);
      else Object.values(dim).forEach((v) => v?.verdict && allVerdicts.push(v.verdict));
    }
  }
  (report.db ?? []).forEach((d) => allVerdicts.push(d.verdict));
  report.regressions = allVerdicts.filter((v) => v === "regression").length;

  const outDir = join(args.out, args.journey);
  mkdirSync(outDir, { recursive: true });
  writeFileSync(join(outDir, "report.json"), JSON.stringify(report, null, 2));
  writeFileSync(join(outDir, "report.md"), renderMarkdown(report));

  console.log(`compare.mjs: ${report.regressions} regression(s) — report at ${join(outDir, "report.md")}`);
  process.exit(report.regressions > 0 ? 1 : 0);
})().catch((err) => {
  console.error(err);
  process.exit(1);
});

function renderMarkdown(report) {
  const lines = [`# compare.mjs report — journey \`${report.journey}\``, "", `Base: ${report.base}`, "", "| Step | Dimension | Verdict | Detail |", "| --- | --- | --- | --- |"];
  for (const step of report.steps) {
    for (const [dimName, dim] of Object.entries(step.dimensions)) {
      if (dim.verdict) {
        lines.push(`| ${step.index} | ${dimName} | ${dim.verdict} | ${dim.detail ?? ""} |`);
      } else {
        for (const [sub, v] of Object.entries(dim)) {
          lines.push(`| ${step.index} | ${dimName}.${sub} | ${v.verdict} | ${v.detail ?? ""} |`);
        }
      }
    }
  }
  for (const d of report.db ?? []) {
    lines.push(`| db | ${d.table} | ${d.verdict} | ${d.detail} |`);
  }
  lines.push("", `**Regressions: ${report.regressions}**`);
  return lines.join("\n") + "\n";
}
