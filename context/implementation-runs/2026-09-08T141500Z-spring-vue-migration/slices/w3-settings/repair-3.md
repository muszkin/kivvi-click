# Repair packet w3-settings / repair-3 (wave-3 cohort round 1: integration FAIL on B01 labels)

Base: wave SHA 32a68311594e611aaa8eaa0803bc72bba7d735a9 (your slice is integrated). Fresh worktree the orchestrator created: /home/muszkin/work/kivvi-click-wt/w3-settings-r3 on branch migration/wave-3/settings-repair3 (identity guard first). New commits only. HIGH reasoning effort. Test-only change.

## R-A — B01 traceability at integration level
The integration verifier maps every behaviour id in scope to a qualifying test BY NAME: real-HTTP `*IT.java` (Testcontainers) for backend, `frontend/test/integration` with real router/store + stubbed fetch for frontend. B01 ("every panel URL renders 200 with its own marker") for the settings (all 8 tabs) rows is currently proved only by the disqualified `@WebMvcTest` `SpaDocumentControllerTest` and by tests tagged with other ids. Add/relabel in `SettingsApiIT` real-HTTP cases `@DisplayName("B01/… GET <url> renders the SPA document 200 …")` for each row (document request → 200 + the shell/api marker the old `PanelPagesTest` asserted), and label the matching frontend integration test(s) (view mounts via real router and shows its own page title) with "B01". Follow the exact convention used by `AutomationsApiIT` (B01/B26) and `EventsApiIT`.
## R-B — shell integration tests for your wave-3 shell change (generic locale toggle)
Add `frontend/test/integration/LocaleToggle.spec.ts`: mount the real `AppLayout`/`Topbar` with the real router at `/pl/settings/account`, `/pl/settings/billing`, `/pl/customers/c_1001` and assert the toggle href (`/en/settings`, `/en/settings/billing`, `/en/customers/c_1001`) — derived from `route.meta.defaultParams` through the real router, not from the helper called directly.


## Gates on the NEW candidate SHA (logs under evidence/repair-3-gates/)
`./mvnw -q test` → `./mvnw -q verify` → `npm run test -- --run` → `npm run test:integration -- --run` → `npm run lint` → `npm run typecheck` → `npm run format:check`. Test-only: compare.mjs/Playwright may be skipped — say so. Commit `test: label B01 settings coverage at integration level (#settings)` (Conventional Commits, English, NO trailers). Append "## Repair-3" to worker-report.md (files changed, gate table, new candidate SHA, `git log --oneline 32a6831..HEAD`, clean status) and return it.
