# Raw contract probes — wave-2, dimension contract

Base: https://localhost:19101 (kivvi-int, wave SHA 22d7fcb6380723728a33fc21fda22a92594a2e88)

## (a) POST /collect idempotency (probe-c25528affd)

1st call → `202 {"status":"accepted"}`
2nd call (same idempotency_id) → `200 {"status":"duplicate"}`

Matches oracle steps 2-3 shape exactly.

## (b) POST /collect non-JSON body

`400 {"error":"Oczekiwano obiektu JSON."}` — exact match to required Polish message.

## (c) POST /collect unknown type (`teleport`, probe id randomized)

`400 {"error":"Nieznany typ zdarzenia „teleport”."}` — exact match to required Polish message,
byte-identical to oracle step 5 (decoded).

## (d) Mercure SSE subscription (`topic=/accounts/1/events`) + fresh POST /collect (probe-c3d34b2416)

SSE frame received:
```
data: {"event":{"time":"22:03:07","type":"Zakup","typeIcon":"money","tone":"good","detail":"PROBE-SSE","customerId":"c_1001","customerName":"Hania","siteName":"aureashop.pl","siteColor":"#7a8763"}}
```
Confirms DEV-3: payload is a JSON `event` object (not `{"html": ...}`), with `type`, `detail`,
`customerName` present as required.

## (e) `event_dedup` row count (docker compose -p kivvi-int exec -T database psql -U app -d app -tAc "select count(*) from event_dedup")

Before probe (d): 3 — After: 4. +1 row for the new idempotency id, consistent with the
DEV-5 mapping (`cache_items` dedup keys → `event_dedup`) confirmed independently in both
journeys' `db.json` (event-stream delta +1 for one new id across steps 2-3; customers delta 0).

## (f) Customers 404 surface

- `GET /api/v1/pl/customers/c_9999` → `404`, **empty body**, no `content-type` header
  (`content-length: 0`). This is new `/api/v1/*` surface with no oracle equivalent (DEV-4
  records `/api/v1/*` as the new baseline, not oracle-compared) — flagged as an OBSERVATION,
  not a contract regression, since there is nothing in the oracle to diff it against. Recorded
  here for visibility: an empty-body 404 (vs. the JSON-shaped errors `/collect` returns) is a
  product-consistency question, out of this dimension's FAIL criteria.
- `GET /pl/customers/c_9999` → `404`, HTML document body `"Nie ma takiego klienta."` — matches
  oracle step 6 (`documentStatus: 404`) at the status level; body/visual comparison is skipped
  per DEV-12 (visual dimension only). Contract dimension requires status parity only, which
  holds (404 == 404).

## Hand audit of event-stream steps 2-5 (`/collect`) vs oracle, byte level

All four steps: method, path, status, and decoded response body content are identical between
oracle and candidate (`{"status":"accepted"}`, `{"status":"duplicate"}`, and the two 400 bodies
with exact Polish text). The only byte-level difference is JSON unicode-escaping style — the
oracle (Symfony/PHP `json_encode`) escapes non-ASCII as `\uXXXX` (e.g. `„`), the candidate
(Spring/Jackson) emits raw UTF-8 (`„`) — both decode to the identical string. Not a semantic
content difference; no deviation id needed for this JSON-encoder artifact. `compare.mjs`'s
`compareHttpEntry()` does not itself diff `/collect` response bodies (its body-check branch only
fires for `/preferences/*` and `/{locale}/login`, and only at `status === 200`), so this hand
audit is the only check of `/collect` body parity — it passes.
