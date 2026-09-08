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
