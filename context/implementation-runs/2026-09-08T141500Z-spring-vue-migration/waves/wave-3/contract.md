# Wave-3 verifier — dimension: contract (round 4)

**Dimension status: PASS**, bound to wave SHA `11d3cc49fdc15e6e696c9a6f175d8953caec8a2e`.

Independent, read-only verification. Ran ALONE on the shared stack (all other wave-3 round-4
dimensions had already finished — `waves/wave-3/visual.md` and `waves/wave-3/round-4-parallel/e2e.md`
were both present before this run started). No worker reports, review files, or earlier-round
evidence (`waves/wave-3/round-1..3/`) were read — those directories were listed only at the
path level to confirm the evidence-output convention, never opened for content. Checkout
confirmed at the wave SHA and clean:

```
$ cd /home/muszkin/work/kivvi-click-wt/verify-wave-3-contract && git rev-parse HEAD && git status --porcelain
11d3cc49fdc15e6e696c9a6f175d8953caec8a2e
(clean)
```

Oracle manifest hash recomputed and matches the packet:

```
$ sha256sum context/migration-oracle/symfony-to-spring-vue/manifest.json
4945a8deb19ea342a4aaa2c0ac681dc13f05c121aa387677c30ecf210a90f74f
```

Stack was already running at `https://localhost:19101` (compose project `kivvi-int`); not
started or stopped by this verifier (`docker compose -p kivvi-int ps` showed all three
containers `Up ... (healthy)` before the first run and throughout).

## Round-4 repair reviewed

`backend/src/main/java/click/kivvi/infrastructure/session/SessionRequestSerializationFilter.java`
(+ `SessionRequestSerializationConfig.java`, `SessionLockRegistry.java`) is new this round: it
serializes requests carrying the same `SESSION` cookie so a reload's `GET` waits for an
in-flight preference `POST` on the same session to finish committing (PHP session-lock parity).
Source review (file is new — no prior version to diff):

- `resolveSessionId()` reads the `SESSION` cookie directly off `HttpServletRequest#getCookies()`;
  when absent, `doFilterInternal` takes the early-return branch (`chain.doFilter(...)` with no
  lock acquired, no delay) — a request without a `SESSION` cookie is provably unlocked by the
  code path itself, not just by observed behaviour. `/collect` is called only from
  event-stream's three unauthenticated steps in this wave's scope (no journey here logs in
  before calling it), so it always takes this branch.
- Lock acquisition is bounded by a 30 s timeout (`kivvi.session-lock.timeout-ms`, default
  `SessionRequestSerializationFilter.DEFAULT_LOCK_TIMEOUT_MS`); exceeding it returns `503`
  rather than deadlocking. Grepped every recorded response status across all six journeys
  replayed below: **zero `503`s** (statuses seen: 200, 202, 302, 400, 404 only).
- The test-only delay knob (`kivvi.testing.preferences-theme-post-delay-ms`) is unset in
  `backend/src/main/resources/` and in `compose.next.yaml` for the `kivvi-int` stack — confirmed
  by `grep`, defaults to 0 — so no artificial delay was active during this replay.

## Commands run

All commands: cwd `/home/muszkin/work/kivvi-click-wt/verify-wave-3-contract`.

```
npm ci                                    (cwd .../tools/migration-verify)   exit 0
npm ci                                    (cwd .../tests/e2e)                exit 0

VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" \
  node tools/migration-verify/compare.mjs --journey automations \
    --base https://localhost:19101 --dimension contract \
    --out <run-dir>/waves/wave-3/contract                                    exit 0, 0 regressions

VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" \
  node tools/migration-verify/compare.mjs --journey settings \
    --base https://localhost:19101 --dimension contract \
    --out <run-dir>/waves/wave-3/contract                                    exit 0, 0 regressions

VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" \
  node tools/migration-verify/compare.mjs --journey campaigns-email-editor \
    --base https://localhost:19101 --dimension contract \
    --out <run-dir>/waves/wave-3/contract                                    exit 0, 0 regressions

VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" \
  node tools/migration-verify/compare.mjs --journey login \
    --base https://localhost:19101 --dimension contract \
    --out <run-dir>/waves/wave-3/contract                                    exit 0, 0 regressions

VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" \
  node tools/migration-verify/compare.mjs --journey event-stream \
    --base https://localhost:19101 --dimension contract \
    --out <run-dir>/waves/wave-3/contract                                    exit 0, 0 regressions

VERIFY_COMPOSE="docker compose -p kivvi-int -f compose.next.yaml" \
  node tools/migration-verify/compare.mjs --journey shell-navigation \
    --base https://localhost:19101 --dimension contract \
    --out <run-dir>/waves/wave-3/contract                                    exit 0, 0 regressions
```

(`<run-dir>` = `context/implementation-runs/2026-09-08T141500Z-spring-vue-migration`.)

No invocation threw or timed out on the (already-warm) stack, so no round needed a retry — every
run above is a single, first-try run. Full stdout/stderr for each is at
`waves/wave-3/contract/<journey>.run1.log`.

## Per-journey verdicts

