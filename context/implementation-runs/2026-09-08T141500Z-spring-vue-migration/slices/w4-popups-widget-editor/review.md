PASS

Independent review of w4-popups-widget-editor, diff `11d3cc4...f278607` (single commit
`f278607 feat: add popup widget list preview and editor (#popups-widget-editor)`). Formed my own
view from the plan/oracle/old-stack sources before reading the worker report.

## 1. Scope — PASS
`git diff --name-only` lists exactly 23 files: `backend/{application,fixtures,web,web/dto}/Widget*`,
3 backend test files under `backend/src/test/java/click/kivvi/{,application/,web/}Widget*`,
8 frontend components (`molecules/{AddSlot,PositionGrid,SwatchGrid,TriggerRow}.vue`,
`organisms/{PopupStage,PopupWidget,PwTypeList,ShopMock}.vue`), `i18n/messages/popups.{pl,en}.ts`,
`router/routes.ts`, `views/{PopupsView,PopupEditorView}.vue`, 3 frontend test files. No forbidden
shared component (`EditorShell`, `BlockLibrary`, `Segmented`, `ToggleRow`, `Field`, `AutoCard`,
`EventStream`, `KpiGrid`, `Table`) touched, no old-stack path, no import-wizard/dashboard file.
`routes.ts` diff (frontend/src/router/routes.ts) is 3 component-line swaps (`popups`, `popup_new`,
`popup_edit`, i.e. the two new view components) + 2 new imports — no other line changed.

