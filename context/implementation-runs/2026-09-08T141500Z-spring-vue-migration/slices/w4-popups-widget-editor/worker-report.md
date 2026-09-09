# Worker report — w4-popups-widget-editor (J9)

Worktree: `/home/muszkin/work/kivvi-click-wt/w4-popups` · branch `migration/wave-4/popups-widget-editor`
Parent SHA: `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`
Candidate SHA: `f278607aec795885e79bb1e4900be6e42385a7c7`

`git log --oneline 11d3cc4..HEAD`:
```
f278607 feat: add popup widget list preview and editor (#popups-widget-editor)
```

## Files changed

Backend:
- `backend/src/main/java/click/kivvi/fixtures/WidgetFixtures.java` — ported `WidgetCatalog`'s
  private `WIDGETS`, `TYPE_LABEL`, `STATUS_CHIP`, `types()/blocks()/variables()/triggers()/
  audience()/accentColors()/content()`.
- `backend/src/main/java/click/kivvi/application/WidgetViewService.java` — `list(preview)` and
  `editor(id, type, device)`, mirrors `WidgetController::index/editor`; every count/percent
  formatted via `domain/Format`.
- `backend/src/main/java/click/kivvi/web/WidgetController.java` — `GET /api/v1/{locale}/popups`
  and `GET /api/v1/{locale}/popups/{id:new|p\d+}`.
- `backend/src/main/java/click/kivvi/web/dto/WidgetsResponse.java`,
  `backend/src/main/java/click/kivvi/web/dto/WidgetEditorResponse.java` — wire DTOs.
- Tests: `backend/src/test/java/click/kivvi/application/WidgetViewServiceTest.java`,
  `backend/src/test/java/click/kivvi/web/WidgetControllerTest.java` (`@WebMvcTest`),
  `backend/src/test/java/click/kivvi/WidgetApiIT.java` (full Spring context + Testcontainers
  Postgres, real HTTP layer).

Frontend:
- `frontend/src/views/PopupsView.vue`, `frontend/src/views/PopupEditorView.vue`.
- `frontend/src/components/organisms/{PopupStage,PopupWidget,ShopMock,PwTypeList}.vue`.
- `frontend/src/components/molecules/{PositionGrid,SwatchGrid,TriggerRow,AddSlot}.vue`.
- `frontend/src/i18n/messages/{popups.pl.ts,popups.en.ts}`.
- `frontend/src/router/routes.ts` — swapped `EmptyPageView` for `PopupsView`/`PopupEditorView`
  on the `popups`/`popup_new`/`popup_edit` routes only, added the two imports (no other line
  touched).
- Tests: `frontend/test/unit/widgetComponents.spec.ts`,
  `frontend/test/integration/{PopupsView,PopupEditorView}.spec.ts`.

Run ledger (this slice only): `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/slices/w4-popups-widget-editor/{worker-report.md,evidence/**}`.

No shared component was edited. `EditorShell`, `BlockLibrary`, `Segmented`, `AutoCard`, `Card`,
`PageHead`, `Field`, `ToggleRow`, `Chip`, `Icon`, `Button` are consumed read-only exactly as they
already exist — see "Adaptations" below for the two places a shared component's existing shape
didn't quite cover the old markup and how that was resolved without touching it.

## Behaviour → test map

