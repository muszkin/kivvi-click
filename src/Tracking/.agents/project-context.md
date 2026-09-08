<!-- BEGIN project-context-initializer:context -->
# Context: `src/Tracking/`

Source `91f8f85` (main), refreshed 2026-09-08. Index: `context/map/INDEX.md`, manifest: `context/map/manifest.json`. Coverage: `own` — the only real domain flow; high risk.

## Purpose
Accept tracked visitor events, reject invalid ones, deduplicate by `idempotency_id`, publish a server-rendered row to Mercure.

## Files and contracts
- `TrackedEvent` — `fromPayload(array)`: requires non-empty `idempotency_id`, `type` ∈ 13 supported (`pageview`, `add_to_cart`, `remove_from_cart`, `purchase`, `login`, `signup`, `search`, `wishlist`, `cart_abandon`, `email_open`, `email_click`, `popup_shown`, `coupon_used`); optional `occurred_at` (parsed, invalid → `InvalidEventPayload`), `detail`, `customer_id`, `customer_name`, `site`.
- `EventIngestion::ingest(TrackedEvent, accountId='1'): bool` — dedup key `event.seen.<xxh128>` in `cache.app`, TTL 86 400 s; publishes `Update(topic, {"html": row})`; returns false on duplicate.
- `InvalidEventPayload` extends `InvalidArgumentException`.
- HTTP surface: `POST /collect` → 202 accepted / 200 duplicate / 400 invalid (`src/Controller/EventIngestionController.php`).

## Dependencies
Inbound: tracker script (not in repo), e2e `events.spec.ts`, `tests/Tracking/EventIngestionTest.php`.
Outbound: `cache.app` (Postgres `cache_items`), Mercure `HubInterface`, Twig (`components/molecules/event-row.html.twig`), `Panel\\EventStreamTopic`, `Panel\\Workspace` (site colour), `Panel\\Content\\EventFeed::TYPES` (icon/tone), translator key `events.<type>`.

## Tests
`tests/Tracking/EventIngestionTest.php` uses `MockHub`; e2e posts to `/collect` and waits for the SSE row (`data-stream-state=live`).

## Invariants
- Duplicate must never publish twice (product promise; e2e "replayed event is not shown twice").
- Row markup lives only in the Twig molecule; do not build rows in TypeScript.
- Response codes 202/200/400 are part of the tracker contract.

## Risks
No auth/site key/rate limit on `/collect`; account hard-coded; dedup lives in a disposable UNLOGGED cache, not a persistent event table (R4, R7); event types listed here vs `EventFeed::TYPES` (8 entries) drift — unknown types fall back to icon `activity`.

## Evidence
`src/Tracking/*.php`, `src/Controller/EventIngestionController.php`, `config/packages/{cache,mercure}.yaml`, `frankenphp/Caddyfile`.
<!-- END project-context-initializer:context -->
