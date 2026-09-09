import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import Pagination from "@/components/molecules/Pagination.vue";
import Table from "@/components/molecules/Table.vue";

interface Row {
    id: string;
    name: string;
}

describe("B25 Table", () => {
    const columns = [
        { label: "Klient" },
        { label: "Akcje", align: "right" as const },
    ];
    const rows: Row[] = [
        { id: "c_1000", name: "Anna K." },
        { id: "c_1001", name: "Kasia N." },
    ];

    it("renders one header cell per column, right-aligned where declared", () => {
        const wrapper = mount(Table, { props: { columns, rows: [] } });

        const headers = wrapper.findAll("thead th");
        expect(headers).toHaveLength(2);
        expect(headers[0]?.classes()).not.toContain("right");
        expect(headers[1]?.classes()).toContain("right");
    });

    it("renders one row per entry via the row slot, in order", () => {
        const wrapper = mount(Table, {
            props: { columns, rows },
            slots: {
                row: `<td>{{ params.row.name }}</td>`,
            },
        });

        const cells = wrapper.findAll("tbody tr td");
        expect(cells).toHaveLength(2);
        expect(cells[0]?.text()).toBe("Anna K.");
        expect(cells[1]?.text()).toBe("Kasia N.");
    });

    it("B25 carries the row intent as data-action/data-payload and a pointer cursor, index-matched to rowActions", () => {
        const wrapper = mount(Table, {
            props: {
                columns,
                rows,
                rowActions: [
                    { action: "go-customer", payload: "c_1000" },
                    undefined,
                ],
            },
            slots: { row: `<td>row</td>` },
        });

        const trs = wrapper.findAll("tbody tr");
        expect(trs[0]?.attributes("data-action")).toBe("go-customer");
        expect(trs[0]?.attributes("data-payload")).toBe("c_1000");
        expect(trs[0]?.attributes("style")).toContain("cursor: pointer");
        expect(trs[1]?.attributes("data-action")).toBeUndefined();
    });
});

describe("B25 Pagination", () => {
    it("disables the previous button on page 1 and enables next when pages remain", () => {
        const wrapper = mount(Pagination, { props: { page: 1, pages: 192 } });

        const buttons = wrapper.findAll("button[data-action='go-page']");
        expect(buttons).toHaveLength(2);
        expect(buttons[0]?.attributes("disabled")).toBeDefined();
        expect(buttons[1]?.attributes("disabled")).toBeUndefined();
        expect(wrapper.find(".mono.muted").text()).toBe("Strona 1 z 192");
    });

    it("disables the next button on the last page", () => {
        const wrapper = mount(Pagination, { props: { page: 192, pages: 192 } });

        const buttons = wrapper.findAll("button[data-action='go-page']");
        expect(buttons[0]?.attributes("disabled")).toBeUndefined();
        expect(buttons[1]?.attributes("disabled")).toBeDefined();
    });

    it("neither button is disabled on a middle page, and payloads are page ± 1", () => {
        const wrapper = mount(Pagination, { props: { page: 5, pages: 192 } });

        const buttons = wrapper.findAll("button[data-action='go-page']");
        expect(buttons[0]?.attributes("disabled")).toBeUndefined();
        expect(buttons[1]?.attributes("disabled")).toBeUndefined();
        expect(buttons[0]?.attributes("data-payload")).toBe("4");
        expect(buttons[1]?.attributes("data-payload")).toBe("6");
    });
});
