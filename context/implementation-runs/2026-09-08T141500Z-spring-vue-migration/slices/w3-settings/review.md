# Independent review — w3-settings (J7 settings)

**Diff:** `b87a701244f5316e1a53bace7a1e41facdffc277...bddc537551fe06ba480037c968ee06ffa9627d31` (one commit)

PASS

## A — step 5 desktop visual regression (7.431%)

**Verdict: environmental, not a markup regression.** The worker's specific claim ("Chromium 151
vs the oracle's Chrome 152") is **false** — disproven directly. This sandbox has real
`google-chrome-stable` at **152.0.7977.82**, byte-identical to the oracle capture manifest's
`"browser": "Google Chrome 152.0.7977.82"`. Running `compare.mjs` myself with the same
`channel: "chrome"` launch both tools use reproduced the **identical 7.431% (96300/1296000)**
regression, deterministically, on the exact browser version that captured the oracle. Waves 0–2's
0.000–0.035% history is real; it just never happened to land on this table's specific knife-edge.

Forensics (candidate, live DOM at 1440×900, `.card table.table` on `/pl/settings/api`):
- Header-label pixel positions in oracle vs. candidate screenshots match within 1–2px (NAZWA/KLUCZ/
  ZAKRESY/UTWORZONY/OSTATNIE UŻYCIE columns are essentially the same width in both) — ruling out a
  column-width redistribution caused by some other cell's markup differing.
- `.table td` padding is `12px 14px` (`assets/styles/03-components.css:640`). NAZWA cell border-box
  width = 159.421875px → content-box = 131.421875px. The "Produkcja — backend" span's natural width
  = 131.265625px. **Margin = 0.15625px** — matches the worker's own "<0.15px" figure exactly.
