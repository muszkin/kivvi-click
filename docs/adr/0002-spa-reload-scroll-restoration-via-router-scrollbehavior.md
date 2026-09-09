# 0002. Restore scroll on reload through vue-router `scrollBehavior` with a history-state reload marker

**Status:** accepted
**Date:** 2026-09-09
**Deciders:** implementation-orchestrator run `2026-09-08T141500Z-spring-vue-migration`; owner Piotr Mucha

## Context

The old multi-page stack delivered full-width HTML synchronously, so Chromium's native scroll restoration after `page.reload()` landed at the pre-reload offset. The SPA paints a narrow pre-hydration shell first; Chromium clamps the restored offset to that width (measured 173 px instead of 346 px on the mobile e-mail editor), a 2.96 % visual regression on `campaigns-email-editor` step 6 (`slices/w3-campaigns-email-editor/review.md`). Observed.

## Decision

Configure vue-router `scrollBehavior(to, from, savedPosition)` (`frontend/src/router/index.ts`, helper `frontend/src/router/scrollRestoration.ts`): on a saved position, resolve after the document's scrollable size has been stable for a fixed number of animation frames (bounded); on a plain push return the top. Because `history.state.scroll` written on `pagehide` does not survive a reload in Chromium 151/152 (verified), a same-href sessionStorage fallback applies only when a history-state marker proves the entry was already visited (reload), never on fresh navigations or popstate.

## Alternatives considered

| Alternative | Why it lost |
| --- | --- |
| Global `history.scrollRestoration = "manual"` + `pagehide` persistence | Disabled native back/forward scroll memory app-wide (no popstate handling) — review-repair-1 FAIL. |
| Detect reload via `performance.getEntriesByType("navigation")` | The parity verifier freezes the page clock (`page.clock.setFixedTime`), so Navigation Timing entries never appear; broke under `compare.mjs`. |
| Accept a visual deviation (DEV row) | The oracle's behaviour is reproducible; deviations are reserved for framework chrome, not layout state. |

## Consequences

Reload restores the old offset once layout is stable; back/forward keep vue-router's native handling; a stale offset can only be applied on a genuine reload of the same entry. Accepted risk: the stability wait adds up to a bounded delay before restoration on pages whose size keeps changing (live event rows); owner: frontend maintainer.

## Revisit when

Chromium changes scroll-restoration timing for SPAs, vue-router persists `history.state.scroll` across reloads, or the shell starts rendering final-width markup before hydration.
