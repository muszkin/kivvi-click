# Repair packet w2-customers / repair-1 (wave-2 cohort: integration FAIL on B03)

Base: wave SHA 22d7fcb6380723728a33fc21fda22a92594a2e88 (contains your integrated slice). Work in the fresh worktree the orchestrator created: /home/muszkin/work/kivvi-click-wt/w2-customers-r1 on branch migration/wave-2/customers-repair1 (identity guard first). New commits only. HIGH reasoning effort. Test-only change unless a seam is missing.

## R1-A — B03 "detail routes keep their index section active" at integration level
- Frontend: a test under `frontend/test/integration/` that mounts the real `AppLayout`/`Sidebar` with the real router at `/pl/customers/c_1001` and the shell store fed from a stubbed `GET /api/v1/pl/shell?route=customer_show` payload (the real API contract), asserting `.nav-item[data-route="customers"]` has `data-active="true"` and `aria-current="page"` — derived from the route, not from a hardcoded `current` prop. `describe("B03 …")`.
- Backend: add to `ShellApiIT` or `CustomersApiIT` a real-HTTP case `GET /api/v1/pl/shell?route=customer_show` → `currentSection: "customers"` and crumb "Klienci" (mirrors `Navigation::currentSection`/`crumb`), `@DisplayName("B03 …")`.
- Confirm the SPA actually sends `route=customer_show` for the detail route (the oracle http.jsonl for customers step 4 shows the document request; the SPA's shell fetch is the DEV-4 baseline — check what the SPA sends today and that the sidebar state on the live detail page comes from that payload).

## Gates on the NEW candidate SHA (logs into evidence/repair-1-gates/)
`./mvnw -q test` → `./mvnw -q verify` → `npm run test -- --run` → `npm run test:integration -- --run` → ArchUnit + `spotless:check` → `npm run lint && npm run typecheck && npm run format:check && npm run build`. Test-only: compare.mjs/Playwright may be skipped — say so. Append "## Repair-1" to /home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/slices/w2-customers/worker-report.md (files changed, gate table, new candidate SHA, `git log --oneline 22d7fcb..HEAD`, clean git status) and return it.
