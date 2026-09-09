# Final gates (Phase 5) — run on the assembled feature SHA after the FINAL cohort passes

1. Reconcile context: refresh `context/map/**` scoped contexts for `backend/`, `frontend/`, `tools/migration-verify/`, `mercure/`, `compose.next*.yaml`; update `AGENTS.md`/`CLAUDE.md` stack sections only via a follow-up plan (R1 rewrite is `migration-planning` scope — record as an open obligation, do not rewrite stack rules in this run). Commit on the feature branch.
2. Freeze the feature SHA. Full build/type/lint/static: `./mvnw -q verify` (incl. spotless, ArchUnit, ITs), `npm run lint && npm run typecheck && npm run format:check && npm run build`, `.github/workflows/next-build.yml` dry-run equivalent (docker build of `backend/Dockerfile` from a clean context).
3. Sonar: not configured in this repository → record `NOT_CONFIGURED` (not DEFERRED_TO_CI, not green).
4. Fresh independent combined review of `merge-base(main, migration/spring-vue)...HEAD` — split by area (backend, frontend, tooling/compose) across three reviewers, each read-only, mutation-testing at least two assertions in its area.
5. Full real-surface E2E: the entire `tests/e2e` suite headless against the feature stack (alone on the host), plus `performance.mjs` budgets.
6. Record FEATURE_LOCAL_GREEN, then CUTOVER_READY with the RR-1 rehearsal packet handed over; ask the operator ONCE before CUT-1 on port 23456.
