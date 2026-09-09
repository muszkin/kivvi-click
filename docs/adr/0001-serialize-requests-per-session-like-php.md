# 0001. Serialize HTTP requests per session in the Spring backend, mirroring PHP's session lock

**Status:** accepted
**Date:** 2026-09-09
**Deciders:** implementation-orchestrator run `2026-09-08T141500Z-spring-vue-migration` (operator contract: fire-and-forget, zero-change parity); owner Piotr Mucha

## Context

The old Symfony stack persisted panel preferences (`POST /preferences/theme`, `/preferences/sidebar`) fire-and-forget and relied on PHP's native session handler, which locks the session for the whole request: a reload issued right after the click always observed the committed preference. Spring Session JDBC has no such lock. `tests/e2e/specs/navigation.spec.ts` ("theme toggle / sidebar collapse survives a reload") flaked 5/20 under host load on the Spring stack (run evidence: `waves/wave-3/round-2/e2e.md`, `slices/w3-shell-preferences/`). Observed, not inferred.

## Decision

Keep the client fire-and-forget order (DOM first, then `fetch(..., { keepalive: true })`) and add `SessionRequestSerializationFilter` (`backend/src/main/java/click/kivvi/infrastructure/session/`): a `OncePerRequestFilter` ordered outside Spring Session's `SessionRepositoryFilter` that holds a per-session `ReentrantLock` (bounded registry, 30 s timeout → 503) for requests carrying the `SESSION` cookie; requests without a session (`/collect`, static assets) are untouched. Additionally (wave-4), the shell store awaits the preference POST before applying the DOM state, because a lowest-priority keepalive request can still reach the server after the reload's shell GET.

## Alternatives considered

| Alternative | Why it lost |
| --- | --- |
| Client-only `keepalive: true` | Guarantees the POST is sent, not that it is committed before the reload's GET; still 1/5 flakes (`waves/wave-3/round-3/e2e.md`). |
| Optimistic local cache (localStorage) of the pending preference | Diverges from the old stack's server-as-truth semantics and masks cross-device state; rejected as a behaviour change. |
| Accept the flake as environmental | Reproduced in isolation on a calm host; the old stack never flaked — a real parity gap. |

## Consequences

Same-session requests are serialized (PHP semantics); parallel API calls from one browser tab queue behind each other for milliseconds. A request holding the lock longer than 30 s yields 503 (none exist; no streaming endpoints — Mercure is a separate service). Session id resolution reads the `SESSION` cookie directly because Tomcat's `getRequestedSessionId()` only knows `JSESSIONID`. Accepted risk: the `ASYNC` dispatcher is implicitly covered; if an async/streaming endpoint is ever added, revisit (owner: backend maintainer).

## Revisit when

An endpoint needs to run longer than the lock timeout, async/SSE is served by the Spring app, or Spring Session gains native per-session locking.
