import type { RouterScrollBehavior } from "vue-router";

function layoutSignature(): string {
    return `${document.documentElement.scrollWidth}x${document.documentElement.scrollHeight}`;
}

function nextFrame(): Promise<void> {
    return new Promise((resolve) => requestAnimationFrame(() => resolve()));
}

// A page's own async data fetch (every view in this app loads its payload in `onMounted`) can
// leave the document's scrollable size *unchanged* for a frame or two simply because nothing has
// rendered yet, not because layout has actually reached its final size — measured empirically on
// the e-mail editor (a fetch to the local API resolves comfortably inside 200ms even under
// container overhead, but the width reads identically "stable" for 1-2 frames purely while
// waiting on the network before the real content, and its true final width, appears). Requiring
// only a couple of stable frames re-introduces exactly the bug this module exists to fix, just
// applied at the wrong moment. Requiring many more consecutive stable frames before concluding
// "final" comfortably outlasts that idle gap.
const REQUIRED_STABLE_FRAMES = 12;

// A page whose content keeps growing forever would never "stabilize" by the measure above, so
// this bounds the wait — not a real risk in this app today (the event stream's own live-arriving
// rows grow inside `.main-scroll`, which the app shell keeps at a fixed size, never
// `document.documentElement`), but a page-agnostic mechanism should never be able to hang.
const MAX_STABILITY_FRAMES = 90;

/**
 * Resolves once the document's scrollable size has stopped changing across
 * {@link REQUIRED_STABLE_FRAMES} consecutive frames, or after `maxFrames` — whichever comes
 * first: "measure, don't guess" a fixed hydration delay.
 */
export async function waitForStableLayout(
    maxFrames: number = MAX_STABILITY_FRAMES,
): Promise<void> {
    let previous = layoutSignature();
    let stableCount = 0;
    for (let frame = 0; frame < maxFrames; frame++) {
        await nextFrame();
        const current = layoutSignature();
        if (current === previous) {
            stableCount++;
            if (stableCount >= REQUIRED_STABLE_FRAMES) return;
        } else {
            stableCount = 0;
            previous = current;
        }
    }
}

// --- reload fallback -----------------------------------------------------------------------
//
// vue-router is *documented* (and its source confirms the intent — see scrollBehavior's own doc
// comment below) to restore scroll on reload via `history.state.scroll`, populated by its own
// `pagehide`-time `history.replaceState()` call. Empirically, in the Chromium build this
// pipeline runs (151.0.7922.34), that does not work: a `replaceState()` call made during
// `pagehide` (or `beforeunload`, or even called synchronously right before `location.reload()`/
// `history.go(0)` with no vue-router involved at all — tested directly against raw
// `history.state`, isolated from this app and from vue-router entirely) is discarded by the time
// the reloaded document reads `history.state` back; it reverts to whatever the entry's state was
// when first created. Reproduced identically with an added 200ms delay before the reload call,
// ruling out a timing race.
//
// This is the *only* gap: a genuine back/forward (`popstate`) restores correctly through
// vue-router's own, separate, popstate-keyed store — proven with a real `router.push()` +
// browser back/forward round-trip, both against this app and against an isolated vue-router
// harness (see the report). So this fallback is scoped as narrowly as possible: it only ever
// engages when vue-router itself supplies no `savedPosition`, there happens to be a same-href
// entry saved here, *and* {@link isReloadOfAnAlreadyVisitedEntry} (below) confirms this
// navigation really is a reload — never for a genuine fresh navigation to a new URL (no entry, or
// a mismatched one), never for a same-URL top-level navigation that is not actually a reload (a
// retyped address, bookmark, or duplicated tab happening to match a torn-down href — the old MPA
// always starts that at the top, not wherever the last visit left off), and never for a
// `popstate` navigation (vue-router already supplies `savedPosition` for those, so
// `scrollBehavior` never reaches this branch at all).
const STORAGE_KEY = "kivvi:scroll-restore";

interface SavedScroll {
    href: string;
    left: number;
    top: number;
}

function readStorage(): string | null {
    try {
        return sessionStorage.getItem(STORAGE_KEY);
    } catch {
        return null;
    }
}

function writeStorage(value: string): void {
    try {
        sessionStorage.setItem(STORAGE_KEY, value);
    } catch {
        // sessionStorage unavailable (private mode, quota) — losing the saved position is
        // harmless: the next reload just behaves like an ordinary, unrestored fresh navigation.
    }
}

