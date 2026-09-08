/**
 * Oracle capture for the Symfony -> Spring Boot + Vue 3 SPA migration.
 *
 * Runs every browser journey from scenarios.json twice (desktop 1440x900, mobile 390x844)
 * against ORACLE_BASE_URL and writes, per step: screenshot, accessibility tree, visible
 * texts, final URL and the normalized HTTP recording. Database row counts are snapshotted
 * before and after each journey. The same file drives later verification runs.
 *
 * Usage (repository root, oracle stack up):
 *   ORACLE_BASE_URL=https://localhost:18443 ORACLE_COMPOSE="docker compose -p kivvi-oracle" \
 *   node context/migration-oracle/symfony-to-spring-vue/capture/capture.mjs [journey ...]
 */
import { createRequire } from "node:module";
import { execSync } from "node:child_process";
import { mkdirSync, writeFileSync, readFileSync, rmSync, existsSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, "../../../..");
const oracleDir = resolve(here, "..");
const require = createRequire(join(root, "tests/e2e/package.json"));
const { chromium } = require("playwright");

const BASE = process.env.ORACLE_BASE_URL ?? "https://localhost:18443";
const COMPOSE = process.env.ORACLE_COMPOSE ?? "docker compose -p kivvi-oracle";
const FIXED_TIME = new Date("2026-09-08T12:00:00Z");
const VIEWPORTS = { desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } };
const RUN_ID = (process.env.ORACLE_RUN_ID ?? Date.now().toString(36)).toLowerCase();
const SETTLE_MS = 400;

const scenarios = JSON.parse(readFileSync(join(here, "scenarios.json"), "utf8"));
const normalize = JSON.parse(readFileSync(join(here, "normalize.json"), "utf8"));
const only = process.argv.slice(2);

const textRules = normalize.text_rules.map((r) => ({ re: new RegExp(r.pattern, "g"), to: r.replace }));
const ignorePaths = normalize.http_rules.find((r) => r.ignore_paths).ignore_paths.map((p) => new RegExp(p));
const stripReq = new Set(normalize.http_rules.find((r) => r.strip_request_headers).strip_request_headers);
const stripRes = new Set(normalize.http_rules.find((r) => r.strip_response_headers).strip_response_headers);

const applyText = (s) => textRules.reduce((acc, r) => acc.replace(r.re, r.to), s);
let runId = RUN_ID;
const sub = (s) => (typeof s === "string" ? s.replaceAll("__RUN__", runId) : s);
const subDeep = (v) => (Array.isArray(v) ? v.map(subDeep) : v && typeof v === "object" ? Object.fromEntries(Object.entries(v).map(([k, x]) => [k, subDeep(x)])) : sub(v));

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

function dbCounts() {
  const sql = "select 'sessions', count(*) from sessions union all select 'cache_items', count(*) from cache_items union all select 'messenger_messages', count(*) from messenger_messages";
  const out = execSync(`${COMPOSE} exec -T database psql -U app -d app -tAc "${sql}"`, { cwd: root, encoding: "utf8" });
  const counts = Object.fromEntries(out.trim().split("\n").filter(Boolean).map((l) => l.split("|")).map(([k, v]) => [k, Number(v)]));
  const uploads = Number(execSync(`${COMPOSE} exec -T php sh -c "ls var/import 2>/dev/null | wc -l"`, { cwd: root, encoding: "utf8" }).trim());
  return { ...counts, "var/import files": uploads };
}

async function settle(page) {
  // No networkidle: the Mercure SSE subscription keeps one request open forever.
  await page.waitForLoadState("load").catch(() => {});
  await page.evaluate(() => document.fonts?.ready).catch(() => {});
  await page.waitForTimeout(SETTLE_MS);
}
// Publishing before the hub accepted the subscription loses the event (Mercure has no replay).
async function ensureLiveStream(page) {
  const stream = page.locator("#event-stream[data-controller]");
  if ((await stream.count()) === 0) return;
  await page.waitForFunction(() => document.querySelector("#event-stream")?.getAttribute("data-stream-state") === "live", null, { timeout: 15000 });
  await page.waitForTimeout(500);
}

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
    await ensureLiveStream(page);
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
    if (s.expectStatus !== undefined && response.status() !== s.expectStatus) {
      throw new Error(`step ${record.index}: expected ${s.expectStatus}, got ${response.status()} for POST ${s.post}`);
    }
  }
  if (s.waitUrl) await page.waitForURL((u) => u.href.includes(s.waitUrl), { timeout: 15000 });
  if (s.waitAttr) await page.locator(s.waitAttr[0]).first().waitFor({ timeout: 15000 }).then(() =>
    page.waitForFunction(([sel, attr, val]) => document.querySelector(sel)?.getAttribute(attr) === val, s.waitAttr, { timeout: 15000 }));
  if (s.waitText) await page.locator(s.waitText[0]).filter({ hasText: s.waitText[1] }).first().waitFor({ timeout: 15000 });
  if (s.goto && s.expectStatus !== undefined && record.documentStatus !== s.expectStatus) {
    throw new Error(`step ${record.index}: expected document status ${s.expectStatus}, got ${record.documentStatus}`);
  }
  if (s.evalStyle) {
    record.computedStyle = { selector: s.evalStyle[0], property: s.evalStyle[1], value: await page.locator(s.evalStyle[0]).first().evaluate((el, p) => getComputedStyle(el)[p], s.evalStyle[1]) };
  }
  return apiEntry;
}