## 2. Zero-change parity — PASS
Spot-checked steps 1 (list, `.pw-title`="Zostań na 10% taniej", 5 `.auto-card`), 3 (editor
`p1`/modal/desktop, a11y textbox "Opis" = full body text, `radiogroup` 9 radios/1 checked, 4
`checkbox` rows, `button "Kolor N"` ×4), 6 (mobile, viewport chip "390 × 844", `tab "Mobile"
[selected]`) against `PopupsView.vue`/`PopupEditorView.vue`/the molecule set — DOM classes,
`data-*`, ids and text nodes match, incl. Vue's `{{ " " }}` whitespace-node guard before `Bloki`/
`Wyzwalacze`/`Kto zobaczy` section titles and the U+202F narrow-no-break-space thousands separator
(`domain/Format.java:21`, matches oracle `texts.json` `'48 210'` byte-for-byte, and
`WidgetViewServiceTest.java` embeds the same char, not a plain space).
Adaptation 1 (`ToggleRow` reused, no `CheckboxRow.vue`): correct — old `inspector.html.twig`
already renders "Kto zobaczy" through `atoms/toggle-row.html.twig` (`.checkbox-row`), not a
dedicated checkbox-row partial; `ToggleRow.vue` reproduces it 1:1, confirmed by a11y (`checkbox`
×4, one `<span class="mono">` via v-html).
Adaptation 2 (`SwatchGrid.vue`, `frontend/src/components/molecules/SwatchGrid.vue`): verified
`components/molecules/swatch-grid.html.twig` is genuinely dead — not `include()`d by
`popup-editor.html.twig`/`inspector.html.twig` or any other template (grepped `templates/`).
`SwatchGrid.vue` instead reproduces the inline accent-colour buttons from `inspector.html.twig`
(`aria-label="Kolor N"`, `aria-pressed`, literal `background`/`border` styles) exactly — a11y
`button "Kolor 1" [pressed]` matches.
Adaptation 3 (plain `<textarea>` at `PopupEditorView.vue:333-340` instead of `Field.vue`): verified
`Field.vue` has no textarea branch at all (always renders `<input>`), so extending it would have
required editing a forbidden shared component. The inline markup's `:value` binding sets the
textarea's live `.value` DOM property, which is what the accessibility tree's textbox `text:` value
is read from (confirmed against `steps/3/a11y.json`'s `textbox "Opis": - text: Zapisz się do
newslettera...`) — accessible-value parity holds. Only a harmless serialization nit: the class
attribute renders `"textarea"` vs Twig's `"textarea "` (trailing space from the unused `mono`
ternary) — same convention `Field.vue`'s own `:class="{ mono }"` already uses everywhere else in
the SPA, not introduced by this slice.

## 3. Backend routes/DTOs — PASS, two LOW naming nits
`WidgetController.java` id pattern `{id:new|p\\d+}` (routes.ts's `popup_edit`) — reproduces
`WidgetCatalog::selected()`'s never-404 fallback (`WidgetViewService.java:122-129 resolve()`)
exactly: unknown `?preview=` or unknown edit `id` returns the synthetic "Nowy widget" shape, never
a 404 — verified against `WidgetCatalog.php:66-89`. `?type=` falls back to the widget's own type
(`editor(): typeParam != null ? typeParam : resolved.type()`), `?device=` falls back to desktop
unless the literal `"mobile"` — matches `WidgetController.php:44-45`. ArchUnit: 5/5 green
(`ArchitectureTest`, reproduced below).
- LOW `backend/src/main/java/click/kivvi/web/dto/WidgetsResponse.java:10`: field is `cards`, but
  `WidgetController::index()` (old stack) rendered the array under the Twig key `popups`
  (`'popups' => $widgets->cards()`). Established precedent elsewhere in this codebase
  (`AutomationListResponse.filters/automations`, matching `AutomationController.php`'s render()
  keys exactly) keeps the Twig key literally. No functional/parity break (frontend and backend
  agree with each other; `compare.mjs` does not diff old vs. new JSON shape per DEV-4), but it
  diverges from the "exact render() parameters" convention. Fix: rename to `popups` (and the
  matching `WidgetViewService.ListPayload`/`PopupsView.vue` field) if that convention is to be
  enforced uniformly.
- LOW `backend/src/main/java/click/kivvi/web/dto/WidgetsResponse.java:10` /
  `WidgetViewService.java:79`: `types` is fetched and returned on the **list** payload, but
  `WidgetController::index()` never passed `types` to `popups.html.twig` (only `editor()` did).
  `PopupsView.vue` never reads `payload.types` — dead field. Fix: drop `types` from
  `WidgetsResponse`/`ListPayload` (list-only), or, if intentionally future-proofing, note that in
  the packet/adaptations.

## 4. Tests — PASS
B29: unit (`WidgetViewServiceTest`, 10 cases), sliced (`WidgetControllerTest`, 8), real-HTTP
(`WidgetApiIT`, 10, Testcontainers Postgres), Vitest unit (`widgetComponents.spec.ts`) and
integration (`PopupsView.spec.ts` 9 + `PopupEditorView.spec.ts` 15, real router + stubbed
`fetch`) — all present, all pass (reproduced below).
B01 (popups rows): the shared, pre-existing (not touched by this diff)
`backend/src/test/java/click/kivvi/web/SpaDocumentControllerTest.java:29` parametrized
`@DisplayName("B01 …")` test already lists `/pl/popups`, `/pl/popups/new`, `/pl/popups/p1` in its
`@ValueSource` — unit-tier B01 coverage exists. `WidgetApiIT.java:37-60` adds the real-HTTP tier
(3 tests, all `@DisplayName("B01 …")`). Frontend integration covers B01/B29 jointly
(`PopupEditorView.spec.ts` "B01/B29 renders the back link, title…" and "…the "new" route…").
- INFO `WidgetApiIT.java:39-43,50,58`: the three B01 `@DisplayName`s claim "marker text" but the
  assertions only check status 200 + `contains("<html")`, not the headline string. Consistent with
  DEV-4 (document bodies aren't parity-compared; the SPA shell never server-renders the headline —
  it's client-rendered from the API payload, verified instead by the frontend `.page-title`
  assertions), but the `@DisplayName` text overstates what the assertion checks. Not a defect;
  tighten the display name or add a body-contains assertion if the wording should stay literal.
Mutation check (manual): flipping `WidgetViewServiceTest.java` line "assertThat(payload.selected()
.type()).isEqualTo("modal")" to `"banner"` — fails as expected. Flipping
`PopupEditorView.spec.ts`'s `.pos-cell[aria-checked="true"]` length assertion from `1` to `2` —
fails as expected (both assertions are load-bearing, not tautological).
DEV-7: grepped the SPA for `/blocks` — the only hit is a comment in `useEditorDrag.ts:8`
explaining the deviation; no `POST …/blocks` call anywhere, confirmed by
`PopupEditorView.spec.ts`'s drag/drop-issues-no-request case.
e2e specs (`lists.spec.ts`, `editors.spec.ts`) unmodified by this diff — confirmed.

## 5. i18n — PASS
`popups.pl.ts`/`popups.en.ts` PL-first, keys with no `messages.en.yaml` entry
(`edit`,`showsWhen`,`triggerSummary`,`editor.abTestBody`,`editor.animation`,`editor.colorName`)
correctly keep the Polish string in both locale files, matching Symfony's translator falling back
to the message id — verified against `translations/messages.{pl,en}.yaml`'s `popups:` block
(only `title`/`sub` exist there; the rest were literal, untranslated Twig strings). No `Intl.` use.

## 6. Commit hygiene — PASS
`f278607`: `feat: add popup widget list preview and editor (#popups-widget-editor)` — Conventional
Commits, English, no trailer, no AI/co-author mention. `2c94622` (docs, evidence-only, correctly
excluded from integration) not part of the reviewed code diff.

## 7. Report accuracy — PASS, exact match
Re-ran independently (worktree, no compose):
- `JAVA_HOME=~/.cache/kivvi-toolchains/jdk-25 ./mvnw -q verify` (backend) → exit 0. surefire
  254/0/0/0, failsafe 79/0/0/1-skip → 333 total, 0 failures/errors — matches report's "254 unit /
  333 verify". `WidgetApiIT`: 10/10. `ArchitectureTest`: 5/5.
- `npm ci && npm run test -- --run` → 144/144 (23 files) — matches.
- `npm run test:integration -- --run` → 113/113 (18 files) — matches.
- `npm run lint` → 0 errors, 6 warnings, all in `HookRow.vue`/`FeedCard.vue`/`ApiTab.vue` (none
  touched by this slice) — matches "6 pre-existing warnings" claim exactly.
- `npm run typecheck` → 0. `npm run format:check` → 0.
`git status --porcelain` empty throughout and at end of this review.

## Verdict
No scope violation, no parity break, no missing test tier, DEV-4/DEV-7 correctly honoured, report
numbers independently reproduced exactly. The two LOW DTO-naming nits (§3) and one INFO
`@DisplayName` wording nit (§4) are cosmetic and non-blocking. PASS.
