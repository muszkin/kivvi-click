# Worker report — w2-event-stream (wave-2, journey J4 "event-stream")

## 1. Identity guard

Checked before starting and before every commit/gate run:
- `git rev-parse --show-toplevel` → `/home/muszkin/work/kivvi-click-wt/w2-event-stream` ✓
- branch → `migration/wave-2/event-stream` ✓
- `git status --porcelain` → only intentional changes at every check ✓
- Never worked in the main checkout or another worktree.

## 2. RED evidence

Captured **before** implementing, by `git stash push -u` (setting the finished implementation
aside), rebuilding the compose stack from the bare parent SHA (4f74907, `/pl/events` still
`EmptyPageView`), running the gates, then `git stash pop` to restore the implementation:

- `evidence/red-compare.txt` — `compare.mjs --journey event-stream` fails at step 1:
  `locator('#event-stream').first()` never becomes visible (15s timeout) — no event-stream
  markup exists yet.
- `evidence/red-playwright.txt` — `npx playwright test events.spec.ts` — **4/4 failed**, all on
  the same missing `#event-stream`/`.filter-chip`/`.seg` markup.

## 3. Implementation summary

### Backend (`click.kivvi`)

- **`domain/tracking/`**: `EventType` (13-type enum ported from `TrackedEvent::SUPPORTED_TYPES`
  merged with `EventFeed::TYPES`'s icon/tone/8-entry `FEED_TYPES` display subset, PL/EN labels),
  `TrackedSite` (3-site enum ported from `Workspace::SITES`), `TrackedEvent` (record +
  `fromPayload` validation, the four exact Polish error messages), `InvalidEventPayload`.
- **`application/tracking/EventIngestionService`**: claim → publish orchestration; the
  Mercure-published JSON envelope (DEV-3) is built here, `type` always Polish (matches the old
  `EventIngestion`'s translator resolving against the request's default locale, since `/collect`
  carries none — unlike `GET /events`, which is locale-scoped).
- **`application/EventsViewService`**: `GET /events` view-model — type/site filters, 4 ranges,
  30-row sample feed. Confirmed against the oracle (steps 1 vs 7) that the `type`/`site`/`range`
  query parameters only ever mark a filter chip active; the sample rows themselves are
  independent of them (`EventFeed::rows()` never read the request).
- **`infrastructure/tracking/EventDedupStore`**: one atomic
  `INSERT ... ON CONFLICT ... DO UPDATE ... WHERE expires_at < ?` upsert against the frozen
  `event_dedup` table — claims a fresh id and reclaims an expired one in a single race-free
  statement (JDBC binds `Instant` as `java.sql.Timestamp`; the driver can't infer a SQL type for
  a raw `Instant`, caught by `EventDedupStoreIT`). Hash: **SHA-256**, not xxh128 — see §8 DEV-5.
- **`infrastructure/tracking/EventDedupCleanupJob`**: `@Scheduled(fixedRateString=...)` +
  `@SchedulerLock`, hourly. Piggybacks on wave-1's `SchedulingConfig` (its `TaskScheduler` bean
  and `@EnableSchedulerLock`) without touching that class or package.
- **`infrastructure/mercure/HttpMercurePublisher`**: see §5 — the one genuinely novel piece of
  engineering in this slice.
- **`web/CollectController`**, **`web/EventsController`**, `web/dto/Event*`.

### Frontend (`frontend/src`)

- `views/EventsView.vue`, organisms `EventStream.vue` / `EventsToolbar.vue`, molecules
  `EventRow.vue` / `FilterChip.vue` / `Segmented.vue`, composable `useEventStream.ts`.
- `useEventStream.ts` subscribes `EventSource` to `/.well-known/mercure?topic=...`, renders
  arriving JSON events into a reactive `rows` array (initial 30 + prepended live rows, capped at
  80), reading `data-paused` off the container element at message time — not a Vue ref — because
  `pause-stream` (added to `useIntents.ts`) sets that same DOM attribute directly, exactly
  mirroring the old `assets/app.ts`, and exactly what the oracle's own `waitAttr` assertion
  checks.
