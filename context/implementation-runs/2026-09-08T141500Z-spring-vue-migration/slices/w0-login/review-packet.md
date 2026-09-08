# Independent review packet — w0-login

Read-only. You did not implement this candidate. Do not edit code. Report findings with evidence; verdict PASS | FAIL | BLOCKED_EXTERNAL.

- Plan: /home/muszkin/work/kivvi-click/context/plans/2026-09-08-symfony-to-spring-vue-migration.md (sha256 84b664562fd00c1de6a2263ee2b32fa48da93d88f989f4aac8d60b46fbd4c017); journey packet section: J0 — login (wave-0 walking skeleton).
- Slice packet: /home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/slices/w0-login/packet.md (sha256 1ec7cf935c0ab028…).
- Base SHA: 8d3fc320354604b641b44a3043a070f279e6d491 · Head SHA: cfe7b48bedd871c2132b2d62cd215d135e5e3627. Verify both with `git rev-parse` in the review checkout before reading the diff.
- Review checkout (detached, read-only): /home/muszkin/work/kivvi-click-wt/review-w0-login — created by the orchestrator with `git worktree add --detach <path> cfe7b48bedd871c2132b2d62cd215d135e5e3627`.
- Diff: `git diff 8d3fc320354604b641b44a3043a070f279e6d491...cfe7b48bedd871c2132b2d62cd215d135e5e3627` (also `git diff --stat`).
- Worker report and gate evidence: /home/muszkin/work/kivvi-click/context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/slices/w0-login/worker-report.md, evidence/.
- Context: context/map/INDEX.md, architecture-and-flows.md, nearest .agents/project-context.md; oracle journey dir; deviations table; rules-translated.md.
- Preserved behaviours: zero-change rule; old stack untouched; specs untouched; no secrets; pinned versions.

Dimensions (review-rubric.md): acceptance correctness against the oracle steps; regression/preserved behaviour; logic; edge cases (empty/malformed inputs, unknown locale, fallback values); security (session cookie flags, input handling, secrets in compose/env, upload rules where applicable); data/migration safety (Flyway baseline only, schema frozen); concurrency (session, dedup, scheduler lock); architecture (ArchUnit/ESLint rules, layering, no old-stack edits, pinned versions); operability (health, logs); tests strength (do the named behaviour tests fail for the intended defect? any test that only asserts truthy?); scope (unrelated files, generated noise, stubs, TODOs, dead code).

Commands you may run read-only in the review checkout: `git log/diff/show`, `grep`, `cat`, `./mvnw -q test` (in backend/), `npm run test -- --run`, `npm run lint`, `npm run typecheck` (in frontend/), `node tools/migration-verify/compare.mjs --journey <id> --base <url>` only if a stack for this SHA is running (the orchestrator will tell you the URL) — otherwise inspect the worker's compare report.

Return: inspected base/head SHAs, commands run, findings (ID, severity, file:line, violated acceptance/invariant, scenario, evidence, required outcome, blocks?), and exactly one verdict.
