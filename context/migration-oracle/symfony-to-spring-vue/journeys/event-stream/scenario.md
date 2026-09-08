# Journey: event-stream

The store owner can watch tracked events arrive live, filter the log, and a replayed event is never shown twice

Source tests: tests/e2e/specs/events.spec.ts, tests/Tracking/EventIngestionTest.php

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

1. `{"goto":"/pl/events","waitAttr":["#event-stream","data-stream-state","live"]}`
2. `{"post":"/collect","json":{"idempotency_id":"oracle-evt-__RUN__","type":"purchase","detail":"412,00 PLN · zamówienie ORACLE-__RUN__","customer_id":"c_1001","customer_name":"Hania Kowalska","site":"aureashop.pl"},"expectStatus":202,"waitText":["#event-stream .event-row","ORACLE-__RUN__"]}`
3. `{"post":"/collect","json":{"idempotency_id":"oracle-evt-__RUN__","type":"purchase","detail":"412,00 PLN · zamówienie ORACLE-__RUN__","customer_id":"c_1001","customer_name":"Hania Kowalska","site":"aureashop.pl"},"expectStatus":200,"note":"duplicate idempotency id"}`
4. `{"post":"/collect","json":{"type":"purchase"},"expectStatus":400,"note":"missing idempotency id"}`
5. `{"post":"/collect","json":{"idempotency_id":"oracle-bad-__RUN__","type":"teleport"},"expectStatus":400,"note":"unknown type"}`
6. `{"click":"[data-action=\"pause-stream\"]","waitAttr":["#event-stream","data-paused","true"]}`
7. `{"goto":"/pl/events?type=purchase&site=aurea&range=24h"}`
