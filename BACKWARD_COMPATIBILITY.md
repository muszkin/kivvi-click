# Protected contract surfaces — kivvi-click

Inventory of what outside callers, the browser, or production depend on. A PR that changes anything
here is a review **blocker** unless its body names the surface, says why it must change, and
describes what happens to existing callers. Everything below was read off the current code, not
assumed.

## HTTP — tracking and integrations

| Surface | Contract | Why it is frozen |
| --- | --- | --- |
| `POST /collect` | `202` for a newly accepted event, `200` for a duplicate, `400` + `{"error": …}` for a malformed payload; JSON body `{"status": …}`. Accepts a JSON object; a JSON array is accepted and read as all-fields-absent. | Called by tracking scripts embedded in customer shops. Those scripts are deployed outside this repository and cannot be updated in lockstep; a status-code change silently breaks retry logic. |
| `POST /import/upload` | `302` to import step 2 on success. Multipart, 8 MB request and file ceiling. | The ceiling mirrors the pre-migration runtime; lowering it rejects uploads that used to work. |
| `POST /theme`, `POST /sidebar` | Session-scoped preference writes used by the SPA shell. | Called from the built SPA; a stale cached bundle keeps calling the old path. |

## HTTP — SPA view API

`GET /api/v1/{pl|en}/{dashboard,events,customers,customers/{id},automations,automations/{new|aNN},campaigns,emails/{new|kNN},popups,popups/{new|pNN},feeds,import/{step},settings/{tab},shell,landing}`

The response shapes are the props the Vue views bind to. Renaming or removing a field breaks the
built SPA for anyone holding a cached bundle. Additive fields are safe; removals and renames are not.

## HTTP — documents and routing

| Surface | Contract |
| --- | --- |
| `/`, `/{pl|en}`, `/{pl|en}/**` | Serves the SPA document for every path in `RouteTable`; `/` is the landing page in the default locale. Anything else is `404` with a minimal 404 body in the path's own language (Polish for `/pl/**`, English for `/en/**` — it was always Polish before PIO-125). Unknown locale, unknown customer id, unknown settings tab and unknown import step all 404 — this is deliberate parity behaviour, not an accident. |
| SPA document `<html>` tag | `lang`, `data-theme`, `data-sidebar`, plus `data-login-error` / `data-last-username` on a failed login POST. The SPA reads these before first paint. Attributes may be added; the existing ones may not change name or meaning. |
| `POST /{pl|en}/login`, `POST /{pl|en}/logout` | Native form POST. Login: `302` to the dashboard on success, `200 text/html` re-render on failure. Logout: `302` to the login page. |
| `GET /{pl|en}/demo` | `302` into the dashboard. Linked from the public landing page. |
| Locale prefix | Exactly `pl` and `en`. English is the default (PIO-125; Polish before): `/` renders `<html lang="en">`, and a route with no locale segment of its own — `POST /import/upload` — redirects into `/en`. Adding a locale means touching `SupportedLocale`, `RouteTable`, `LandingFixtures`, both message bundles and both i18n catalogues together. |
| `Content-Type` on HTML responses | `text/html; charset=UTF-8`, always explicit. |

## Real-time

| Surface | Contract |
| --- | --- |
| Mercure topic `/accounts/{id}/events` | The browser subscribes through the public Mercure edge. The topic string is built only in `domain.tracking.EventStreamTopic`. |
| SSE envelope | `{"event": {time, type, typeIcon, tone, detail, customerId, customerName, siteName, siteColor}}`. The SPA renders it with the same `EventRow` component the initial page load uses; a field rename breaks live rows without breaking any test that loads the page fresh. |
| `/.well-known/mercure*` | Served by the Mercure hub itself, never proxied to the API. |

## Session and storage

| Surface | Contract |
| --- | --- |
| Session cookie `SESSION` | `HttpOnly`, `SameSite=Lax`, `Secure`. Renaming it logs out every live session. |
| `spring_session`, `spring_session_attributes` | Official Spring Session JDBC schema, created by `V1__baseline.sql`. Managed by the framework, not by application code. |
| `shedlock` | Lock table for the scheduled jobs; a restart of the single `api` process must resume rather than double-fire. |
| `event_dedup` | 24-hour idempotency ledger for `/collect`. |
| `V1__baseline.sql` | Frozen cutover baseline. Never edited — only extended by new versioned migrations. |

## Frontend contract used by the test suite

The Playwright suite asserts against class names and DOM structure of the ported design system
(`.hero`, `.hero-cta a`, `.hero-preview .kpi`, `.feat`, `.price-card`, `.page-title`, `.sb-foot`,
`#f-<field-name>` input ids). Renaming a class or changing a count is a visible-behaviour change:
update the specs in the same PR and say so, rather than weakening the assertion.

## Operations

| Surface | Contract |
| --- | --- |
| Production compose project | `-p kivvi-click`, run only from the repository directory, port `23456` behind the external TLS proxy for `https://kivvi.click`. A worktree's different directory name would start a second, colliding stack. |
| Development compose project | `-p kivvi-dev`, always explicit. Omitting it collides with production on this host. |
| `/actuator/*` | Exposed on the API only, never routed through the public Mercure edge. Only the `health` endpoint is enabled. |
| Postgres data volume | Mounted at `/var/lib/postgresql` (not `/var/lib/postgresql/data`) — Postgres 18 exits otherwise. |
| `X-Forwarded-*` | Honoured through Tomcat's `RemoteIpValve` (`server.forward-headers-strategy: native`), which trusts only private-range peers. Switching to `framework` would trust any caller. |
