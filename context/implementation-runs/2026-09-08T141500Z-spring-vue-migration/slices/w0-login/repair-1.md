# Repair packet w0-login / repair-1 (independent review FAIL — F1 HIGH)

Base: your candidate f4ac025ca10a90ab41733a942000dc93769a273c on branch migration/wave-0/login in /home/muszkin/work/kivvi-click-wt/w0-login (identity guard first). Produce a NEW candidate commit; never amend or rewrite f4ac025.

## F1 (HIGH, blocking) — prove or fix Spring Session JDBC persistence
Evidence: your own `evidence/compare/login/db.json` shows `spring_session` 0 → 0 across the journey although step 4 signs in (`request.getSession(true)` + attribute). `SessionRoundTripIT` never queries the table. Required outcome:
1. Bring your stack up (`kivvi-w-login`, ports 19000/19001), sign in via `curl -k -c jar -X POST https://localhost:19001/pl/login -d '_username=anna@aureashop.pl&_password=x'`, then query the compose database directly: `select count(*) from spring_session; select count(*) from spring_session_attributes;` — record the raw output in `evidence/repair-1-session-proof.txt`. Also GET `/api/v1/pl/shell` with the cookie jar and confirm `user.email` is anna@aureashop.pl, then RESTART the api container (`docker compose -p kivvi-w-login -f compose.next.yaml restart api`) and repeat the GET with the same cookie: the identity must survive the restart (that is the durability the architecture requires).
2. If the row is not written: diagnose (Spring Boot 4.1 module split — is `spring-session-jdbc` auto-configuration active? `spring.session.store-type`/`spring.session.jdbc.*`, the `SessionRepositoryFilter` bean, flush mode, schema names vs Flyway baseline) and fix at the root cause. Do not paper over it with a manual insert.
3. Strengthen `SessionRoundTripIT`: assert the `spring_session` row count and the attribute row after sign-in via the Testcontainers datasource, and assert the identity survives a simulated new application context / repository read (at minimum: read the session back through `JdbcIndexedSessionRepository` by id). The test must fail if Spring Session JDBC is not active.
4. Update `compare.mjs`'s DEV-5 db mapping only if your investigation shows the oracle's own `sessions` 0→0 (PHP) is a capture-timing artefact; document what you found about both stacks in the report (the orchestrator will decide whether the oracle needs a note). Do not change the oracle.

## F4 (LOW) — remove the dead-branch test
`frontend/test/integration/LoginView.spec.ts` asserts a `data-last-username=""` state the backend never emits. Replace it with the real contract (empty submission → default e-mail redisplayed) or delete it; keep the B12/B13 behaviour tests meaningful.

F2 (Secure=true hard-coded) and F3 (DEV-11 steps 4–8) are accepted by the orchestrator as documented judgment calls — no change.

## Gate chain (full rerun on the NEW candidate SHA, in this order; record everything in the report)
focused (mvnw test, npm test) → integration (mvnw verify incl. the strengthened IT, npm test:integration) → architecture/static (ArchUnit, spotless:check, lint, typecheck, build) → compare.mjs login (0 regressions) → Playwright login/sidebar/theme specs → performance.mjs. Tear the stack down (`down -v`) afterwards.

Report: append a section "## Repair-1" to worker-report.md (do not delete earlier sections) with the session proof, root cause (or "already persisted; proof attached"), files changed, the gate table for the new SHA, and the new candidate SHA + clean `git status`. Return the same content.