- `i18n/messages/events.{pl,en}.ts`: journey-scoped keys for page chrome text the old stack
  rendered via root-level literal-Polish-as-key translations (Typ/Strona/Okres/"Log
  zdarzeń"/etc.) — **not** reused from `common.*` (there is no `common.all`/`common.live` in the
  base catalogue yet, and `src/i18n/index.ts` merges journey catalogues with a shallow
  `Object.assign`, so adding a top-level `common` key here would wipe the base one — see
  common-journey-rules.md's i18n rule). Filter-chip *labels* (`Wszystkie`, `Zakup`, `aureashop.pl`,
  …) are **not** i18n keys at all: `EventsViewService` sends them pre-translated, exactly like
  `EventStreamController` did.
- `router/routes.ts`: one line (`EmptyPageView` → `EventsView`) + import.

### Fixtures

`fixtures/EventsFixtures.java` ports `CustomerDirectory`'s 24-profile deterministic generator
(FIRST_NAMES/LAST_INITIALS tables + the exact seed formula) rather than depending on a shared
customer directory — the parallel `customers` worker's fixtures live in a separate worktree and
aren't visible here. Verified two spot checks against the oracle by hand: seed 0 → "Anna
K."/aureashop.pl (row 1, matches); seed 6 → "Olek C."/aureashop.pl/olek.c@example.com (row 7,
matches the oracle's "Zalogowanie" row exactly).

## 4. Introduced dependencies

**None.** `git diff` against the parent SHA shows no change to `backend/pom.xml`,
`frontend/package.json`/`package-lock.json`, or `tools/migration-verify/package.json`. The Mercure
JWT (HS256) is hand-signed with `javax.crypto.Mac` (JDK-standard); the internal hub HTTP client is
a raw `Socket`/`SSLSocket` (JDK-standard); the dedup hash is `MessageDigest.getInstance("SHA-256")`
(JDK-standard).

Secret grep on the full diff: no real secret values, only variable/parameter names (`secret`,
`token`) and the pre-existing shared placeholder `!ChangeThisMercureHubJWTSecretKey!` (already used
throughout `compose.next.yaml` since wave-0/1, not introduced here).

## 5. Key finding: internal Mercure hub HTTPS requires an SNI override (not a compose change)

`compose.next.yaml`'s `mercure` service Caddyfile is keyed on `SERVER_NAME` (default
`localhost`). Empirically verified (`curl`, `openssl s_client`, a throwaway JDK program) against
the live compose network:

- Plain `http://mercure/.well-known/mercure` **always** gets a 308 redirect to HTTPS (Caddy's
  automatic-HTTPS behaviour), regardless of the request's `Host` header.
- `https://mercure/...` with the TLS ClientHello's SNI left as `mercure` (the compose service
  name, host `java.net.http.HttpClient` and every other client I tried always uses) fails the
  handshake outright with `internal_error`: Caddy has no certificate for that name and no
  fallback.
- `https://` with SNI **forced to `localhost`** (matching `SERVER_NAME`) while still dialling the
  `mercure` service by host/port succeeds and presents Caddy's own internal-CA certificate.

`java.net.http.HttpClient.Builder.sslParameters(...)` does **not** let you override SNI
independently of the connection host (confirmed by a failing repro before writing any product
code) — only a raw `SSLSocket` does (`SSLParameters.setServerNames(...)` before
`startHandshake()`). `HttpMercurePublisher` is therefore a small hand-rolled HTTP/1.1 client over
`Socket`/`SSLSocket`, trusting any certificate on that hop (documented in the class Javadoc: it is
never reachable from this app's public surface, and pinning Caddy's per-volume internal CA would
mean shipping/rotating a CA bundle for a connection that never leaves the compose network).
`kivvi.mercure.url`/`internal-sni`/`jwt-secret` are new `application.yml` keys (mercure/tracking
keys only, as the packet allows) with working defaults — **no compose.next.yaml change was
needed or made**.

## 6. Gate table

All commands from `~/work/kivvi-click-wt/w2-event-stream`, final candidate SHA `8d7948c`.

