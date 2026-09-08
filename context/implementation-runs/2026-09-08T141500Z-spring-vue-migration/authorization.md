# Authorization envelope

Source: operator messages of 2026-09-08 in the trusted conversation ("zatwierdzam" / "ok" / "1 ale effort high. do tego chce by projekt żył pod tym samym portem na końcu bym mógł sam zweryfikować").

| Capability | Value |
| --- | --- |
| local branches, worktrees, commits and tests | allowed (worktrees under `/home/muszkin/work/kivvi-click-wt/`, outside the tracked tree) |
| dependency installation or lockfile changes | allowed inside worktrees (`backend/`, `frontend/`, `tests/e2e` unchanged); Maven wrapper, npm, Docker images; JDK 25 tarball cached under `~/.cache/kivvi-toolchains/` (read-only shared); no global installs |
| schema or data migration | constrained: Flyway baseline for the NEW database only (`database_data_next` volumes); the old prod database is never touched |
| remote push and integration PR | excluded |
| integration-branch merge (`migration/spring-vue` → `main`) | excluded |
| staging deployment | not applicable (RR-1 stack on this host requires separate confirmation) |
| staging test data or external side effects | excluded |
| production-target PR creation | excluded |
| production merge and deployment (CUT-1 on port 23456) | excluded in this run; operator pre-signalled intent — ask once after CUTOVER_READY |
| production smoke/E2E and test data | excluded |
| production rollback | not applicable |
| old stack edits (`src/`, `templates/`, `assets/`, `composer.*`, `compose*.yaml`, `Dockerfile`, `frankenphp/`, `tests/**/*.php`, `tests/e2e/specs/`) | excluded for every worker; only CON-1 (separate authorization) |
| resource pool | compose projects `kivvi-w-<journey>`, ports 19000–19199, never 23456/23457/18080/18443/8080/8443 |

Runtime resource isolation contract: see `resources.md`.
