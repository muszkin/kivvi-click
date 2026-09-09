# Independent review — w3-campaigns-email-editor (J8)

FAIL

Diff reviewed: `b87a701244f5316e1a53bace7a1e41facdffc277...4829252f08bf5375c35d195d41096a214b8df19c`.
Verdict rests entirely on finding A (real regression, uncovered by any deviation); every other
rubric item is clean, including the worker's own report accuracy.

## A — step 6 (reload) mobile, 2.961% — verdict: real-regression

Reproduced exactly (`compare.mjs --dimension visual`: step 6 screenshotMobile 2.961%, 9746/329160,
all other 39/40 checks parity). Pixel data confirms both images are genuinely dark-themed (sampled
RGB ≈(18,24,19) both sides) — the apparent colour difference in a quick visual glance is a
rendering artifact of the diff viewer, not a real theme bug.

Root cause, found empirically with a Playwright probe against the live candidate (measuring
`window.scrollX`, `document.documentElement.scrollWidth` across the journey):
- Steps 1–4 (fresh navigations): `scrollX=0`. The campaigns list and the editor are *both*
  already wider than the 390px mobile viewport (`scrollWidth=736`; widest elements `table.table`
  ~1010px, `span.mono` ~1064px) — this overflow is pre-existing, byte-identical CSS
  (`assets/styles/04-patterns.css:516-529`'s `.email-editor{grid-template-columns:240px 1fr}` at
  ≤1300px, plus `.table`), present on both stacks, not introduced here.
- Step 5 (click theme toggle, off-screen at the far right of an overflowing topbar): Playwright's
  `.click()` auto-scrolls the target into view → `scrollX=346` (exactly `736-390`, i.e. scrolled
  to the max). Same mechanism would apply identically to the old stack's own oracle capture.
- Step 6 (`reload`): measured `scrollX=173` at the very first instant after `domcontentloaded`
  (t+0ms), **before** Vue has mounted, when `document.documentElement.scrollWidth` was still only
  563 (the pre-hydration shell) — `173 = 563-390`, i.e. Chromium clamped its native
  scroll-restoration to the *intermediate, not-yet-hydrated* width. Once Vue mounts ~150ms later
  and `scrollWidth` grows to 736, the browser does **not** re-apply the recorded 346 offset —
  `scrollX` stays frozen at 173. This is exactly half of the pre-reload 346, and 100% deterministic
  across repeated runs (confirmed with `t+0..2850ms` polling).

The old stack is a traditional Symfony/Twig MPA: a reload's single server response already
contains the full final-width HTML, so its native scroll-restoration decision is made against the
*true* width from the start — it lands at (or near) 346, matching the oracle's own screenshot
(visibly "more scrolled" than the candidate). The SPA's two-phase paint (near-empty shell →
Vue-hydrated DOM) is what makes the candidate diverge, and it is **not** anything in this slice's
own components: grepped `useEditorDrag.ts`, `EditorShell.vue`, `EmailDocument.vue`,
`EmailInspector.vue`, `BlockLibrary.vue` and `router/routes.ts` for
`scrollIntoView|\.focus(|autofocus|scrollTo|scrollLeft|scrollBehavior` — zero matches. The same
reload-after-theme-click pattern also exists in `shell-navigation`'s own oracle (steps 12, 15,
wave-5, not yet implemented) — this will recur there for the same reason.

This is a real, deterministic, user-visible difference from old-stack behaviour, not test-harness
noise — but it is **not fixable inside w3's touch scope**: native scroll-restoration is decided
before any application JS runs, so a fix has to live earlier than any view's `onMounted` (e.g.
`history.scrollRestoration = 'manual'` set inline in the SPA document `SpaDocumentController`
serves, or in `frontend/src/main.ts`, restoring only after first paint stabilizes) — both wave-0
owned, out of this packet's allowed paths. The worker's own attempted shared-tool fix (force
`window.scrollTo(0,0)` before every `compare.mjs` screenshot) is **not legitimate**: it would
silently diverge every step from the oracle's own recorded (non-zero) ground truth rather than
matching it, and the worker correctly reverted it after it broke unrelated steps.