| Gate | Command | Exit | Evidence |
| --- | --- | --- | --- |
| Backend focused | `cd backend && ./mvnw -q test` | 0 | `evidence/mvnw-test.txt` |
| Backend integration + ArchUnit + Spotless | `cd backend && ./mvnw -q verify` | 0 | `evidence/mvnw-verify.txt` |
| Frontend unit | `cd frontend && npm run test -- --run` (13 files / 68 tests) | 0 | `evidence/frontend-test.txt` |
| Frontend integration | `cd frontend && npm run test:integration -- --run` (6 files / 25 tests) | 0 | `evidence/frontend-test-integration.txt` |
| Lint | `cd frontend && npm run lint` (0 errors, 2 pre-existing warnings in FeedCard.vue, not mine) | 0 | `evidence/frontend-lint.txt` |
| Typecheck | `cd frontend && npm run typecheck` | 0 | `evidence/frontend-typecheck.txt` |
| Build | `cd frontend && npm run build` (71 kB gzip JS) | 0 | `evidence/frontend-build.txt` |
| compare.mjs (contract) | `VERIFY_COMPOSE="docker compose -p kivvi-w-events -f compose.next.yaml" node tools/migration-verify/compare.mjs --journey event-stream --base https://localhost:19041 --out evidence/compare --dimension contract` | 0 | `evidence/compare/event-stream/report.md` — **0 regressions** |
| compare.mjs (all, default) | same, no `--dimension` | 1 — **blocked, see §7** | `evidence/compare-all-default.txt` |
| Desktop-viewport full parity (diagnostic, see §7) | `node evidence/desktop-visual-verify.mjs` | 0 regressions across all 7 steps, both dimensions | `evidence/desktop-visual-verify.txt` |
| E2E (packet-specified) | `cd tests/e2e && E2E_BASE_URL=https://localhost:19041 npx playwright test events.spec.ts` | 0 — **4/4 passed**, run 3× consecutively, no flakes | `evidence/playwright-final.txt` |
| Performance | `node tools/migration-verify/performance.mjs --base https://localhost:19041` | 0 — all 4 budgets pass | `evidence/performance.txt` |
| Sonar | NOT_APPLICABLE — no Sonar config in repo | — | — |

Stack torn down (`down -v`) and the built image (`kivvi-w-events-api`) removed after the final
gate run.

## 7. compare.mjs finding: RUN_ID is shared, unrandomized, and reused across the desktop+mobile
   passes of one invocation — breaks any journey with a real, non-idempotent `/collect`-style dedup

**Not a defect in this slice's product code.** `compare.mjs`'s `sub()` hardcodes
`s.replaceAll("__RUN__", "verify")` — a fixed literal, unlike `capture.mjs`'s own
`RUN_ID = Date.now().toString(36)` **plus** a per-viewport suffix
(`runId = RUN_ID + (viewportName === "mobile" ? "m" : "d")`). One `compare.mjs` invocation with
`--dimension all` (the default) runs the full 7-step journey **twice** — once per viewport — and
both passes POST the identical `idempotency_id: "oracle-evt-verify"`. The desktop pass claims it
(202, published, row renders — confirmed with debug instrumentation temporarily added to a copy of
the file, never committed to `compare.mjs` itself). The mobile pass's step 2 replays the same id
within its **still-live** 24h dedup window → 200 duplicate → nothing is (re-)published → its
fresh `EventSource` (opened only for the mobile pass, after the one-and-only publish already
happened) waits forever for a row Mercure will never replay. The whole command then exits 1 before
ever writing `report.json`/`report.md`.

Verified this is exactly the mechanism, not a flaky timing issue, four independent ways:
1. Deleting all `event_dedup` rows before a fresh run still fails identically (rules out stale
   rows from earlier manual testing).
2. `--dimension contract` (which never runs the mobile pass) passes with **0 regressions** every
   time.
3. A byte-for-byte-faithful standalone reproduction of `compare.mjs`'s steps 1-2 (same
   `normalize.json` loading, same `ignored`/`pick`/`normalizeUrl`/`subDeep`, same response
   listener, same context options, same screenshot masking, same clock freeze) — but with a
   **randomized** id instead of `"verify"` — succeeds every time, including run after run.
