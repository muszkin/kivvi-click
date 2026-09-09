FAIL

# Final combined review — Tooling & Ops
Area: `tools/migration-verify/**`, `mercure/**`, `compose.next.yaml`, `compose.next.prod.yaml`,
`.github/workflows/next-build.yml`, `backend/Dockerfile`, `frontend/README.md`.
Diff: `git diff 8d3fc32...dae1696 -- tools mercure compose.next.yaml compose.next.prod.yaml
.github backend/Dockerfile frontend/README.md README.md` (root `README.md` unchanged in range).
Reviewed in worktree `verify-final-architecture`, HEAD `dae1696`. `docker compose ... config`
used read-only (Compose v5.5.1) to validate merge output — never `up`.

## Findings (by severity)

### 1. BLOCKER — new prod stack's Postgres volume collides with the old prod stack's
`compose.next.prod.yaml:48-52` (`database` service `volumes:` and top-level `volumes:` key)
defines volume name **`database_data_prod`** — byte-identical to the name already used by the
*old* stack's `compose.prod.yaml:52,71` (`database_data_prod`). No compose file in either stack
sets a top-level `name:`, and the plan's own CUT-1 packet pins **both** stacks to the same
Compose project name `kivvi-click` (`context/plans/.../migration.md:509`). Docker namespaces
volumes as `<project>_<name>`, so under project `kivvi-click` both stacks resolve to the exact
same volume `kivvi-click_database_data_prod`. `docker compose down` (no `-v`) leaves the volume
in place, so bringing the new stack `up` after `down`-ing the old one **mounts the old Doctrine/
Postgres 18 cluster straight into the new Spring/JPA `database` service** — same Postgres major
version (18 both sides, confirmed via `.env.prod.docker.example` and `compose.next.yaml:83`), so
it won't crash-loop, it will silently start against legacy schema/tables instead of a clean one.
This directly contradicts the plan's own stated rollback invariant: "`database_data_prod` volume
untouched because the new stack uses a new volume `database_data_next`" (plan line 509) — the
name `database_data_next` was never implemented. The file's own comment ("production never
shares a database... with a dev stack that might run from the same host") only guards against
the *dev* stack, not the old *prod* stack, which is the actual collision.
**Note:** RR-1 rehearsal (project `kivvi-stage`) will NOT catch this — it uses an isolated
project name, so the collision only manifests at real CUT-1 with project `kivvi-click`.
**Fix:** rename the volume in `compose.next.prod.yaml` to `database_data_next` (top-level key
and the service's `- database_data_next:/var/lib/postgresql:rw` mount), matching what the plan
already promises.

### 2. BLOCKER — `SPRING_DATASOURCE_PASSWORD` is not linked to `POSTGRES_PASSWORD`, and neither it nor `MERCURE_JWT_SECRET` are documented anywhere
`compose.next.prod.yaml:11` sets `SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}`
(no default) as a variable *independent* from `database`'s `POSTGRES_PASSWORD` (line 47) — unlike
the base file (`compose.next.yaml:14`) which correctly derives it from
`${POSTGRES_PASSWORD:-!ChangeMe!}`. Verified empirically: with only `POSTGRES_PASSWORD` set (the
variable name an operator would carry over from `.env.prod.docker.example`, which is the file
CUT-1 is documented to reuse), `docker compose config` renders `SPRING_DATASOURCE_PASSWORD: ""`
with a `"variable is not set, defaulting to a blank string"` warning — the `api` container would
fail to authenticate against Postgres at boot, and `up --wait` would time out on the healthcheck.
Same problem for `MERCURE_JWT_SECRET` (line 12-13, and `mercure`'s `MERCURE_PUBLISHER_JWT_KEY`/
`MERCURE_SUBSCRIBER_JWT_KEY` in the same overlay) — this key does not exist in
`.env.prod.docker.example` (the old stack's equivalent is a *differently-named*
`CADDY_MERCURE_JWT_SECRET`). `.env.prod.docker.example` was not touched by this diff and lists
none of the next stack's required keys.
**Env keys `compose.next.prod.yaml` needs, with default status:**
`IMAGES_PREFIX` (defaults `""`), `SPRING_DATASOURCE_PASSWORD` (**no default**),
`MERCURE_JWT_SECRET` (**no default**), `TRUSTED_PROXIES` (defaults `private_ranges`),
`SERVER_NAME` (defaults `:80`), `HTTP_PORT` (defaults `23456`, already used by the old stack —
fine), `POSTGRES_PASSWORD` (**no default**, already present in `.env.prod.docker.example`).
**Fix:** change line 11 to `SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}` (drop the redundant
variable, mirror the base file), and add `MERCURE_JWT_SECRET` to `.env.prod.docker.example` (and
the real `.env.prod.docker`) with a comment — or, if the separate name is intentional, document
both new keys in the example file before CUT-1.

### 3. HIGH — the plan's documented CUT-1 command is invalid as written
`context/plans/2026-09-08-symfony-to-spring-vue-migration.md:509` documents CUT-1 as
`docker compose -p kivvi-click --env-file .env.prod.docker -f compose.next.prod.yaml up -d
--build --wait`, i.e. `compose.next.prod.yaml` alone. Verified: this errors immediately —
`service "mercure" has neither an image nor a build context specified: invalid compose
project` — because `compose.next.prod.yaml` is override-only (no `image:`/`build:` for
`mercure`). It must always be combined with the base file, exactly like the old stack's
documented `-f compose.yaml -f compose.prod.yaml` pattern.
**Fix:** correct the plan text (and any CUT-1 runbook) to
`-f compose.next.yaml -f compose.next.prod.yaml`.

### 4. MEDIUM — `next-build.yml` never builds `backend/Dockerfile`
`.github/workflows/next-build.yml`'s `backend` job (lines 27-56) hand-rolls the frontend-build-
then-copy-into-`resources/static` steps and runs `mvnw verify`, but never invokes
`docker build -f backend/Dockerfile .` — unlike the old `.github/workflows/docker-build.yml:20-26`
which explicitly build-checks the production image (`push: false`). The actual CUT-1 artifact —
the image `compose.next.prod.yaml` builds from `backend/Dockerfile` — is unverified by CI; a
broken `COPY`/stage reference in the Dockerfile would only surface at cutover.
**Fix:** add a `docker build -f backend/Dockerfile -t kivvi-click-api:ci .` step (push: false),
mirroring `docker-build.yml`.

### 5. LOW — `deviations.json` has entries the plan's canonical table doesn't record
`tools/migration-verify/deviations.json` DEV-12 (lines 99-107, skip-on-404 for
shell-navigation/customers/settings/import-wizard) and the "landing step 5" extension folded
into DEV-11 (lines 90-98) are real, wired into `compare.mjs` (its own header says "DEV-1..12"),
and internally justified by citing wave-packet bullets — but
`context/plans/.../migration.md`'s "Accepted deviations" table (lines 412-426) stops at DEV-11
and only covers "login steps 4 and 8," with no DEV-12 row at all. `DEV-13` is correctly absent/
unused, matching expectation. Not a runtime risk (the mechanism is exercised and documented in
`deviations.json` itself), but the master plan document is now out of sync with what
`compare.mjs` actually checks — reconcile before CON-1 closes the plan out.

### 6. LOW — plan text vs. delivered ports mismatch
Plan line 509 says CUT-1 publishes "port 23456 ... and 23457"; `compose.next.prod.yaml:36-41`
(`ports: !override`) deliberately publishes **only** 23456 (http; https/http3 dropped). The
delivered behavior is arguably better (no dangling inert port), but the plan text and the
artifact disagree — update the plan line or record it as an explicit accepted deviation.

### 7. NOTE — no `TRUSTED_HOSTS`-equivalent in the new stack
Old `compose.prod.yaml:19` sets `TRUSTED_HOSTS: ^(localhost|kivvi\.click)$$`; nothing analogous
appears in `compose.next.prod.yaml`. Whether Spring needs/has an equivalent Host-header allow-
list is an app-config question outside this diff's file set — flag for backend review, not a
blocker for this slice.

### 8. NOTE — `frontend/README.md:22-24` under-specifies the "run the whole stack" step
Tells the reader to "run the whole stack through `compose.next.yaml` instead" with no example
command/ports. Minor; a reader can infer it, but worth one line (`docker compose -f
compose.next.yaml up -d --wait`).

### 9. NOTE — `performance.mjs` "initial JS" measurement is correct only because there is no code-splitting yet
`initialJsGzipBytes()` (lines 38-45) sums every `.js` file under `frontend/dist/assets`. This is
accurate today because `frontend/src/router/routes.ts` imports every view eagerly (no dynamic
`import()`), so every emitted chunk loads on first paint. If route-level code-splitting is added
later without updating this function, the DEV-8 "initial JS" budget will silently start
overcounting. Not a current defect.

## RR-1 / CUT-1 command sequence for this host (with the fixes above applied first)

```sh
# --- Prerequisite fixes (must land before either stage) ---
#   compose.next.prod.yaml: rename database_data_prod -> database_data_next
#   compose.next.prod.yaml: SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
#   .env.prod.docker(+.example): add MERCURE_JWT_SECRET
#   plan doc: CUT-1 row must use -f compose.next.yaml -f compose.next.prod.yaml

# --- RR-1: rehearsal, isolated project/ports, names-only copy of secrets ---
cp .env.prod.docker /path/rehearsal.env   # values only, never committed
docker compose -p kivvi-stage --env-file /path/rehearsal.env \
  -f compose.next.yaml -f compose.next.prod.yaml \
  up -d --build --wait   # HTTP_PORT=23458 in rehearsal.env

curl -I http://localhost:23458/pl
cd tests/e2e && E2E_BASE_URL=http://localhost:23458 npx playwright test
# all-journey cohort:
for j in login landing feeds scheduler-heartbeat customers event-stream automations \
         campaigns-email-editor settings shell-preferences dashboard import-wizard \
         popups-widget-editor shell-navigation; do
  node tools/migration-verify/compare.mjs --journey "$j" --base http://localhost:23458 --dimension all
done

docker compose -p kivvi-stage -f compose.next.yaml -f compose.next.prod.yaml down -v
docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --wait
docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml ps   # expect healthy

# --- CUT-1: real cutover, same project name as old stack (kivvi-click) ---
docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml down
docker compose -p kivvi-click --env-file .env.prod.docker \
  -f compose.next.yaml -f compose.next.prod.yaml up -d --build --wait

curl -I http://localhost:23456/pl
# SSE probe against https://kivvi.click/.well-known/mercure
cd tests/e2e && E2E_BASE_URL=https://kivvi.click npx playwright test

# Data/session note: app has no domain entities/auth ("no user data to migrate", confirmed in
# context/map). Old PHPSESSID sessions (Postgres `sessions` table) and any uploaded-file pointer
# are abandoned, not migrated — accepted under DEV-9 ("no real users"). With finding #1 fixed,
# the old database_data_prod volume is never touched by the new stack, so it is preserved as-is
# for the observation window.

# --- Rollback ---
docker compose -p kivvi-click -f compose.next.yaml -f compose.next.prod.yaml down
docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --wait
```

## Accepted as-is

- `backend/Dockerfile`: correct 3-stage build (Node → Maven → JRE), non-root `kivvi` user,
  minimal runtime (only `curl` added, for the compose healthcheck).
- `compose.next.prod.yaml`'s `ports: !override` on `mercure` correctly drops https/http3 (Compose
  target-based list-merge semantics confirmed live via `docker compose config`, v5.5.1 supports
  `!override`/`!reset`); `database`'s `ports: !reset []` correctly closes Postgres's dev-only
  random published port in prod.
