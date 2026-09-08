PASS

# Independent review — w3-automations (J6 automations)

Range reviewed: `b87a7012..1113128` (commits 02a8c31, 1113128). Verified by re-running gates
against the worktree (no compose stack started), reading the old-stack sources, the oracle, the
plan/packet, and mutation-testing one backend + one frontend assertion. `git status --porcelain`
left empty.

## Rubric results

1. **Scope — PASS.** `git diff --name-only` lists exactly the packet's "Touch only" set: the four
   backend main classes, four `backend/src/test/**/automation*` files, the two views, the eight
   listed components (`CondRule.vue` correctly withheld — see below), `useIntents.ts` (only
   `go-automation` added), the two i18n message files, `routes.ts` (only the 3 automations route
   lines + 2 imports), and 3 `frontend/test/**/automation*` files. No old-stack path, no
   settings/campaigns file, no `pl.ts`/`en.ts`/`index.ts` touched.

2. **Zero-change parity — PASS.** Spot-checked steps 2 (`?status=active` index), 4 (`a1` list
   editor) and 5 (`a1?view=flow`) `texts.json` against the Vue templates and old Twig
   (`templates/pages/automations.html.twig`, `automation-editor.html.twig`,
   `automations/simulation.html.twig`, `rule-pipeline.html.twig`, `flow-canvas.html.twig`,
   `flow-node.html.twig`, `rb-block.html.twig`). Byte-checked the NBSP-vs-ASCII-space case the
   worker fixed: `AutomationsFixtures.java:299` (`"Wykonania (1 287)"`) correctly uses ASCII
   U+0020 — matches both the oracle `texts.json` step-4 bytes and the PHP literal in
   `AutomationCatalog.php`'s `editorTabs()` — while the simulation numbers
   (`AutomationsFixtures.java:393,396,400`) correctly use U+202F, matching `Format.java:21`'s
   `THOUSANDS_SEPARATOR` and the oracle bytes for `"2 412"`/`"1 287"`/`"24 800"`. No other
   hand-typed-vs-`Format`-generated mismatch found. Compiled `FlowNode.vue`'s
   `<Icon v-if="icon".../>\n{{ kicker }}` with `@vue/compiler-dom` directly to confirm Vue's
   "condense" whitespace mode collapses the newline-separated icon→text gap to a single space
   (`_createTextVNode(" " + kicker)`), reproducing the old Twig's literal `{{ icon }} {{ kicker }}`
   — not a regression despite looking like the wave-0-documented whitespace pitfall.