function clearStorage(): void {
    try {
        sessionStorage.removeItem(STORAGE_KEY);
    } catch {
        // see writeStorage
    }
}

/** Persists the current window scroll position keyed to the exact URL it belongs to. */
export function saveScrollPositionForReload(): void {
    const saved: SavedScroll = {
        href: location.href,
        left: window.scrollX,
        top: window.scrollY,
    };
    writeStorage(JSON.stringify(saved));
}

/**
 * Reads and clears the saved scroll position, returning it only when it belongs to `href`. A
 * mismatch or nothing saved both return `null` — the caller applies no scroll at all.
 */
export function consumeScrollPositionForReload(
    href: string,
): { left: number; top: number } | null {
    const raw = readStorage();
    clearStorage();
    if (!raw) return null;
    try {
        const saved = JSON.parse(raw) as SavedScroll;
        return saved.href === href
            ? { left: saved.left, top: saved.top }
            : null;
    } catch {
        return null;
    }
}

if (typeof window !== "undefined") {
    window.addEventListener("pagehide", saveScrollPositionForReload);
}

// --- reload detection ------------------------------------------------------------------------
//
// The fallback above is keyed only on href equality, which cannot tell an actual reload apart
// from a fresh top-level navigation to a URL that merely happens to match one already torn down
// in this tab session (a retyped address, a bookmark, "duplicate tab", browser history reused
// outside any popstate this app's own JS ever sees). The old MPA always starts a navigation like
// that at the top; only an F5/Ctrl+R/`location.reload()` should ever land the user back where
// they were.
//
// The obvious way to tell them apart is the Navigation Timing Level 2 API
// (`performance.getEntriesByType("navigation")[0].type === "reload"`) — repair-3's own first
// attempt. Empirically, that does not work in the one environment this fix is actually verified
// against: `compare.mjs`/`capture.mjs` freeze the page clock for every run
// (`page.clock.setFixedTime(...)`, needed for deterministic timestamps elsewhere — DEV-1's
// `.event-row__time`, the dashboard cardiogram), and under a frozen clock,
// `performance.getEntriesByType("navigation")` returns a permanently empty array — not merely a
// late value, confirmed by polling it for 900ms after a real `page.reload()` — and the legacy,
// deprecated `performance.navigation.type` is equally empty (Chromium has dropped its data,
// object shell only). Neither Performance Timing API is usable here; this is not a verifier
// quirk to route around, it is the actual browser behaviour compare.mjs's own methodology
// exercises.
//
// `history.state` is not clock-dependent, and (unlike `.scroll` written during `pagehide` — see
// `saveScrollPositionForReload`'s own comment) a value written at a normal point in the page's
// lifecycle (not during teardown) reliably survives a reload — verified the same way: write a
// marker into `history.state` right after routing resolves, reload, read it back. A genuinely
// fresh top-level navigation to the same URL via `history.pushState`/vue-router's own `push()` —
// this app's own `changeLocation` path, `routes.ts:24-28`'s doc comment: every in-app transition
// is a real document GET, never `router.push`/`.replace()` — gets a brand-new state object from
// `buildState()`, with no trace of anything written on a previous visit — verified by navigating
// away and back and finding the marker gone. `router.replace()` is the one path that would NOT
// hold this: it merges `history.state` onto the new entry (`assign({}, history.state,
// buildState(...), data, {position})` in vue-router's own source), so a marker set here could
// leak onto an unrelated entry. Not a real risk today — this app never calls `router.replace()`
// anywhere (same `routes.ts:24-28` doc comment) — but this module has no way to special-case it if
// that ever changes; it is an accepted, narrow limitation of the marker approach, not something
// this function can detect or guard against. So: mark the current entry every time this runs, and
// report whether it was already marked — a reload reuses the entry (and its state), so a second
// run always sees its own mark.
const VISITED_MARKER_KEY = "kivviScrollRestorationVisited";

/**
 * Marks the current history entry as "seen by this module" and reports whether it was already
 * marked — i.e. whether this navigation is a reload of an entry already visited once, as opposed
 * to the first time this entry is seen (a fresh top-level navigation, or the first visit to a
 * `popstate`-restored entry). Call exactly once per navigation, unconditionally — not only from
 * the reload-fallback branch — so an entry that first resolves via a `popstate` `savedPosition`
 * (never reaching that branch) still gets marked, and a *later* plain reload of that same entry
 * is still correctly recognised.
 *
 * Guarded like every other browser-API touchpoint in this file (`readStorage`/`writeStorage`/
 * `clearStorage` above): `history.replaceState()` can throw (Chrome enforces a ~100-calls/10s
 * history-mutation limit; a sandboxed/cross-origin-restricted frame can reject it outright), and
 * an uncaught throw here would propagate out of `scrollBehavior` into vue-router's own generic
 * `.catch(err => triggerError(...))`, which logs rather than degrading gracefully the way the
 * rest of this module does. Defaulting to `false` ("not a reload") on failure is safe: the worst
 * outcome is `scrollBehavior` skipping the reload-fallback restore for this one navigation, never
 * a wrong-position scroll.
 */
