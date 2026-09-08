# Worker report — w3-automations (J6 "automations")

**Worktree:** `/home/muszkin/work/kivvi-click-wt/w3-automations` · branch `migration/wave-3/automations`
**Parent SHA:** `b87a701244f5316e1a53bace7a1e41facdffc277`
**Candidate SHA:** `1113128 1a05eb87cdef8e4fb2352706339274380` (see exact hash below)
**Identity guard:** passed at start (`git rev-parse HEAD` == parent SHA, `git status --porcelain` empty, branch `migration/wave-3/automations`).

```
$ git log --oneline b87a701..HEAD
1113128 fix: automations gate-5 regression found by compare.mjs (#automations)
02a8c31 feat: add automations index and rule editor (#automations)
$ git rev-parse HEAD
11131281a05eb87cdef8e4fb2352706339274380
```

## Files changed

Backend:
- `backend/src/main/java/click/kivvi/fixtures/AutomationsFixtures.java` (extended — kept `ActiveAutomation`/`activeForCustomer()`, added the index/editor seed data: `Automation`, `StatusChip`, `Header`, `EditorTab`, `PipelineBlock`/`PipelineStep`, `FlowNode`, `Edge`, `SimulationItem`, and `header(id)`'s fallback-to-draft behaviour ported from `AutomationCatalog::header`)
- `backend/src/main/java/click/kivvi/application/AutomationsViewService.java` (new — assembles list/editor view-models, applies `Format.number/percent/money`)
- `backend/src/main/java/click/kivvi/web/AutomationsController.java` (new — `GET /api/v1/{locale}/automations`, `GET /api/v1/{locale}/automations/{id:new|a\d+}`)
- `backend/src/main/java/click/kivvi/web/dto/AutomationListResponse.java`, `AutomationEditorResponse.java` (new — wire DTOs)
- `backend/src/test/java/click/kivvi/fixtures/AutomationsFixturesTest.java` (new, 7 tests)
- `backend/src/test/java/click/kivvi/application/AutomationsViewServiceTest.java` (new, 9 tests)
- `backend/src/test/java/click/kivvi/web/AutomationsControllerTest.java` (new, 7 tests — `@WebMvcTest` slice)
- `backend/src/test/java/click/kivvi/AutomationsApiIT.java` (new, 7 tests — real HTTP, Testcontainers Postgres)

Frontend:
- `frontend/src/views/AutomationsView.vue`, `AutomationEditorView.vue` (new)
- `frontend/src/components/organisms/AutoCard.vue`, `RulePipeline.vue`, `FlowCanvas.vue` (new)
- `frontend/src/components/molecules/RbStep.vue`, `RbBlock.vue`, `FlowNode.vue`, `SimulationCard.vue` (new)
- `frontend/src/i18n/messages/automations.pl.ts`, `automations.en.ts` (new)
- `frontend/src/composables/useIntents.ts` (extended — added `go-automation`, mirroring the existing `go-customer` pattern and the (unused-by-any-Twig-template but still present) `go-automation` intent already in `assets/app.ts`)
- `frontend/src/router/routes.ts` (the three `automations`/`automation_new`/`automation_edit` route lines: `component` swapped from `EmptyPageView` to `AutomationsView`/`AutomationEditorView`, plus the two new imports)
- `frontend/test/unit/automationsComponents.spec.ts` (new, 7 tests — FlowCanvas, RulePipeline, AutoCard)
- `frontend/test/integration/AutomationsView.spec.ts` (new, 6 tests), `AutomationEditorView.spec.ts` (new, 9 tests)

**Not created:** `CondRule.vue`. The packet's old-stack-sources line listed `cond-rule` speculatively, but it is not referenced by `automations.html.twig`/`automation-editor.html.twig` or any organism they include (grep confirms — it is only used by `templates/pages/import/segments.html.twig`, the import-wizard journey, wave-4). Building it here would be dead code with no oracle-derived test and a real risk of colliding with however the import-wizard worker designs its own `CondRule` contract. Also not touched: `editor-shell` (confirmed unused by the automation editor — belongs to campaigns, as the packet already flagged) and `assets/controllers`-equivalent TS (no `flow-canvas` controller exists old-stack-side either; `FlowCanvas.vue` reproduces the bare `data-controller="flow-canvas"` attribute with no behaviour, matching the packet's note).