**Recommended path (needs Piotr Mucha sign-off, not a worker or reviewer call):** either (1) a
wave-0-level fix — set `history.scrollRestoration = 'manual'` as early as possible in the shell
document/`main.ts` and defer any restoration until after first mount, or (2) a new accepted
deviation masking `screenshotMobile` specifically on `reload` steps at viewports where a
byte-identical, pre-existing CSS overflow makes scroll position observable. Until one of these
lands, the visual gate is not green and no existing DEV id shelters it — hence FAIL.

## B — `tools/migration-verify/compare.mjs` change — verdict: confirmed, narrow, does not loosen anything

`tools/migration-verify/compare.mjs:295-312`: the passive `page.on("response")` listener now
`await response.text()`s into `entry.body`, but **only** when `isFullParityPath(entry.path)`
(`compare.mjs:123-131`, the same `FULL_PARITY_PATTERNS` list `compareHttpEntry` already deep-checks
at `compare.mjs:487-509`), wrapped in try/catch. Before the fix, `entry.body` was always
`undefined` for responses reached via a `click` step, and `compareHttpEntry`'s
`candidateEntry.body ?? "{}"` turned that into a **false regression** whenever the oracle's own
body was non-empty (`body: {"theme":"dark"} vs {}`) — i.e. the bug made verification *stricter*
than correct, never looser. The fix only ever turns a previously-`undefined` body into a real one
for 6 named paths; every other response (assets, `/api/v1/**`) is untouched, and no status/body
*exclusion* was added anywhere.

Checked which journeys have a click-triggered POST to a full-parity path (grepped every
`journeys/*/scenario.md` for `"click"` steps against `/collect`, `/preferences/*`,
`/import/upload`, `/(pl|en)/login`, `/(pl|en)/logout`): only `campaigns-email-editor` (this slice,
`set-theme` ×2) and `shell-navigation` (`toggle-sidebar` ×2, `set-theme` ×2, wave-5, not yet
implemented). `login`'s own click-submit reaches `/pl/login` but always via a 302 there (the deep
body check only runs on status 200 — `compare.mjs:488`), and login's real 200-body checks (failed
login, steps 5–6) come from the explicit `s.post()` branch, which already captured bodies before
this fix (`compare.mjs:280-291`). Ran `compare.mjs --journey login --dimension contract` against
this candidate stack: **0 regressions**, confirming waves 0–2 are unaffected.

## C — DEV-7 — verdict: confirmed

`frontend/src/composables/useEditorDrag.ts:30-34` (`onDrop`): calls only `ev.preventDefault()` —
no `fetch`/`XMLHttpRequest` anywhere in the file or in `EditorShell.vue` (its only consumer,
`EditorShell.vue:37`). The dead `POST {editor}/blocks` 404 is recorded in the **static** endpoint
inventory (`context/migration-oracle/symfony-to-spring-vue/inventory/endpoints.json:51-56`:
`"contract": "no server route exists (404)", "preserve": "Out of scope (R6)"`), built from
scanning `assets/controllers/editor.ts`'s source — **not** from a browser-captured `http.jsonl`,
because no step of `campaigns-email-editor`'s 8-step oracle scenario ever performs a drag/drop
(checked `scenario.md` in full: goto/click/reload/click/goto only). DEV-7's plan row explicitly
covers exactly this: "ignore absence of a 404 request in HTTP recording." `EmailEditorView.spec.ts:184-211`
dispatches real `dragstart`+`drop` events and asserts the `fetch` mock's call count is unchanged.

## Standard rubric

- **Scope** — PASS. `git diff --name-only` matches the packet's allowed-touch list exactly (27
  files); no automations/settings files; `useIntents.ts` only adds `go-email`/`copy-variable`
  (verified both were absent before, and both already exist in old-stack `assets/app.ts:96,86`);
  `routes.ts` diff is only the 2 imports + swapping 3 `EmptyPageView` components.
