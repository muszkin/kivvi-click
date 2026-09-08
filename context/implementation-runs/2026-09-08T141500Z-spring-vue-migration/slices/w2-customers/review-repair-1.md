# Review — w2-customers / repair-1 (candidate `d0545af8f158573e686b70c9588bf200641bb79a`)

PASS

## Findings

### 1. Scope — PASS
Diff touches exactly two files, both under packet-allowed paths: `backend/src/test/java/click/kivvi/CustomersApiIT.java` (+26, existing file) and `frontend/test/integration/CustomerDetailSidebarSection.spec.ts` (new, +142). No product code, `ArchitectureTest.java`, `eslint.config.js`, or old-stack path (`src/`, `templates/`, `assets/`, `tests/**/*.php`, `tests/e2e/specs/`) touched. `git diff --stat` confirms `2 files changed, 168 insertions(+)`, 0 deletions.

### 2. Real chain, not a stubbed-store shortcut — PASS
`frontend/test/integration/CustomerDetailSidebarSection.spec.ts:47-56` mounts the real `AppLayout.vue` (not `Sidebar.vue` directly) with a real `vue-router` navigated to `/pl/customers/c_1001` and a real `Pinia` instance; it stubs only `global.fetch`, not the store's state. Traced the chain in source: `AppLayout.vue:9-16` reads `route.name` and calls `shell.load(name)`; `stores/shell.ts:52-73` builds `GET /api/v1/{locale}/shell?route=...` and copies the JSON response into `currentSection`/`navGroups`/etc.; `AppShell.vue:29-34` passes `:current="shell.currentSection"` into `Sidebar`; `Sidebar.vue:29` sets `:active="item.route === current"`; `NavItem.vue:20-23` renders `data-route`/`data-active`/`aria-current` from that `active` prop. The stub payload's shape (`locale, theme, sidebar, navGroups[{label,items[{label,icon,route,href,badge}]}], currentSection, crumb, workspace{name,meta,mark}, user{name,email}`) matches `ShellView.java` field-for-field (verified against `application/ShellView.java` and `ShellViewService.java`).
Mutation-tested live in the worktree (reverted after, `git status` clean): hardcoding `Sidebar.vue`'s `active` to `item.route === 'customers'` fails test 3 ("the same chain still highlights the dashboard item…"); zeroing `AppLayout.vue`'s `route.name` read fails tests 1 and 3. The suite is not vacuous.

### 3. Proof the SPA sends `route=customer_show` — PASS
`CustomerDetailSidebarSection.spec.ts:64-72` asserts `fetch` was called with the literal URL `/api/v1/pl/shell?route=customer_show` for `/pl/customers/c_1001`, and a second case (:110-131) proves the same mechanism for `/pl/dashboard` → `route=dashboard`, so it's route-driven both ways, not coincidence. Confirmed live: reverting `route.name` capture in `AppLayout.vue` breaks this exact assertion (see finding 2). Backend companion in `CustomersApiIT.java:112-136` hits the literal query `?route=customer_show` over real HTTP.

### 4. Backend IT is real HTTP — PASS
`CustomersApiIT.java` class is `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@AutoConfigureTestRestTemplate` + `@Testcontainers`/`@Container PostgreSQLContainer` (lines 29-36), not a `@WebMvcTest` slice. The new case (`:112-136`) uses `restTemplate.getForEntity(...)` and asserts both `shell.currentSection()).isEqualTo("customers")` and `shell.crumb()).isEqualTo("Klienci")`. Cross-checked `NavigationCatalog.INDEX_OF_DETAIL` maps `customer_show → customers`, and `messages_pl.properties:7` has `nav.customers=Klienci`.

### 5. Test quality — PASS
Deterministic: only `flushPromises()`, no `setTimeout`/sleep. No `.only`/`.skip`/stray `console.*` in either new test. `describe("B03 detail routes keep their index section active (integration)")` and the backend `@DisplayName` both name B03. `afterEach` unstubs `fetch`; independently ran `npm run test:integration -- --run` in the worktree — 9 files / 39 tests green, fail-on-warning setup (`frontend/test/setup.ts`) intact and untripped.

### 6. Commit hygiene — PASS
Single commit `d0545af`, Conventional Commits (`test: prove B03 detail-route sidebar section at integration level (#customers)`), English, body has no `Co-Authored-By`, no `Claude-Session`, no AI/assistant mention. Matches the branch's existing convention (`(#event-stream)`, `(#customers)` slice tags seen in prior commits on this branch).

### 7. Report accuracy — PASS
Independently re-ran both spot-checked gates in the worktree:
- `npm run test:integration -- --run` (frontend/): 9 files / 39 tests passed — matches report and `evidence/repair-1-gates/04-npm-test-integration.txt`.
- `JAVA_HOME=~/.cache/kivvi-toolchains/jdk-25 ./mvnw -q test` (backend/): exit 0, no `ERROR`/`FAIL` lines, consistent across 3 runs — matches report's "162 tests… incl. ArchitectureTest unchanged/green" and `evidence/repair-1-gates/01-mvnw-test.txt`.
Also cross-checked the recorded evidence directly: `02-mvnw-verify.txt` shows `CustomersApiIT` at "Tests run: 7" (6 pre-existing + 1 new) and `01-mvnw-test.txt` shows `ArchitectureTest` "Tests run: 3" unchanged; `05-lint-typecheck-format-build.txt` matches the report's claimed 2 pre-existing `FeedCard.vue` lint warnings, clean typecheck/format/build. No discrepancies found between the worker report's gate table and the evidence logs.

## Minor observations (non-blocking)
- The new frontend spec never calls `wrapper.unmount()`; `@vue/test-utils`'s `mount()` does not auto-attach to `document.body` here (no `attachTo` option), so this does not leak DOM across the file's three tests, but explicit unmount would be marginally more defensive — not required, and consistent with sibling specs in the same directory.
