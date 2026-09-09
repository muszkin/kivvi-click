import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import FeedCard from "@/components/organisms/FeedCard.vue";

const SYNCED = {
    name: "aureashop.pl — Google Merchant",
    url: "https://aureashop.pl/feeds/google.xml",
    source: "google" as const,
    status: "synced" as const,
    products: "1 284",
    mapped: "1 284",
    mappedPercent: "100,0%",
    mismatched: 0,
    lastSync: "12 min temu",
    schedule: "co 6 godzin",
};

describe("FeedCard", () => {
    it("B30 renders the failing feed's HTTP 503 error strip", () => {
        const wrapper = mount(FeedCard, {
            props: {
                ...SYNCED,
                name: "mlot-narzedzia.pl — Google Merchant",
                status: "error",
                error: "HTTP 503 — Service Unavailable",
                products: "0",
                mapped: "0",
                mappedPercent: null,
                mismatched: 0,
            },
        });

        expect(wrapper.findAll(".feed-card__err")).toHaveLength(1);
        expect(wrapper.find(".feed-card__err").text()).toContain("HTTP 503");
        // No products means no rate to compute a percentage from — feed-card.html.twig's own
        // `{% if products %}` guard, reproduced here.
        expect(wrapper.find(".feed-stats").text()).not.toContain("%");
    });

    it("B30 a synced feed carries no error strip and shows the mapped percentage", () => {
        const wrapper = mount(FeedCard, { props: SYNCED });

        expect(wrapper.find(".feed-card__err").exists()).toBe(false);
        expect(wrapper.find(".feed-stats").text()).toContain("(100,0%)");
    });

    it("B30 a syncing feed shows a live info chip", () => {
        const wrapper = mount(FeedCard, {
            props: { ...SYNCED, status: "syncing" },
        });

        expect(wrapper.findAll(".chip.info .dot.live")).toHaveLength(1);
    });

    it("colours the mismatched count bad only when it is non-zero", () => {
        const clean = mount(FeedCard, { props: SYNCED });
        const dirty = mount(FeedCard, { props: { ...SYNCED, mismatched: 2 } });

        expect(
            clean.find(".feed-stat:nth-child(3) .mono").attributes("style"),
        ).toContain("var(--good)");
        expect(
            dirty.find(".feed-stat:nth-child(3) .mono").attributes("style"),
        ).toContain("var(--bad)");
    });
});
