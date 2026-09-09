import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import EmailDocument, {
    type DocumentSection,
} from "@/components/organisms/EmailDocument.vue";

// 04-patterns.css carries `.ee-doc { background: white; ... }` as a plain, non-themed rule (see
// EmailDocument.vue's class comment) — read straight off disk and injected into jsdom's own
// <style> tag rather than relying on Vite's CSS pipeline (Vitest does not apply component
// <style> blocks or plain CSS imports to the DOM by default), so this test exercises the actual
// cascade rather than only the component's own markup.
const PATTERNS_CSS = readFileSync(
    resolve(process.cwd(), "src/styles/04-patterns.css"),
    "utf-8",
);

const SECTIONS: DocumentSection[] = [
    { type: "hero", kicker: "AUREASHOP", title: "Hania, Twój koszyk czeka." },
];

describe("B28 EmailDocument keeps a literal white background regardless of theme", () => {
    let styleEl: HTMLStyleElement;

    beforeEach(() => {
        styleEl = document.createElement("style");
        styleEl.textContent = PATTERNS_CSS;
        document.head.appendChild(styleEl);
    });

    afterEach(() => {
        styleEl.remove();
        document.documentElement.removeAttribute("data-theme");
    });

    it("computes rgb(255, 255, 255) for .ee-doc under data-theme=dark", () => {
        document.documentElement.dataset.theme = "dark";
        const wrapper = mount(EmailDocument, {
            props: { sections: SECTIONS },
            attachTo: document.body,
        });

        const background = getComputedStyle(
            wrapper.find(".ee-doc").element,
        ).backgroundColor;

        expect(background).toBe("rgb(255, 255, 255)");
        wrapper.unmount();
    });

    it("computes the same rgb(255, 255, 255) under data-theme=light (never theme-dependent)", () => {
        document.documentElement.dataset.theme = "light";
        const wrapper = mount(EmailDocument, {
            props: { sections: SECTIONS },
            attachTo: document.body,
        });

        const background = getComputedStyle(
            wrapper.find(".ee-doc").element,
        ).backgroundColor;

        expect(background).toBe("rgb(255, 255, 255)");
        wrapper.unmount();
    });
});
