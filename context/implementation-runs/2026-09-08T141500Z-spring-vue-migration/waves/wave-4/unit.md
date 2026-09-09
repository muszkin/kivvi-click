# Wave-4 verifier — dimension: unit (round 3)

**Dimension status: PASS**, bound to wave SHA `60445ebac139bcf1179b397c997600073a356e95`.

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit`, detached at the wave SHA, tree
clean before and after the run (`git status --porcelain` empty; only `backend/target/` and
`frontend/node_modules/` untracked, both deleted at the end per the packet's disk instruction). No
tracked file was edited.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q test` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit/backend` | 0 |
| 2 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit/frontend` | 0 |
| 3 | `npm run test -- --run` (= `vitest run test/unit --run`, per `frontend/package.json`'s `"test"` script) | `/home/muszkin/work/kivvi-click-wt/verify-wave-4-unit/frontend` | 0 |

Java: Temurin 25.0.4.1 LTS (`$JAVA_HOME` set per packet). Node: v26.8.1 (host).

## Results

- Backend: **48 Surefire `*Test.java` classes, 289 tests, 0 failures, 0 errors, 0 skipped.**
  No `*ApiIT.java` (Failsafe integration test) leaked into this run — confirmed no `*IT.txt` in
  `backend/target/surefire-reports/` (`./mvnw -q test` never invokes Failsafe). Full per-class
  breakdown: `unit/backend-surefire-summary.txt`.
- Frontend: **26 test files, 164 tests, 0 failures**, scoped to `frontend/test/unit/` only (per
  `package.json`'s `test` script `vitest run test/unit`; files under `frontend/test/integration/`
  are out of this command's reach and therefore out of the unit dimension's scope). Full output:
  `unit/frontend-vitest-output.txt`.

## Behaviour-id → named-test mapping (in scope: B29, B31, B23, and B01 rows for popups /
popup-editor / import / dashboard)

Raw grep evidence: `unit/behaviour-name-grep.txt`. Summary below (all listed tests are part of the
green runs above).

### B29 (popups-widget-editor)

- `backend/src/test/java/click/kivvi/web/WidgetControllerTest.java` — 6 of 8 `@Test`s tagged `B29`
  (list payload shape/preview selection, known-widget editor payload, `new` blank draft,
  type/device query overrides, editor payload constant shape) plus one `B29/DEV-7` test (`GET
  /api/v1/pl/popups/{id}` 404s for an id shape outside `new`/`p\d+` — the shape that a nonexistent
  `POST …/blocks` id would also hit, corroborating DEV-7 "no `POST …/blocks` call" at the contract
  dimension).
- `backend/src/test/java/click/kivvi/application/WidgetViewServiceTest.java` — 10 tests, all
  tagged `B29` (cards match oracle names/metrics, preview fallback/selection/unknown-id draft
  fallback, known editor payload, unknown-id blank draft, type/device query overrides, constant
  editor payload shape: 5 types/10 blocks/4 variables/3 triggers/4 audience rows).
- `frontend/test/unit/widgetComponents.spec.ts` — 7 `describe` blocks, all tagged `B29`
  (`PopupStage`, `ShopMock`, `PopupWidget` × 3 shapes, `PositionGrid`, `SwatchGrid`, `TriggerRow`,
  `AddSlot`, `PwTypeList`), 24 `it`s total.

### B31 (import-wizard)

- `backend/src/test/java/click/kivvi/web/ImportControllerTest.java` — 4 of 6 tests tagged `B31`
  (step-1 full payload shape, `/import/5` and `/import/0` out-of-range 404s, shared 14-option
  target list).
- `backend/src/test/java/click/kivvi/web/ImportUploadControllerTest.java` — both tests tagged
  `B31` (POST stores file + 302 to `/pl/import/2`; missing file part 404s).
- `backend/src/test/java/click/kivvi/infrastructure/importing/ImportUploadStorageTest.java` — the
  import-upload security control unit tests: 7 of 9 tests tagged `B31` — path-traversal name never
  escapes the upload directory (stored name is a random 32-hex name, only the basename is
  remembered), 8-char extension whitelist boundary accepted, 9-char extension rejected → falls
  back to `csv`, mixed-case extension lower-cased, no extension falls back to `csv`,
  non-alphanumeric extension rejected, two same-session uploads never collide on the generated
  name. Uses a private (non-static) `@TempDir Path uploadDirectory` per test method.
- `backend/src/test/java/click/kivvi/application/ImportViewServiceTest.java` — 7 tests, all
  tagged `B31` (file-name fallback/override, detection counts 9/1/1, skipped-column mapping,
  null-ltv "?" rendering, plain-space vs narrow-no-break-space row-count formatting, and
  `B31/DEV-12` — step outside 1-4 throws `NoSuchElementException`, which `ImportController`
  translates to the 404 covered by the controller test above).
- `frontend/test/unit/importComponents.spec.ts` — `describe` blocks `B31 Stepper …` (4 tests) and
  `B31 Dropzone …` (4 tests, incl. the redirected-navigation and drag-state behaviour).

### B23 (dashboard)

- `backend/src/test/java/click/kivvi/web/DashboardControllerTest.java` — 3 of 4 tests tagged
  `B23` (wire-shape: 4 KPIs/3 legend/10 events/6 customers/4 automations/`mercureTopic`; first
  recent customer; first top automation).
