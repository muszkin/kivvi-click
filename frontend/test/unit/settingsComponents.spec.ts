import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import CodeBlock from "@/components/atoms/CodeBlock.vue";
import ToggleRow from "@/components/atoms/ToggleRow.vue";
import DnsRow from "@/components/molecules/DnsRow.vue";
import HookRow from "@/components/molecules/HookRow.vue";
import SettingsNav from "@/components/organisms/SettingsNav.vue";

describe("B32 SettingsNav", () => {
    const tabs = [
        {
            id: "account",
            icon: "user",
            label: "Konto",
            href: "/pl/settings",
            active: true,
        },
        {
            id: "sites",
            icon: "globe",
            label: "Śledzone strony",
            href: "/pl/settings/sites",
            active: false,
        },
    ];

    it("marks the active tab aria-current=true and every other tab aria-current=false", () => {
        const wrapper = mount(SettingsNav, { props: { tabs } });

        const links = wrapper.findAll(".settings-nav a");
        expect(links).toHaveLength(2);
        expect(links[0]?.attributes("aria-current")).toBe("true");
        expect(links[1]?.attributes("aria-current")).toBe("false");
    });

    it("renders a real navigation link (href), not a router-link", () => {
        const wrapper = mount(SettingsNav, { props: { tabs } });

        expect(wrapper.findAll(".settings-nav a")[1]?.attributes("href")).toBe(
            "/pl/settings/sites",
        );
    });

    it("carries the tab label as text", () => {
        const wrapper = mount(SettingsNav, { props: { tabs } });

        expect(wrapper.text()).toContain("Śledzone strony");
    });
});

describe("B32 ToggleRow", () => {
    it("renders a checked checkbox with its label", () => {
        const wrapper = mount(ToggleRow, {
            props: { name: "require_mfa", label: "Wymagaj 2FA", checked: true },
        });

        const input = wrapper.find("input");
        expect(input.attributes("type")).toBe("checkbox");
        expect(input.attributes("name")).toBe("require_mfa");
        expect((input.element as HTMLInputElement).checked).toBe(true);
        expect(wrapper.text()).toBe("Wymagaj 2FA");
    });

    it("renders the label with v-html, so an embedded span survives as markup", () => {
        const wrapper = mount(ToggleRow, {
            props: {
                name: "invoice_recipient",
                label: 'Wysyłaj faktury na <span class="mono">ksiegowosc@aureashop.pl</span>',
            },
        });

        expect(wrapper.find("span.mono").exists()).toBe(true);
        expect(wrapper.find("span.mono").text()).toBe(
            "ksiegowosc@aureashop.pl",
        );
    });
});

describe("B32 DnsRow", () => {
    it("renders a good chip for a verified record and a warn chip for an unverified one", () => {
        const good = mount(DnsRow, {
            props: { record: "SPF", value: "v=spf1 …", ok: true },
        });
        const warn = mount(DnsRow, {
            props: { record: "BIMI", value: "nie skonfigurowane", ok: false },
        });

        expect(good.find(".chip.good").exists()).toBe(true);
        expect(warn.find(".chip.warn").exists()).toBe(true);
        expect(good.find(".dns-row").text()).toContain("v=spf1 …");
    });
});

describe("B32 HookRow", () => {
    it("renders a good chip for a 2xx status and a bad chip for a non-2xx one", () => {
        const ok = mount(HookRow, {
            props: {
                url: "https://example.com/hooks",
                events: ["purchase"],
                code: 200,
            },
        });
        const failing = mount(HookRow, {
            props: {
                url: "https://hooks.slack.com/services/T0…",
                events: ["automation.failed"],
                code: 410,
            },
        });

        expect(ok.find(".chip.good").text()).toBe("200");
        expect(failing.find(".chip.bad").text()).toBe("410");
        expect(failing.find(".hook-row").text()).toContain("automation.failed");
    });
});

describe("B32 CodeBlock", () => {
    it("renders the highlighted markup and the data-lang attribute", () => {
        const wrapper = mount(CodeBlock, {
            props: { code: 'src="x"', lang: "html" },
        });

        expect(wrapper.attributes("data-lang")).toBe("html");
        expect(wrapper.find("span.k").exists()).toBe(true);
        expect(wrapper.find("span.s").exists()).toBe(true);
    });

    it("omits data-lang when no lang is given", () => {
        const wrapper = mount(CodeBlock, { props: { code: "x" } });

        expect(wrapper.attributes("data-lang")).toBeUndefined();
    });
});