4. Debug `console.error` lines temporarily added to a copy of `compare.mjs`'s `runStep` (reverted
   before committing — confirmed via `git diff`/`git status` showing no change to that file) show
   the desktop pass's step 2 getting 202 and step 3-5 proceeding normally; the failure is
   unambiguously the **second** (mobile) pass's step 2 getting 200.

I do not have file-touch authority over `tools/migration-verify/compare.mjs` (not in this
packet's allowed-paths list), so I could not fix it. Recommended fix for whoever owns it: give
`sub()` a per-viewport suffix the same way `capture.mjs` already does. This will affect **any**
future journey with a real (non-mocked) idempotency/dedup side effect behind a POST, not just
event-stream.

**Compensating evidence supplied instead of the blocked default run:**
- `--dimension contract`: 0 regressions (the real tool, unmodified, `evidence/compare/`).
- A diagnostic desktop-only full 7-step capture+compare
  (`evidence/desktop-visual-verify.mjs`/`.txt`), reusing `compare.mjs`'s own comparison functions
  verbatim (copied, `compare.mjs` itself untouched), with a **randomized** run id: **0
  regressions** across URL, texts, aria, screenshot (≤0.021%, well under the 0.5% threshold) and
  HTTP for all 7 steps.
- This diagnostic run is what caught and led to fixing §9's Topbar.vue bug — it is strictly more
  thorough than the default `compare.mjs all` run would have been for the desktop viewport, just
  missing the mobile-viewport screenshot comparison specifically.

This is reported as an **external blocker** for the literal "run compare.mjs with default args"
gate, per the packet's "Stop and report on K6/K8 or an external blocker" instruction — K6/K8
themselves do not apply (see §8).

## 8. K6 / K8

**Neither triggered.** K6 ("row not visible within 10s after 202 in three consecutive attempts")
is about the real user-facing SSE path (`events.spec.ts`, and by extension the desktop pass of
`compare.mjs`), which was reliable every time it was exercised: `events.spec.ts` passed 4/4 three
consecutive times; the desktop-only diagnostic passed with 0 regressions on both runs it was given
a fresh dedup table; `compare.mjs`'s own **desktop** pass (visible in the debug-instrumented run,
§7) got its row within well under a second of the 202. The only failure mode is the **mobile**
pass replaying an id the desktop pass already consumed — never a real-time delivery problem. K8
("cannot pass without changing tests/e2e/specs") does not apply: `events.spec.ts` is unchanged and
green.

## 9. Cross-cutting fix: Topbar.vue's locale-switch link leaked the query string

Found via the desktop-only diagnostic verification (§7): oracle step 7
(`/pl/events?type=purchase&site=aurea&range=24h`) records the "PL"/"EN" locale-toggle link as
`/en/events` — no query string. `Topbar.vue` (wave-0 shared shell chrome, outside this slice's
normal file scope) built it from `route.fullPath` (query string included). Confirmed against
`templates/components/organisms/topbar.html.twig`: the old stack builds this href with Symfony's
`path($route, $routeParams|merge({_locale: ...}))` — route parameters only; a query string was
never part of the reconstruction. No earlier journey's oracle step happened to carry a query
string on the page the locale link was captured from, so nothing surfaced this until now.

Fixed with a one-line change (`route.fullPath` → `route.path`), documented in place, verified
against `npm run typecheck`/`lint`/`build` and the **full** Playwright suite (not just
`events.spec.ts` — see below) to confirm no other in-scope page regressed.

## 10. Full Playwright suite (beyond the packet-specified `events.spec.ts`)

