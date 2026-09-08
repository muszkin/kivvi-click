# Wave wave-2 — dimension: integration — round 2 (independent re-verification)

**Dimension status: PASS**

Bound to wave SHA `b87a701244f5316e1a53bace7a1e41facdffc277`, checked out (detached HEAD)
at `/home/muszkin/work/kivvi-click-wt/verify-wave-2-integration` and verified there only.
No compose stack was started or stopped by this verifier; the running `kivvi-int` stack
(https://localhost:19101) was not touched. Backend integration tests use Testcontainers
(ephemeral `postgres:18-alpine` containers reaped by Ryuk), which the packet explicitly
allows.

## Journeys in scope and verdicts

| Journey | Verdict | Basis |
| --- | --- | --- |
| event-stream | parity | `CollectApiIT` (6/6, real HTTP + Testcontainers Postgres) covers B15, B16, B18, B19 and the DEV-3 hub-payload shape; `EventsApiIT` (3/3) covers B01 (document 200) and B24 (`/api/v1/pl/events` payload shape); `EventsView.spec.ts` (9/9, real router + i18n, network boundary stubbed) covers B24 component/store behaviour incl. live SSE row prepend/cap/pause. |
| customers | parity | `CustomersApiIT` (7/7, real HTTP + Testcontainers Postgres) covers B25 (index + detail payload, SPA document), B05 (unknown id 404 on both API and document routes), B03 (shell `currentSection`/`crumb` for `customer_show`); `CustomersView.spec.ts` (5/5), `CustomerView.spec.ts` (5/5) and `CustomerDetailSidebarSection.spec.ts` (3/3) — all real router (+ Pinia for the sidebar spec), network boundary stubbed — cover B25 and the frontend half of B03. |

No regression: the same `mvnw verify` and `npm run test:integration` runs also executed every
other wave's integration tests (`EventsApiIT`\*, `FeedsApiIT`, `LandingApiIT`, `ShellApiIT`,
`SessionRoundTripIT`, `HeartbeatSchedulerIT`, `EventDedupStoreIT` server-side;
`landing.spec.ts`, `FeedsView.spec.ts`, `LoginView.spec.ts`, `shellStore.spec.ts`,
`Sidebar.spec.ts` frontend-side) as regression guards — all green, 0 failures.

\* `EventsApiIT` is itself part of this wave's own journey (event-stream), not a regression
guard; listed once above under its journey.

## DEV-3 note (informational, not gating this dimension)

`deviations.json` assigns DEV-3 (Mercure publish payload: JSON event instead of server-rendered
HTML) to the **contract** dimension, not integration; `capture/normalize.json` excludes
`/.well-known/mercure` from HTTP recordings so `compare.mjs` has nothing to diff there. The
deviation record itself names `CollectApiIT`'s `acceptedEventIsPublishedAsJson` test (here
passing, `@DisplayName("DEV-3 the accepted event is published to the hub as JSON, not
server-rendered HTML")`) as one of its three verification points. Observed here as expected,
accepted behaviour — not a regression against this dimension's own qualifying-test bar.

## Commands run

| # | Command | cwd | Exit code |
| --- | --- | --- | --- |
| 1 | `npm ci` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-integration/frontend` | 0 |
| 2 | `npm run test:integration` (→ `vitest run test/integration`) | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-integration/frontend` | 0 — 9 files / 43 tests passed |
| 3 | `export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH; ./mvnw -q verify` | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-integration/backend` | 0 — failsafe summary: 39 completed, 0 errors, 0 failures, 0 skipped |
| 4 | `npm run test:integration` (re-run, output captured to evidence) | `/home/muszkin/work/kivvi-click-wt/verify-wave-2-integration/frontend` | 0 — 9 files / 43 tests passed (same result as #2) |

Java: Temurin 25.0.4.1 LTS (`java -version` confirmed before running Maven). Node: v26.8.1 (host).
Docker: available, used only for Testcontainers-managed Postgres containers (not for any compose
project).

## Evidence paths

- `waves/wave-2/integration/backend/mvnw-verify.log` — full `./mvnw -q verify` console output.
- `waves/wave-2/integration/backend/failsafe-summary.xml` — aggregate IT result (39/0/0/0).
- `waves/wave-2/integration/backend/click.kivvi.CollectApiIT.txt` — 6/6, 0 failures.
- `waves/wave-2/integration/backend/TEST-click.kivvi.CollectApiIT.xml` — per-test XML.
- `waves/wave-2/integration/backend/click.kivvi.CustomersApiIT.txt` — 7/7, 0 failures.
- `waves/wave-2/integration/backend/TEST-click.kivvi.CustomersApiIT.xml` — per-test XML.
- `waves/wave-2/integration/backend/click.kivvi.EventsApiIT.txt` — 3/3, 0 failures (in-scope journey).
- `waves/wave-2/integration/backend/click.kivvi.{FeedsApiIT,LandingApiIT,ShellApiIT,SessionRoundTripIT,infrastructure.scheduling.HeartbeatSchedulerIT,infrastructure.tracking.EventDedupStoreIT}.txt` — regression guards, all 0 failures.
- `waves/wave-2/integration/frontend/vitest-integration.log` — full `npm run test:integration` console output (9 files / 43 tests passed).

## Environment / scope notes

- Worked only inside `/home/muszkin/work/kivvi-click-wt/verify-wave-2-integration`; no other
  checkout, the main repo, or the oracle was touched.
- No tracked file in the checkout was edited.
- Evidence was written only under this run's `waves/wave-2/integration/` directory and this
  summary file.
- Round-1 evidence under `waves/wave-2/round-1/` was not read; this verification was performed
  independently, fresh, against the wave SHA only.
- Worker reports and review files were not read.
