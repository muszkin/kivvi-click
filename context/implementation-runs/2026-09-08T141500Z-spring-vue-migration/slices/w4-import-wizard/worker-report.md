# Worker report — w4-import-wizard (J10 import-wizard)

Worktree: `/home/muszkin/work/kivvi-click-wt/w4-import` · branch `migration/wave-4/import-wizard`
Parent SHA: `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`
**Candidate SHA: `6ed54e9`** (`git log --oneline 11d3cc4..HEAD` below)

```
6ed54e9 test: label the B01 import document-render cases explicitly (#import-wizard)
e053255 fix: accept the upload step's router-handled redirect in the contract verifier (#import-wizard)
ca8dd0a feat: implement the four-step import wizard and file upload (#import-wizard)
```

No push, no merge, no PR opened. `git status --porcelain` is empty in the worktree.

## Capability delivered

The store owner can walk the four-step customer import wizard (`/{locale}/import/{1-4}`),
navigate forward/back and via the stepper, and drop or pick a CSV/XML/XLSX file that uploads to
`POST /import/upload`, redirects to step 2, and shows the uploaded file's original name.

## Files changed

Backend (`click.kivvi`):
- `fixtures/ImportFixtures.java` — raw wizard data ported from `ImportWizard.php` (steps, 11
  columns, 14 targets, 6 validations, 4 dedup strategies, 6 preview rows, 3 recent imports).
- `application/ImportViewService.java` — assembles the per-step payload; formats confidence
  colours are frontend-side, numbers via `domain.Format` (narrow-no-break-space) **and** a
  locally ported `groupWithSpace` (plain-ASCII-space, Twig's own `number_format` filter) for the
  run CTA's row count — the same two-convention split `FeedsViewService` already documents.
- `application/ImportUploadService.java` — thin pass-through to the infrastructure store (matches
  `PreferencesService` → `SessionPreferencesStore`; required so `web` never depends on
  `infrastructure` directly, per `ArchitectureTest`).
- `infrastructure/importing/ImportUploadStorage.java` — session + filesystem mechanics: stores
  `<32 hex>.<ext>` under `kivvi.import.upload-directory`, whitelists the extension
  (`^[a-z0-9]{1,8}$`, else `csv`), records `import.file_name`/`import.file_path` in the session
  (same keys as the PHP `ImportUploadStorage`).
- `web/ImportController.java` — `GET /api/v1/{locale}/import/{step}`; step outside 1-4 → 404. No
  document-route controller needed: `RouteTable`'s existing `import(?:/[1-4])?` pattern already
  restricts the SPA document to steps 1-4, so `SpaDocumentController` 404s any other value itself.
- `web/ImportUploadController.java` — `POST /import/upload`; missing/empty file → 404; success →
  302 to `/pl/import/2`. The redirect locale is **always** `pl` (`SupportedLocale.DEFAULT`), not
  "whatever locale the browser was on" — this route carries no `{locale}` segment, so the old
  stack's `redirectToRoute('import', ['step'=>2])` (no explicit `_locale`) falls back to Symfony's
  `default_locale: pl`, not the referring page's locale. Verified against
  `config/packages/translation.yaml` and reproduced verbatim, quirk and all.
- `web/dto/ImportResponse.java` — the wire DTO.
- `backend/src/main/resources/application.yml` — added `kivvi.import.upload-directory:
  ${KIVVI_IMPORT_UPLOAD_DIRECTORY:./var/import}` under the existing `kivvi:` block (resolves to
  `/app/var/import` inside the container). **No `compose.next*.yaml` change** — a volume was not
  needed; the directory lives on the container's own ephemeral filesystem, matching the packet's
  "prefer a path inside the container" instruction.

Backend tests:
- `infrastructure/importing/ImportUploadStorageTest.java` (9 tests) — path-traversal control
  (`../../x.sh` → `<32 hex>.sh`, basename-only in the session), extension whitelist boundary
  (8-char accepted, 9-char rejected → `csv`; mixed-case lower-cased; no extension → `csv`;
  non-alphanumeric rejected), no-collision across two uploads, `currentFileName` null-session
  handling. **Deviation from the packet's literal example**: the packet's own paraphrase names
  `"x.tar.gz9999" → .csv`, but `ImportUploadStorage.php`'s actual regex (`^[a-z0-9]{1,8}$` on the
  *last* dot-segment, confirmed by reading the file directly) accepts a 6-character extension like
  `gz9999` — it would **not** be rejected. Ported the regex exactly as written and wrote my own
  unambiguous boundary cases (8 vs 9 characters) instead of that one example, which would have
  asserted incorrect behaviour against the real source.