async function runJourney(id, journey, viewportName) {
  runId = RUN_ID + (viewportName === "mobile" ? "m" : "d");
  const browser = await chromium.launch({ channel: "chrome", headless: true });
  const context = await browser.newContext({ viewport: VIEWPORTS[viewportName], ignoreHTTPSErrors: true, reducedMotion: "reduce", locale: "pl-PL", timezoneId: "Europe/Warsaw" });
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
    if (/json/.test(entry.contentType ?? "")) entry.body = applyText((await response.text().catch(() => "")).slice(0, 4000));
    http.push(entry);
  });

  const dir = join(oracleDir, "journeys", id);
  const isDesktop = viewportName === "desktop";
  const before = isDesktop ? dbCounts() : null;
  const stepRecords = [];
  for (const [i, step] of journey.steps.entries()) {
    const n = i + 1;
    const stepDir = join(dir, "steps", String(n));
    mkdirSync(stepDir, { recursive: true });
    http = [];
    const record = { index: n, step: subDeep(step) };
    const apiEntry = await runStep(page, context, step, record);
    await settle(page);
    const masks = normalize.screenshot_masks.map((m) => page.locator(m.selector));
    await page.screenshot({ path: join(stepDir, `${viewportName}.png`), fullPage: false, mask: masks, maskColor: "#ff00ff", animations: "disabled" });
    if (isDesktop) {
      const aria = await page.locator("body").ariaSnapshot();
      const texts = (await page.locator("body").innerText()).split("\n").map((l) => applyText(l.trim())).filter(Boolean);
      writeFileSync(join(stepDir, "a11y.json"), JSON.stringify({ aria: applyText(aria) }, null, 2));
      writeFileSync(join(stepDir, "texts.json"), JSON.stringify(texts, null, 2));
      writeFileSync(join(stepDir, "url.txt"), normalizeUrl(page.url()) + "\n");
      const lines = [...(apiEntry ? [apiEntry] : []), ...http].map((e) => JSON.stringify(e)).join("\n");
      writeFileSync(join(stepDir, "http.jsonl"), lines + (lines ? "\n" : ""));
      writeFileSync(join(stepDir, "step.json"), JSON.stringify({ ...record, title: document_title_safe(await page.title()) }, null, 2));
    }
    stepRecords.push(record);
    process.stdout.write(`  ${id} [${viewportName}] step ${n}/${journey.steps.length} ok\n`);
  }
  if (isDesktop) {
    const after = dbCounts();
    const delta = Object.fromEntries(Object.keys(after).map((k) => [k, after[k] - before[k]]));
    writeFileSync(join(dir, "db.json"), JSON.stringify({ before, after, delta, note: "row counts from the seeded oracle database; delta is the journey's persisted side effect" }, null, 2));
  }
  await browser.close();
  return stepRecords;
}
const document_title_safe = (t) => applyText(t);

function scenarioMarkdown(id, journey) {
  const lines = [`# Journey: ${id}`, "", journey.title, "", `Source tests: ${journey.spec}`, "", `Run id placeholder: \`__RUN__\` (per-run value normalized to \`<RUN-ID>\`).`, "", "## Steps", ""];
  journey.steps.forEach((s, i) => lines.push(`${i + 1}. \`${JSON.stringify(s)}\``));
  if (journey.static) lines.push("No browser steps: this journey is a background job. Its contract is recorded in `contract.md`.");
  return lines.join("\n") + "\n";
}

(async () => {
  const ids = Object.keys(scenarios).filter((id) => only.length === 0 || only.includes(id));
  for (const id of ids) {
    const journey = scenarios[id];
    const dir = join(oracleDir, "journeys", id);
    if (existsSync(join(dir, "steps"))) rmSync(join(dir, "steps"), { recursive: true });
    mkdirSync(dir, { recursive: true });
    writeFileSync(join(dir, "scenario.md"), scenarioMarkdown(id, journey));
    if (journey.static) { process.stdout.write(`  ${id} static (no browser)\n`); continue; }
    await runJourney(id, journey, "desktop");
    await runJourney(id, journey, "mobile");
  }
  process.stdout.write(`capture done, run id ${RUN_ID}\n`);
})().catch((err) => { console.error(err); process.exit(1); });