- `SERVER_NAME: :80` correctly disables Caddy auto-HTTPS/redirect on the internal port —
  same pattern already proven on the old stack.
- `mercure/Caddyfile` blocks `/actuator/*` (404) at the public edge; the compose healthcheck
  curls the `api` container directly, bypassing the edge — actuator never publicly reachable.
- `tools/migration-verify/compare.mjs` and `performance.mjs`: no hardcoded prod URLs, hostnames,
  or secrets — safe to keep in the repo; default `VERIFY_COMPOSE` only names a wave-0 dev project.
- `budget.json` matches DEV-8's plan text exactly (300 kB gzip / 2000 ms LCP / 2500 ms TTI /
  500 ms p95 `/collect`); `performance.mjs` measures all four.
- `next-build.yml`'s push trigger (`branches: [main]`) matches the old workflow; the added
  `pull_request` trigger matches the plan's own stated wave-0 intent (must be green on PR head).
- Root `README.md`/`CLAUDE.md` untouched — intentionally scoped to CON-1, not this slice.
- No separate `migrations` one-shot service in the new stack — Flyway (present in
  `backend/pom.xml`/`application.yml`) runs migrations on boot; consistent with DEV-6 (no
  separate worker process).
- Absence of CORS config in compose is correct: SPA and API share one public origin
  (`kivvi.click`) behind the Mercure edge, so no cross-origin requests exist.
