/** Static inventory of the pre-migration stack: routes, components, endpoints, i18n keys, session state, flags. */
import { readdirSync, readFileSync, statSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
const here = dirname(fileURLToPath(import.meta.url));
const oracleDir = resolve(here, "..");
const root = resolve(oracleDir, "../../..");
const walk = (d) => readdirSync(d).flatMap((n) => { const p = join(d, n); return statSync(p).isDirectory() ? walk(p) : [p]; });
const read = (p) => readFileSync(join(root, p), "utf8");
// routes: class-level prefix + method-level routes
const routes = [];
for (const file of walk(join(root, "src/Controller")).filter((p) => p.endsWith(".php"))) {
  const src = readFileSync(file, "utf8");
  const cls = src.match(/#\[Route\('([^']*)'[^\]]*\)\]\s*(?:final )?class/);
  const prefix = cls ? cls[1] : "";
  const condition = (src.match(/condition: "([^"]+)"/) ?? [])[1] ?? null;
  for (const m of src.matchAll(/#\[Route\(\s*'([^']*)'(?:,\s*name: '([^']*)')?[^\]]*?(?:methods: \[([^\]]*)\])?[^\]]*\)\]\s*public function (\w+)/gs)) {
    routes.push({ controller: relative(root, file), path: (prefix + m[1]) || "/", name: m[2] ?? null, methods: m[3] ? m[3].replace(/['\s]/g, "").split(",") : ["ANY"], action: m[4], condition, auth: "none (no firewall)", renders: /render\('([^']+)'/.exec(src.slice(m.index, m.index + 1500))?.[1] ?? null });
  }
}
// components with usage counts
const componentFiles = walk(join(root, "templates/components")).map((p) => relative(join(root, "templates"), p));
const allTwig = walk(join(root, "templates")).concat(walk(join(root, "src"))).filter((p) => /\.(twig|php)$/.test(p)).map((p) => readFileSync(p, "utf8")).join("\n");
const components = componentFiles.map((c) => ({ template: c, tier: c.split("/")[1], usages: (allTwig.match(new RegExp(`include\\('${c.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}'`, "g")) ?? []).length }));
// endpoints consumed by the browser / tracker (non-page contracts)
const endpoints = [
  { path: "/collect", method: "POST", consumer: "tracker script k.js (planned), e2e", contract: "JSON {idempotency_id,type,detail?,customer_id?,customer_name?,site?,occurred_at?} -> 202 {status:accepted} | 200 {status:duplicate} | 400 {error}", preserve: "exact" },
  { path: "/preferences/theme", method: "POST", consumer: "assets/app.ts", contract: "JSON {theme} -> 200 {theme}", preserve: "exact" },
  { path: "/preferences/sidebar", method: "POST", consumer: "assets/app.ts", contract: "JSON {state} -> 200 {state}", preserve: "exact" },
  { path: "/import/upload", method: "POST", consumer: "assets/controllers/upload.ts", contract: "multipart file -> 302 to /{locale}/import/2", preserve: "status+redirect target" },
  { path: "/.well-known/mercure?topic=/accounts/{id}/events", method: "GET (SSE)", consumer: "assets/controllers/event-stream.ts", contract: "event data JSON {html} per published event", preserve: "exact (hub kept)" },
  { path: "/{locale}/login", method: "POST", consumer: "login form", contract: "form _username,_password -> 302 dashboard | 200 form with error", preserve: "behaviour; response shape may become JSON (DEV-4)" },
  { path: "/{locale}/logout", method: "POST", consumer: "none in UI", contract: "-> 302 login", preserve: "behaviour" },
  { path: "{editor}/blocks", method: "POST", consumer: "assets/controllers/editor.ts", contract: "no server route exists (404)", preserve: "Out of scope (R6)" },
];
// i18n
const i18n = {};
for (const locale of ["pl", "en"]) {
  const keys = [];
  const walkYaml = (obj, prefix) => Object.entries(obj).forEach(([k, v]) => typeof v === "object" && v ? walkYaml(v, prefix + k + ".") : keys.push(prefix + k));
  const yaml = read(`translations/messages.${locale}.yaml`);
  // minimal YAML reader for the flat 2-level catalogue
  const obj = {}; let section = null;
  for (const line of yaml.split("\n")) {
    const top = /^([a-z_]+):\s*$/.exec(line); const leaf = /^\s+([a-z_0-9]+):\s*(.+)$/.exec(line); const flat = /^([a-z_]+):\s*(.+)$/.exec(line);
    if (top) { section = top[1]; obj[section] = {}; } else if (leaf && section) obj[section][leaf[1]] = leaf[2]; else if (flat) obj[flat[1]] = flat[2];
  }
  walkYaml(obj, "");
  i18n[locale] = { file: `translations/messages.${locale}.yaml`, keys };
}
const stores = [
  { store: "session (Postgres `sessions`)", keys: ["panel.identity", "panel.theme", "panel.sidebar", "import.file_name", "import.file_path"], owner: "src/Panel/PanelIdentity.php, PanelPreferences.php, ImportUploadStorage.php" },
  { store: "cache.app (Postgres `cache_items`, UNLOGGED)", keys: ["event.seen.<xxh128(idempotency_id)> TTL 86400", "scheduler state"], owner: "src/Tracking/EventIngestion.php, src/Schedule.php" },
  { store: "messenger (Postgres `messenger_messages`)", keys: ["async", "failed"], owner: "config/packages/messenger.yaml" },
  { store: "filesystem var/import", keys: ["<32hex>.<ext>"], owner: "src/Panel/ImportUploadStorage.php" },
];
const flags = [{ note: "no feature flags exist; APP_ENV in ['dev','test'] gates /_storybook" }];
const templates = walk(join(root, "templates/pages")).map((p) => relative(join(root, "templates"), p));
mkdirSync(join(oracleDir, "inventory"), { recursive: true });
const out = { routes, components, endpoints, i18n, stores, flags, templates };
for (const [name, value] of Object.entries(out)) writeFileSync(join(oracleDir, "inventory", `${name}.json`), JSON.stringify(value, null, 2) + "\n");
console.log({ routes: routes.length, components: components.length, endpoints: endpoints.length, i18n_pl: i18n.pl.keys.length, i18n_en: i18n.en.keys.length, page_templates: templates.length });