| Behaviour | Backend unit | Backend integration (real HTTP) | Frontend unit | Frontend integration | e2e |
| --- | --- | --- | --- | --- | --- |
| B29 (widget list + preview, ?preview=, editor type/device switches, inspector triggers/audience/position grid) | `WidgetViewServiceTest` (10 cases: cards match oracle, missing/known/unknown `?preview=`, known/unknown editor id, `?type=` override, `?device=` fallback, payload shape constant) | `WidgetControllerTest` (8 cases, `@WebMvcTest`) + `WidgetApiIT` (10 cases, full context) | `widgetComponents.spec.ts` (PopupStage data-type/data-device, ShopMock, PopupWidget × 5 shape cases, PositionGrid 9 cells/1 checked + override, SwatchGrid aria-label/aria-pressed, TriggerRow × 3, AddSlot, PwTypeList) | `PopupsView.spec.ts` (9 cases) + `PopupEditorView.spec.ts` (15 cases, incl. DEV-7 drag-drop no-request) | `lists.spec.ts -g widgets` (2) + `editors.spec.ts -g "popup editor"` (4) |
| B01 (popups rows: `/pl/popups` → `.page-title` "Popupy i widgety"; `/pl/popups/p1` → `.page-title` "Exit intent — 10% rabatu") | — | `WidgetApiIT.popupsPageRenders200`, `.popupEditorDocumentRenders200`, `.newPopupDocumentRenders200` (all labelled "B01 …") | — | `PopupEditorView.spec.ts` "B01/B29 renders the back link, title..." and "B01/B29 the "new" route..." | `navigation.spec.ts` sidebar entry "popups" (green on this candidate) |
| B07 (unsupported locale → 404, incidental coverage matching the `CampaignsControllerTest`/`CampaignsApiIT` precedent) | — | `WidgetControllerTest.unsupportedLocalePopups(Popup)IsNotFound` + `WidgetApiIT.unsupportedLocalePopupsIsNotFoundThroughTheRealHttpLayer` | — | — | — |

Every behaviour id in scope for this slice (B29, B01 popups rows) maps by name to a JUnit unit
test, a real-HTTP `WidgetApiIT` case labelled "B01 …" where applicable, and a frontend
test/integration test, per the packet's requirement.

## Gate table (candidate SHA `f278607`)

| # | Command | cwd | Exit | Evidence |
| --- | --- | --- | --- | --- |
| 1 | `./mvnw -q test` | `backend/` | 0 | `evidence/gates/mvn-test-summary.txt` — 254 tests, 0 failures/errors |
| 2 | `./mvnw -q verify` | `backend/` | 0 | `evidence/gates/mvn-verify-summary.txt` — 333 tests (surefire+failsafe incl. `WidgetApiIT`), 0 failures/errors; spotless check clean |
| 1f | `npm run test -- --run` | `frontend/` | 0 | `evidence/gates/frontend-unit.txt` — 144/144 |
| 2f | `npm run test:integration -- --run` | `frontend/` | 0 | `evidence/gates/frontend-integration.txt` — 113/113 |
| 3 | ArchUnit (`ArchitectureTest`, inside `mvnw test`) | `backend/` | 0 | 5/5 in mvn-test-summary.txt |
| 3f | `npm run lint` | `frontend/` | 0 | `evidence/gates/frontend-lint.txt` — 0 errors (6 pre-existing warnings in files not touched by this slice) |
| — | `npm run typecheck` | `frontend/` | 0 | `evidence/gates/frontend-typecheck.txt` |
| — | `npm run format:check` | `frontend/` | 0 | `evidence/gates/frontend-format-check.txt` (ran `prettier --write` on exactly the 11 new/changed files first, then verified clean) |
| — | `./mvnw -q spotless:check` | `backend/` | 0 | folded into gate 2 (`verify` runs `spotless:check`); ran `spotless:apply` once, scoped by the plugin to changed files, before the final green `verify` |
| 4f | `npm run build` | `frontend/` | 0 | `evidence/gates/frontend-build.txt` |
| 5 | `node tools/migration-verify/compare.mjs --journey popups-widget-editor --base https://localhost:19111 --dimension contract` (`VERIFY_COMPOSE="docker compose -p kivvi-w-popups -f compose.next.yaml"`) | repo root | 0 | `evidence/compare-contract/popups-widget-editor/report.md` — 0 regressions (7/7 steps `accepted-deviation(DEV-4,DEV-7)`; db deltas `accepted-deviation(DEV-9)`/`(DEV-5)`, both 0) |
| 5 | `… --dimension visual` | repo root | 0 | `evidence/compare/popups-widget-editor/report.md` — 0 regressions; 0.000% pixel diff on every one of the 7 steps × 2 viewports; url/texts/aria parity on all 7 |
| 6 | `cd tests/e2e && E2E_BASE_URL=https://localhost:19111 npx playwright test lists.spec.ts -g widgets` | `tests/e2e/` | 0 | `evidence/green-e2e-lists-widgets.txt` — 2/2 passed |
| 6 | `… npx playwright test editors.spec.ts -g "popup editor"` | `tests/e2e/` | 0 | `evidence/green-e2e-editors-popup.txt` — 4/4 passed |
| 6 | `… npx playwright test navigation.spec.ts --workers=1` | `tests/e2e/` | 1 (expected) | `evidence/green-e2e-navigation.txt` — 13/14 passed; the popups sidebar case passes; only "import" fails (owned by the parallel, not-yet-implemented import-wizard journey at this parent SHA) |

