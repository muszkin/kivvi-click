PASS

Independent review of `git diff 718278b26893...47793d9654d7` (20ce337 fix, fd7b755 ci, 47793d9
docs) in worktree `w5-ops-cutover`. Docker daemon was up; ran `docker compose ... config`
read-only twice, no `up`, no Maven. `git status --porcelain` empty at end of review.

## Rubric results

1. **Volume isolation — PASS.** `docker compose -f compose.next.yaml -f compose.next.prod.yaml
   --env-file .env.prod.docker.example config` rendered with **zero stderr** (no
   "variable is not set" warnings). `volumes:` block: `database_data_next` → name
   `kivvi-next_database_data`, `mercure_data_next` → `kivvi-next_mercure_data`,
   `mercure_config_next` → `kivvi-next_mercure_config`
   (`compose.next.prod.yaml:374-379`). Explicit `name:` bypasses project-name prefixing, so
   these can never collide with `compose.prod.yaml:69-71`'s `database_data_prod`/
   `caddy_data_prod`/`caddy_config_prod` even under the shared project name `kivvi-click`
   (confirmed risk R17/R5 in context/map).

2. **Env wiring — PASS.** `compose.next.prod.yaml:322` `SPRING_DATASOURCE_PASSWORD:
   ${POSTGRES_PASSWORD}` (was a second, undocumented var). Mercure:
   `MERCURE_PUBLISHER_JWT_KEY`/`MERCURE_SUBSCRIBER_JWT_KEY`/`MERCURE_JWT_SECRET` all derive
   from one key `MERCURE_JWT_SECRET` (`compose.next.prod.yaml:326-328,341-342`), matching
   `mercure/Caddyfile:20-23`'s `{env.MERCURE_PUBLISHER_JWT_KEY}`/`{env.
   MERCURE_SUBSCRIBER_JWT_KEY}` and `application.yml:94`'s `kivvi.mercure.jwt-secret:
   ${MERCURE_JWT_SECRET:...}`. Grepped every `${VAR}` (no default) in both compose files
   against `.env.prod.docker.example:1-67`: `POSTGRES_PASSWORD` and `MERCURE_JWT_SECRET` are
   the only two, both present with per-key comments. Cross-check confirmed by the empty-string
   (not "unset") render in finding 1 — an explicitly blank placeholder, not a missing key.

3. **Forwarded headers + cookies — PASS.** Old stack: `.env:31-33`/`compose.prod.yaml:18-19`
   comment states the Docker bridge is the trusted network ("private_ranges" = 10/8, 172.16/12,
   192.168/16) — confirms the reverse proxy's connection reaches the app via the Docker bridge
   gateway IP, not the real client IP. Tomcat's `native`-mode default `internal-proxies` regex
   (10/8, 172.16-31/12, 192.168/16, 169.254/16, 127/8 — `application.yml:33-44`'s comment
   verified accurate against Tomcat's actual `RemoteIpValve` default) covers every address a
   default Compose bridge network hands out, so dropping `TRUSTED_PROXIES` (Symfony CIDR/
   keyword syntax, not a Tomcat regex — correctly identified as unportable in
   `compose.next.prod.yaml:330-336`) and relying on the built-in default is sound, and is
   documented in the compose comment as required. `LoginController.java:50-51,71-72`,
   `LandingController.java:37-38`, `ImportUploadController.java:56-57` — grepped the entire
   backend: every `302 Location` is `URI.create("/" + locale + "/...")`, never built from
   `getScheme()`/`getServerName()`/`isSecure()` (zero hits for any of those). The new
   `LoginControllerForwardedHeaderTest.java` proves exactly this (Location is
   `/pl/dashboard` regardless of spoofed `X-Forwarded-Proto`/`-Host`) and its Javadoc honestly
   states it cannot exercise `RemoteIpValve` itself (MockMvc has no real Tomcat connector) —
   this is real, correctly-scoped supplementary coverage, not a padding test. Session cookie
   `secure: true` (`application.yml:63`) is unconditional across dev/prod — **pre-existing,
   untouched by this diff** (only its comment changed); both dev (Caddy auto-HTTPS for
   `localhost`) and prod (external TLS proxy) always terminate as HTTPS ahead of this app, so
   the invariant holds in both; out of this diff's scope to change.

4. **Multipart limits — PASS.** Grepped the old stack for `upload_max_filesize`/
   `post_max_size` in `frankenphp/conf.d/{10-app,20-app.dev,20-app.prod}.ini` and the root
   `Dockerfile`: no hits anywhere — confirms `application.yml:20-27`'s claim that FrankenPHP
   ran on stock `php.ini-production` (`upload_max_filesize=2M`, `post_max_size=8M`). `8MB` on
   both `max-file-size`/`max-request-size` matches the binding `post_max_size` ceiling exactly.

5. **CI — PASS.** `.github/workflows/next-build.yml:81-96` new `backend-image` job:
   `docker/setup-buildx-action@v3` + `docker/build-push-action@v6` (version-pinned, matching
   both the pre-existing `backend`/`frontend` jobs in this same file and the old
   `docker-build.yml`'s own pins), `push: false`, no credentials/secrets referenced. Builds
   `backend/Dockerfile` with `context: .`, matching `compose.next*.yaml`'s build stanza and the
   Dockerfile's single final `runtime` stage (no `target:` needed). Triggers (`push:
   branches:[main]` + `pull_request`) were pre-existing in this file, unchanged by this diff;
   push-to-main matches the old workflow's own trigger.

6. **README runbook — PASS.** RR-1 uses isolated project `kivvi-stage` + spare port 23458 (adds
   HTTP_PORT override in a copied rehearsal env, never touches the real `.env.prod.docker`);
   teardown is `down -v` (correct — rehearsal state is disposable) followed by bringing the old
   prod stack back `up` and checking `ps`. CUT-1 uses `-p kivvi-click` (matches the old stack's
   default project name, derived from the repo directory basename — consistent with
   `context/map/risks-and-unknowns.md` R5's "dev/prod share compose project" finding), old
   stack `down` **without** `-v` (preserves `database_data_prod`), new stack `up` on the real
   port 23456. Rollback is a plain `down` (no `-v`) + `up` old stack — correct, since the new
   stack's volumes are already isolated (finding 1) so the old volume was never touched. The
   "no data migration" claim (sessions expire on their own; `kivvi.import.upload-directory`
   resolves to a path inside the container, not a named volume, per `application.yml:101-107`'s
   comment — dies with the container, matching the old stack's own per-wizard temp files) is
   accurate. README.md:115-119 explicitly states this section supersedes the plan's CUT-1 line
   and explains why (`compose.next.prod.yaml` alone has no `image:`/`build:` for `mercure`).

7. **deviations.json — PASS.** Diffed hunk only appends text to the existing `note` field;
   `dimension`, `mechanism`, and `rule.skipWhen`/`rule.skips` are byte-identical before/after —
   confirmed a pure documentation reconciliation, no mechanism change.

8. **Commit hygiene — PASS.** All three subjects are Conventional Commits, English, no
   trailers (`git show -s --format=%B` on all three shows subject-only, no blank-line body, no
   `Co-Authored-By`/AI mentions). File list across the full range: `.env.prod.docker.example`,
   `.github/workflows/next-build.yml`, `README.md`, `backend/src/main/resources/
   application.yml`, `backend/src/test/java/.../LoginControllerForwardedHeaderTest.java`,
   `compose.next.prod.yaml`, `compose.next.yaml`, `tools/migration-verify/deviations.json` —
   no old-stack path (`compose.yaml`, `compose.prod.yaml`, `frankenphp/`, `config/`, `src/`,
   `templates/`) touched beyond the two authorized files. No ledger/lease files added.
   `git status --porcelain` empty.

## Notes (non-blocking)

- `./mvnw -q verify` (Testcontainers) could not be independently re-run under this task's "no
  Maven" constraint; worker-report.md attributes its failures to host Postgres-container-startup
  contention (shared NAS-backed Docker root, multiple concurrent tenants), not a logic failure,
  consistent with `final-review-backend.md` finding #1 (no shared Testcontainers instance across
  `*ApiIT` classes) — a pre-existing condition outside this slice's authorized file set.
  `./mvnw -q test` (unit-only) is reported green; this diff's only backend code change besides
  config is one new, structurally-verified `@WebMvcTest` (identical `@Import` shape to the
  pre-existing, passing `LoginControllerTest.java`).
- `frontend/.agents/project-context.md` Prettier failure is pre-existing and untouched by this
  slice — correctly out of scope.

## RR-1 / CUT-1 / rollback command sequence (verified correct as delivered — no corrections needed)

```bash
# --- RR-1: rollback rehearsal, isolated project + spare port ---
cp .env.prod.docker /path/rehearsal.env   # values only, never committed
# rehearsal.env additionally sets HTTP_PORT=23458

