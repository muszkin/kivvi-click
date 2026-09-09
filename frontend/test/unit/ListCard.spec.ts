import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import ListCard from "@/components/organisms/ListCard.vue";

interface Row {
    href: string;
    columns: string;
    name: string;
}

const ROWS: Row[] = [
    {
        href: "/pl/customers/c_1000",
        columns: "auto 1fr auto auto",
        name: "Anna K.",
    },
    {
        href: "/pl/customers/c_1001",
        columns: "auto 1fr auto auto",
        name: "Kasia N.",
    },
];

describe("ListCard", () => {
    it("renders one real <a class=event-row href> link per row, in order", () => {
        const wrapper = mount(ListCard, {
            props: { rows: ROWS },
            slots: {
                default: `<template #default="{ row }">{{ row.name }}</template>`,
            },
        });

        const links = wrapper.findAll("a.event-row");
        expect(links).toHaveLength(2);
        expect(links[0]?.attributes("href")).toBe("/pl/customers/c_1000");
        expect(links[0]?.element.tagName).toBe("A");
        expect(links[1]?.attributes("href")).toBe("/pl/customers/c_1001");
    });

    it("passes each row's own grid-template-columns through as an inline style", () => {
        const wrapper = mount(ListCard, {
            props: {
                rows: [
                    {
                        href: "/pl/automations/a1",
                        columns: "1fr auto auto auto",
                        name: "x",
                    },
                ],
            },
            slots: {
                default: `<template #default="{ row }">{{ row.name }}</template>`,
            },
        });

        const link = wrapper.get("a.event-row");
        expect(link.attributes("style")).toContain(
            "grid-template-columns: 1fr auto auto auto",
        );
        expect(link.attributes("style")).toContain("text-decoration: none");
    });

    it("renders the caller's slot content for each row, scoped to that row", () => {
        const wrapper = mount(ListCard, {
            props: { rows: ROWS },
            slots: {
                default: `<template #default="{ row }">{{ row.name }}</template>`,
            },
        });

        const links = wrapper.findAll("a.event-row");
        expect(links[0]?.text()).toBe("Anna K.");
        expect(links[1]?.text()).toBe("Kasia N.");
    });

    it("renders nothing when there are no rows", () => {
        const wrapper = mount(ListCard, {
            props: { rows: [] },
            slots: {
                default: `<template #default="{ row }">{{ row.name }}</template>`,
            },
        });

        expect(wrapper.findAll("a.event-row")).toHaveLength(0);
    });
});
