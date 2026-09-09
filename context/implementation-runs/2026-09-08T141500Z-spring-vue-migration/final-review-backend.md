PASS

Scope reviewed: `git diff 8d3fc32...dae1696 -- backend compose.next.yaml compose.next.prod.yaml
.github/workflows/next-build.yml` (181 files, +16257/-0 — the whole Spring Boot backend is new).
`./mvnw -q verify` run twice against Testcontainers Postgres 18 (Docker daemon required a manual
`sudo systemctl start docker` — inactive at session start); full suite green (all unit + 17 ITs +
ArchUnit + Spotless). Two mutation probes applied and reverted (see below). `backend/target`
deleted; `git status --porcelain` empty at end.

## Findings (ordered by severity)

### 1. MEDIUM — IT suite never reuses a Spring context/Testcontainers instance across classes
Every one of the 18 `*ApiIT`/`*IT` classes (`CollectApiIT.java:51-52`, `AutomationsApiIT.java:33`,
`ImportApiIT.java:48`, `EventsApiIT.java:28`, `WidgetApiIT.java:31`, `SettingsApiIT.java:34`,
`CampaignsApiIT.java:31`, `ShellApiIT.java:40`, `FeedsApiIT.java:30`, `HeartbeatSchedulerIT.java`,
`EventDedupStoreIT.java`, etc.) declares its own `static PostgreSQLContainer`. Because Spring's
`@SpringBootTest` context cache keys on the merged config (including the datasource URL each
container publishes on a random port), no two classes ever share a context — 13+ full Spring
contexts (Tomcat + Hikari pool + Flyway run) were alive at once by the end of the run. The verify
log shows the shutdown-hook cascade (13 sequential `HikariPool-N - Shutdown …` pairs, ~2s each)
running past Surefire's grace window: `[ERROR] Surefire is going to kill self fork JVM. The exit
has elapsed 30 seconds after System.exit(0).` Harmless today (exit code 0), but it is a real,
worsening cost (wall-clock IT time, CI log noise, and a forced-kill path that could someday race a
real failure into ambiguity) that will only grow as more `*ApiIT` classes are added.
**Fix:** introduce a shared abstract base (`AbstractPostgresIT`) with one `static @Container`
Postgres (JVM-singleton pattern or `.withReuse(true)`) that every `*ApiIT` extends, so the Spring
test context cache actually hits.

### 2. MEDIUM — `next-build.yml` never builds `backend/Dockerfile`
`.github/workflows/next-build.yml:45-56` reproduces the multi-stage Dockerfile's steps by hand
(`npm run build` → copy into `src/main/resources/static` → `mvnw verify`) but never runs
`docker build -f backend/Dockerfile .`. The three-stage Dockerfile (frontend build → jar package →
non-root JRE runtime, `backend/Dockerfile:1-34`) is exactly the image `compose.next.yaml`/
`compose.next.prod.yaml` ship, and it is not exercised by CI at all — a broken `COPY --from=` path
or a Docker-only build failure would only surface when someone manually runs
`docker compose -f compose.next.yaml up --build`.
**Fix:** add a `docker build -f backend/Dockerfile -t kivvi-click-api:ci .` step (no push needed,
consistent with "not deployed by this slice") to catch Dockerfile breakage before CUT-1.

### 3. MEDIUM/LOW — `TRUSTED_PROXIES` is dead, misleading configuration
`compose.next.yaml:39` (`TRUSTED_PROXIES: ${TRUSTED_PROXIES:-0.0.0.0/0}`) and
`compose.next.prod.yaml:16` (`${TRUSTED_PROXIES:-private_ranges}`) both set this env var, and it
reads as a proxy allow-list gating `application.yml:21`'s `server.forward-headers-strategy:
framework`. It is never consumed anywhere in `backend/src/main/java` or `application.yml` —
grepped, zero hits. Spring's `ForwardedHeaderFilter` (the "framework" strategy) has no
trusted-proxy concept at all; only the "native" (Tomcat `RemoteIpValve`) strategy does. Today this
is low-impact because `api` publishes no host port (`compose.next.yaml` has no `ports:` for `api`)
and is reachable only from the `mercure` Caddy edge over the private compose network, which itself
computes `X-Forwarded-*` from the real connection — but the env var actively misrepresents a
control that does not exist, and would become a real gap the day `api` is ever exposed directly.
**Fix:** either wire it (switch to `native` + `server.tomcat.remoteip.internal-proxies`) or delete
the variable and document in `application.yml` why "framework" needs no allow-list here.

### 4. LOW — multipart upload size left at Spring Boot defaults
`application.yml` has no `spring.servlet.multipart.*` block, so the 1 MB/file, 10 MB/request Boot
defaults apply to `POST /import/upload` (`ImportUploadController.java:46-59`). Not a security
issue (it *bounds* upload size), but a possible functional-parity gap: the old FrankenPHP stack's
own `upload_max_filesize`/`post_max_size` were not found configured in this repo either, so the
effective old-stack ceiling is unverified. A real customer CSV over 1 MB would now 413 where it
previously may have been accepted.
**Fix:** confirm the intended ceiling and set the properties explicitly (self-documenting even if
the value chosen equals the default).

### 5. LOW — comment/Javadoc noise vs. the repo's "minimal, purposeful comments" rule
- `infrastructure/mercure/HttpMercurePublisher.java:14-33` — 20-line class Javadoc narrating the
  old stack's Caddy `php:80`/SNI trick; the "no SNI to reason about" aside and the historical frame
  add nothing a maintainer needs. Trim by half.
- `application/WidgetViewService.java:8-19` — cross-file narrative ("see that view's own comment",
  "`AutomationsView.vue`/`useIntents.ts`'s own … precedent") reads like a design-review breadcrumb
  trail, not documentation. Trim to one sentence.
- `web/ImportUploadController.java:37-45` — a "Follow-up (review finding, LOW)" Javadoc narrating a
  prior review round and an "earlier version of this method" that no longer exists in the diff. Cut
  the historical narration; keep only the current behaviour (present-but-empty file is stored, not
  rejected).
- `infrastructure/session/SessionRequestSerializationFilter.java:13-51` — ~40-line class Javadoc.
  Each paragraph does carry a real, non-obvious invariant (filter order vs. `SessionRepositoryFilter`,
  why `getCookies()` not `getSession()`), so this is the most defensible of the four, but could
  still be trimmed ~30%.

### 6. LOW — raw `Thread.sleep` in concurrency tests
`SessionRequestSerializationIT.java:110`, `SessionRequestSerializationRedProofIT.java:94`,
`SessionLockRegistryTest.java:124` use `Thread.sleep(20)`/`Thread.sleep(50)` after a
`CountDownLatch.await` rather than a poll/`Awaitility` barrier. Margins are generous (a 400 ms
artificial delay vs. a 20 ms sleep), so this is not an active flake source, but it is the one
non-deterministic pattern in an otherwise solid concurrency test suite.

## Mutation-test evidence (both probes applied, confirmed RED, reverted; `git status --porcelain`
clean afterward)

- **Unit:** `EmailValidation.errorFor` (`domain/EmailValidation.java`) forced to always return
  `Optional.empty()` → `EmailValidationTest` failed 2/3 (`emptyEmailIsRejected`,
  `malformedEmailIsRejected`), confirming B12/B13 are load-bearing.
- **Integration:** `EventDedupStore.claim` (`infrastructure/tracking/EventDedupStore.java`) forced
  to always return `true` (dedup disabled) → `CollectApiIT` failed 2/6
  (`acceptedThenDuplicateThenBadRequest`, `duplicateIdempotencyIdIsNeverPublishedTwice`), both
  expecting 200/duplicate but observing 202/accepted — confirming B18/B19 exercise the real dedup
  path through the full HTTP/DB stack, not just a mock.

## Data-parity spot-check (3 non-trivial services vs. PHP originals)

Dashboard (`DashboardViewService`/`DashboardFixtures` vs `DashboardController.php` +
`DashboardMetrics.php`/`EventFeed.php`/`CustomerDirectory.php`/`AutomationCatalog.php`), Settings
(`SettingsViewService`/`SettingsController`/`SettingsFixtures` vs `SettingsController.php` +
`SettingsCatalog.php`), and Customers (`CustomersViewService` vs `CustomerController.php`) were
each read field-by-field against their PHP sources. **No data-parity bugs found.** KPI values,
sparkline formulas, event-feed row formatting (including the `formatPricePl` plain-ASCII-space
quirk), the two divergent thousands-separator conventions in Customers, the Settings "Konto" tab
href quirk, and all literal fixture datasets match value-for-value; every apparent oddity is a
documented ported behaviour, not a divergence.

## Accepted as-is

- No CSRF token / no real authentication on `POST /{locale}/login` — the old `SecurityController`
  (`src/Controller/SecurityController.php`) has neither either (verified); the plan's threat model
  explicitly defers security work to post-cutover Linear P0. `LoginService.attemptSignIn` also does
  not rotate the session id on sign-in, matching the old stack's `PanelIdentity::signIn` exactly
  (no `session()->migrate()` there either) — pre-existing, not a regression.
- Mercure publisher JWT (`MercureJwt.java`) is minted once, no `exp` claim, HS256 — matches the old
  stack's publish flow; hub is reachable only over the private compose network.
- Dev-only default secrets in `compose.next.yaml` (`!ChangeMe!`, `!ChangeThisMercureHubJWTSecretKey!`)
  — clearly named placeholders; `compose.next.prod.yaml` requires real env vars with no fallback.
- `SpaDocument.inject()` (`domain/SpaDocument.java:52-58`) correctly HTML-attribute-escapes
  `loginError`/`lastUsername` (`&`, `"`, `<`, `>`) before embedding them in `<html …>` — verified
  no reflected-XSS path from the login form.
- `ImportUploadStorage` — random 128-bit hex stored name, whitelisted extension, basename-stripped
  original name; path traversal not reachable. The documented LOW follow-up (empty file is stored,
  not rejected, matching PHP) is a deliberate, correctly-reasoned parity choice, not a bug.
- `EventDedupStore.claim` — single atomic `INSERT … ON CONFLICT … WHERE expires_at < ?` upsert, no
  check-then-act race.
- Flyway `V1__baseline.sql` (official `spring-session-jdbc`/ShedLock schemas, `event_dedup` frozen),
  `spring.session.jdbc.initialize-schema: never` avoiding double-creation, and `SchedulingConfig`'s
  ShedLock wiring are all standard and correct.
- `pom.xml` — `-Xlint:all -Werror`, Spotless bound to the `verify` phase (not just invokable by
  name), pinned dependency versions with reasoned comments for every non-obvious Boot-4 module
  split. Solid gate discipline.
- `SessionRequestSerializationRedProofIT` is deliberately `@Disabled` (a documented RED-proof of the
  pre-fix race) — legitimate, though note it never runs in CI so its assertions could silently
  bit-rot; not a blocker.
- No hardcoded secrets found in source beyond the documented dev placeholders above.