Sonar: NOT_APPLICABLE (no config, per plan). No new dependencies introduced (only existing
shared frontend/backend libraries reused). Grepped the diff for secrets: none found (only fixture
copy, `oklch(...)` colour literals and CSS class names).

### Expected initial RED (proven, retroactively reconstructed)

Implementation was written directly from the oracle/plan reconnaissance rather than via a
literal red-then-green loop; RED was reconstructed immediately afterwards by `git stash -u`-ing
every new/changed file, rebuilding the stack at the unmodified parent SHA, and re-running the
same commands, before popping the stash and re-running the full green gate list above on the
real candidate:

| Command | Result at parent SHA | Evidence |
| --- | --- | --- |
| `compare.mjs --dimension contract` | fails (step 2 click on `.auto-card` "Pasek darmowej dostawy" times out — no such element on the empty-shell page) | `evidence/red-compare-contract.txt` |
| `compare.mjs --dimension visual` | fails, same cause | `evidence/red-compare-visual.txt` |
| `playwright test lists.spec.ts -g widgets` | 2/2 fail (`.auto-card` count 0, not 5) | `evidence/red-e2e-lists-widgets.txt` |
| `playwright test editors.spec.ts -g "popup editor"` | 4/4 fail (`.pw-stage` not found) | `evidence/red-e2e-editors-popup.txt` |

