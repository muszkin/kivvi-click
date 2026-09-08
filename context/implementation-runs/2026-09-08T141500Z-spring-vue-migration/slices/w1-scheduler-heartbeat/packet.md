# Slice packet w1-scheduler-heartbeat — wave-1, journey J3 "scheduler-heartbeat" (backend only)

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w1-scheduler-heartbeat · branch `migration/wave-1/scheduler-heartbeat` · parent SHA <PARENT-SHA>. **Lease:** compose project `kivvi-w-heartbeat`, HTTP_PORT=19030, HTTPS_PORT=19031. **Model:** sonnet, effort HIGH.

**Capability:** The system emits a scheduler heartbeat every hour through the worker and logs it.
**Oracle:** journeys/scheduler-heartbeat/contract.md (static). Behaviours: B20, B33.
**Old-stack sources:** `src/Schedule.php`, `src/Message/Heartbeat.php`, `src/MessageHandler/HeartbeatHandler.php`, `compose.yaml` worker service, `tests/CacheTest.php`.

## In scope
- Add ShedLock 7.10.0 (`net.javacrumbs.shedlock:shedlock-spring` and `shedlock-provider-jdbc-template`, Apache-2.0) to `backend/pom.xml`; `@EnableScheduling` + `@EnableSchedulerLock(defaultLockAtMostFor = "PT55M")`; a `HeartbeatJob` in `application` (or `infrastructure/scheduling`) with `@Scheduled(fixedRate = 1 hour)` and `@SchedulerLock(name = "heartbeat", lockAtLeastFor = "PT1M")` that logs exactly `Scheduler heartbeat tick.` at INFO; the `shedlock` table already exists in `V1__baseline.sql` (verify its columns match ShedLock's JDBC template schema; if not, STOP and report — schema is frozen).
- Missed-run semantics: after downtime exactly one catch-up run (ShedLock `lockAtMostFor` + fixedRate gives at-most-one; document the behaviour and test it with a mutable `Clock`).
- DEV-6: the job runs inside `api` (no separate worker container) — document in your report.
- Tests: unit with a `Clock`; integration (Testcontainers) asserting one `shedlock` row and one log line captured with a Logback list appender (B20 = Postgres-backed lock/state round-trip, B33 = tick logged); ArchUnit must still pass (scheduling code may live in `infrastructure`; if you put it in `application` it must not import Spring JDBC directly — respect the layering).
- compare.mjs: this journey is static (no browser steps) — `compare.mjs --journey scheduler-heartbeat` must report the journey as static/parity by checking the db mapping only; if the tool cannot handle a static journey, add minimal handling in `tools/migration-verify/compare.mjs` (only the static-journey branch) and record it.

## Out of scope
Any page, any frontend file, any compose change (the job runs in the existing `api` service).

## Deviations in scope
DEV-5, DEV-6.

## Specs to run
None (no browser surface). E2E gate for this journey = the integration test + a stack boot check: bring your stack up, confirm the `api` log shows the scheduler initialised (or the tick if you shorten the rate via a test-only property — never in the default profile) and `shedlock` exists. Record as evidence.

## Parallel-safety obligations
Shared wave with `landing` and `feeds`. Touch only: `backend/pom.xml` (dependency block only), `backend/src/main/java/click/kivvi/{application|infrastructure}/scheduling/**`, `backend/src/main/java/click/kivvi/KivviApplication.java` (annotations only — verify the actual main class name) or a new `infrastructure/config/SchedulingConfig.java` (preferred: no edit to the main class), `backend/src/main/resources/application*.yaml|properties` (scheduling keys only), `backend/src/test/**/heartbeat*|scheduling*`, `tools/migration-verify/compare.mjs` (static-journey branch only, if needed), your report/evidence.
