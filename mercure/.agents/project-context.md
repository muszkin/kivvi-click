<!-- BEGIN project-context-initializer:context -->
# `mercure/` — project context

| Field | Value |
| --- | --- |
| Path | `mercure/` |
| Scope | Mercure hub configuration for the next stack's public edge (`migration/spring-vue`) |
| Source revision | `dae169614a52532c130bd34435994d6a914165c0` on `migration/spring-vue` (worktree `/home/muszkin/work/kivvi-click-wt/integration`) |
| Refreshed | 2026-09-09 |
| Coverage role | `own` (single file, no children) |

Provenance labels: **Observed** (cited file), **Inferred** (named inference), **Unknown**.

## Purpose

Caddyfile for the `mercure` service in `../compose.next.yaml` / `compose.next.prod.yaml`: the
standalone `dunglas/mercure:v0.24.2` image (Caddy with the Mercure module bundled) that is the
**edge** of the whole next stack — it terminates TLS (dev: self-signed via automatic HTTPS; prod:
plain HTTP behind the external kivvi.click TLS proxy, same as today), serves the Mercure hub itself
at `/.well-known/mercure*`, and reverse-proxies everything else to the Spring Boot `api` service
(`../backend/`). This directly mirrors `../../frankenphp/Caddyfile`'s shape on the old stack (same
log filter, same `anonymous`+`subscriptions` hub config) with the reverse-proxy target swapped from
FrankenPHP's embedded PHP worker to `api:8080` (Observed, file header comment).

Chosen over a Spring-hosted SSE endpoint specifically to preserve the browser-facing
`/.well-known/mercure` contract unchanged (plan §"Technology decisions": "Real-time hub" row) — the
Vue SPA's `useEventStream` composable (`../frontend/src/composables/useEventStream.ts`) opens the
exact same `EventSource` URL the old stack's `assets/controllers/event-stream.ts` did.

## Invariants (do not break)

- **This is the only public entry point for the next stack.** `respond /actuator/* 404` explicitly
  keeps Spring Boot Actuator internal-only — do not remove that line or add a route that exposes
  `/actuator/**` through the edge.
- **`SERVER_NAME` carries a second, explicit-port site address for internal publish traffic** (dev:
  `mercure:80` alongside the public auto-HTTPS address, set in `../compose.next.yaml`'s `mercure`
  service environment; prod: a bare `:80` with no hostname, set in `../compose.next.prod.yaml`,
  because TLS is already terminated externally there). Caddy only enables automatic HTTPS (and its
  HTTP→HTTPS redirect) for a site address with **no explicit port** — the second address is what
  lets `../backend/`'s `HttpMercurePublisher` reach the hub over plain HTTP inside the compose
  network without a certificate. This is the exact same trick the old stack's own `compose.yaml`
  uses (`php:80` alongside the public address) — do not "simplify" this to a single address; it
  will silently force HTTPS onto the internal publish call and break `/collect`.
- **Publisher/subscriber JWT keys come from `MERCURE_PUBLISHER_JWT_KEY`/`MERCURE_SUBSCRIBER_JWT_KEY`
  env vars** (referenced as `{env.MERCURE_PUBLISHER_JWT_KEY}` / `{env.MERCURE_SUBSCRIBER_JWT_KEY}`
  in the Caddyfile) — never hard-code a key here; `../compose.next.yaml` and
  `../compose.next.prod.yaml` supply them, and `../backend/`'s `application.yml`
  (`kivvi.mercure.jwt-secret`) must use the matching value to sign publish requests the hub will
  accept.
- **`anonymous` and `subscriptions` are both enabled**, matching the old stack's own Caddyfile —
  any authenticated browser can subscribe to any topic today (this is a carried-over, accepted
  product risk, not something to "fix" locally — see R4 in
  `../../context/map/risks-and-unknowns.md`, unchanged by the migration).
- **The `authorization` query parameter is redacted in the access log** (`log { format filter {
  request>uri query { replace authorization REDACTED } } }`) — keep this filter if the log format
  block is ever touched; it is the one thing standing between an access log and a leaked Mercure
  JWT in a URL query string.
- **AGPL-3.0 licensing**: `dunglas/mercure` is used *unmodified* as a network service (image
  reference only, no fork, no embedded custom code) — this Caddyfile is configuration, not a
  derivative work. Revisit this note if the hub image is ever forked or patched (plan §"Accepted
  risks").

## Commands

None of its own — the file is mounted read-only into the `mercure` container
(`../compose.next.yaml`: `volumes: - ./mercure/Caddyfile:/etc/caddy/Caddyfile:ro`). To validate a
change: bring the compose stack up and confirm `curl -k https://localhost:<https-port>/pl` reaches
the SPA document and `GET /.well-known/mercure` (with a valid subscriber JWT/query) opens an SSE
stream.

## Tests

No dedicated test suite. Covered indirectly by every journey's e2e gate (the browser's
`EventSource` connection is exercised by `../../tests/e2e/specs/events.spec.ts`,
`--dimension contract`/`--dimension visual` runs of `../tools/migration-verify/compare.mjs`, and the
event-stream journey's own contract/integration tests in `../backend/`).

## Ports / leases / hazards

- Dev/verification: published via `../compose.next.yaml`'s `mercure` service —
  `HTTP_PORT`/`HTTPS_PORT`/`HTTP3_PORT` env vars, defaulting to 80/443/443 (a journey worktree
  overrides these to its leased pool, `19000 + 10·n`).
- Prod (not deployed yet — CUT-1 is a separate, explicitly authorized packet):
  `../compose.next.prod.yaml` overrides `ports` to publish only plain HTTP on
  `${HTTP_PORT:-23456}` (the same port the old stack's Caddy currently serves prod on) and drops the
  https/http3 entries — TLS terminates at the external kivvi.click proxy, unchanged.
- Never touches the old stack's own Caddy edge (`../../frankenphp/Caddyfile`, port 23456 today) —
  they are different containers on different compose files; do not merge or alias them before
  CUT-1.

## Evidence paths

`mercure/Caddyfile`, `../compose.next.yaml` (`mercure` service), `../compose.next.prod.yaml`
(`mercure` service override), `../backend/src/main/java/click/kivvi/infrastructure/mercure/HttpMercurePublisher.java`,
`../frontend/src/composables/useEventStream.ts`,
`../../frankenphp/Caddyfile` (old-stack equivalent, for comparison),
`../../context/plans/2026-09-08-symfony-to-spring-vue-migration.md` §"Technology decisions" (Real-time hub row) and §"Accepted risks".
<!-- END project-context-initializer:context -->
