import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type {
    RouteLocationNormalized,
    RouteLocationNormalizedLoaded,
} from "vue-router";
import {
    consumeScrollPositionForReload,
    isReloadOfAnAlreadyVisitedEntry,
    saveScrollPositionForReload,
    scrollBehavior,
    waitForStableLayout,
} from "@/router/scrollRestoration";

// scrollBehavior never reads `to`/`from` — only `savedPosition` — so an empty stub satisfies the
// type without needing a full route location.
const fakeTo = {} as RouteLocationNormalized;
const fakeFrom = {} as RouteLocationNormalizedLoaded;

/** Counts requestAnimationFrame invocations while resolving each one immediately. */
function stubCountingAnimationFrame(): { calls: () => number } {
    let count = 0;
    vi.stubGlobal("requestAnimationFrame", (cb: FrameRequestCallback) => {
        count++;
        cb(0);
        return count;
    });
    return { calls: () => count };
}

/**
 * Redefines `document.documentElement.scrollWidth` to read a fixed sequence of values, then
 * settle on the sequence's last value forever after — simulates the real DOM's own
 * pre-hydration-shell → final-layout transition without needing a real browser. `scrollHeight`
 * stays constant so only `scrollWidth` drives the signature change, matching the mobile
 * horizontal-overflow case this module exists to fix.
 */
function stubLayoutWidthSequence(widths: number[]): void {
    let index = 0;
    const finalValue = widths[widths.length - 1];
    Object.defineProperty(document.documentElement, "scrollWidth", {
        configurable: true,
        get: () => {
            const value = index < widths.length ? widths[index] : finalValue;
            index++;
            return value;
        },
    });
    Object.defineProperty(document.documentElement, "scrollHeight", {
        configurable: true,
        get: () => 844,
    });
}

/** A width that is different on every single read — the layout that never settles. */
function stubNeverStableLayout(): void {
    let counter = 0;
    Object.defineProperty(document.documentElement, "scrollWidth", {
        configurable: true,
        get: () => counter++,
    });
    Object.defineProperty(document.documentElement, "scrollHeight", {
        configurable: true,
        get: () => 844,
    });
}

beforeEach(() => {
    sessionStorage.clear();
    // A bare pushState with an explicit `null` state — a genuinely fresh entry, never visited by
    // this module (history.state starts out `null`, exactly as it would for a brand-new URL).
    history.pushState(null, "", "/pl/campaigns");
    window.scrollX = 0;
    window.scrollY = 0;
});

afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
    // Redefining scrollWidth/scrollHeight above shadows Element.prototype's own getter with an
    // own property on this one documentElement instance — deleting it un-shadows the prototype's
    // real (jsdom-default, constant) getter for the next test.
    delete (document.documentElement as unknown as Record<string, unknown>)
        .scrollWidth;
    delete (document.documentElement as unknown as Record<string, unknown>)
        .scrollHeight;
});

describe("waitForStableLayout", () => {
    it(
        "does not resolve until the layout has been unchanged for many consecutive frames " +
            "(mutation-detectable: reducing the stability requirement, or dropping the wait " +
            "entirely, makes this assertion fail)",
        async () => {
            const raf = stubCountingAnimationFrame();
            // Two real changes (100→200→300), then stable at 300 forever.
            stubLayoutWidthSequence([100, 200, 300]);

            await waitForStableLayout();

            // 2 changes + this module's own stability requirement — a literal 14, not the
            // module's own REQUIRED_STABLE_FRAMES constant, so a mutation to that constant (or
            // deleting the wait) cannot make this assertion trivially re-pass against itself.
            expect(raf.calls()).toBe(14);
        },
    );

    it("gives up after the bounded max-wait when the layout never settles", async () => {
        const raf = stubCountingAnimationFrame();
        stubNeverStableLayout();

        await waitForStableLayout(5); // small override — keeps the test fast, same code path

        expect(raf.calls()).toBe(5);
    });

    it("resolves immediately (one stable run) when the layout never changes at all", async () => {
        const raf = stubCountingAnimationFrame();
        // jsdom's default scrollWidth/scrollHeight are constant — no stub needed.

        await waitForStableLayout();

        expect(raf.calls()).toBe(12);
    });
});

describe("isReloadOfAnAlreadyVisitedEntry", () => {
    it("is false the first time a history entry is seen, and marks it", () => {
        expect(history.state).toBeNull();

        const first = isReloadOfAnAlreadyVisitedEntry();

        expect(first).toBe(false);
        expect(history.state).toMatchObject({
            kivviScrollRestorationVisited: true,
        });
    });

    it(
        "is true the second time the SAME entry is seen (a reload) — mutation-detectable: " +
            "always returning false, or never marking the entry, makes this assertion fail",
        () => {
            isReloadOfAnAlreadyVisitedEntry(); // first visit — marks the entry

            const second = isReloadOfAnAlreadyVisitedEntry(); // simulates the reload re-running it

            expect(second).toBe(true);
        },
    );

    it("preserves other fields already on history.state (merges, does not replace)", () => {
        history.replaceState({ back: null, position: 1 }, "");

        isReloadOfAnAlreadyVisitedEntry();

        expect(history.state).toMatchObject({
            back: null,
            position: 1,
            kivviScrollRestorationVisited: true,
        });
    });

    it("is false again for a different entry (a fresh top-level navigation), even after another entry was marked", () => {
        isReloadOfAnAlreadyVisitedEntry(); // marks /pl/campaigns
        history.pushState(null, "", "/pl/emails/k1"); // a genuinely different, fresh entry

        const result = isReloadOfAnAlreadyVisitedEntry();

        expect(result).toBe(false);
    });

    it(
        "is false (never throws) when history.replaceState throws — mutation-detectable: " +
            "removing the try/catch makes this assertion throw instead of returning",
        () => {
            const replaceState = vi
                .spyOn(history, "replaceState")
                .mockImplementation(() => {
                    throw new DOMException(
                        "history-mutation limit exceeded",
                        "SecurityError",
                    );
                });

            const result = isReloadOfAnAlreadyVisitedEntry();

            expect(result).toBe(false);
            replaceState.mockRestore();
        },
    );
});