| Journey | Role | Verdict | Deviation ids | Regressions (compare.mjs) |
| --- | --- | --- | --- | --- |
| automations | wave-3 primary (B26, B01) | **parity** | DEV-4 (document reduction, every step) | 0 |
| settings | wave-3 primary (B06, B32, B01) | **parity** (step 10 accepted-deviation) | DEV-4 (all steps); DEV-12 (step 10, 404 document — status 404 confirmed identical, only visual/a11y/text skipped, which is not this dimension's concern) | 0 |
| campaigns-email-editor | wave-3 primary (B27, B28, B01) | **parity** | DEV-4 (all steps); DEV-7 (no POST …/blocks on either side — grepped, confirmed) | 0 |
| login | regression guard | **parity** | DEV-4 (document reduction); DEV-5 (cache_items→event_dedup+shedlock mapping, 0/0); DEV-9 (sessions delta oracle 0 vs candidate 1, within declared tolerance — Spring Session eager persistence on sign-in) | 0 |
| event-stream | regression guard (exercises `/collect`) | **parity** | DEV-4 (full-parity `/collect` bodies byte-identical after JSON canonicalization, steps 2-4); DEV-3 (Mercure JSON payload — not diffable by compare.mjs, `/.well-known/mercure` is normalize.json-excluded from all recordings; SSE-delivered row content instead verified by the oracle scenario's own `waitText` assertion, which passed); DEV-5 (cache delta 1/1, exact) | 0 |
| shell-navigation | regression guard (exercises the round-4 fix directly: toggle-sidebar → reload, set-theme → reload) | **parity** | DEV-4 (full-parity `/preferences/sidebar` and `/preferences/theme` bodies byte-identical, steps 11/14; both immediate reloads at steps 12/15 returned 200 with no 503); DEV-9 (session cookie name never compared — stripped by normalize.json on both sides; sessions delta 1/1, exact); step 21 (`/de/dashboard`) 404 confirmed identical | 0 |

## Independent verification performed (beyond trusting compare.mjs's own verdict)

Full detail in `waves/wave-3/contract/independent-diff.txt`. Summary:

- **db.json deltas**: hand-computed from each journey's raw `before`/`after` row counts and
  cross-checked against the oracle's own `journeys/<journey>/db.json` after applying the DEV-5
  mapping by hand (not just reading compare.mjs's `report.md` line). All six journeys match
  exactly except login's `sessions` delta (oracle 0 → candidate 1), which is within DEV-9's
  declared ±1 tolerance. The chained `before` counts across the six sequential runs
  (`spring_session`: 38→38→39→40→40→41; `event_dedup`: 104→104→104→104→105→105) confirm no
  other process wrote to the database while this dimension ran alone.
- **Full-parity body diffs**: manually compared candidate vs. oracle `http.jsonl` bytes for
  every full-parity endpoint hit in scope — `/collect` (event-stream steps 2-4),
  `/preferences/sidebar` and `/preferences/theme` (shell-navigation steps 11/14), `/pl/login`
  and `/pl/logout` (login steps 4-7) — rather than relying only on compare.mjs's empty `detail`
  field for a passing step.
- **DEV-7**: grepped all 8 `campaigns-email-editor` candidate step recordings for `"blocks"` —
  zero matches.
- **DEV-12**: read settings step 10's candidate and oracle `step.json`/`http.jsonl` directly —
  both `documentStatus: 404`, method/path/status identical; only the HTML page title differs
  (visual dimension's concern, already masked there per `waves/wave-3/visual.md`).
- **503 sweep**: grepped every candidate `steps/*/http.jsonl` across all six journeys for
  status 503 — none found.
- **`/collect` lock-bypass reasoning**: read `SessionRequestSerializationFilter.java` directly
  (see "Round-4 repair reviewed" above) rather than inferring behaviour only from the absence of
  a 503 in the recording.

## Evidence paths

- `waves/wave-3/contract/<journey>/report.md`, `report.json` — compare.mjs's own per-step
  verdict table, for automations, settings, campaigns-email-editor, login, event-stream,
  shell-navigation.
- `waves/wave-3/contract/<journey>/steps/<n>/{http.jsonl,step.json,url.txt}` — raw recorded
  HTTP traffic per step (contract dimension does not capture screenshots/a11y/texts).
- `waves/wave-3/contract/<journey>/db.json` — before/after/delta row counts per journey.
- `waves/wave-3/contract/<journey>.run1.log` — full stdout of each compare.mjs invocation.
- `waves/wave-3/contract/independent-diff.txt` — this verifier's hand-computed cross-checks
  (db deltas, full-parity body diffs, DEV-7/DEV-12 spot checks, 503 sweep, filter source review).

## Deviations applied

DEV-4 (all journeys, all steps — document/api-baseline reduction to method+path+status(+location),
full parity for `/collect`, `/preferences/theme`, `/preferences/sidebar`, `/import/upload`,
`/{locale}/login`, `/{locale}/logout`), DEV-7 (campaigns-email-editor: absence of a `POST …/blocks`
call is expected on both sides), DEV-12 (settings step 10: 404 document, contract dimension
still requires and confirms status 404 — only visual/a11y/text are skipped, out of this
dimension's scope). DEV-5 and DEV-9 applied as regression-guard context on login/event-stream/
shell-navigation (already active from earlier waves, re-confirmed here, not newly introduced by
wave-3).