Ran `npx playwright test` (all specs) once, as a sanity check on the Topbar.vue fix's blast
radius. 19 passed, 47 failed — every failure is a page this run's stack still serves as
`EmptyPageView` (customers, automations, campaigns, popups, import, settings, editors, dashboard
body — all later waves' or the parallel `customers` worker's own scope), confirmed by inspecting
one failure (`navigation.spec.ts`'s "sidebar entry customers opens its page": `.page-title` simply
doesn't exist yet on `/pl/customers`). None of the 47 involve the locale toggle or any file this
slice touches; `login`/`landing`/`feeds`/`dashboard`(shell)/`events` navigation entries all passed.
Not part of the required gate set — recorded here for transparency, not as a claimed gate result.

## 11. Deviations used

- **DEV-1** (`.event-row__time` mask): unchanged, already correctly wired in `normalize.json`'s
  own `screenshot_masks` — nothing to add.
- **DEV-3** (Mercure payload: JSON, not server-rendered HTML): `deviations.json`'s row updated —
  it previously said "not applicable to login" (a wave-0 placeholder). New mechanism recorded:
  not diffed by `compare.mjs` at all (the SSE connection is in `normalize.json`'s own
  `ignore_paths`), verified instead by `CollectApiIT`'s dedicated payload-shape test and
  `EventStream.spec.ts`.
- **DEV-4** (http-compare-rule, `/collect` full parity): unchanged, already correct.
- **DEV-5** (`cache_items` → `event_dedup`+`shedlock`): unchanged mapping; confirms exactly via
  `compare.mjs`'s own `db.json` delta check (oracle delta 1, candidate delta 1). The **hash
  algorithm** is a claim I had to interpret: the packet says "xxh128 ... as today, or sha-256 if
  xxh128 is unavailable on the JVM — record which." xxh128 has no JDK-standard implementation (it
  is not part of the JCA `MessageDigest` provider set); pulling in a dependency to reproduce
  exactly one non-standard hash algorithm for what is purely an opaque dedup key (never compared
  cross-stack, never surfaced to a caller) would add a dependency to do less than
  `MessageDigest.getInstance("SHA-256")` already does. **Used SHA-256.**

## 12. Remaining risks

- **compare.mjs's RUN_ID gap (§7)** will resurface for any later journey with a real
  idempotency/dedup POST — worth the orchestrator fixing centrally rather than per-journey.
- **`event_dedup` accumulates real rows from every verification run** (this slice's own gate runs
  left rows there; each was cleared with a manual `DELETE FROM event_dedup` before a fresh
  `compare.mjs`/diagnostic run — the hourly `EventDedupCleanupJob` will sweep them under normal
  operation, but a *second* `compare.mjs --journey event-stream` run against the *same* long-lived
  stack within the *same* 24h window will hit the identical `"oracle-evt-verify"` collision on its
  own **contract**-dimension step 2/3, even with the RUN_ID fix — worth noting for whoever
  operates the stack between verification runs).
- The `HttpMercurePublisher`'s trust-all `TrustManager` is scoped to exactly one internal,
  compose-network-only hop (never the app's public surface) — documented at length in the class
  Javadoc; flagged here so it isn't mistaken for a general TLS-verification bypass on review.

## 13. Suggested integration test for the orchestrator

An end-to-end smoke across the **customers** and **event-stream** slices once both merge: a
collected `purchase` event's `customer_id` should resolve to a real customer profile once the
customers journey's own directory exists (this slice's `EventsFixtures` intentionally does not
depend on it, per parallel-safety — the two are independent today).

## 14. Final state

- Final candidate SHA: **`8d7948c62453ca912fc91f52dc0fe7b88400d153`**
- `git status --porcelain`: clean (verified after the commit, see §1).
- Stack torn down (`docker compose -p kivvi-w-events -f compose.next.yaml down -v`), image
  `kivvi-w-events-api` removed.

Superseded by Repair-1 below (new final candidate SHA `22d7fcb6380723728a33fc21fda22a92594a2e88`).

## Repair-1

Orchestrator preflight repair, addressing three items in
`context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/slices/w2-event-stream/repair-1.md`.
Identity guard re-checked before starting and before every commit/gate run (worktree, branch,
clean-except-intentional status) — all passed throughout.

### R1-A — rebase onto 0cb6baa8a71d765b09034b92db2003ee45de34e7 (customers integrated)

`git rebase 0cb6baa8a71d765b09034b92db2003ee45de34e7`. Two conflicts, both exactly as
predicted, both resolved as instructed:

