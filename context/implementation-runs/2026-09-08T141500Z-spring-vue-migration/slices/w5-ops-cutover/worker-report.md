# Worker report — w5-ops-cutover

Worktree: `/home/muszkin/work/kivvi-click-wt/w5-ops` · branch `migration/wave-5/ops-cutover` ·
parent SHA `718278b26893bb556541b113baf4455a06638831` · candidate SHA
**`47793d9654d7ec25fb60c2491d4abcc1cf81b68f`**. Working tree clean (`git status --porcelain`
empty) after the final commit.

## Files changed

| File | Change |
| --- | --- |
| `compose.next.prod.yaml` | Every volume renamed to an explicit `name:` ending `_next` (`database_data_next` → `kivvi-next_database_data`, `mercure_data_next` → `kivvi-next_mercure_data`, `mercure_config_next` → `kivvi-next_mercure_config`) so it can never resolve to the old prod stack's `database_data_prod`/`caddy_data_prod`/`caddy_config_prod` even under the same Compose project name. `api`'s `SPRING_DATASOURCE_PASSWORD` now derives from `${POSTGRES_PASSWORD}` (was an independent, undocumented `${SPRING_DATASOURCE_PASSWORD}`). Dropped the dead `TRUSTED_PROXIES` env var. Added header comment documenting the empty-database/no-migrated-state invariant. |
| `compose.next.yaml` | Dropped the dev-side `TRUSTED_PROXIES: ${TRUSTED_PROXIES:-0.0.0.0/0}` line from `api` — the app no longer reads this var anywhere (see application.yml below), so leaving it in either compose file would have been the same "dead, misleading configuration" the review flagged, just relocated. |
| `backend/src/main/resources/application.yml` | `server.forward-headers-strategy` switched `framework` → `native` (Tomcat `RemoteIpValve`, trusted-proxy aware, with no explicit `internal-proxies` override so Tomcat's own built-in private-range regex applies — already covers every RFC1918 range a Docker compose network can hand out). Added `spring.servlet.multipart.max-file-size`/`max-request-size: 8MB`, matching FrankenPHP's stock `php.ini-production` `post_max_size` default (no override was found anywhere in the old stack's own config). `secure: true` on the session cookie left untouched — already correct for both dev and prod as hardcoded (documented why in an updated comment). |
| `backend/src/test/java/click/kivvi/web/LoginControllerForwardedHeaderTest.java` (new) | Supplementary MockMvc test for the forward-headers-strategy switch. Javadoc is explicit about what it can and cannot prove (MockMvc never opens a real Tomcat connector, so it cannot exercise `RemoteIpValve` itself — that only runs live under RR-1/CUT-1). What it does prove and pins as a regression guard: `LoginController`'s redirect `Location` never derives from the request's scheme/host, so a spoofed or proxy-supplied `X-Forwarded-Proto`/`X-Forwarded-Host` can never smuggle a different origin into the `302`. |
| `.env.prod.docker.example` | Added a "Next stack (Spring Boot + Vue) only" section documenting the two new keys (`MERCURE_JWT_SECRET`, `IMAGES_PREFIX`) with per-key comments; clarified which existing keys (`SERVER_NAME`, `HTTP_PORT`, `POSTGRES_*`) are now shared between both stacks and why `SPRING_DATASOURCE_PASSWORD` needs no separate entry (derives from `POSTGRES_PASSWORD`). |
| `.github/workflows/next-build.yml` | Added a `backend-image` job (`docker/setup-buildx-action@v3` + `docker/build-push-action@v6`, `push: false`, tag `kivvi-click-api:ci`) that builds `backend/Dockerfile` on every push/PR — mirrors the old stack's `docker-build.yml`. `mvnw verify` in the `backend` job is unchanged. |
| `README.md` | New "## Next stack (Spring Boot + Vue)" section: RR-1 rehearsal and CUT-1/rollback command sequences (both always combine `compose.next.yaml` + `compose.next.prod.yaml`, correcting the plan's CUT-1 line which named the override file alone and fails to start on its own), explicit statement that this section supersedes the plan's CUT-1 line, and the volume-isolation/empty-database rationale. |
| `tools/migration-verify/deviations.json` | DEV-12's `note` field appended (not replaced) with a short reconciliation addendum: the plan's own "Accepted deviations" table now carries a DEV-12 row, so the two documents agree; DEV-13 is confirmed correctly absent (contract-verifier tolerance with nothing to encode yet). No mechanism/scope change — pure text addendum, minimal 2-line diff. |

Candidate SHA `47793d9`'s commit range:

```
$ git log --oneline 718278b..HEAD
47793d9 docs: next-stack rehearsal and cutover runbook (#ops)
fd7b755 ci: build the backend image in next-build (#ops)
20ce337 fix: isolate the next stack's prod volumes and wire its secrets (#ops)
```

No commit trailers (verified `git log -1 --format=%B` on all three). Nothing committed outside the
authorized file set; old-stack paths (`src/`, `templates/`, `assets/`, `translations/`,
`migrations/`, `tests/**/*.php`, `tests/e2e/specs/`, `composer.*`, `compose.yaml`,
`compose.override.yaml`, `compose.prod.yaml`, `Dockerfile`, `frankenphp/`, `config/`, `public/`)
untouched.

## TRUSTED_PROXIES decision

Both `final-review-tooling-ops.md` finding #2 and `final-review-backend.md` finding #3 offered two
fix paths: wire `TRUSTED_PROXIES` into Tomcat's `server.tomcat.remoteip.internal-proxies`, or
remove the dead env var. Wiring it verbatim was **not** safe: the existing values
(`0.0.0.0/0` in dev, `private_ranges` in prod) are Symfony's CIDR/keyword convention, not Tomcat's
regex-only `internal-proxies` syntax — piping either straight through would have silently matched
no real IP ever, making `native` mode trust nothing and quietly negating the whole switch while
looking configured. Chose: implement `forward-headers-strategy: native` (as the packet directs)
and remove `TRUSTED_PROXIES` from both compose files, relying on Tomcat's own built-in
private-range default (already documented in `application.yml`'s comment) — functionally
equivalent to Symfony's `private_ranges` and a strict improvement over dev's old `0.0.0.0/0`
trust-everyone default.

## Gates

| # | Gate | Command | Result |
| --- | --- | --- | --- |
| 1 | Unit tests | `JAVA_HOME=~/.cache/kivvi-toolchains/jdk-25 ./mvnw -q test` (backend) | PASS (exit 0), re-run after spotless:apply, still PASS |
| 2 | Spotless | `./mvnw -q spotless:check` | PASS (exit 0) — one violation on first run (Javadoc wrapping in the new test), fixed via `spotless:apply`, re-verified clean |
| 3 | Frontend lint | `npm run lint` (frontend) | PASS (exit 0) — 12 pre-existing warnings, 0 errors, in files this slice never touched |
| 4 | Frontend format | `npm run format:check` (frontend) | **FAIL (exit 1)**, pre-existing: `frontend/.agents/project-context.md` — file untouched by this slice (`git diff --stat` empty for it against the parent SHA); not caused by any change in this packet |
| 5 | Compose render | `docker compose -f compose.next.yaml -f compose.next.prod.yaml --env-file .env.prod.docker.example config > /dev/null` | PASS (exit 0, empty stderr — no "variable is not set" warnings), verified twice (pre- and post-commit) |
| 6 | `./mvnw -q verify` (Testcontainers/Docker) | same command, backend | **FAIL, infrastructure-only** — see below |
| 7 | `docker build -f backend/Dockerfile .` | `docker build -f backend/Dockerfile -t kivvi-click-api:w5ops-verify .` | PASS (exit 0), image built (400MB), removed after verification |

### `./mvnw -q verify` — infrastructure failure, not a code defect

Attempted 4 times total (once before Docker was confirmed back, three after). Every failure across
all 4 runs is `org.testcontainers.containers.ContainerLaunchException: Timed out waiting for log
output matching '.*database system is ready to accept connections.*'` — a `postgres:18-alpine`
Testcontainers container failing to reach ready state in time. **Zero assertion/logic failures** in
any run (the cleanest run: `Tests run: 38, Failures: 0, Errors: 14, Skipped: 1` — all 14 errors are
`ContainerLaunch`, not a single behavioural mismatch).

Root cause, confirmed independently of Maven: a raw `docker run -d postgres:18-alpine` diagnostic
took 35s just for `docker run` to return, and was still mid-`initdb` 19s after its own `StartedAt`.
This host's Docker data root is `/mnt/nas/docker-root` (network-attached RAID, `/dev/md0`) and is
shared with several other tenants' live stacks (`kivvi-int-database-1`, two `magellanerp-core`
Postgres containers, `oracle-src`, two `immich` containers, `sonarqube`, `paperless`, plus another
worktree's own concurrent `integration/backend` verify run observed mid-flight) — container startup
latency here is well past Testcontainers' default log-wait timeout. Mid-session the Docker daemon
itself briefly became unreachable and recovered on its own (~30s) with no action from this worker;
`kivvi-int-database-1`/`kivvi-click-database-1` (production, untouched by this worker) restarted
automatically via their own `restart: unless-stopped` policy, exactly as designed.

This matches `final-review-backend.md` finding #1 (18 `*ApiIT` classes each spin up their own
`static PostgreSQLContainer` instead of sharing one) — a pre-existing backend-testing-architecture
characteristic that makes the suite fragile under host contention, not one of this packet's 7
numbered items and not in this slice's authorized file set (would require touching every
`*ApiIT` class). None of this packet's 8 changed files touch Testcontainers configuration, IT test
classes, or Postgres startup — `./mvnw -q test` (unit-only, no Testcontainers) is green.

Self-correction during this run: two earlier `verify` attempts were mis-diagnosed as "killed" by a
`pgrep` pattern that didn't match the actual `java`/surefire-booter command lines, so a third
attempt was started concurrently with the still-running first two — three simultaneous full IT
suites competing for the same host resources, which is what produced the very first `Connection
refused` failure. Found via `pgrep -af "w5-ops/backend"`, killed all three (`pkill -9`), removed
the orphaned `postgres:18-alpine`/`ryuk` containers left behind, and ran one single, correctly
supervised final attempt — which still failed for the container-startup-timeout reason above, with
zero logic failures.

## Rendered `docker compose … config` excerpt (volumes + `api` environment)

```yaml
name: w5-ops
services:
  api:
    build:
      context: /home/muszkin/work/kivvi-click-wt/w5-ops
      dockerfile: backend/Dockerfile
    depends_on:
      database:
        condition: service_healthy
        required: true
    environment:
      MERCURE_JWT_SECRET: ""
      MERCURE_PUBLISHER_JWT_KEY: ""
      MERCURE_SUBSCRIBER_JWT_KEY: ""
      MERCURE_URL: http://mercure/.well-known/mercure
      SPRING_DATASOURCE_PASSWORD: ""
      SPRING_DATASOURCE_URL: jdbc:postgresql://database:5432/app
      SPRING_DATASOURCE_USERNAME: app
    image: kivvi-click-api
    restart: unless-stopped
  database:
    environment:
      POSTGRES_DB: app
      POSTGRES_PASSWORD: ""
      POSTGRES_USER: app
    image: postgres:18-alpine
    restart: unless-stopped
    volumes:
      - type: volume
        source: database_data_next
        target: /var/lib/postgresql
        volume: {}
  mercure:
    environment:
      MERCURE_PUBLISHER_JWT_KEY: ""
      MERCURE_SUBSCRIBER_JWT_KEY: ""
      SERVER_NAME: :80
    image: dunglas/mercure:v0.24.2
    ports:
      - name: http
        mode: ingress
        target: 80
        published: "23456"
        protocol: tcp
    restart: unless-stopped
    volumes:
      - type: bind
        source: /home/muszkin/work/kivvi-click-wt/w5-ops/mercure/Caddyfile
        target: /etc/caddy/Caddyfile
        read_only: true
        bind: {}
      - type: volume
        source: mercure_data_next
        target: /data
        volume: {}
      - type: volume
        source: mercure_config_next
        target: /config
        volume: {}
volumes:
  database_data_next:
    name: kivvi-next_database_data
  mercure_config_next:
    name: kivvi-next_mercure_config
  mercure_data_next:
    name: kivvi-next_mercure_data
```

`SPRING_DATASOURCE_PASSWORD`/`POSTGRES_PASSWORD`/`MERCURE_JWT_SECRET*` all render `""` because
`.env.prod.docker.example` intentionally ships blank secret placeholders (an explicitly-set empty
value, not an absent one) — `docker compose config` emitted zero stderr warnings either way,
confirmed twice. No `TRUSTED_PROXIES` key appears anywhere in the render, confirming full removal.

## Cleanup performed

- `backend/target` deleted (both after the final `mvnw` run and again after `docker build`).
- `frontend/node_modules` deleted after the lint/format gates; `npm cache clean --force` run.
- `kivvi-click-api:w5ops-verify` image removed after inspection (`docker rmi`).
- All orphaned `postgres:18-alpine`/`testcontainers-ryuk-*` containers from the killed/failed
  `verify` attempts removed; left untouched: `kivvi-int-database-1`, `kivvi-click-database-1`
  (other tenants'/production, never started or stopped by this worker), and the other worktree's
  own live `integration/backend` run and its containers.
- `git status --porcelain` empty at end; disk on `/` at 46G available (was ~4.7G before cleanup).

## Not done / explicitly out of scope

- `final-review-backend.md` finding #1 (share one Testcontainers Postgres instance across
  `*ApiIT` classes) — not one of this packet's 7 numbered items, not in the authorized file set,
  and is the direct cause of the `verify` infra failures above; flagging for a future slice/finding
  rather than fixing here.
- `frontend/.agents/project-context.md` Prettier violation — pre-existing, file untouched by this
  slice, outside the authorized file set.
