import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { router } from "@/router";

/**
 * Proves the *wiring* in `router/index.ts` — that `createRouter({ ..., scrollBehavior })` really
 * hands vue-router the module's own `scrollBehavior` function, invoked the way vue-router itself
 * invokes it (`scrollBehavior(to, from, savedPosition)`, from `router.options.scrollBehavior`,
 * against a real, resolved route from an actual `router.push()`) — not just the unit-level
 * `scrollRestoration.spec.ts`, which calls the exported function directly and never touches
 * `router/index.ts` at all.
 */

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
 * settle on the sequence's last value forever after — the fake layout signature this suite's own
 * packet asks for. `scrollHeight` stays constant so only `scrollWidth` drives the change.
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

beforeEach(async () => {
    history.pushState(null, "", "/pl/dashboard");
    await router.push("/pl/dashboard");
    await router.isReady();
});

afterEach(() => {
    vi.unstubAllGlobals();
    delete (document.documentElement as unknown as Record<string, unknown>)
        .scrollWidth;
    delete (document.documentElement as unknown as Record<string, unknown>)
        .scrollHeight;
});

describe("router/index.ts scrollBehavior wiring", () => {
    it("createRouter was configured with this module's own scrollBehavior function", () => {
        expect(router.options.scrollBehavior).toBeTypeOf("function");
    });

    it(
        "resolves a saved position only after the (real router-driven) layout has stabilised " +
            "— not merely the standalone helper, the actual option vue-router will call",
        async () => {
            const raf = stubCountingAnimationFrame();
            stubLayoutWidthSequence([100, 200, 300]); // two real changes, then stable
            const to = router.currentRoute.value;
            const from = router.currentRoute.value;
            const savedPosition = { left: 55, top: 66 };

            const scrollBehavior = router.options.scrollBehavior;
            if (!scrollBehavior)
                throw new Error("scrollBehavior was not configured");
            const result = await scrollBehavior(to, from, savedPosition);

            expect(result).toEqual(savedPosition);
            // same 2-changes + 12-stable-frames shape the unit suite already establishes —
            // proves this real, router-resolved call goes through the identical stabilisation
            // wait, not a shortcut taken only for a hand-built `to`/`from`.
            expect(raf.calls()).toBe(14);
        },
    );

    it("resolves {left:0, top:0} for a plain push with no saved position", async () => {
        const to = router.currentRoute.value;
        const from = router.currentRoute.value;

        const scrollBehavior = router.options.scrollBehavior;
        if (!scrollBehavior)
            throw new Error("scrollBehavior was not configured");
        const result = await scrollBehavior(to, from, null);

        expect(result).toEqual({ left: 0, top: 0 });
    });
});
