# Slice packet w2-event-stream — wave-2, journey J4 "event-stream"

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w2-event-stream · branch `migration/wave-2/event-stream` · parent SHA <PARENT-SHA> (feature HEAD after wave-1). **Lease:** compose project `kivvi-w-events`, HTTP_PORT=19040, HTTPS_PORT=19041. **Model:** sonnet, effort HIGH.

**Capability:** The store owner can watch tracked events arrive live, filter the log, and a replayed event is never shown twice.
**Oracle:** journeys/event-stream/steps/1..7 (`/pl/events` with `data-stream-state=live`; `POST /collect` 202 then the row with the marker appears; replay 200; two 400 cases; pause; `?type=purchase&site=aurea&range=24h`). Behaviours: B15–B19, B24, B01 (events row).
**Old-stack sources:** `src/Controller/EventStreamController.php`, `src/Controller/EventIngestionController.php`, `src/Tracking/{TrackedEvent,EventIngestion,InvalidEventPayload}.php`, `src/Panel/Content/EventFeed.php`, `src/Panel/EventStreamTopic.php`, `templates/pages/events.html.twig`, organisms `event-stream`, molecules `event-row`, `filter-chip`, `segmented`, `assets/controllers/event-stream.ts`, `config/packages/mercure.yaml`, `frankenphp/Caddyfile` (hub config), `tests/Tracking/EventIngestionTest.php`, `tests/e2e/specs/events.spec.ts`; `mercure/Caddyfile` and `compose.next.yaml` in your worktree (hub already runs as the edge).

## In scope
- Backend: `POST /collect` — exact contract: JSON body → `TrackedEvent` validation identical to the PHP (`idempotency_id` required → 400 `{"error":"Pole „idempotency_id” jest wymagane."}`; `type` ∈ the 13 supported → else 400 `{"error":"Nieznany typ zdarzenia „<type>”."}`; invalid `occurred_at` → 400 `{"error":"Pole „occurred_at” nie jest poprawną datą."}`; non-JSON body → 400 `{"error":"Oczekiwano obiektu JSON."}`); dedup in the `event_dedup` table (hash = xxh128 of the id as today, or sha-256 if xxh128 is unavailable on the JVM — record which; 24 h expiry; replay → 200 `{"status":"duplicate"}`; new → 202 `{"status":"accepted"}` after publishing); Mercure publish to `/accounts/1/events` on the hub (`MERCURE_URL` internal, JWT HS256 with `publish: ["*"]` from `MERCURE_JWT_SECRET`) with the JSON event payload per DEV-3: `{"event":{time,type,typeIcon,tone,detail,customerId?,customerName?,siteName?,siteColor?}}` where `type` is the translated label (`events.<type>` PL) and icon/tone from the `EventFeed::TYPES` table, unknown types → icon `activity`, tone `""`; hourly cleanup of expired dedup rows piggybacking on the existing scheduler.
- `GET /api/v1/{locale}/events?type=&site=&range=` → `{ typeFilters, siteFilters, ranges, events (30 rows), total ("9 360"), shown (30), mercureTopic }` exactly as `EventStreamController` passes them (filters as link objects with `active` flags; the 30 sample rows generated exactly like `EventFeed::rows()` — same deterministic pattern relative to the request clock).
- Frontend: `EventsView.vue` = `pages/events.html.twig`; components `EventStream` (organism: `#event-stream`, `data-controller="event-stream"`, `data-event-stream-topic`, `data-stream-state` connecting→live→reconnecting, `data-paused`, `aria-live="polite"`), `EventRow` (molecule: identical DOM incl. `data-action="go-customer"`, `.event-row__time`, `__icon`, `__type`, `__customer`, `__site` and the `new` class flash), `FilterChip`, `Segmented`, the events toolbar; the SSE subscriber (`EventSource` on `/.well-known/mercure?topic=…`, `withCredentials`) renders `EventRow` from the JSON event (DEV-3) and prepends, capping at 80, honouring `data-paused`; `pause-stream` intent.
- Tests: JUnit for `TrackedEvent` (13 types, all four error messages), dedup repository (insert/conflict/expiry), publisher (JWT signed, topic, payload shape — mock the HTTP hub); IT: `/collect` 202→200→400 with Testcontainers and a stubbed hub endpoint; Vitest for `EventStream` with a fake `EventSource` (prepend, cap 80, paused) and `EventRow` markup; e2e `events.spec.ts` (all 4 tests) against your stack — the SSE path must work end to end through the Mercure edge.
- `tools/migration-verify/performance.mjs`: enable the `/collect` p95 measurement (50 sequential POSTs, fresh ids) — budget 500 ms.

## Out of scope
Dashboard, customers, persisting events beyond the dedup table (Linear PIO-81), authentication on `/collect`.

## Deviations in scope
DEV-1 (time text/mask), DEV-3 (payload), DEV-4, DEV-5 (`event_dedup` mapping).

## Specs to run
`cd tests/e2e && E2E_BASE_URL=https://localhost:19041 npx playwright test events.spec.ts` (one invocation, no -g).

## Kill criterion K6
If the row is not visible within 10 s after 202 in three consecutive attempts after a real fix, STOP and report (planning revision on edge/hub topology).

## Parallel-safety obligations
Shared wave with `customers`. Touch only: `backend/src/main/java/click/kivvi/{web/CollectController.java,web/EventsController.java,web/dto/Event*.java,application/tracking/**,application/EventsViewService.java,domain/tracking/**,infrastructure/mercure/**,infrastructure/tracking/**,fixtures/EventsFixtures.java}`, `backend/src/main/resources/application*.y*ml` (mercure/tracking keys only), `backend/src/test/**/{tracking,events,collect,mercure}*`, `frontend/src/views/EventsView.vue`, `frontend/src/components/**/{EventStream,EventRow,FilterChip,Segmented,EventsToolbar}.vue`, `frontend/src/composables/useEventStream.ts`, `frontend/src/composables/useIntents.ts` (add `pause-stream` only), `frontend/src/i18n/messages/events.*.ts`, `frontend/src/router/routes.ts` (your one line + import), `frontend/test/**/{events,event-stream,event-row}*`, `tools/migration-verify/performance.mjs` (collect section only), your report/evidence.
