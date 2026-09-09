<!-- BEGIN project-context-initializer:context -->
# `mercure/` — project context

| Field | Value |
| --- | --- |
| Path | `mercure/` |
| Scope | Mercure hub configuration — the stack's public edge |
| Source revision | `fe9c3fe06b919302d322994438a8fbb177c8b2a0` on `main` |
| Refreshed | 2026-09-09 |
| Coverage role | `own` (single file, no children) |

Provenance labels: **Observed** (cited file), **Inferred** (named inference), **Unknown**.

## Purpose

Caddyfile for the `mercure` service in `../compose.yaml` / `compose.prod.yaml`: the standalone
`dunglas/mercure:v0.24.2` image (Caddy with the Mercure module bundled) that is the **edge** of
the whole stack — it terminates TLS in dev (self-signed via automatic HTTPS) or sits behind the
external `kivvi.click` TLS proxy in prod (plain HTTP on the published port), serves the Mercure
hub itself at `/.well-known/mercure*`, and reverse-proxies everything else to the Spring Boot
`api` service (`../backend/`). This is now the **only** public entry point for the application —
the migration that introduced this container has completed cutover; there is no longer a
parallel FrankenPHP-based edge to compare against (that container/Caddyfile was deleted by the
migration's CON-1 cleanup, though the file header comment in `Caddyfile` itself still says it
"mirrors `frankenphp/Caddyfile`'s shape" — a harmless but stale cross-reference, see R23 in
`../context/map/risks-and-unknowns.md`).

## Invariants (do not break)

- **This is the only public entry point for the stack.** `respond /actuator/* 404` explicitly
  keeps Spring Boot Actuator internal-only — do not remove that line or add a route that exposes
  `/actuator/**` through the edge.
- **`SERVER_NAME` carries a second, explicit-port site address for internal publish traffic**
  (dev: `mercure:80` alongside the public auto-HTTPS address, set in `../compose.yaml`'s
  `mercure` service environment; prod: a bare `:80` with no hostname, set in
  `../compose.prod.yaml`, because TLS is already terminated externally there). Caddy only
  enables automatic HTTPS (and its HTTP→HTTPS redirect) for a site address with **no explicit
  port** — the second address is what lets `../backend/`'s `HttpMercurePublisher` reach the hub
  over plain HTTP inside the compose network without a certificate. Do not "simplify" this to a
  single address; it will silently force HTTPS onto the internal publish call and break
  `/collect`.
- **Publisher/subscriber JWT keys come from `MERCURE_PUBLISHER_JWT_KEY`/
  `MERCURE_SUBSCRIBER_JWT_KEY` env vars** (referenced as `{env.MERCURE_PUBLISHER_JWT_KEY}` /
  `{env.MERCURE_SUBSCRIBER_JWT_KEY}` in the Caddyfile) — never hard-code a key here;
  `../compose.yaml` and `../compose.prod.yaml` supply them, and `../backend/`'s
  `application.yml` (`kivvi.mercure.jwt-secret`) must use the matching value to sign publish
  requests the hub will accept.
- **`anonymous` and `subscriptions` are both enabled** — any browser can subscribe to any topic
  today (this is a carried-over, accepted product risk, not something to "fix" locally — see R4
  in `../context/map/risks-and-unknowns.md`).
- **The `authorization` query parameter is redacted in the access log** (`log { format filter {
  request>uri query { replace authorization REDACTED } } }`) — keep this filter if the log
  format block is ever touched; it is the one thing standing between an access log and a leaked
  Mercure JWT in a URL query string.
- **AGPL-3.0 licensing**: `dunglas/mercure` is used *unmodified* as a network service (image
  reference only, no fork, no embedded custom code) — this Caddyfile is configuration, not a
  derivative work.

## Commands

None of its own — the file is mounted read-only into the `mercure` container
(`../compose.yaml`: `volumes: - ./mercure/Caddyfile:/etc/caddy/Caddyfile:ro`). To validate a
change: bring the compose stack up (with `-p kivvi-dev` — never against the running production
project name) and confirm `curl -k https://localhost:<https-port>/pl` reaches the SPA document
and `GET /.well-known/mercure` (with a valid subscriber JWT/query) opens an SSE stream.

## Tests

No dedicated test suite. Covered indirectly by the event-stream journey's own contract/
integration tests in `../backend/` and by `../tests/e2e/specs/events.spec.ts` — last run against
production (through this exact edge container) 2026-09-09T16:04-16:07Z, PASS
(`../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/cutover/con1-e2e.md`,
which also confirmed zero old-stack-only requests and clean error/5xx logs on this container
over its access-log window).

## Deployment (current, single stack)

- Dev: `../compose.yaml`'s `mercure` service — `HTTP_PORT`/`HTTPS_PORT`/`HTTP3_PORT` env vars,
  defaulting to 80/443/443.
- Prod, live on this host since 2026-09-09T14:18:42Z: `../compose.prod.yaml` overrides `ports`
  to publish only plain HTTP on `${HTTP_PORT:-23456}` and drops the https/http3 entries — TLS
  terminates at the external `kivvi.click` proxy. Container name `kivvi-click-mercure-1`,
  observed healthy (`docker ps`, 2026-09-09T16:20Z).
- This is the **only** container with a published host port in either compose file — `api` and
  `database` are reachable only over the internal compose network.

## Evidence paths

`mercure/Caddyfile`, `../compose.yaml` (`mercure` service), `../compose.prod.yaml` (`mercure`
service override),
`../backend/src/main/java/click/kivvi/infrastructure/mercure/HttpMercurePublisher.java`,
`../frontend/src/composables/useEventStream.ts`,
`../context/implementation-runs/2026-09-08T141500Z-spring-vue-migration/cutover/con1-e2e.md`,
`../context/plans/2026-09-08-symfony-to-spring-vue-migration.md` §"Technology decisions" (Real-
time hub row) and §"Accepted risks".
<!-- END project-context-initializer:context -->