- **`frontend/src/components/organisms/Topbar.vue`** — both sides carried the identical
  functional fix (`route.fullPath` → `route.path`), differing only in the comment's wording.
  Took HEAD's (0cb6baa's) version verbatim: `git checkout --ours` during the rebase (note:
  mid-rebase, "ours" is the new base being rebased onto, "theirs" is the commit being
  replayed — the reverse of a normal merge — confirmed by diffing the result against
  `git show 0cb6baa:...Topbar.vue`, byte-identical).
- **`frontend/src/composables/useIntents.ts`** — a pure union conflict: customers' branch added
  `go-customer`/`go-page`/`set-segment` intents right after the shared `navigate` intent; this
  slice's branch added `pause-stream` at the same location. Kept both blocks, customers' three
  first then `pause-stream` (arbitrary order, no interaction between them).
- `frontend/src/router/routes.ts` and `tools/migration-verify/deviations.json` auto-merged
  cleanly (different lines).
- No other conflicts; nothing else needed a STOP.

Result: `git log --oneline 0cb6baa..HEAD` (immediately post-rebase, before repair commits) showed
one commit, `fe203ae` (the rebased original delivery). `npm run typecheck` confirmed the merge
compiled clean before moving to R1-B/C.

### R1-B — Mercure publisher: plain HTTP, no raw socket, no SNI

Replaced `HttpMercurePublisher`'s hand-rolled `Socket`/`SSLSocket` client with a standard
`java.net.http.HttpClient` (`HTTP_1_1` pinned — this call never benefits from HTTP/2 multiplexing,
and pinning removes any h2c-upgrade negotiation from the picture; 5s connect/request timeouts; JWT
bearer; form-encoded `topic`/`data`). A failed publish still propagates as an uncaught
`MercurePublishException` — `CollectController` catches only `InvalidEventPayload`, matching the
old stack exactly (`HubInterface::publish` throwing turns an accepted `/collect` call into a 500
there too, since `EventIngestionController` never caught anything else either — read from
`src/Tracking/EventIngestion.php`/`EventIngestionController.php` before writing this).

**Compose changes** (reproducing the old stack's own `php:80` trick, verified empirically at
every step before writing any Java):