- `backend/src/test/java/click/kivvi/application/DashboardViewServiceTest.java` — 7 tests, all
  tagged `B23` (4 KPIs with 40-point sparklines matching the oracle exactly, deterministic series,
  3-entry cardiogram legend, 10 live rows + account Mercure topic matching the oracle's first row,
  EN type-label translation, 6 recently-seen customers, 4 best-performing automations
  active-only/highest-revenue-first) — these exact-string assertions (e.g. `"teraz"`, `"1 min
  temu"`) are the unit-level counterpart to DEV-1's visual-dimension `.event-row__time` mask.
- `frontend/test/unit/Cardiogram.spec.ts` — 4 tests tagged `B23` (canvas `role=img` + accessible
  name, draws once on mount, redraws when `<html data-theme>` mutates without reconnecting, redraws
  once a second) — the unit-level counterpart to DEV-2's visual-dimension canvas mask.

### B01 rows for popups / popup-editor / import / dashboard

- `backend/src/test/java/click/kivvi/domain/RouteTableTest.java` (`B01 every panel/public URL in
  the route table is known`) resolves `/pl/popups`, `/pl/popups/new`, `/pl/popups/p3`,
  `/pl/import`, `/pl/import/2`, `/pl/dashboard` to their route names.
- `backend/src/test/java/click/kivvi/web/SpaDocumentControllerTest.java`
  (`@ParameterizedTest B01 every panel/public URL renders 200 and carries the SPA document`) — a
  `@WebMvcTest` (Surefire-collected) asserting 200 + `<html` for, among the 21-URL `@ValueSource`,
  `/pl/dashboard`, `/pl/popups`, `/pl/popups/new`, `/pl/popups/p1`, `/pl/import`, `/pl/import/2`.
- `frontend/test/unit/routes.spec.ts` (`B01 every panel/public route in the table resolves`) —
  the same 21-path list resolved through the real Vue Router, including the same six paths above.

All three land the same six B01 rows (popups, popups/new, popups/pN, import, import/N,
dashboard), backend and frontend, independently.

## Explicit note on "the shell store's preference tests"

`frontend/test/integration/shellStore.spec.ts` (`describe("B08/B10 shell store preference
toggles")`) exists and is green in this checkout, but it is **out of the unit dimension's
scope on two independent grounds**, not a gap:

1. Location: `frontend/package.json`'s `"test"` script is `vitest run test/unit` — it never
   reaches `frontend/test/integration/`. That directory is the `integration` dimension's
   command (`npm run test:integration` = `vitest run test/integration`), not this one's.
2. Behaviour scope: it is tagged `B08`/`B10` (shell preference toggles), not `B29`/`B31`/`B23`/
   the in-scope `B01` rows, and its journeys (shell preferences / navigation) are not one of this
   wave's three journeys (popups-widget-editor, import-wizard, dashboard).

`frontend/test/unit/useIntents.spec.ts` — itself green in the run above — documents this split
explicitly in its header comment: it covers the DOM-level `useIntents` keepalive-POST composable
at the unit level, and says "see `shellStore.spec.ts` for that, store-level, test", confirming
the division is intentional, not an oversight from the round-3 `stores/shell.ts` fix (await the
preference POST before applying the DOM attribute/state). I did not run
`vitest run test/integration/shellStore.spec.ts` myself: doing so would exceed this dimension's
command contract (`./mvnw -q test` and `npm run test -- --run` only, per the packet's own "Unit ="
definition) and duplicate the integration verifier's job.

## Per-journey verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| popups-widget-editor | parity | `WidgetControllerTest` (8/8 green), `WidgetViewServiceTest` (10/10 green), `widgetComponents.spec.ts` (24/24 green); B29 and the popups/popup-editor B01 rows all map to named green tests; DEV-7 corroborated. |
| import-wizard | parity | `ImportControllerTest` (6/6), `ImportUploadControllerTest` (2/2), `ImportUploadStorageTest` (9/9, incl. the upload security controls), `ImportViewServiceTest` (7/7, incl. DEV-12's underlying throw), `importComponents.spec.ts` (8/8); B31 and the import B01 row all map to named green tests. |
| dashboard | parity | `DashboardControllerTest` (4/4), `DashboardViewServiceTest` (7/7), `Cardiogram.spec.ts` (4/4 relevant + 4 more); B23 and the dashboard B01 row all map to named green tests; DEV-1/DEV-2 corroborated at unit granularity. |

No accepted-deviation or regression verdicts were needed for this dimension this round — every
in-scope behaviour id resolved to a green, by-name test.

## Evidence paths

- `unit/backend-surefire-summary.txt` — per-class Surefire summary (48 classes, 289 tests, 0
  failures, 0 errors), extracted from `backend/target/surefire-reports/*.txt` before cleanup.
- `unit/frontend-vitest-output.txt` — full `npm run test -- --run` console output (26 files, 164
  tests, 0 failures).
- `unit/behaviour-name-grep.txt` — raw `grep -rn 'B01\|B23\|B29\|B31'` hits across
  `backend/src/test` and `frontend/test/unit`, the source for the mapping tables above.

## Cleanup

`backend/target/` and `frontend/node_modules/` deleted from this checkout after evidence
extraction, per the packet's disk-space instruction (host `/` was at 96% / ~4.4 GB free before
cleanup).