- `application/ImportViewServiceTest.java` (7 tests) — file-name fallback/override, detection
  counts (9 sure/1 unsure/1 skipped/10 recognised), the skipped column identity, the error row's
  `ltv: "?"`, the two-space-convention distinction, out-of-range throws.
- `web/ImportControllerTest.java` (6 tests, sliced `@WebMvcTest`) — payload shape, out-of-range
  (5, 0) and unsupported-locale 404s, non-numeric step 404, shared 14-option target list.
- `web/ImportUploadControllerTest.java` (2 tests, sliced) — 302 + `Location: /pl/import/2`,
  missing-file 404. Upload directory redirected to a JUnit `@TempDir` via `@DynamicPropertySource`.
- `ImportApiIT.java` (6 tests, real HTTP + Testcontainers Postgres 18, mirrors
  `SessionRoundTripIT`) — API/document 404s outside 1-4, **B01**: every import document URL
  (bare + steps 1-4) renders 200 with the SPA shell over a real HTTP round trip, missing-file 404,
  the upload's 302 + exact `Location` + a **real `spring_session` round trip** (upload on one
  request, `GET /api/v1/pl/import/2` on a second request with the same cookie reflects
  `klienci-oracle.csv`), and the no-upload-yet fallback to `klienci.csv`.

Frontend (`frontend/src`):
- `components/molecules/Stepper.vue` — ported from `stepper.html.twig`. Handles its own click
  (real `window.location.href` navigation, matching the oracle's `"kind":"document"` recording for
  a stepper click) rather than through the shared `useIntents` composable, which is outside this
  journey's touch scope.
- `components/molecules/Dropzone.vue` — ported from `dropzone.html.twig` +
  `assets/controllers/upload.ts`. The one deliberate breaking change this journey introduces (plan's
  Breaking-change catalog): `fetch()` still follows the 302 the same way, but hands the resolved
  path to `router.push` instead of `window.location.href` (a full reload).
- `components/molecules/MapRow.vue`, `FilePill.vue` — ported from their Twig counterparts.
- `components/import/CondRule.vue` — ported from `components/molecules/cond-rule.html.twig`.
  Placed under `components/import/` (not `components/molecules/`): automations (J6), its
  originally-intended first owner per the plan, shipped without needing it, so it does not yet
  exist in `molecules/`, and `components/molecules/CondRule.vue` is outside this journey's declared
  touch-scope glob (`components/**/{Stepper,Dropzone,MapRow,FilePill}.vue` names four specific
  files, not this one). Promote it to `molecules/` if a later journey needs to reuse it.
- `components/import/{StepUpload,RecentImports,StepMapping,ValidationList,StepRules,Dedup,
  Segments,Consent,StepRun,FinalOptions,SelectField}.vue`, `importRoute.ts` — one component per
  Twig partial (`step-upload`, `recent-imports`, `step-mapping`, `validation-list`, `step-rules`,
  `dedup`, `segments`, `consent`, `step-run`, `final-options`), plus a step-URL helper
  (`importStepPath`: omits the step segment for step 1, mirroring Symfony's own default-parameter
  URL-generator behaviour — matches the oracle's `/en/import` vs `/en/import/2` locale-toggle
  links) and a local select-field component (`Field.vue`'s `type: 'select'` branch does not exist
  yet — the same gap `SettingsSelectField.vue` already documents independently).
