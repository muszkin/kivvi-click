import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import WorkspaceCard from "@/components/molecules/WorkspaceCard.vue";

describe("WorkspaceCard", () => {
    it(
        "keeps a space between the name and the meta text (regression: Vue's whitespace " +
            "condense mode strips newline-only whitespace between adjacent inline spans, which " +
            "otherwise silently concatenates them — the oracle's own text was 'aureashop.pl " +
            "Plan Pro · 3 strony'; PIO-123 dropped the plan name, the whitespace guarantee is " +
            "unchanged)",
        () => {
            const wrapper = mount(WorkspaceCard, {
                props: {
                    name: "aureashop.pl",
                    meta: "3 strony",
                    mark: "AS",
                },
            });

            expect(wrapper.text()).toContain("aureashop.pl 3 strony");
        },
    );
});
