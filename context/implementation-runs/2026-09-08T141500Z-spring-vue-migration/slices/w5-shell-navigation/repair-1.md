# Repair packet w5-shell-navigation / repair-1 (FINAL cohort round 1: unit FAIL — B02/B03/B21 unit-level traceability)

Fresh worktree /home/muszkin/work/kivvi-click-wt/w5-shell-r1 · branch `migration/wave-5/shell-navigation-repair1` · base 7e6a66a77d4f97ecd2aaea8ad2eb3b6c66015194. HIGH reasoning effort. Test-only. No stack. DISK CRITICAL (~2.8 GB): one node_modules + one target, deleted before reporting.

## Finding (waves/final/unit.md, unit/behaviour-mapping.md)
The unit verifier maps EVERY behaviour id in behaviours.json to a named test under `./mvnw -q test` (`*Test.java`) and `npm run test -- --run` (frontend/test/unit). Missing: B02 (breadcrumb names the section), B03 (detail route keeps its index section active — `ShellViewServiceTest.detailRouteKeepsItsIndexSectionCurrent` exists but lacks the "B03" prefix, flagged as GAP-1 in wave-2), B21 (only `.main-scroll` scrolls).

## Required change
1. Label existing unit tests: `@DisplayName("B03 …")` on `ShellViewServiceTest.detailRouteKeepsItsIndexSectionCurrent` (and any NavigationCatalog crumb test → "B02 …"); if no unit test for B02 exists, add one (NavigationCatalog/ShellViewService crumb per route) and a frontend unit test for the breadcrumb component text.
2. B21: add a unit test that pins the structural invariant the frozen CSS relies on — the app shell renders exactly one `.main-scroll` container inside `.app` and the router view is inside it (frontend/test/unit, `describe("B21 only .main-scroll scrolls …")`), plus, if `App.vue`/layout sets any `overflow` inline or attribute, assert it; the pixel behaviour stays proven by e2e.
3. Sweep: produce `evidence/repair-1/behaviour-unit-map.md` mapping EVERY B01–B32 id to a `*Test.java` @DisplayName or a `test/unit` describe/it name, and fix any other gap you find the same way (labels or minimal unit tests) — this is the last cohort; no more label-only rounds.
4. Gates: `./mvnw -q test`, `npm run test -- --run`, `npm run lint`, `npm run typecheck`, `npm run format:check`, `./mvnw -q spotless:check`. Commit `test: label B02/B03/B21 unit coverage and pin the main-scroll invariant (#shell-navigation)` (Conventional Commits, NO trailers). Append "## Repair-1" to worker-report.md with the map and the new SHA; `git status --porcelain` empty; return the SHA.