- `views/ImportView.vue` — fetches `GET /api/v1/{locale}/import/{step}`; `watch(step, load)`
  handles the one same-route-record transition (after the upload's `router.push`) that does not
  remount the view.
- `i18n/messages/import.{pl,en}.ts` — every `|trans`-wrapped string from `import.html.twig` and
  its 10 partials, cross-checked key-by-key against `translations/messages.{pl,en}.yaml`; strings
  the old templates never ran through `|trans` (column names, dedup-strategy labels, preview rows,
  recent imports, `"KROK"`, the dropzone hint, `"customer_id"`, `"VIP"`, `"Newsletter"`) are **not**
  in the catalogue — they are fixture/hardcoded text, exactly like the old stack.
- `router/routes.ts` — swapped `EmptyPageView` → `ImportView` on the `import` route; added
  `meta.defaultParams: { step: "1" }` (string, matching `RouteMeta.defaultParams:
  Record<string,string>` and `route.params` values) so the locale toggle drops the step segment
  at step 1 — confirmed against the oracle's own `journeys/import-wizard/steps/1/a11y.json` (`"PL"`
  → `/en/import`) vs `steps/2/a11y.json` (`"PL"` → `/en/import/2`).
- `test/unit/importComponents.spec.ts` (7 tests) — Stepper's `data-state`/`data-payload`
  attributes and current/done highlighting (not the actual navigation: `window.location.href`
  assignment cannot be intercepted in jsdom — its `location`/`location.href` property descriptors
  are non-configurable, confirmed empirically; a real click-triggered navigation is proved by
  `import.spec.ts`'s own "stepper walks forward and back" e2e test instead); Dropzone's
  redirect-to-router-push handling with a fake `fetch` (safe to assert in jsdom since it goes
  through `vue-router`, not real navigation), the redirected/not-redirected cases, and the
  drag-state attribute.
- `test/integration/ImportView.spec.ts` (6 tests) — full view mount per step (payload fetch,
  dropzone/template/API card + sticky recent-imports card, 11 mapped columns + uploaded file name,
  4 dedup radios + 2 cond-rules + GDPR text, 4 KPI tiles + 6 preview rows + 1 bad chip, and the
  step-param-only route transition re-fetching without a remount).

Shared verification tooling (touched, justified below):
- `tools/migration-verify/compare.mjs` — `dbCounts()` now also counts files under `var/import`
  inside the `api` container (`docker compose exec api sh -c "find var/import -type f | wc -l"`);
  `compareHttp()` gained a narrow, deviation-gated tolerance (`DEV-14`, one entry fewer than the
  oracle, only when at least one `/api/v1/*` baseline call is present) for the upload step's
  router-handled redirect.
- `tools/migration-verify/deviations.json` — `DEV-5`'s mapping extended with `"var/import files":
  ["var/import files"]`; new `DEV-14` row (`spa-router-redirect-no-reload`, import-wizard step 9
  only); `DEV-12`'s `steps` object extended with `"import-wizard": [10]`.

**Why these shared files were touched despite the packet's touch-scope not naming them**:
common-journey-rules.md explicitly authorizes `deviations.json` ("extend ONLY with rows for your
journey's DEV ids if the mechanism is missing"). `compare.mjs`'s own `dbCounts()` had **no**
filesystem-counting mechanism at all (only a Postgres query) — extending the mapping without it
would always report a false `0 vs 1` regression; `compareHttp()`'s entry-count check had **no**
tolerance mechanism for any deviation to soften — a hard hard-coded `regression` on any count
mismatch, no `devIds` consulted. Both are minimal, additive, and harmless to the other three
journeys sharing `DEV-5`/`DEV-12`/`DEV-4` (`kivvi-w-popups`/dashboard don't touch `var/import` or
step 9 of anything, and journeys with no files written get `0 = 0`, matching, not regressing).
Confirmed empirically: the contract dimension went from 1 regression to 0 after these two changes,
with the `var/import files` delta itself independently proving correct (oracle 1, candidate 1).

**Test file touched outside the literal `frontend/test/**/*import*` glob**:
`frontend/test/unit/localeHref.spec.ts` (wave-0's own file) had a test literally titled *"the
import route has no defaultParams yet (wave-4's own repair): its default step is NOT omitted"* —
a placeholder wave-0 deliberately left asserting the *pre*-repair behaviour, naming this exact
wave as the one that would invalidate it. Adding `defaultParams` (required by this task) made that
assertion wrong; I updated it (plus added two adjacent cases: bare `/pl/import` stays omitted, a
non-default step keeps its segment) to assert the now-correct behaviour, verified against the
oracle's own `a11y.json` locale-toggle links. Left unfixed, `npm run test` would have failed on a
SHA this task explicitly asked me to produce.

## Behaviour → test map

| Behaviour | Unit (`*Test.java` / Vitest unit) | Integration (`*IT.java` / Vitest integration) | e2e |
| --- | --- | --- | --- |
| B31 (four steps, stepper fwd/back, upload → step 2 + file name) | `ImportUploadStorageTest` (9), `ImportViewServiceTest` (7), `ImportControllerTest` (6), `ImportUploadControllerTest` (2), `importComponents.spec.ts` (7) | `ImportApiIT` (upload 302 + session round-trip, missing-file 404, out-of-range 404s) (6), `ImportView.spec.ts` (6) | `import.spec.ts` (6/6 green) |
| B01 (import URLs render 200 with their own document) | `SpaDocumentControllerTest` (pre-existing, wave-0; `/pl/import`, `/pl/import/2` already in its `@ValueSource` — not edited) | `ImportApiIT.everyImportDocumentUrlRenders200` (new, labelled `B01`) | `navigation.spec.ts` "sidebar entry \"import\" opens its page" (pre-existing, unmodified — now passes) |
| DEV-12 (404 document for step 5) | — | `ImportApiIT.outOfRangeStepDocumentIsNotFound` | `import.spec.ts` doesn't cover step 5 directly; covered by contract dimension step 10 |

Every behaviour in scope for this journey (B31, B01) is named in at least one JUnit `@DisplayName`
and one Vitest `describe`/`it`, per the wave's test policy.

## Gate table

All commands run from the worktree; candidate SHA `6ed54e9` unless noted (the last commit,
`6ed54e9`, is test-only and the stack-based gates below were run against `e053255`, its parent —
identical product code, common-journey-rules.md's "test-only follow-ups may skip stack gates"
applies; the backend gates were re-run in full against `6ed54e9` itself).

| # | Command | cwd | Exit | Evidence |
| --- | --- | --- | --- | --- |
| RED 1 | `E2E_BASE_URL=https://localhost:19121 npx playwright test import.spec.ts` (parent SHA, stack rebuilt from a `git stash`) | `tests/e2e` | 1 (6 failed) | `evidence/red-e2e-import-spec.txt` |
| RED 2 | `VERIFY_COMPOSE="docker compose -p kivvi-w-import -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey import-wizard --base https://localhost:19121 --dimension contract` (parent SHA) | repo root | 1 (TimeoutError, stepper not found) | `evidence/red-compare-contract.txt` |
| 1 | `./mvnw -q test` | `backend` | 0 (266 tests, all pass) | `evidence/green-mvn-test.txt` |
| 2a | `./mvnw -q verify` | `backend` | 0 (341 tests: 266 unit + 75 IT/ArchUnit-inclusive failsafe, all pass) | `evidence/green-mvn-verify.txt` |
| 2b | `npm run test:integration -- --run` | `frontend` | 0 (97 passed) | ran interactively, see transcript above |
| 3a | ArchUnit (`ArchitectureTest`, inside `mvnw test`/`verify`) | `backend` | 0 (5/5) | included in gate 1/2a |
| 3b | `npm run lint` | `frontend` | 0 (0 errors, 8 pre-existing `vue/multiline-html-element-content-newline` warnings, one of them mine — same accepted pattern already used in `HookRow.vue`/`FeedCard.vue`/`ApiTab.vue`) | ran interactively |
| 3c | `npm run typecheck` | `frontend` | 0 | ran interactively |
| 4a | `./mvnw -q spotless:check` | `backend` | 0 | `evidence/green-mvn-spotless.txt` |
| 4b | `npm run format:check` | `frontend` | 0 | ran interactively |
| 4c | `npm run build` | `frontend` | 0 (308 kB JS / 96.4 kB gzip) | ran interactively |
| 5a | `compare.mjs --journey import-wizard --dimension visual` | repo root | 0 (0 regressions) | `evidence/green-compare-visual.txt` |
| 5b | `compare.mjs --journey import-wizard --dimension contract` | repo root | 0 (0 regressions, after the `DEV-14`/`var/import` fixes above; first attempt was 1 regression, see below) | `evidence/green-compare-contract.txt` |
| 6a | `E2E_BASE_URL=https://localhost:19121 npx playwright test import.spec.ts` | `tests/e2e` | 0 (6/6) | `evidence/green-e2e-import-spec-final.txt` |
| 6b | `E2E_BASE_URL=https://localhost:19121 npx playwright test navigation.spec.ts --workers=1` | `tests/e2e` | 1 (13/14 — **only "popups" fails, exactly as the packet predicted; "import" passes**) | `evidence/green-e2e-navigation-spec-final.txt` |
| 7 | `node tools/migration-verify/performance.mjs --base https://localhost:19121` | repo root | 0 (all four budgets pass: JS 95.6 kB/307 kB, LCP 164 ms/2000 ms, TTI 16.7 ms/2500 ms, `/collect` p95 6.7 ms/500 ms) | `evidence/green-performance.txt` |

`npm run test -- --run`: 137 passed (frontend unit, incl. the updated `localeHref.spec.ts`).
Sonar: not applicable (no config in the repo, per the plan). Introduced dependencies: **none** —
no new Maven or npm packages; multipart upload uses Spring MVC's built-in support.
`git diff` grep for secrets: none found.

### Contract-dimension first attempt (documented, not hidden)

The first `--dimension contract` run reported **1 regression**: step 9 (the upload) — "expected 3
document/api request(s), candidate made 2". This is the deliberately catalogued breaking change
(plan's Breaking-change catalog: "Import upload redirect handled by the SPA router after 302",
proving verifiers "contract, e2e") — the old stack's oracle recorded a *third*, old-stack-only
`"kind":"document"` GET (the full-page reload once `upload.ts`'s own `fetch()` had already followed
the 302), which the SPA's `router.push` never produces. `compareHttp()` had no mechanism at all for
a candidate making fewer requests than the oracle. Added `DEV-14` (see above) and re-ran — 0
regressions, `db.json`'s `var/import files` delta (1 = 1) and `sessions` delta (1 = 1, `DEV-9`
tolerance) both correctly recognised.

