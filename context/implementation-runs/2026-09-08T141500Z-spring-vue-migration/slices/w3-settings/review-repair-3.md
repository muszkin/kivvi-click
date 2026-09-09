# Review — w3-settings / repair-3

PASS

Diff reviewed: `32a68311594e611aaa8eaa0803bc72bba7d735a9...0a337f9c287aa997fb6b95d6acb713cfbec33d79`
Worktree: `/home/muszkin/work/kivvi-click-wt/w3-settings-r3`, branch `migration/wave-3/settings-repair3`.

## Findings

### 1. Test-only — PASS
`git diff --stat` shows exactly 3 files, all tests: `backend/src/test/java/click/kivvi/SettingsApiIT.java`,
`frontend/test/integration/LocaleToggle.spec.ts` (new), `frontend/test/integration/SettingsView.spec.ts`.
No `src/main`/product code touched.

### 2. B01 traceability — PASS
`SettingsApiIT.java:53-194` adds one `B01`-labelled real-HTTP case per settings row (account, sites,
team, providers, api, notifications, billing, gdpr) — document route 200 + `contains("<html")`
(`SettingsApiIT.java:59` etc., matching `AutomationsApiIT.java:43`'s pattern) plus the `/api/v1/...`
payload's `tabSubtitle` equal to the byte-identical value from the old `SettingsCatalog::SUBTITLES`
(verified against `src/Panel/Content/SettingsCatalog.php:30-40` on `main`). Verified `SettingsController.java`'s
`document()` method (`GET /{locale}/settings/{tab}`) delegates to `spaDocumentService.render(...)` —
the same generic, tab-agnostic SPA shell used everywhere else — so no per-tab marker exists in the raw
HTML at all; `tabSubtitle` is genuinely the strongest server-side marker available, and the Javadoc at
`SettingsApiIT.java:37-51` states this honestly rather than overclaiming parity with the old
`.card-title` selector. Judged acceptable.

Frontend: `SettingsView.spec.ts` relabels 5 of its existing tests with `B01` — `account`/`team`/
`billing`/`gdpr` marker-text tests (lines 322, 337, 373, 391) reproducing `PanelPagesTest`'s exact
`.card-title` strings, plus the page-title test (line 397). All run through `mountAt()` (line 257),
which uses a real `createRouter`+`routes` and real `i18n`, stubbing only `fetch` — a qualifying
integration test. No assertions changed, confirmed by diff. Minor: only 4/8 rows carry a frontend `B01`
label (sites/providers/api/notifications still untagged, though their tests exist and pass); the
packet's R-A frontend clause says "test(s) ... with B01", not "each row" (unlike its backend clause),
so this reads as in-scope-complete, not a gap — noting for awareness only, not a blocker.

### 3. LocaleToggle.spec.ts — PASS
`LocaleToggle.spec.ts:38-45` mounts the real `AppLayout` with a real `createRouter`/`routes` and real
`Pinia`/`i18n`; `Topbar.vue:63` is the only `<a class="tb-btn">` element (the other two `tb-btn`s are
`<button>`), so `find("a.tb-btn")` (line 51) unambiguously targets the locale toggle. Mutation test
performed: removed `defaultParams: { tab: "account" }` from `routes.ts:143` in the worktree — the
`/pl/settings/account` case failed (`expected '/en/settings/account' to be '/en/settings'`), the other
two cases stayed green as expected; reverted via `git checkout`, `git status --porcelain` clean
afterward.

### 4. Naming/hygiene — PASS
Plain `B01` (no slash-combined sibling tag) matches the convention already used where a row has no
second in-scope behaviour riding along (`CampaignsApiIT.java:36`, `FeedsApiIT.java:36`,
`EventsApiIT.java:34`, `LandingApiIT.java:39`) — `B01/B26`-style combining is only used where the same
test also proves a second tagged behaviour (`AutomationsApiIT.java:38`), which doesn't apply here.
Commit `0a337f9`: `test: label B01 settings coverage at integration level (#settings)` — Conventional
Commits, English, single line, no trailers, no AI/co-author mentions.

### 5. Report accuracy — PASS
Re-ran all gates independently in the worktree, results match `worker-report.md`'s "## Repair-3" table
exactly:
- `./mvnw -q verify` (JAVA_HOME=jdk-25): exit 0, all 12 `*IT.java` classes green, 65 IT tests total incl.
  `SettingsApiIT` 12/12 (`target/failsafe-reports/click.kivvi.SettingsApiIT.txt`).
- `npm run test -- --run`: 125/125, 21 files.
- `npm run test:integration -- --run`: 86/86, 15 files (incl. new `LocaleToggle.spec.ts`, 3/3).
- `npm run lint`: 0 errors, 6 pre-existing warnings (unrelated files: `HookRow.vue`, `FeedCard.vue`,
  `ApiTab.vue`).
- `npm run typecheck`: clean.
- `npm run format:check`: clean.
- `compare.mjs`/Playwright: correctly skipped (test-only change, packet-authorized).

`git status --porcelain` empty at end of review; no throwaway files left behind.
