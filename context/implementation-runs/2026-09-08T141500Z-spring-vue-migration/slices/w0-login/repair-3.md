# Repair packet w0-login / repair-3 (wave-0 cohort round 2: unit FAIL, integration FAIL; contract/visual/e2e PASS)

Base: your candidate 2d2b5a8d25a73bf291394d8b5d9cbcff200f940f on branch migration/wave-0/login in /home/muszkin/work/kivvi-click-wt/w0-login (identity guard first). New candidate commit on top; never amend. HIGH reasoning effort. Test-only changes expected: do NOT touch product code, markup, CSS, compose or tooling unless a test proves a defect.

## R3-A (integration FAIL) — B12 and B13 need IT-level coverage
Add to `backend/src/test/java/click/kivvi/ShellApiIT.java` (or a new `LoginApiIT` with the same Testcontainers + full-context + real HTTP setup): `POST /pl/login` with `_username=` (empty) → 200, SPA document carries `data-login-error="Podaj adres e-mail."` and `data-last-username="maciej@aureashop.pl"`; `POST /pl/login` with `_username=not-an-email` → 200, `data-login-error="To nie wygląda na poprawny adres e-mail."`, `data-last-username="not-an-email"`; and no session identity is set afterwards (`GET /api/v1/pl/shell` with the same cookie jar still shows the default identity). Names: `@DisplayName("B12 …")`, `@DisplayName("B13 …")`. Fix the sibling test whose `@DisplayName` cites B14 without exercising logout (rename honestly).

## R3-B (unit FAIL) — B22 login part needs a unit-dimension test
Add `frontend/test/unit/LoginView.spec.ts` (runs under `npm run test`) asserting the rendered form contract that makes the browser block a malformed address: `#f-_username` has `type="email"` and `required`, `#f-_password` has `type="password"`, the form is `method="post"` with `action` ending in `/login`, and the submit button exists — `describe("B22 …")`. Keep the existing integration spec as is.

## R3-C (architecture note, non-blocking but cheap — do it) — bind formatting checks to the build
`spotless-maven-plugin` has no `<executions>` binding, so `./mvnw verify` never runs `spotless:check`; the CI workflow comment claims it does. Bind `spotless:check` to the `verify` phase (or `validate`), add a `"format:check": "prettier --check ."` script in frontend/package.json and call it in `.github/workflows/next-build.yml`; correct the workflow comment. Keep `spotless:apply`/`prettier --write` as the developer commands.

## Gate chain on the NEW candidate SHA (in order; logs into evidence/repair-3-gates/)
`./mvnw -q test` (clean, no static/) → `./mvnw -q verify` → `npm run test -- --run` → `npm run test:integration -- --run` → ArchUnit + `spotless:check` → `npm run lint && npm run typecheck && npm run build`. Because only test files change, you may skip re-running compare.mjs/Playwright/performance — but state that explicitly in the report; the wave cohort re-runs everything anyway. Report: append "## Repair-3" to worker-report.md with files changed, the gate table, the new candidate SHA and clean `git status`; return it.