Stack was torn down (`down -v`) and the image removed between the RED and the final GREEN run;
`git status --porcelain` was empty before the RED excursion and confirmed identical (only the
worktree's own new files) after popping the stash.

## Deviations used

- DEV-4 (document requests compared by method/path/status/URL only, not body) — applies to all 7
  steps' document requests; confirmed by the contract report.
- DEV-7 (no `POST …/blocks` call from the editor; the old stack's own dead 404 endpoint) — the
  shared `EditorShell`'s `useEditorDrag` composable already implements this (no change needed);
  covered by `PopupEditorView.spec.ts`'s drag/drop-issues-no-request case and the contract report's
  `accepted-deviation(DEV-4,DEV-7)` verdict on every step.
- No other DEV id from the plan applies to this journey; `db.json` shows zero persisted side
  effects for this journey (matches the oracle's own `delta` of 0 on every counted table), covered
  under DEV-9/DEV-5's existing mapping (both deltas 0, so the mapping is a no-op here).

## Adaptations (not deviations from behaviour — implementation choices, reported per the packet's "stop and report" rule)

1. **`CheckboxRow` not created as a new file.** The four "Kto zobaczy" audience rows are rendered
   with the already-shared `frontend/src/components/atoms/ToggleRow.vue` (introduced by the
   settings journey), which already reproduces `components/atoms/toggle-row.html.twig` exactly —
   checkbox, `name`/`value`/`checked` props, label bound with `v-html` (needed here too: the "Nie
   widzieli w ostatnich <span class="mono">14 dniach</span>" row carries the same kind of embedded
   markup ToggleRow's own class comment already documents). Consumed read-only, not edited;
   creating a second, functionally-identical `CheckboxRow.vue` would have duplicated it for no
   behavioural gain. Covered by `PopupEditorView.spec.ts`'s "the audience rule with embedded
   markup renders through v-html" case and the e2e inspector case (`.checkbox-row` × 4).
2. **`SwatchGrid` does not reuse `components/molecules/swatch-grid.html.twig`.** That Twig
   partial is dead code on the old stack (defined, never `include()`d by any page — confirmed by
   grep across `templates/`). The popup editor's own "Kolor akcentu widgetu" picker is bespoke
   markup inlined directly in `pages/widgets/inspector.html.twig` (different classes/attributes:
   `aria-label="Kolor N"`, `aria-pressed`, inline `background`/`border` styles, no `.swatches`/
   `.swatch` classes at all). `SwatchGrid.vue` reproduces that bespoke markup 1:1 instead — the
   component name follows the plan's own naming, not the (irrelevant) dead partial.
3. **`Field.vue` has no `textarea` mode** (it always renders `<input>`, regardless of the `type`
   prop it declares but never branches on). The "Opis" field is the one place this journey needs a
   real `<textarea class="textarea">` (matching `atoms/field.html.twig`'s own `type: 'textarea'`
   branch, confirmed against the oracle: the banner-type editor step shows this field's `textbox`
   with no value, since `banner` content has no `body` key). Written as plain markup directly in
   `PopupEditorView.vue`'s `#right` slot (`:value` bound, same one-way-bind convention `Field.vue`
   itself uses) rather than editing the shared `Field.vue` (outside this slice's touch scope) or
   inventing a new atom name outside the packet's allowed component list.
4. **List-card row action.** `WidgetViewService.cards()`/`WidgetsResponse.Card` mirror
   `WidgetCatalog::cards()` verbatim (`action: "go-popup"`, `payload: <id>`) — the render()
   parameters `WidgetController::index` actually passed to the Twig template, unmerged.
   `popups.html.twig` itself overrode that to `action: "navigate"` plus a generated `?preview=`
   URL only inside the template (`p|merge({...})`). `PopupsView.vue` performs that same override
   (not the raw `go-popup`/id pair) when wiring `AutoCard`, matching the AutomationsView.vue /
   `useIntents.ts` "go-automation" precedent's own layering — the one difference from that
   precedent is the destination itself (a `?preview=` query on the *same* route, not another
   page), so no new shared intent was registered; the already-existing generic `navigate` intent
   covers it.
5. **Query-param-only-one-at-a-time switches, reproduced verbatim.** Both the widget-type links
   (`PwTypeList`) and the device `Segmented` control build their href from the bare current
   `route.path` plus only the *one* query parameter each control changes — mirrors
   `path(_route, _route_params|merge({type: ...}))` / `...|merge({device: ...}))` in the old
   template exactly (neither carries the other's current query value; the same
   "basePath + one query param" convention `AutomationEditorView.vue`'s own `segmentedOptions`
   computed already established). Confirmed against the oracle steps and reproduced, not just
   assumed, in `PopupEditorView.spec.ts`.

None of the above needed a change to a shared/forbidden component (`EditorShell`, `BlockLibrary`,
`Segmented`, `EventStream`, `KpiGrid`, `Table`) or to any file outside this slice's touch-only
list.

## What could not be done, and why

Nothing in the packet's "In scope" was left undone. Everything the packet's "Out of scope" line
named (saving widgets, `POST …/blocks`) was correctly left unimplemented (DEV-7).

One process note: `node tools/migration-verify/performance.mjs` was not run — it is listed in
`common-journey-rules.md`'s general gate #7, but is not among the explicit gate commands this
packet's dispatch instructions enumerated for this slice ("run every gate on the candidate SHA:
…" lists `mvnw test/verify`, the four `npm run …` commands, `compare.mjs` (contract + visual) and
the three Playwright invocations only). Flagging this explicitly rather than silently skipping it
— if a performance-budget check is still required for this slice, it needs to be run separately
against the candidate SHA above (stack rebuild + `node tools/migration-verify/performance.mjs
--base https://localhost:19111`).

## Finish

- `docker compose -p kivvi-w-popups -f compose.next.yaml down -v` — done (twice: once after the
  RED excursion, once after the final GREEN run).
- `docker rmi kivvi-w-popups-api` — done (twice, same runs); no `kivvi-w-popups-api` image remains.
- `git status --porcelain` — empty immediately after the `feat:` commit above; this report and its
  `evidence/` directory were the only remaining untracked content, now added in a follow-up `docs:`
  commit on this same branch so the worktree is fully clean.