describe("scrollBehavior", () => {
    it("returns {left:0, top:0} for a push with no saved position (a fresh forward navigation)", async () => {
        const result = await scrollBehavior(fakeTo, fakeFrom, null);

        expect(result).toEqual({ left: 0, top: 0 });
    });

    it("resolves to the saved position unchanged, once the layout is stable (reload or back/forward)", async () => {
        stubCountingAnimationFrame();
        const saved = { left: 12, top: 34 };

        const result = await scrollBehavior(fakeTo, fakeFrom, saved);

        expect(result).toEqual(saved);
    });

    it("waits for the layout to settle before resolving a saved position on an overflowing page", async () => {
        const raf = stubCountingAnimationFrame();
        stubLayoutWidthSequence([562, 564, 736]);
        const saved = { left: 346, top: 0 };

        const result = await scrollBehavior(fakeTo, fakeFrom, saved);

        expect(result).toEqual(saved);
        expect(raf.calls()).toBe(14); // same 2-changes + 12-stable-frames shape as above
    });

    it("marks the current entry as visited even when savedPosition already resolves the navigation", async () => {
        expect(history.state).toBeNull();

        await scrollBehavior(fakeTo, fakeFrom, { left: 1, top: 1 });

        expect(history.state).toMatchObject({
            kivviScrollRestorationVisited: true,
        });
    });
});

// vue-router's own documented reload-scroll mechanism (`history.state.scroll`, populated by its
// `pagehide`-time `replaceState`) does not survive a reload in the Chromium build this pipeline
// runs (verified empirically against raw `history.state`, isolated from vue-router entirely —
// see scrollRestoration.ts's own comment above `saveScrollPositionForReload`), so `scrollBehavior`
// falls back to this sessionStorage save/restore whenever vue-router itself supplies no
// `savedPosition`.
describe("saveScrollPositionForReload / consumeScrollPositionForReload", () => {
    it("round-trips the current scroll position for the current href", () => {
        window.scrollX = 120;
        window.scrollY = 40;

        saveScrollPositionForReload();

        expect(consumeScrollPositionForReload(location.href)).toEqual({
            left: 120,
            top: 40,
        });
    });

    it("consumes (clears) the saved position — a second read returns null", () => {
        saveScrollPositionForReload();

        consumeScrollPositionForReload(location.href);

        expect(consumeScrollPositionForReload(location.href)).toBeNull();
    });

    it("ignores a saved position that belongs to a different href", () => {
        saveScrollPositionForReload();

        expect(
            consumeScrollPositionForReload(
                "http://localhost:3000/pl/emails/k2",
            ),
        ).toBeNull();
    });

    it("returns null when nothing was ever saved", () => {
        expect(consumeScrollPositionForReload(location.href)).toBeNull();
    });
});

describe("scrollBehavior falling back to the reload save when vue-router supplies no savedPosition", () => {
    it("restores a same-href reload position vue-router itself did not supply, on an actual reload", async () => {
        stubCountingAnimationFrame();
        isReloadOfAnAlreadyVisitedEntry(); // simulates the entry's first visit, before "reloading" it
        window.scrollX = 347;
        window.scrollY = 0;
        saveScrollPositionForReload(); // simulates this module's own pagehide listener firing

        const result = await scrollBehavior(fakeTo, fakeFrom, null);

        expect(result).toEqual({ left: 347, top: 0 });
    });

    it("still lands at {left:0, top:0} when the saved fallback belongs to a different href", async () => {
        window.scrollX = 347;
        saveScrollPositionForReload();
        history.pushState(null, "", "/pl/emails/k2"); // now on a different page

        const result = await scrollBehavior(fakeTo, fakeFrom, null);

        expect(result).toEqual({ left: 0, top: 0 });
    });

    it("never touches the fallback when vue-router already supplied a savedPosition (popstate)", async () => {
        stubCountingAnimationFrame();
        saveScrollPositionForReload(); // an unrelated fallback entry happens to be sitting here
        const saved = { left: 9, top: 9 };

        const result = await scrollBehavior(fakeTo, fakeFrom, saved);

        expect(result).toEqual(saved);
        // the fallback entry must still be there — scrollBehavior never consumed it, because it
        // never needed to (savedPosition already won)
        expect(consumeScrollPositionForReload(location.href)).not.toBeNull();
    });

    it(
        "lands at {left:0, top:0}, not the stale position, for a same-href navigation that is " +
            "NOT a reload (the first time this entry is seen — a fresh top-level nav happening " +
            "to match a torn-down href)",
        async () => {
            window.scrollX = 347;
            saveScrollPositionForReload();
            // no isReloadOfAnAlreadyVisitedEntry() pre-call here — this is the entry's first
            // visit (history.state starts `null`, per beforeEach), so scrollBehavior's own
            // internal check must see "not a reload".

            const result = await scrollBehavior(fakeTo, fakeFrom, null);

            expect(result).toEqual({ left: 0, top: 0 });
        },
    );

    it("still consumes (clears) the same-href fallback entry even when it is not applied", async () => {
        window.scrollX = 347;
        saveScrollPositionForReload();

        await scrollBehavior(fakeTo, fakeFrom, null); // first visit, not a reload

        expect(consumeScrollPositionForReload(location.href)).toBeNull();
    });
});