## Behaviour → test map

**B26** ("6 cards, editor list (3 steps) and flow (6 nodes, 5 edges) views, simulation card, publish button"):
- Unit (JUnit): `AutomationsFixturesTest` — `sixSeededAutomationsInOracleOrder`, `headerOfASeededAutomation`, `headerOfNewFallsBackToADraftHeader` (the packet's explicit `header("new")` fixture test), `headerOfAnUnseededShapeValidIdAlsoFallsBack`, `pipelineHasThreeStepsThreeBlocksEach`, `flowGraphHasSixNodesAndFiveEdges`, `simulationHasThreeReadouts`.
- Unit (JUnit): `AutomationsViewServiceTest` — `statusFiltersMatchTheOracle`, `activeStatusMarksOnlyAktywneActive`, `unrecognisedStatusMarksNoChipActive`, `statusNeverFiltersTheCardList`, `firstCardMatchesTheOracle`, `zeroConversionAutomationRendersEmDash`, `editorDefaultsToListView`, `editorResolvesViewExactly`, `editorForNewCarriesTheSameFixedRuleContent`.
- Integration (Spring slice): `AutomationsControllerTest` — `listPayloadShape`, `statusQueryOnlyMarksAFilterActive`, `editorPayloadShapeForASeededAutomation`, `editorPayloadReportsTheFlowView`, `editorPayloadForNewIsAGenericDraft`, `unknownIdShapeApiIsNotFound`, `unsupportedLocaleApiIsNotFound` (B07).
- Integration (real HTTP, Testcontainers Postgres — the "qualifying integration test"): `AutomationsApiIT` — `automationsIndexPageRenders200`, `automationNewPageRenders200`, `unseededShapeValidAutomationDocumentStillRenders200`, `automationsListPayloadMatchesTheOracleThroughTheRealHttpLayer`, `automationEditorPayloadMatchesTheOracleThroughTheRealHttpLayer`, `unknownIdShapeApiIsNotFoundThroughTheRealHttpLayer`, `shellForAutomationEditRouteKeepsAutomationsSectionActiveThroughTheRealHttpLayer` (B03, proving `NavigationCatalog`/shell wiring end to end for this journey's routes).
- Unit (Vitest): `automationsComponents.spec.ts` — `FlowCanvas` (6 nodes / 5 paths / footer count / edge-path geometry), `RulePipeline` (3 steps, 3 blocks in step 2, KIEDY kicker), `AutoCard` (intent wiring).
- Integration (Vitest, real router + i18n + stubbed `fetch` — the "qualifying integration test" for the frontend): `AutomationsView.spec.ts` (6 tests: fetch URL with/without `?status=`, 6 cards/4 chips, first-card content, `go-automation` wiring, "Nowa automatyzacja" link), `AutomationEditorView.spec.ts` (9 tests: fetch URL with/without `?view=`, list view DOM, flow view DOM, the `navigate`-intent Lista/Diagram switch, simulation card + publish button, back link, `/automations/new` draft header).
- E2E (unmodified): `tests/e2e/specs/automations.spec.ts`, 4 tests, green against the candidate stack.

Every B26-related assertion has both a named unit test and a qualifying integration test on each side of the stack, per the wave-3 rule motivated by the wave-2 gap.

## Gate table (candidate SHA `11131281a05eb87cdef8e4fb2352706339274380`)

| # | Command | cwd | Exit | Evidence |
| - | - | - | - | - |
| 1 | `./mvnw -q -o test` | `backend/` | 0 | `evidence/final-mvn-test.log` |
| 1 | `npm run test -- --run` | `frontend/` | 0 (15 files, 80 tests) | `evidence/final-npm-test.log` |
| 2 | `./mvnw -q -o verify` (Testcontainers ITs + spotless + ArchUnit) | `backend/` | 0 (46 ITs, incl. 7 `AutomationsApiIT`) | `evidence/final-mvn-verify.log` |
| 2 | `npm run test:integration -- --run` | `frontend/` | 0 (11 files, 57 tests) | `evidence/final-npm-test-integration.log` |
| 3 | ArchUnit (`./mvnw -q -o test -Dtest=ArchitectureTest`, included in verify above) | `backend/` | 0 | `evidence/final-mvn-verify.log` |
| 3 | `npm run lint` | `frontend/` | 0 (2 pre-existing warnings in `FeedCard.vue`, not my file) | `evidence/final-npm-lint.log` |
| 3 | `npm run typecheck` | `frontend/` | 0 | `evidence/final-npm-typecheck.log` |
| 4 | `./mvnw -q -o spotless:check` | `backend/` | 0 | `evidence/final-mvn-spotless.log` |
| 4 | `npm run format:check` | `frontend/` | 0 | `evidence/final-npm-format-check.log` |
| 4 | `npm run build` | `frontend/` | 0 | `evidence/final-npm-build.log` |
| 5 | `node tools/migration-verify/compare.mjs --journey automations --base https://localhost:19061 --dimension visual` | repo root, `VERIFY_COMPOSE="docker compose -p kivvi-w-automations -f compose.next.yaml"` | 0 (0 regressions) | `evidence/green-compare-visual-final.txt`, `evidence/compare/automations/report.md` |
| 5 | `node tools/migration-verify/compare.mjs --journey automations --base https://localhost:19061 --dimension contract` (run alone, after e2e+visual, per the wave-3 contract-isolation rule) | repo root | 0 (0 regressions) | `evidence/green-compare-contract-final.txt` |
| 6 | `E2E_BASE_URL=https://localhost:19061 npx playwright test automations.spec.ts` | `tests/e2e/` | 0 (4/4 passed, spec unchanged) | `evidence/green-e2e-automations-final.txt` |
| 7 | `node tools/migration-verify/performance.mjs --base https://localhost:19061` | repo root | 0 (all 4 budgets pass: JS 75.6 kB/300 kB, LCP 152 ms/2000 ms, TTI 17.9 ms/2500 ms, `/collect` p95 7.1 ms/500 ms) | `evidence/green-performance.txt` |

Sonar: NOT_APPLICABLE (no config). Introduced dependencies: none (no new backend or frontend dependency added; `npm ci`/`mvnw` only installed already-pinned lockfile dependencies for tooling not previously exercised in this worktree — `tests/e2e/node_modules`, `tools/migration-verify/node_modules`, `frontend/node_modules`). Diff grepped for secrets: none found.

## Expected initial RED (proven, then implemented)

Per the loop's discipline, RED was captured **before** writing the implementation: I `git stash`'d the (already-written) changes back to the clean parent-SHA tree, brought up the lease stack, and confirmed:
- `GET /pl/automations` → 200 (empty `EmptyPageView` shell only)
- `GET /api/v1/pl/automations` and `/api/v1/pl/automations/a1` → 404 (route doesn't exist yet) — `evidence/red-api-check.txt`
- `tests/e2e/specs/automations.spec.ts` → all 4 tests fail (`.auto-card` never appears) — `evidence/red-e2e-automations.txt`
- `compare.mjs --dimension contract` → times out waiting for `.auto-card` (same underlying cause) — `evidence/red-compare-contract.txt`

Stack was torn down and the image removed before `git stash pop` restored the implementation, to keep disk (~5–6 GB free throughout) from holding two built images at once.

## Repair loop

The first full GREEN pass found a genuine regression: `compare.mjs --dimension visual` failed steps 4–7 (the editor pages) with "text arrays differ". Diffing oracle vs. candidate `texts.json` isolated it to one string: the editor tab label **"Wykonania (1 287)"**. I had (incorrectly) used the narrow-no-break-space (U+202F) `Format.number`/`Format.money` convention for it, but that literal is a hand-typed PHP string constant in `AutomationCatalog::editorTabs()` (`'Wykonania (1 287)'`, never run through `Format::number`), so the oracle byte-for-byte uses a plain ASCII space there — unlike the *card metrics* and *simulation* numbers, which really are `Format::number`/`Format::money` output and really do use U+202F (verified against the oracle's own `texts.json` bytes for both). Fixed in `AutomationsFixtures.java` (one character), committed separately as `fix: automations gate-5 regression found by compare.mjs (#automations)` (mirroring the `#customers` gate-5 repair commit already on this branch's ancestry), then re-ran visual (0 regressions), contract (0 regressions), e2e (4/4), performance (all budgets) and the full backend/frontend gate set again — all green on the final candidate SHA above.

## Deviations used

- **DEV-4** (every journey, document requests): applied as designed by the shared verifier — `compare.mjs --dimension contract` compares `kind=document` requests by method/path/status/final-URL only, and compares the new `GET /api/v1/...` calls in full. No journey-specific extension of `deviations.json` was needed.

## Design decisions worth recording

- **`go-automation` intent, raw id payload** (not the literal `data-action="navigate"` + resolved-URL the Twig template's own `automations.html.twig` merge produces): mirrors the precedent `go-customer` already set for the customers index in wave-2 — same final navigation target, invisible to a11y/text/screenshot comparison (the card is a plain non-interactive `div` in the a11y tree either way), and `go-automation` is in fact the exact intent name already present — unused — in the old stack's own `assets/app.ts`.
- **`navigate` intent used literally for the Lista/Diagram segmented switch**: not optional here — `tests/e2e/specs/automations.spec.ts` asserts `.seg [data-action="navigate"]` by exact selector.
- **API `id` path-variable constraint `{id:new|a\d+}`**: mirrors the shape-only routing constraint the old Symfony route requirement had (`'id' => 'a\d+'`) plus the new `new` route; a shape match with no seeded row (e.g. `a99`) still renders the editor with a generic draft header (ported verbatim from `AutomationCatalog::header`'s own fallback) rather than 404 — proved by both `AutomationsFixturesTest.headerOfAnUnseededShapeValidIdAlsoFallsBack` and `AutomationsApiIT.unseededShapeValidAutomationDocumentStillRenders200`.
- **Status filter is cosmetic**: `?status=` only marks a filter-chip active; the card list is always all 6 automations, exactly reproducing `AutomationController::index`'s own behaviour (it always called `$automations->cards()` unconditionally). Covered by `statusNeverFiltersTheCardList`.
- Kept `AutomationsFixtures` a single class extended in place (not split/renamed), per the packet's explicit instruction, since `CustomersViewService` already depends on `AutomationsFixtures.activeForCustomer()`/`ActiveAutomation`.

## What I could not do / left out

- `CondRule.vue` not created — see "Files changed" above for the reasoning (unused by this journey's actual DOM; would be untested dead code and a likely naming collision with the import-wizard worker's own design).
- No changes were needed to `src/i18n/index.ts`, `src/i18n/pl.ts`, `src/i18n/en.ts`, or `lib/icons.ts` — the glob loader already merges `messages/automations.{pl,en}.ts`, and every icon this journey needs (`filter`, `book`, `plus`, `eye`, `play`, `list`, `flow`, `cart`, `globe`, `user`, `mail`, `coupon`, `layout`, `move`, `target`, `pause`) already existed in `lib/icons.ts` from earlier waves.
- Nothing was needed from the `settings` or `campaigns-email-editor` parallel journeys, and I touched none of their files.

## Housekeeping

- Stack torn down (`docker compose -p kivvi-w-automations -f compose.next.yaml down -v`) and the `kivvi-w-automations-api` image removed after the final gate run.
- `git status --porcelain` empty at finish; `HEAD` at `11131281a05eb87cdef8e4fb2352706339274380` on `migration/wave-3/automations`.
- No push, no merge, no PR opened.

## Follow-up

Independent review (`review.md`) verdict: **PASS**, with two non-blocking findings closed in this
follow-up, on top of `1113128` — no functional change, test-name/comment-only.

1. **B01 traceability (MEDIUM).** `AutomationsApiIT.java`'s three document-render tests
   (`automationsIndexPageRenders200`, `automationNewPageRenders200`,
   `unseededShapeValidAutomationDocumentStillRenders200`) are functionally the journey's B01 tests
   (packet: "Behaviours: B26, B01") but were tagged `"B26 ..."` only, unlike the sibling-journey
   convention (`EventsApiIT.java:34`, `FeedsApiIT.java:36`). Fixed by prefixing all three
   `@DisplayName`s with `"B01/B26 "` (kept both ids — each test also proves a B26-specific fact:
   card page vs. new-editor page vs. shape-valid-but-unseeded-id fallback). Also relabelled the one
   matching frontend test — `AutomationEditorView.spec.ts`'s `/automations/new` mount test, which
   already asserted `.page-title` — to `"B01/B26 GET /automations/new renders and shows its own
   headline (the generic draft header)"`, mirroring `automationNewPageRenders200` on the frontend
   side. No other journey's frontend integration spec carries a `"B01"` tag either (the frontend
   behaviour is otherwise covered generically by the shared, non-journey-owned
   `frontend/test/unit/routes.spec.ts`), so this is the first per-journey frontend `"B01"` label —
   scoped to the one test that is the direct frontend counterpart of the relabelled backend test,
   not a new pattern applied elsewhere.
2. **Inaccurate citation (LOW).** `AutomationsViewService.java`'s class Javadoc cited a
   non-existent `architecture/rules-translated.md` "query-string state only marks a chip active"
   row. Repointed to `common-journey-rules.md`'s actual query-string-state rule, quoting it
   directly instead of citing a table row that doesn't exist there.

No test assertions changed — only `@DisplayName`/test-name strings and one Javadoc comment.

**New candidate SHA:** `2ca5f07cf0dfc405feb6b13a9302883f134c328b`

```
$ git log --oneline b87a701..HEAD
2ca5f07 test: label B01 automations coverage (#automations)
1113128 fix: automations gate-5 regression found by compare.mjs (#automations)
02a8c31 feat: add automations index and rule editor (#automations)
```

### Follow-up gate table (candidate SHA `2ca5f07cf0dfc405feb6b13a9302883f134c328b`)

| Command | cwd | Exit | Evidence |
| - | - | - | - |
| `./mvnw -q -o test` | `backend/` | 0 | `evidence/followup-gates/mvn-test.log` |
| `./mvnw -q -o verify` (Testcontainers ITs, incl. 7/7 `AutomationsApiIT`, + spotless + ArchUnit) | `backend/` | 0 | `evidence/followup-gates/mvn-verify.log` |
| `npm run test -- --run` | `frontend/` | 0 (15 files, 80 tests) | `evidence/followup-gates/npm-test.log` |
| `npm run test:integration -- --run` | `frontend/` | 0 (11 files, 57 tests) | `evidence/followup-gates/npm-test-integration.log` |
| `npm run lint` | `frontend/` | 0 (2 pre-existing warnings in unrelated `FeedCard.vue`) | `evidence/followup-gates/npm-lint.log` |
| `npm run format:check` | `frontend/` | 0 | `evidence/followup-gates/npm-format-check.log` |

`git status --porcelain` empty after the commit; no compose stack was started for this follow-up
(no compose-stack-dependent gate — e2e/compare/performance — was affected by a test-name/comment
change, and none of them were re-run here).
