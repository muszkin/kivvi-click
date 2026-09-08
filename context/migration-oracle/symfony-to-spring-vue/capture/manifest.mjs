/** Builds manifest.json: hashes every oracle file and records tool versions and journey status. */
import { createHash } from "node:crypto";
import { readdirSync, readFileSync, statSync, writeFileSync, existsSync } from "node:fs";
import { execSync } from "node:child_process";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
const here = dirname(fileURLToPath(import.meta.url));
const oracleDir = resolve(here, "..");
const root = resolve(oracleDir, "../../..");
const scenarios = JSON.parse(readFileSync(join(here, "scenarios.json"), "utf8"));
const walk = (d) => readdirSync(d).flatMap((n) => { const p = join(d, n); return statSync(p).isDirectory() ? walk(p) : [p]; });
const files = walk(oracleDir).filter((p) => relative(oracleDir, p) !== "manifest.json").sort()
  .map((p) => ({ path: relative(oracleDir, p), sha256: createHash("sha256").update(readFileSync(p)).digest("hex") }));
const journeys = Object.entries(scenarios).map(([id, j]) => {
  const dir = join(oracleDir, "journeys", id);
  if (j.static) return { id, steps: 0, status: existsSync(join(dir, "contract.md")) ? "complete" : "blocked: contract.md missing", static: true };
  const captured = existsSync(join(dir, "steps")) ? readdirSync(join(dir, "steps")).length : 0;
  const ok = captured === j.steps.length && j.steps.every((_, i) => ["desktop.png", "mobile.png", "a11y.json", "texts.json", "url.txt", "http.jsonl"].every((f) => existsSync(join(dir, "steps", String(i + 1), f))));
  return { id, steps: j.steps.length, status: ok ? "complete" : `blocked: ${captured}/${j.steps.length} steps captured` };
});
const sh = (c) => execSync(c, { encoding: "utf8", cwd: root }).trim();
const manifest = {
  migration: "symfony-to-spring-vue",
  source_sha: process.env.ORACLE_SOURCE_SHA ?? sh("git rev-parse HEAD"),
  product_code_sha: "91f8f85302a8084346eb5ce0b0b04d7ac143cd08",
  captured_at: new Date().toISOString(),
  base_url: process.env.ORACLE_BASE_URL ?? "https://localhost:18443",
  tool_versions: { browser: sh("google-chrome --version"), runner: `playwright ${JSON.parse(readFileSync(join(root, "tests/e2e/node_modules/playwright/package.json"), "utf8")).version}`, node: process.version, php_image: sh("docker compose -p kivvi-oracle images php --format '{{.Repository}}:{{.Tag}}' 2>/dev/null || echo unknown"), postgres: sh("docker compose -p kivvi-oracle exec -T database psql -U app -d app -tAc 'select version()' 2>/dev/null || echo unknown") },
  viewports: { desktop: "1440x900", mobile: "390x844" },
  fixed_browser_time: "2026-09-08T12:00:00Z",
  files,
  journeys,
};
writeFileSync(join(oracleDir, "manifest.json"), JSON.stringify(manifest, null, 2) + "\n");
console.log(JSON.stringify({ files: files.length, journeys: journeys.map((j) => `${j.id}:${j.status}`) }, null, 1));
