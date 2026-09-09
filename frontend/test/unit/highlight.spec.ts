import { describe, expect, it } from "vitest";
import { highlight } from "@/highlight";

// The exact tracker snippet source SettingsFixtures.TRACKER_SNIPPET carries (see
// backend/src/main/java/click/kivvi/fixtures/SettingsFixtures.java and the old
// SettingsCatalog::TRACKER_SNIPPET it mirrors).
const SNIPPET = `<!-- Kivvi-click tracker · ~2KB, no deps -->
<script async
  src="https://cdn.kivvi-click.io/k.js"
  data-site="aureashop.pl"
  data-key="pk_live_8a4f2c..."></script>

// then anywhere in your shop code:
window.kvi('purchase', {
  order_id: 'AR-1287',
  value:     412.00,
  currency:  'PLN',
  items: [
    { sku: 'TEA-SENCHA-100', qty: 1, price: 38.90 },
    { sku: 'CUP-NORA',       qty: 2, price: 50.00 },
  ],
});`;

// Captured by running the real CodeHighlightExtension::highlight() (src/Twig/
// CodeHighlightExtension.php) against SNIPPET with `php -r`, byte-for-byte — this is the parity
// bar the port must clear (packet: "The kivvi_highlight port must produce identical HTML for the
// tracker snippet: same regexes, HTML-escape first, same span classes").
const EXPECTED = `&lt;!-- Kivvi-click tracker · ~2KB, no deps --&gt;
<span class="k">&lt;script</span> <span class="k">async</span>
  <span class="k">src</span>=<span class="s">&quot;https:<span class="c">//cdn.kivvi-click.io/k.js&quot;</span></span>
  <span class="k">data-site</span>=<span class="s">&quot;aureashop.pl&quot;</span>
  <span class="k">data-key</span>=<span class="s">&quot;pk_live_8a4f2c...&quot;</span>&gt;<span class="k">&lt;/script</span>&gt;

<span class="c">// then anywhere in your shop code:</span>
<span class="k">window</span>.kvi(&#039;purchase&#039;, {
  order_id: &#039;AR-1287&#039;,
  value:     412.00,
  currency:  &#039;PLN&#039;,
  items: [
    { sku: &#039;TEA-SENCHA-100&#039;, qty: 1, price: 38.90 },
    { sku: &#039;CUP-NORA&#039;,       qty: 2, price: 50.00 },
  ],
});`;

describe("B32 highlight() reproduces CodeHighlightExtension::highlight() byte-for-byte", () => {
    it("matches the PHP-rendered tracker snippet exactly", () => {
        expect(highlight(SNIPPET)).toBe(EXPECTED);
    });

    it("HTML-escapes before highlighting, so a raw '<' never survives", () => {
        expect(highlight("<div>")).toBe('<span class="k">&lt;div</span>&gt;');
    });

    it("wraps a double-quoted string in span.s", () => {
        expect(highlight('src="x"')).toContain(
            '<span class="s">&quot;x&quot;</span>',
        );
    });

    it(
        "never wraps a single-quoted string in span.s: ENT_QUOTES escapes ' to &#039; before " +
            "the STRING pattern runs, so the single-quote branch can never match in practice — " +
            "reproduced verbatim, not fixed",
        () => {
            const result = highlight("const x = 'hi';");
            expect(result).not.toContain('<span class="s">');
            expect(result).toContain("&#039;hi&#039;");
        },
    );

    it("wraps a line comment in span.c", () => {
        expect(highlight("// note\ncode")).toBe(
            '<span class="c">// note</span>\ncode',
        );
    });
});