3. **Backend — PASS.** `AutomationsController.java:500-525` routes
   (`GET /api/v1/{locale}/automations`, `GET /api/v1/{locale}/automations/{id:new|a\d+}?view=`)
   and defaults (`status` default `"all"`, `view` untyped) match `AutomationController.php`
   exactly, including the same-shape `id` regex merging `/automations/new` and
   `/automations/{id}` (`a\d+`) into one API route; a bare-shape mismatch (`xyz`) 404s
   (`AutomationsControllerTest.java:1132`), a shape-valid-but-unseeded id (`a99`) falls back to a
   generic draft header rather than 404ing, ported verbatim from `AutomationCatalog::header`'s own
   fallback. `AutomationListResponse`/`AutomationEditorResponse` field names and order are an exact
   match for `AutomationController::index`/`::editor`'s `render()` parameter lists. `web →
   fixtures` (`AutomationsController.java:474`) is an established precedent, not a new violation
   (`CustomersController`, `FeedsController`, `LoginController` do the same); `./mvnw -q verify`
   (ArchUnit included) passes clean.

4. **Tests — PASS, with one MEDIUM naming gap.** Every B26 assertion has a named unit test
   (`AutomationsFixturesTest`, `AutomationsViewServiceTest`, Vitest `automationsComponents.spec.ts`)
   and a qualifying integration test: `AutomationsApiIT.java` is real-HTTP with Testcontainers
   Postgres (`*IT.java`, runs under `mvn verify`, not `mvn test`) — `AutomationsControllerTest.java`
   is correctly *not* counted as the qualifying integration test (plain `@WebMvcTest` slice).
   Frontend integration tests mount real views with the real router and a stubbed `fetch`
   (`AutomationsView.spec.ts`, `AutomationEditorView.spec.ts`). Mutation-tested one backend
   assertion (flipped `AutomationsViewService`'s `hasRevenue = automation.revenue() > 0` to
   `= true`) — `AutomationsViewServiceTest.zeroConversionAutomationRendersEmDash` failed as
   expected (`expected: "—" but was: "0 zł"`), then reverted. Mutation-tested one frontend
   assertion (`FlowCanvas.vue`'s edge-anchor `from.y + 32` → `+ 30`) —
   `automationsComponents.spec.ts`'s edge-path-geometry test failed as expected, then reverted.
   `git status --porcelain` empty after both reverts.
   — **Finding (MEDIUM):** the packet lists **B01** as in scope ("Behaviours: B26, B01 (automations
   + editor rows)"), but no test in this diff carries a `"B01"` `@DisplayName`/`describe` tag,
   breaking the convention every sibling journey's `*ApiIT` follows for the same "renders 200"
   behaviour — `EventsApiIT.java:34`, `FeedsApiIT.java:36`, `LandingApiIT.java:39` all prefix their
   equivalent test with `"B01 ..."`. `AutomationsApiIT.java:38,47,56`
   (`automationsIndexPageRenders200`, `automationNewPageRenders200`,
   `unseededShapeValidAutomationDocumentStillRenders200`) are functionally the B01 tests for this
   journey but are tagged `"B26 ..."` instead. The behaviour itself is not missing — it is also
   covered generically by the pre-existing `SpaDocumentControllerTest`/`RouteTableTest`/
   `routes.spec.ts` (parametrized over every `RouteTable` route, automations included) — this is a
   traceability/consistency gap, not a functional regression. **Fix:** prefix those three
   `@DisplayName`s with `"B01 "` (in addition to, or in place of, `"B26 "`) in
   `AutomationsApiIT.java:38,47,56`.

5. **i18n — PASS.** `automations.pl.ts`/`automations.en.ts` keys verified byte-for-byte against
   `translations/messages.pl.yaml`/`messages.en.yaml` (`automations.title/sub`, `common.new_automation`,
   `common.draft/publish`, and the literal-Polish-as-msgid keys `Filtry→filters`, `Szablony→templates`,
   `Lista→list`, `Diagram→diagram`, `Podgląd→preview`, `← Powrót do listy→backToList`,
   `Test reguły→ruleTestTitle`, `Uruchom symulację…→ruleTestSub`, `Uruchom test→runTest`) — all exact
   matches including the EN translations. No `Intl` usage in any new file. `v-html` in
   `RbBlock.vue:14`, `SimulationCard.vue:26/34`, `AutomationEditorView.vue` (`statusLabel`) is
   fixture-authored content only (never reflected input), consistent with the codebase's existing,
   scoped `vue/no-v-html: "off"` rationale. `npm run lint` — 0 errors (2 pre-existing warnings in
   unrelated `FeedCard.vue`).

6. **Commit hygiene — PASS.** `02a8c31 feat: add automations index and rule editor (#automations)`,
   `1113128 fix: automations gate-5 regression found by compare.mjs (#automations)` — Conventional
   Commits, English, issue-style reference, no trailers, no AI/co-author mentions.

7. **Report accuracy — PASS.** Re-ran the report's own gate table (excluding compose-stack-dependent
   e2e/compare/performance, per this review's own constraints): `./mvnw -q test` exit 0;
   `./mvnw -q verify` (Testcontainers) exit 0; `./mvnw -q spotless:check` exit 0; `npm run test --
   run` → 15 files / 80 tests passed (matches report); `npm run test:integration -- --run` → 11
   files / 57 tests passed (matches report); `npm run lint` exit 0 (2 pre-existing warnings,
   matches report); `npm run typecheck` exit 0; `npm run format:check` exit 0. The **`CondRule.vue`
   non-build decision is correct**: `grep -rl cond-rule templates/` shows it is referenced only by
   `templates/pages/import/segments.html.twig` (import-wizard, wave-4) — never by
   `automations.html.twig`/`automation-editor.html.twig` or anything they include.

## Additional finding

- **LOW — inaccurate citation.** `AutomationsViewService.java:24-28`'s Javadoc cites
  `architecture/rules-translated.md`'s "query-string state only marks a chip active" row as the
  source for the status-filter-is-cosmetic behaviour. That row does not exist —
  `context/migration-oracle/symfony-to-spring-vue/architecture/rules-translated.md` has 9 rows, none
  about query-string state. The applicable rule is actually in
  `context/implementation-runs/.../common-journey-rules.md:15` ("Query-string state … is read by
  the backend view service and reflected in the payload, exactly as the Symfony controller did.").
  **Fix:** repoint the Javadoc citation to `common-journey-rules.md`.

## Verdict

No FAIL-level findings. Scope, parity, backend contract, i18n, commit hygiene and report accuracy
all check out against independent re-verification and mutation testing. The two findings above are
non-blocking (test-naming traceability and a doc citation) — recommend folding the `AutomationsApiIT`
`@DisplayName` fix into this journey's next touch rather than reopening the slice.