- **Parity spot-check (3/3)** — PASS. `CampaignsView.vue` vs `campaigns.html.twig`: KPI grid,
  filter rail, 8-column table, actions all match (uses the pre-existing `go-email` intent instead
  of Twig's `navigate`+computed-path — both are real full `window.location.href` navigations,
  functionally identical, `go-email` already existed in `assets/app.ts:96-99`). `EditorShell.vue`
  vs `editor-shell.html.twig`, `EmailEnvelope.vue` vs `envelope.html.twig`, `EmailDocument.vue`/
  `CouponCode.vue` vs their Twig originals: structurally identical, including the whitespace-node
  fix (`{{ " " }}`) and literal `oklch()`/`white` colours untouched by theme.
- **Backend routes/404** — PASS. `GET /api/v1/{locale:pl|en}/campaigns?filter=` (default `all`)
  and `GET /api/v1/{locale:pl|en}/emails/{id:new|k\d+}` (`CampaignsController.java:32,44`) match
  `CampaignController.php`'s `{_locale: pl|en}` prefix, `filter` default, and combined
  `new`/`k\d+` id set. DTO fields are camelCase (`selectedBlock` vs Twig's `selected_block`) —
  consistent with every other DTO in the codebase (no snake_case anywhere, no
  `PropertyNamingStrategy` configured), an established convention, not a deviation.
- **Test → behaviour mapping + mutation testing** — PASS. B27/B28/B01/B07 present as
  `@DisplayName`-tagged JUnit (`CampaignsViewServiceTest`, `CampaignsControllerTest`,
  `CampaignsApiIT`) and `describe("Bnn …")` Vitest (unit + integration). Independently
  mutation-tested one of each of the four categories and reverted cleanly: JUnit unit
  (`CampaignsViewService.java:132` `"—"`→`"N/A"` failed `rowsMatchTheOracle`), real-HTTP IT
  (`NEW_TEMPLATE_NAME` mutated, failed `CampaignsApiIT.newEmailPayloadMatchesTheOracle…`), Vitest
  unit (`frontend/src/styles/04-patterns.css:591` `background:white`→`black` failed
  `emailDocument.spec.ts`, confirming the test reads the real cascade, not a stub), Vitest
  integration (`CampaignsView.vue`'s `action:"go-email"`→`"go-emailx"` failed
  `CampaignsView.spec.ts`).
- **i18n** — PASS. No `Intl.*` usage. Cross-checked every `campaigns.editor.*` key against
  Symfony's literal-string translation catalog (`translations/messages.en.yaml`): every key with a
  real catalog entry (`Bloki`, `Szablony`, `Zmienne`, `Filtry`, `Wybrany blok`, `Wyślij test`,
  `← Powrót`, `common.new_campaign`) is translated in `campaigns.en.ts`; every key with none
  correctly keeps the Polish string, reproducing Symfony's id-fallback exactly.
- **Commit hygiene** — PASS. Both commits are Conventional Commits, English, no trailers, no
  AI/co-author mentions, tagged `(#campaigns-email-editor)` matching this repo's established
  convention (e.g. parent commit `b87a701`'s `(#event-stream)`).
- **Report accuracy** — PASS. Independently re-ran every gate: `./mvnw -q verify` (0 failures),
  frontend `test`/`test:integration`/`lint`/`typecheck`/`format:check`/`build` (all exit 0,
  matching counts/warnings), `compare.mjs --dimension contract` (0 regressions) and `--dimension
  visual` (1 regression, reproduced exactly), `lists.spec.ts -g campaigns` (1 passed),
  `editors.spec.ts -g "email editor"` (2 passed), `performance.mjs` (4/4 budgets pass). The
  worker's report is accurate and did not hide or hand-wave finding A.

## Disposition

Only blocker: **A**. Nothing in this slice's own diff needs a code change. Orchestrator must pick
a remediation (wave-0 `history.scrollRestoration` fix, or a new accepted deviation) before this
slice can pass; re-run `compare.mjs --dimension visual` afterward to confirm 0 regressions.
