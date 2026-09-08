# Independent review packet — <slice-id>

Read-only. You did not implement this candidate. Do not edit code. Report findings with evidence; verdict PASS | FAIL | BLOCKED_EXTERNAL.

- Plan: /home/muszkin/work/kivvi-click/context/plans/2026-09-08-symfony-to-spring-vue-migration.md (sha256 <plan-sha>); journey packet section: <J-id>.
- Slice packet: <packet path> (sha256 <packet-sha>).
- Base SHA: <parent> · Head SHA: <candidate>. Verify both with `git rev-parse` in the review checkout before reading the diff.
- Review checkout (detached, read-only): <path> — created by the orchestrator with `git worktree add --detach <path> <candidate>`.
- Diff: `git diff <parent>...<candidate>` (also `git diff --stat`).
- Worker report and gate evidence: <run-dir>/slices/<slice-id>/worker-report.md, evidence/.
- Context: context/map/INDEX.md, architecture-and-flows.md, nearest .agents/project-context.md; oracle journey dir; deviations table; rules-translated.md.
- Preserved behaviours: zero-change rule; old stack untouched; specs untouched; no secrets; pinned versions.

Dimensions (review-rubric.md): acceptance correctness against the oracle steps; regression/preserved behaviour; logic; edge cases (empty/malformed inputs, unknown locale, fallback values); security (session cookie flags, input handling, secrets in compose/env, upload rules where applicable); data/migration safety (Flyway baseline only, schema frozen); concurrency (session, dedup, scheduler lock); architecture (ArchUnit/ESLint rules, layering, no old-stack edits, pinned versions); operability (health, logs); tests strength (do the named behaviour tests fail for the intended defect? any test that only asserts truthy?); scope (unrelated files, generated noise, stubs, TODOs, dead code).

Commands you may run read-only in the review checkout: `git log/diff/show`, `grep`, `cat`, `./mvnw -q test` (in backend/), `npm run test -- --run`, `npm run lint`, `npm run typecheck` (in frontend/), `node tools/migration-verify/compare.mjs --journey <id> --base <url>` only if a stack for this SHA is running (the orchestrator will tell you the URL) — otherwise inspect the worker's compare report.

Return: inspected base/head SHAs, commands run, findings (ID, severity, file:line, violated acceptance/invariant, scenario, evidence, required outcome, blocks?), and exactly one verdict.