docker compose -p kivvi-stage --env-file /path/rehearsal.env \
  -f compose.next.yaml -f compose.next.prod.yaml \
  up -d --build --wait

curl -I http://localhost:23458/pl
cd tests/e2e && E2E_BASE_URL=http://localhost:23458 npx playwright test
for j in login landing feeds scheduler-heartbeat customers event-stream automations \
         campaigns-email-editor settings shell-preferences dashboard import-wizard \
         popups-widget-editor shell-navigation; do
  node tools/migration-verify/compare.mjs --journey "$j" --base http://localhost:23458 --dimension all
done

docker compose -p kivvi-stage -f compose.next.yaml -f compose.next.prod.yaml down -v
docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --wait
docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml ps   # expect healthy

# --- CUT-1: cutover, same project name as the old stack, real port 23456 ---
docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml down   # NOT down -v
docker compose -p kivvi-click --env-file .env.prod.docker \
  -f compose.next.yaml -f compose.next.prod.yaml up -d --build --wait

curl -I http://localhost:23456/pl
cd tests/e2e && E2E_BASE_URL=https://kivvi.click npx playwright test

# --- Rollback ---
docker compose -p kivvi-click -f compose.next.yaml -f compose.next.prod.yaml down   # not down -v
docker compose --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml up -d --wait
```
