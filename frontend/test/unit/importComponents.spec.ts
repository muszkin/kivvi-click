import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import Dropzone from "@/components/molecules/Dropzone.vue";
import Stepper from "@/components/molecules/Stepper.vue";

const STEPS = [
    { n: 1, label: "Plik" },
    { n: 2, label: "Mapowanie kolumn" },
    { n: 3, label: "Reguły i segmenty" },
    { n: 4, label: "Podgląd i start" },
];

describe("B31 Stepper marks the current step and the ones already passed", () => {
    it("step 1 (todo/todo/todo from step 1) marks nothing done, step 1 current", () => {
        const wrapper = mount(Stepper, {
            props: { steps: STEPS, current: 1, locale: "pl" },
        });

        const items = wrapper.findAll(".step");
        expect(items).toHaveLength(4);
        expect(items[0]?.attributes("data-state")).toBe("cur");
        expect(items[1]?.attributes("data-state")).toBe("todo");
        expect(items[3]?.attributes("data-state")).toBe("todo");
    });

    it("current step 3 marks steps 1-2 done, step 3 current, step 4 todo — matches the oracle's own step-6 assertion (2 done items after jumping to step 3)", () => {
        const wrapper = mount(Stepper, {
            props: { steps: STEPS, current: 3, locale: "pl" },
        });

        const items = wrapper.findAll(".step");
        expect(
            items.filter((i) => i.attributes("data-state") === "done"),
        ).toHaveLength(2);
        expect(items[2]?.attributes("data-state")).toBe("cur");
        expect(items[3]?.attributes("data-state")).toBe("todo");
    });

    it('every step carries data-action=set-import-step and its own number as data-payload, for the e2e .step[data-payload="3"] selector', () => {
        const wrapper = mount(Stepper, {
            props: { steps: STEPS, current: 1, locale: "pl" },
        });

        const items = wrapper.findAll(".step");
        items.forEach((item, index) => {
            expect(item.attributes("data-action")).toBe("set-import-step");
            expect(item.attributes("data-payload")).toBe(String(index + 1));
        });
    });

    it("a done step's number is replaced by a check icon, matching stepper.html.twig", () => {
        const wrapper = mount(Stepper, {
            props: { steps: STEPS, current: 2, locale: "pl" },
        });

        const items = wrapper.findAll(".step");
        expect(items[0]?.find(".step__num svg").exists()).toBe(true);
        expect(items[1]?.find(".step__num svg").exists()).toBe(false);
        expect(items[1]?.find(".step__num").text()).toBe("2");
    });
});

describe("B31 Dropzone posts the file and follows a redirected response with a full navigation, exactly like assets/controllers/upload.ts", () => {
    let originalLocation: Location;

    beforeEach(() => {
        // jsdom's own `window.location`/`location.href` property descriptors are
        // non-configurable (`Object.getOwnPropertyDescriptor(window, "location").configurable
        // === false`, confirmed empirically): neither `delete window.location` nor
        // `Object.defineProperty(window, "location", ...)` can replace them directly, and
        // assigning `window.location.href` for real triggers jsdom's own "Not implemented:
        // navigation to another Document" console.error, which the frontend's fail-on-warning
        // test policy (test/setup.ts) would turn into a false failure. `vi.stubGlobal` swaps the
        // whole `location` binding on `globalThis` instead of writing through the existing
        // accessor, so the component's `window.location.href = ...` assignment lands on this
        // plain, inspectable stub — proving the same navigation `import.spec.ts`'s e2e-level
        // `waitForURL` observes for real, without ever touching jsdom's real navigation code path.
        originalLocation = window.location;
        vi.stubGlobal("location", { href: "https://localhost/pl/import/1" });
    });

    afterEach(() => {
        vi.stubGlobal("location", originalLocation);
        vi.unstubAllGlobals();
    });

    it('follows fetch\'s "redirected" response with window.location.href = r.url — a real document navigation, not router.push (repair-1)', async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(
                async () =>
                    ({
                        redirected: true,
                        url: "https://localhost/pl/import/2",
                    }) as unknown as Response,
            ),
        );

        const wrapper = mount(Dropzone, {
            props: {
                title: "Przeciągnij plik tutaj lub kliknij aby wybrać",
                locale: "pl",
            },
        });

        const input = wrapper.find('input[type="file"]');
        const file = new File(
            ["email;imie\nhania.k@aurea.pl;Hania\n"],
            "klienci-e2e.csv",
            {
                type: "text/csv",
            },
        );
        Object.defineProperty(input.element, "files", { value: [file] });
        await input.trigger("change");
        await flushPromises();

        expect(fetch).toHaveBeenCalledTimes(1);
        const [url, init] = vi.mocked(fetch).mock.calls[0]!;
        expect(url).toBe("/import/upload");
        expect((init as RequestInit).method).toBe("POST");
        expect((init as RequestInit).body).toBeInstanceOf(FormData);
        expect(window.location.href).toBe("https://localhost/pl/import/2");
    });

    it("does not navigate when the upload response was not redirected", async () => {
        vi.stubGlobal(
            "fetch",
            vi.fn(
                async () =>
                    ({
                        redirected: false,
                        url: "https://localhost/import/upload",
                    }) as unknown as Response,
            ),
        );

        const wrapper = mount(Dropzone, {
            props: { title: "x", locale: "pl" },
        });

        const input = wrapper.find('input[type="file"]');
        Object.defineProperty(input.element, "files", {
            value: [new File(["x"], "a.csv", { type: "text/csv" })],
        });
        await input.trigger("change");
        await flushPromises();

        expect(window.location.href).toBe("https://localhost/pl/import/1");
    });

    // PIO-125: the upload route has no locale segment, so the page's own locale travels in the
    // body — otherwise the server can only redirect into the default one (English), and a Polish
    // user uploading from /pl/import/1 would be dropped into /en/import/2.
    for (const locale of ["pl", "en"]) {
        it(`sends the page's locale (${locale}) with the file, so the redirect returns to it`, async () => {
            vi.stubGlobal(
                "fetch",
                vi.fn(
                    async () =>
                        ({
                            redirected: false,
                            url: "https://localhost/import/upload",
                        }) as unknown as Response,
                ),
            );
            const wrapper = mount(Dropzone, { props: { title: "x", locale } });

            const input = wrapper.find('input[type="file"]');
            const file = new File(["x"], "a.csv", { type: "text/csv" });
            Object.defineProperty(input.element, "files", { value: [file] });
            await input.trigger("change");
            await flushPromises();

            const [, init] = vi.mocked(fetch).mock.calls[0]!;
            const body = (init as RequestInit).body as FormData;
            expect(body.get("locale")).toBe(locale);
            expect(body.get("file")).toBe(file);
        });
    }

    it("a drag over the dropzone sets data-dragging, cleared again on drag leave", async () => {
        const wrapper = mount(Dropzone, {
            props: { title: "x", locale: "pl" },
        });

        await wrapper.find("label.dropzone").trigger("dragenter");
        expect(wrapper.find("label.dropzone").attributes("data-dragging")).toBe(
            "true",
        );

        await wrapper.find("label.dropzone").trigger("dragleave");
        expect(
            wrapper.find("label.dropzone").attributes("data-dragging"),
        ).toBeUndefined();
    });
});