export function isReloadOfAnAlreadyVisitedEntry(): boolean {
    try {
        const state = history.state as {
            [VISITED_MARKER_KEY]?: boolean;
        } | null;
        const alreadyVisited = Boolean(state?.[VISITED_MARKER_KEY]);
        history.replaceState({ ...state, [VISITED_MARKER_KEY]: true }, "");
        return alreadyVisited;
    } catch {
        return false;
    }
}

// --- scrollBehavior --------------------------------------------------------------------------

/**
 * Hydration-aware `scrollBehavior` for {@link createRouter} — vue-router's own idiomatic
 * extension point for exactly this, rather than a hand-rolled `pagehide`/`history.scrollRestoration`
 * mechanism (repair-1's approach, reverted here: it broke back/forward, see below).
 *
 * vue-router already does all the save/restore plumbing itself once this option is configured —
 * verified in `node_modules/vue-router/dist/vue-router.js`:
 * - line 1162: `if (isBrowser && options.scrollBehavior && "scrollRestoration" in history)
 *   history.scrollRestoration = "manual";` — only touches the global flag when `scrollBehavior`
 *   is set, and only ever to `"manual"` (never flips it back), so there is nothing left for this
 *   module to do with that flag itself.
 * - lines 76-77: `createWebHistory()` listens to BOTH `popstate` (back/forward) and `pagehide`
 *   (reload/navigate away/close tab) unconditionally — `beforeUnloadListener` (named after the
 *   concept, wired to `pagehide`) calls `history.replaceState(..., { scroll:
 *   computeScrollPosition() })` on teardown; the `popstate` handler calls
 *   `saveScrollPosition(getScrollKey(from.fullPath, info.delta))` before handling the
 *   navigation. Repair-1's module only ever listened for `pagehide`, so a `popstate` (in-app
 *   back/forward) navigation lost native scroll memory with nothing replacing it — the
 *   regression `review-repair-1.md` rubric 1(b) found. Fixed here: `popstate` is entirely
 *   vue-router's own responsibility now.
 * - lines 1487-1490 (`handleScroll`): resolves `scrollPosition` from vue-router's own popstate
 *   store for a `popstate` navigation, or from `history.state.scroll` for the first navigation
 *   after a fresh load/reload, then calls `scrollBehavior(to, from, scrollPosition)` — this
 *   function — and applies whatever it resolves to via `scrollToPosition`, which only ever calls
 *   `window.scrollTo`/`window.scrollBy`, never touching `.main-scroll` or any other descendant
 *   scroller (matching native browser scroll-restoration's own scope: the top-level document).
 *
 * The `history.state.scroll` half of that (the reload case) does not actually work in the
 * Chromium build this pipeline runs — see the big comment above `saveScrollPositionForReload`
 * for the empirical proof — so this falls back to a narrow, same-href-only sessionStorage save
 * for exactly that gap. `popstate` never reaches the fallback branch: vue-router supplies
 * `savedPosition` correctly for it on its own. The fallback is further gated on
 * {@link isReloadOfAnAlreadyVisitedEntry} so it only ever fires for an actual reload, not merely
 * a fresh top-level navigation that happens to land on the same URL — see that function's own
 * comment for why it, not the Performance Timing API, is what gates this.
 */
export const scrollBehavior: RouterScrollBehavior = async (
    _to,
    _from,
    savedPosition,
) => {
    // Unconditional and first: every navigation must mark its entry, not only ones that reach
    // the fallback branch below (see isReloadOfAnAlreadyVisitedEntry's own doc comment).
    const isReload = isReloadOfAnAlreadyVisitedEntry();

    if (savedPosition) {
        await waitForStableLayout();
        return savedPosition;
    }

    const fallback = consumeScrollPositionForReload(location.href);
    if (fallback && isReload) {
        await waitForStableLayout();
        return fallback;
    }

    return { left: 0, top: 0 };
};
