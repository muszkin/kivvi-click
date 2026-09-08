# Independent review — w0-login candidate f4ac025 (2026-09-08)

Verdict: **FAIL**. Reviewer: independent sonnet agent (high effort), detached checkout /home/muszkin/work/kivvi-click-wt/review-w0-login.

| ID | Severity | Location | Finding | Blocks |
| --- | --- | --- | --- | --- |
| F1 | HIGH | backend/.../application/LoginService.java:33; evidence/compare/login/db.json; compare.mjs:527-544 | `spring_session` count 0→0 across the login journey although step 4 signs in; `SessionRoundTripIT` never queries the table — JDBC persistence unproven | yes |
| F2 | LOW | backend/src/main/resources/application.yml:34 | `Secure` cookie flag hard-coded true instead of "auto" — justified by topology, disclosed | no (accepted by orchestrator) |
| F3 | LOW | tools/migration-verify/deviations.json DEV-11 | DEV-11 applied to steps 4–8 (plan wording: 4 and 8); mechanically justified | no (accepted by orchestrator: page stays on dashboard for steps 5–7) |
| F4 | LOW | frontend/test/integration/LoginView.spec.ts | test exercises `data-last-username=""` which the backend never emits | no (fix bundled into repair-1) |

Verified by the reviewer: scope (no old-stack files), identity/validation parity incl. Twig `default()` quirk, preferences fallbacks, 404 handling, ArchUnit + ESLint rules, pinned versions, compose (ports, volume path, restart policy, placeholder secrets), compare.mjs is a real pixelmatch diff (step-1 screenshots byte-identical, md5 match).

Routing: repair-1.md → same logical owner. Downstream gates (e2e) marked STALE for f4ac025.

## Re-review — candidate cfe7b48 (fresh independent reviewer, 2026-09-08)

Verdict: **PASS**. F1 resolved (`spring-boot-starter-session-jdbc`; BEFORE/AFTER psql proof, restart survival, strengthened `SessionRoundTripIT` fails at context start without the starter); F4 resolved; whole-surface check for the Boot 4.1 module-split failure mode (Flyway, Actuator, Jackson, Testcontainers) found no further gap; DEV-9 delta 0→1 accepted, not masked. Limitation: reviewer could not re-execute mvnw (JDK 21 in its shell); evidence cross-checked instead — the wave cohort re-executes everything.

## Review — candidate 2d2b5a8 (repair-2, fresh independent reviewer)

Verdict: **PASS**. R2-A/B/C resolved with reproducible evidence (reviewer ran clean `./mvnw -q test`/`verify`, frontend unit/integration/lint/typecheck; 66 backend tests, ShellApiIT 5/5, SessionRoundTripIT 4/4, FailOnWarnLogExtension registered globally via META-INF/services + autodetection). Scope clean, no trailers.

## Review — candidate dae1d73 (repair-3/3b, fresh independent reviewer)

Verdict: **PASS**. B12/B13 IT via TestRestTemplate + Testcontainers with exact PL messages and no-identity guard; B22 unit test reachable by `npm run test`; `required` correctly absent on both stacks (verified against `field.html.twig` and oracle a11y); spotless binding proven to fail the build on a probe; `.prettierignore` proven load-bearing; all five CSS files byte-identical; scope clean.
