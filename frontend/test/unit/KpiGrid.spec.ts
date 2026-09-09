import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import KpiGrid, { type KpiTileData } from "@/components/organisms/KpiGrid.vue";

const TILES: KpiTileData[] = [
    {
        label: "Aktywne feedy",
        value: "3",
        unit: "/4",
        delta: "1 z błędem",
        dir: "down",
    },
    {
        label: "Produktów w katalogu",
        value: "2 648",
        delta: "+42 w tym tyg.",
        dir: "up",
    },
    {
        label: "Dopasowanie zdarzeń → produkty",
        value: "94,8",
        unit: "%",
        delta: "ostatnia doba",
        dir: "up",
    },
    {
        label: "Wartość koszyków (24h)",
        value: "382 140 zł",
        delta: "z dopasowanymi cenami",
        dir: "up",
    },
];

describe("KpiGrid", () => {
    it("renders one .kpi tile per entry, in order", () => {
        const wrapper = mount(KpiGrid, { props: { tiles: TILES } });

        const tiles = wrapper.findAll(".kpi");
        expect(tiles).toHaveLength(4);
        expect(tiles[0]?.find(".kpi-value").text()).toBe("3/4");
        expect(tiles[1]?.find(".kpi-value").text()).toBe("2 648");
    });

    it("renders no tiles for an empty array", () => {
        const wrapper = mount(KpiGrid, { props: { tiles: [] } });

        expect(wrapper.findAll(".kpi")).toHaveLength(0);
    });
});
