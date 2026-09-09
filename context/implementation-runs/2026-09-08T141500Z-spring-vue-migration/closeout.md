# Closeout: Symfony/Twig → Spring Boot 4.1 + Vue 3 SPA migration run (2026-09-09)

**Request:** execute the approved plan `context/plans/2026-09-08-symfony-to-spring-vue-migration.md` in the `migration` profile; operator contract 2026-09-08: fire-and-forget, terminal outcome `local-green` (`CUTOVER_READY` on `migration/spring-vue`), daily-coding model at HIGH effort; the operator wants the new stack to end up on the same port (23456) for their own verification, after one explicit confirmation.
**Delivered:** all 13 journeys at oracle parity on the new stack; feature branch `migration/spring-vue` @ `92052947a6238ecf4ee177f4e72493122315ce32` (worktree `/home/muszkin/work/kivvi-click-wt/integration`), not pushed, not merged; production untouched by the run. Cutover runbook in `README.md` ("Next stack (Spring Boot + Vue)").
**Identifiers:** run `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration`; waves 887af45 → 4f74907 → b87a701 → 11d3cc4 → 60445eb → dae1696 (final cohort) → 9205294 (final gates); oracle manifest sha256 `4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f`; plan revisions recorded in `run.json`.
**Terminal outcome reached:** `local-green` selected; `CUTOVER_READY` proved (`run.json.state`). RR-1/CUT-1/CON-1 not executed (authorization boundary).

## Gates

Copied from `run.json`/`events.jsonl`:

- Wave cohorts (unit, integration, architecture, contract, visual, e2e), all ending 6/6 PASS: wave-0 3 rounds, wave-1 2, wave-2 2, wave-3 4 rounds (repair budget K1 fully used — 3 repairs), wave-4 3, wave-5 + FINAL all-journey cohort 2 rounds (`waves/final/`, SHA dae1696).
- Final gates on 9205294 (`final-gates/9205294/`): `./mvnw verify` exit 0 (299 unit, 142 IT, 1 disabled red-proof, spotless, ArchUnit) — attempt 1 had 13 transient `NoSuchMessageException` in `ShellViewServiceTest`, not reproducible; `docker build` exit 0; vitest 169 unit / 152 integration; lint, typecheck, format:check, build exit 0; `docker compose … config` (prod overlay) renders without warnings; full Playwright suite 122/122 incl. `navigation.spec.ts` ×5 at 14/14; performance budgets 4/4; contract 13/13 (DEV-13 confirmed unused).
- Sonar: `NOT_CONFIGURED` (no Sonar in the repository) — not green, not deferred.
- Independent combined review: backend PASS, frontend PASS, tooling/ops FAIL → repaired in `w5-ops-cutover` (volume names `kivvi-next_*`, secrets wiring, `forward-headers-strategy: native`, multipart 8 MB, CI image build, runbook) → PASS.
- Inconclusive runs ruled and re-run in isolation: wave-3 round-4 e2e and wave-4 round-2 e2e (host CPU starvation), Testcontainers startup timeouts on the NAS-backed Docker root (`final-gates/97253f6/`).

## Open obligations

| Obligation | Owner | Trigger to close |
| --- | --- | --- |
| RR-1 rehearsal (project `kivvi-stage`, port 23458) then CUT-1 on 23456; rollback = `up` the old stack | Piotr Mucha | explicit go; runbook `README.md#next-stack` |
| CON-1: remove the old stack, rewrite `AGENTS.md`/`CLAUDE.md` stack rules (R19), re-point Linear tasks PIO-70..115, retire DEV-13, plan errata (CUT-1 command, ports, fixtures as Java classes — R20) | Piotr Mucha / `migration-planning` follow-up | after CUT-1 observation window |
| Frontend: no error handling on view data fetches; sidebar double-click within one RTT | frontend maintainer | post-cutover hardening |
| Backend: 18 IT classes boot separate contexts/containers; comment noise in 4 files | backend maintainer | test-infra hygiene |
| Monitoring for production still absent (PIO-112); R17 restart policy fixed | Piotr Mucha | Linear PIO-112 |
| Host: other projects' containers/GitHub runners and the Docker data-root move to `/mnt/nas/docker-root` (2026-09-09) contend for CPU/disk (R21) | Piotr Mucha | host capacity decision |

## Incident

2026-09-09 11:52–12:11Z the operator masked and stopped Docker to rsync `/var/lib/docker` to `/mnt/nas/docker-root` (production down by the operator's own action). At 11:55:19Z the backend final-review subagent ran `sudo systemctl start docker`, possibly interfering with the operator's first rsync attempt. Rule added to `common-journey-rules.md`; proposed for the global agent instructions (see report).

## Discarded during triage

- Per-journey implementation details (routes, DTOs, components) — already recorded by the code, tests and commit history.
- Verification sequencing, disk hygiene, whitespace-node and frozen-clock rules — already recorded in `common-journey-rules.md` and the refreshed scoped contexts (`backend/.agents/project-context.md` etc.).
- DEV-14 rejection (upload redirect reproduced as a full navigation) — an application of the plan's zero-change rule, recorded in `slices/w4-import-wizard/repair-1.md`; no new decision.
- Transient tooling friction (backgrounded shells killed by the harness, `open('w')` truncation, heredoc backtick mangling) — private agent memory only.


## Addendum: cutover and consolidation (2026-09-09)

- RR-1 PASS (`cutover/rr1.md`). CUT-1 live 14:18:42Z after two rolled-back attempts caused by the orchestrator (wrong compose project directory; Symfony auto-created tables in the fresh volume) — outages 14:08:42–14:13:23Z and 14:13:53–14:16:10Z; production data never mounted by the new stack. 20-min observation clean; public e2e 66/66 (`cutover/cut1.md`).
- CON-1 (operator: "zrob to"): squash merge `8b224c5` on `main`; old stack deleted (`7287390`, `25329ac`, `0b46320`), `compose.next*` → `compose*`, workflow `build.yml`; plan errata + DEV-13 retirement (`8c37b92`); docs rewritten (`fe9c3fe`); context map refreshed; 46 Linear issues re-pointed; old images/volumes removed after `pg_dumpall` (personal backup path recorded in the run ledger only); exit e2e green (`cutover/con1-e2e.md`).
- Not done: `git push` (operator's call); `.claude/settings.json` format hook still targets the old stack (R22); stale "old stack" comments in compose/Caddyfile/env example (R23); monitoring (PIO-112).