- Text bytes are identical (confirmed by hex dump of both `SettingsCatalog.php` and
  `SettingsFixtures.java:208`, and by `compare.mjs`'s own `visual.texts: parity` on step 5).
- `Geist` weight 500 (the span's actual font, `computedFontFamily` confirmed) is loaded from a
  **live, unpinned Google Fonts CDN** (`frontend/index.html:15`, `display=swap`); 5 repeated runs
  here show it deterministically finishes loading well inside the fixed 400ms settle window
  (`tools/migration-verify/compare.mjs:60,319`) — that specific race is not reproducible in this
  sandbox today, but it is architecturally the same wait strategy the oracle's own
  `capture/capture.mjs:30,71,90` uses (not a tooling asymmetry introduced by this worker), applied
  against a font whose actual served bytes are not pinned/vendored and can silently change over
  time. At a genuine 0.156px margin, this is the only remaining variable capable of flipping the
  wrap, and the plan's own risk register anticipated exactly this failure class ("sub-pixel layout
  shifts … fonts/antialiasing identical (same host, same Chrome)", plan line 126).
- No missing wrapper/class/element was found: `ApiTab.vue`'s NAZWA cell
  (`frontend/src/components/settings/ApiTab.vue:58-60`) reproduces
  `templates/pages/settings/api.html.twig:14` verbatim (`<span style="font-weight:500">NAME</span>`,
  no `nowrap`/`mono`/extra wrapper either side).

**Not fixed, no code fix needed in the port.** But gate 5 (visual) is genuinely red (7.431% ≫ the
plan's 0.5% budget, `context/plans/...md:244`) with **no accepted-deviation entry** covering it —
unlike DEV-12, which the worker correctly added for step 10. Required before this run can call
gate 5 green: either (a) add a new deviation row (mask or step-skip, same pattern as DEV-1/DEV-2)
with explicit sign-off, or (b) make `compare.mjs` wait on `document.fonts.ready` before every
screenshot (shared tool, outside this packet's touch-list) and re-verify. This is an orchestrator/
tooling action item, not a defect to send back to this worker.

## B — Topbar.vue locale-toggle special case

**Parity claim: correct.** Oracle `journeys/settings/steps/1/a11y.json`: at `/pl/settings/account`
the "PL" link targets `/en/settings` (not `/en/settings/account`), matching
`src/Controller/SettingsController.php:18` (`defaults: ['tab' => SettingsCatalog::DEFAULT_TAB]`).
The backend already reproduces this correctly and generically for its own nav hrefs
(`SettingsViewService.java:39-47`, tested in `SettingsViewServiceTest.java:30-48`) — good, that's
the right pattern. The `Topbar.vue` fix (lines 32-46) is functionally correct and doesn't break
any other journey's toggle: checked every route in `routes.ts`, and no currently-implemented route
besides `settings/:tab?` has an optional param with a default value.

**Not a one-off, though.** `routes.ts:124` — `/:locale/import/:step(1|2|3|4)?` — has the identical
shape, and the oracle already shows the identical bug for it:
`journeys/import-wizard/steps/1/a11y.json`, at `/pl/import/1`, "PL" → `/en/import` (drops the
default step `1`, per `src/Controller/ImportController.php:29`,
`defaults: ['step' => ImportWizard::FIRST_STEP]`). The current fix only matches
`route.name === "settings" && route.params.tab === "account"`, so wave-4's import-wizard (J10)
will hit the same oracle mismatch and will either have to bolt on a second hardcoded branch to this
same shared, out-of-allowlist file, or ship with an unfixed toggle. **Recommendation:** generalize
before/alongside J10 — carry each route's default param(s) in `routes.ts` meta (e.g.
`meta.defaultParams: { tab: "account" }` / `{ step: "1" }`) and have `Topbar.vue`'s `localeHref`
strip any trailing segment that matches its own route's declared default, instead of naming a
route. This keeps Topbar journey-agnostic and stops the pattern from recurring per-journey.

**Scope:** this file is not in the packet's touch-list (wave-0 shell chrome). The worker disclosed
it prominently and narrowly, per protocol ("stop and report") — acceptable to keep for this slice,
but needs explicit orchestrator sign-off (this review provides it) and, per the rubric below, a
regression test is missing.

## C — deviations.json DEV-12 change

Confirmed via diff: the only change is `"steps": {...}` gaining `"settings": [10]` and the `note`
field gaining one clause describing it. No other journey's entry, rule, or mechanism was touched or
loosened. Clean, correctly scoped, matches the packet's "Deviations in scope: DEV-12 (step 10 is a
404 document)."

## Rubric

| Check | Verdict | Evidence |
| --- | --- | --- |
| Scope: allowed paths | **FAIL** (1 file) | `frontend/src/components/organisms/Topbar.vue` is outside the packet touch-list (see B). All other 32 changed files match the packet's list exactly. |
| `routes.ts` / `useIntents.ts` touched only as permitted | PASS | `routes.ts`: one import + one `component:` line (`routes.ts:12,131`). `useIntents.ts`: purely additive `copy-api-key`/`copy-dns`/`copy-snippet`, byte-identical port of `assets/app.ts:128-142`. |
| Zero-change parity spot-check, 3 other steps | PASS | Live `compare.mjs --dimension visual` run: `visual.texts`/`visual.aria` = `parity` for **every** step 1–9 (only step 5's screenshot regresses); spot-read step 1 (topbar/nav a11y) and step 2 (sites tab full `texts.json`) confirm byte-for-byte match against Twig sources. |
| Highlight port parity | PASS | Ran the real `CodeHighlightExtension::highlight()` PHP logic (`php8.4`) against the exact snippet in `highlight.spec.ts` — output is **byte-for-byte identical** to the test's `EXPECTED` constant. Regex order, escape-first, `span.s/.k/.c` all match `src/Twig/CodeHighlightExtension.php`. |
| Backend routes/404 vs `SettingsController.php`; DTO keys = Twig params | PASS | `SettingsController.java:54-78` mirrors Symfony's `createNotFoundException`/route-default behaviour (404 on API+document for unknown tab, bare `/settings` still 200 via `SpaDocumentController`). `SettingsResponse.java` carries all 13 sub-collections + `tab`/`tabs`/`tabSubtitle`/`trackerSnippet`, matching the Twig `render()` call 1:1. Fixture data spot-checked (DNS 3 good/1 warn, webhooks incl. 410, notification matrix 15/24) byte-identical to `SettingsCatalog.php`. |
| B06/B32/B01 → `*Test.java` + real-HTTP `*IT.java` + frontend unit/integration | PASS | `SettingsFixturesTest`, `SettingsViewServiceTest`, `SettingsControllerTest` (`@WebMvcTest`) + `SettingsApiIT` (real HTTP, Testcontainers) all present and passing; `highlight.spec.ts`, `settingsComponents.spec.ts`, `SettingsView.spec.ts` cover the frontend side. Mutation-tested: renamed `span.s`→`span.str` in `highlight.ts` → 2 tests failed as expected; removed the 404 guard in `SettingsView.vue:68-70` → B06 test failed as expected (reverted both, tree clean). Did not mutate a JUnit test live — a concurrent full `./mvnw verify` was mid-build at the time and mutating source risked corrupting it. |
| i18n PL-first/EN, no `Intl` | PASS | No `Intl.` usage anywhere in the new files. Spot-checked `settings.en.ts` against `translations/messages.en.yaml`: both the real-translation rows (`Anuluj`→`Cancel`, `Śledzone strony`→`Tracked sites`, etc.) and the "no en.yaml entry → same Polish text" fallback claim check out exactly. |
| Commit hygiene | PASS | Single commit `feat: add settings page with eight tabs (#settings)` — Conventional Commits, English, no trailers, no AI mentions. |
| Report accuracy | PASS | Re-ran every gate: frontend unit 88/88, integration 57/57, lint 0 errors/6 warnings (2 pre-existing pattern, 2 new — same class as existing `FeedCard.vue` warning), typecheck clean, `format:check` clean, `npm run build` clean, e2e `settings.spec.ts` 12/12, `performance.mjs` all `pass:true`, `compare.mjs --dimension contract` 0 regressions, `compare.mjs --dimension visual` 1 regression matching the worker's own reported step/number exactly. Backend: filtered `mvnw -q -Dtest=Settings* test` (unit + `SettingsApiIT`) exit 0; full `./mvnw -q verify` (whole backend) also re-run to completion — exit 0, see note below. |

## Findings by severity

**HIGH-1** — `tools/migration-verify/deviations.json` has no entry excusing the settings-step-5
desktop screenshot; gate 5 is not actually green (7.431% vs 0.5% budget). Fix: orchestrator adds a
deviation entry (mask or accept, DEV-1/DEV-2 style) or fixes `compare.mjs` to await
`document.fonts.ready` before screenshotting, then re-verify. See A above.

**HIGH-2** — `frontend/src/components/organisms/Topbar.vue:32-46` is an out-of-allowlist edit that
special-cases one route instead of a generic mechanism, and will need a second special case for
`import/:step(1|2|3|4)?` in wave-4 (J10) for the identical oracle-proven bug. Fix: add a
`defaultParams`-style route meta and make `localeHref` generic before/alongside J10. See B above.

**MEDIUM-1** — No unit test covers `Topbar.vue`'s new `localeHref` branching (none existed before
either). Add a Vitest case pinning `settings`+`account` → `/en/settings` before merge.

**MEDIUM-2** — `frontend/src/highlight.ts`'s STRING-before-KEYWORD ordering isn't actually pinned
by a test: swapping the two passes left all 5 `highlight.spec.ts` tests green, because no keyword
token happens to sit inside the one string literal in the fixture snippet. The shipped code is
still correct (verified byte-for-byte against real PHP output), this is a coverage gap only. Add a
case like `highlight('data-key="src=window"')` to lock in pass order.

**LOW-1** — `HookRow.vue:45-46` and `ApiTab.vue:78,80` add 2 `vue/multiline-html-element-content-
newline` ESLint warnings (0 errors, same pre-existing class as `FeedCard.vue:164,169`). Cosmetic;
fixable with `--fix`.

## Note on backend verify

`cd backend && ./mvnw -q verify` was re-run twice against the candidate SHA's full backend (all
waves merged so far, not just this journey): the first attempt was killed mid-run by the sandbox
when backgrounded with a bare `nohup … &` (an environment artifact of this review session, not a
build problem); re-run properly backgrounded, it ran to completion — **exit code 0**, no
`BUILD FAILURE` / no failed test in the log — covering ArchUnit, `spotless:check`, and every unit +
real-HTTP integration test in the repository, this journey's included. Combined with the
independently re-run, isolated `-Dtest=Settings*` pass (unit + `SettingsApiIT`, exit 0) done
earlier, gates 1–4 are confirmed green on the backend side.