## Deviations used

DEV-4 (SPA-shell document parity, full parity for `/import/upload`), DEV-5 (extended with
`var/import files`), DEV-9 (session count tolerance), DEV-12 (extended with `import-wizard: [10]`),
and the new DEV-14 (this journey's own upload-redirect breaking change — see above; not one of the
plan's original numbered rows, added because the mechanism did not exist yet).

## Anything not done, and why

- No `compose.next*.yaml` change — a volume for `var/import` was avoidable (the container's own
  ephemeral filesystem is sufficient for this journey's scope; the file only needs to survive
  until the next step's read, not a container restart).
- `Field.vue` (shared atom) still has no `type: 'select'`/`'textarea'` branch; `Callout.vue`
  (shared molecule) still has no `action` slot; `Table.vue` (shared molecule) still has no
  `scroll` wrapper. All three gaps were worked around locally (a local `SelectField.vue`, inline
  `.callout` markup in `Dedup.vue`, an external `overflow-x:auto` wrapper around `<Table>` in
  `StepRun.vue`) rather than editing the shared files, per the packet's "shared components
  read-only" rule. Flagged here for whoever next needs one of these on a shared component.
- `CondRule.vue` lives under `components/import/`, not `components/molecules/`, for the touch-scope
  reason explained above — a deliberate placement choice, not an oversight.
- Did not implement CSV parsing, mapping persistence or actually running the import — explicitly
  out of scope per the packet.

## Disk / resource notes

Host disk stayed tight throughout (5.3-6.8 GB free on a 98 GB volume at ~94-95% use). Pruned
`docker builder prune -f` before the first build. Built the `kivvi-w-import-api` image twice
(once for the RED proof against a `git stash`ed parent-SHA tree, once for GREEN); both times
removed via `docker compose ... down -v` + `docker image rm kivvi-w-import-api` immediately after
its gates finished, before moving on. No leftover `kivvi-w-import*` containers, images or volumes
at handoff (confirmed via `docker ps -a` / `docker images` / `docker volume ls`, all empty).