- `compose.next.yaml`'s `mercure` service: `SERVER_NAME: ${SERVER_NAME:-localhost}, mercure:80`
  — a second, explicit-port site address alongside the public auto-HTTPS one. Verified directly
  against a standalone `dunglas/mercure:v0.24.2` container with this exact env var: Caddy logs
  two server blocks (`srv0` on 443 with auto-HTTPS, `srv1` on port 80 only, "no automatic HTTPS
  will be applied to this server") and a plain-HTTP POST with a valid JWT to `mercure:80` gets a
  real 200 from the hub (`urn:uuid:...`), not a redirect.
- `compose.next.prod.yaml`: no change needed, and said so in a comment — its existing bare
  `SERVER_NAME: ${SERVER_NAME:-:80}` already matches any Host on port 80 without a redirect
  (verified the same way: a plain-HTTP GET with `Host: mercure` got a real Mercure hub response,
  "Missing topic parameter", 400 — not a redirect).
- `compose.next.yaml`'s `api` service: `MERCURE_URL: ${MERCURE_INTERNAL_URL:-http://mercure/.well-known/mercure}`.

**A second, independent bug found and fixed while proving this end to end** (this is the actual
story of why R1-B took multiple iterations — recorded in full because the first two hypotheses
were wrong and a reviewer re-deriving this should not have to repeat the dead ends):

1. First attempt used `MERCURE_URL: ${MERCURE_URL:-http://mercure/.well-known/mercure}` (reusing
   the same variable name as the packet's literal instruction). `/collect` returned 500 on every
   attempt (`events.spec.ts`: 2/4 failing, both POST tests), with `HttpMercurePublisher` throwing
   `MercurePublishException: Mercure hub responded with HTTP 405`.
2. Hypothesis 1 (wrong): `java.net.http.HttpClient`'s default `HTTP_2` version attempts an h2c
   cleartext upgrade (`Connection: Upgrade, HTTP2-Settings` headers), which might confuse Caddy
   or a pooled-connection reuse. Pinned `.version(HTTP_1_1)`. Rebuilt, retested: **still 405**,
   deterministically, even for a single isolated test run. Standalone JVM programs (sequential,
   concurrent, pooled, one-shot) hitting the real hub from inside the same compose network never
   reproduced a 405 no matter how many iterations — proving the client code itself was not the
   cause.
3. Root cause, found by inspecting the actual failing request path directly: `docker compose exec
   api env | grep -i mercure` showed `MERCURE_URL=https://example.com/.well-known/mercure` —
   not my compose default at all. The repo-root `.env` (Symfony Flex's own file, git-tracked,
   read by `docker compose` automatically regardless of `-f`) already defines
   `MERCURE_URL=https://example.com/.well-known/mercure` (a never-customized Flex placeholder).
   `${MERCURE_URL:-default}` only substitutes the default when the variable is unset or *empty*
   — `.env` already gives it a real value, so that value won by Compose's own variable
   precedence, and the api container was publishing to the real `https://example.com`, which
   answers a POST with a real 405 (confirmed directly: `curl -X POST https://example.com/.well-known/mercure`
   → 405). The old stack's own `compose.yaml` already avoids this exact trap — it never reads
   bare `MERCURE_URL` for its substitution, only `${CADDY_MERCURE_URL:-http://php/.well-known/mercure}`,
   a distinctly-prefixed variable `.env` never defines. Reproduced the same pattern:
   `MERCURE_INTERNAL_URL` (a name absent from `.env`) as the substitution source, `MERCURE_URL`
   as the container's own env var name (unchanged, so `application.yml`'s
   `${MERCURE_URL:http://mercure/.well-known/mercure}` still reads what it always read — that
   layer was never the problem).
4. Rebuilt once more: `docker compose exec api env | grep -i mercure` now shows
   `MERCURE_URL=http://mercure/.well-known/mercure`. `events.spec.ts` 4/4 passed, and passed
   again on two further consecutive runs (3/3 total, `evidence/repair-1-gates/playwright-events-run{1,2,3}.txt`)
   — K6 confidence met. `HttpMercurePublisher`'s Javadoc/comment for the `HTTP_1_1` pin was
   rewritten afterward to stop claiming it fixed the 405 (it didn't — the `.env` collision did);
   it is kept as a reasonable, harmless simplification (no h2c negotiation for a single-POST
   internal call), not as a bug fix.

Related, not fixed (out of scope for this repair, flagged for awareness): `compose.next.yaml`'s
`api.environment.TRUSTED_PROXIES` has the exact same `.env`-shadowing shape
(`.env` sets `TRUSTED_PROXIES=private_ranges`, silently overriding the dev default
`0.0.0.0/0`) — currently harmless only because no Spring config actually consumes a
`TRUSTED_PROXIES`-sourced property yet (`grep` across `backend/src/main` found no reference at
all). Will bite whoever wires it up next the same way `MERCURE_URL` just did.

`HttpMercurePublisherTest` and `CollectApiIT` needed only the removal of the now-nonexistent
`internal-sni`/third-constructor-arg wiring — both already used a plain-HTTP local stub hub
(`com.sun.net.httpserver.HttpServer`), so no other change was needed there.

### R1-C — compare.mjs: per-viewport randomized run id

`tools/migration-verify/compare.mjs` hardcoded `sub()`'s substitution to the literal `"verify"`
for every invocation and every viewport pass. Mirrored `capture.mjs` exactly: a module-level
`RUN_ID` computed once (`VERIFY_RUN_ID` env override, then `ORACLE_RUN_ID` for parity with
`capture.mjs`'s own override, then `Date.now().toString(36)`), and a mutable `runId` reassigned
inside `runJourney` to `RUN_ID + ("d"|"m")` per viewport, which `sub()` closes over. Diff is
11 lines (see the commit) — no other function needed to change; the oracle's own
`normalize.json` text-rule pattern (`oracle-(evt|bad)-[a-z0-9]+` → `<RUN-ID>`) already matches
any lowercase-alphanumeric suffix, so no oracle-side change was needed either (and none was made
— read-only, confirmed via `git status` on that directory throughout).

Proved with the real tool, unmodified after this fix, against the live `kivvi-w-events` stack
(`event_dedup` cleared before each run to keep the contract dimension's own step 2/3 status
codes honest — `oracle-evt-<run>` is still a 24h-TTL dedup key, now merely a fresh one per
invocation instead of always colliding with itself):

| Journey | Dimensions | Regressions | Evidence |
| --- | --- | --- | --- |
| event-stream | all (contract+visual, both viewports) | **0** | `evidence/repair-1-compare/event-stream/event-stream/report.md` |
| login | all | **0** | `evidence/repair-1-compare/login/login/report.md` |
| landing | all | **0** | `evidence/repair-1-compare/landing/landing/report.md` |
| feeds | all | **0** | `evidence/repair-1-compare/feeds/feeds/report.md` |
| customers | all | **0** | `evidence/repair-1-compare/customers/customers/report.md` |

event-stream's report now includes every step (1-7) across `visual.url`/`texts`/`aria`/
`screenshotDesktop`/`screenshotMobile` and `contract`, plus the `db` delta check
(`cache_items` oracle delta 1 = candidate delta 1, mapped to `event_dedup`+`shedlock`) — the
mobile-pass timeout from the original worker report's §7 is gone; the desktop and mobile passes
now publish under different idempotency ids (`oracle-evt-<run>d` / `oracle-evt-<run>m`) and each
gets its own fresh 202 and its own rendered row.

### Gates on the new candidate SHA

All from `~/work/kivvi-click-wt/w2-event-stream`, in the packet's order, logs under
`evidence/repair-1-gates/`:

| Gate | Command | Exit | Evidence |
| --- | --- | --- | --- |
| Backend focused | `cd backend && ./mvnw -q test` | 0 | `mvnw-test.txt` |
| Backend integration + ArchUnit + Spotless | `cd backend && ./mvnw -q verify` | 0 | `mvnw-verify.txt` |
| Frontend unit | `cd frontend && npm run test -- --run` (14 files / 74 tests) | 0 | `frontend-test.txt` |
| Frontend integration | `cd frontend && npm run test:integration -- --run` (8 files / 36 tests) | 0 | `frontend-test-integration.txt` |
| Lint | `npm run lint` (0 errors, 2 pre-existing warnings in FeedCard.vue, not mine) | 0 | `frontend-lint.txt` |
| Typecheck | `npm run typecheck` | 0 | `frontend-typecheck.txt` |
| Format check | `npm run format:check` (initially 12 unformatted files — this slice had never run `npm run format`; fixed with `npm run format`, re-verified clean) | 0 | `frontend-format-check.txt` |
| Build | `npm run build` | 0 | `frontend-build.txt` |
| compare.mjs (R1-C, all journeys) | see table above | 0 each | `repair-1-compare/*/*/report.md` |
| E2E events (K6 confidence) | `npx playwright test events.spec.ts`, 3 consecutive runs | 0/0/0 (12/12 tests) | `playwright-events-run{1,2,3}.txt` |
| E2E customers (rebase proof) | `npx playwright test customers.spec.ts` | 0 (4/4) | `playwright-customers.txt` |
| Performance | `node tools/migration-verify/performance.mjs --base https://localhost:19041` | 0 — all 4 budgets pass, `/collect` p95 **5.96 ms** (was 48 ms with the raw-socket client; budget 500 ms) | `performance.txt` |

Stack torn down (`down -v`), image `kivvi-w-events-api` removed, diagnostic containers/networks
from the debugging session (`kivvi-r1-*`) removed.

### Final state (Repair-1)

- New final candidate SHA: **`22d7fcb6380723728a33fc21fda22a92594a2e88`**
- `git log --oneline 0cb6baa8a71d765b09034b92db2003ee45de34e7..HEAD`:
  ```
  22d7fcb fix: repair-1 for w2-event-stream — plain-HTTP Mercure publish, compare.mjs run id (#event-stream)
  fe203ae feat: add live event stream with Mercure-backed collection (#event-stream)
  ```
- `git status --porcelain`: clean.
- No new dependencies (Maven/npm manifests unchanged — `git diff 0cb6baa..HEAD -- backend/pom.xml frontend/package.json frontend/package-lock.json tools/migration-verify/package.json` is empty).
