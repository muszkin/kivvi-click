// Client-side port of src/Twig/CodeHighlightExtension.php's `kivvi_highlight` Twig filter.
// Highlighting moved from the server (a Twig filter applied once at render time) to the browser
// (the code-block atom now receives the raw snippet and highlights it itself) — see the settings
// journey packet: "The kivvi_highlight port must produce identical HTML for the tracker snippet
// (same regexes, HTML-escape first, same span classes)".
//
// The three regexes and their application order are copied verbatim, including the old
// implementation's own quirk: HTML-escaping runs first (matching PHP's
// `htmlspecialchars($code, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8')`), which turns every `'` into
// `&#039;` — so the STRING pattern's `'[^']*'` branch can only ever match a double-quoted
// (`&quot;…&quot;`) span in practice, never a single-quoted one. That is not a bug this port
// fixes: it is exactly what the oracle's rendered markup shows (single-quoted JS string literals
// in the tracker snippet are never wrapped in `<span class="s">`), so it is reproduced as-is.
const STRING_PATTERN = /(&quot;[^&]*?&quot;|'[^']*')/g;
const KEYWORD_PATTERN =
    /(&lt;\/?[a-z]+|\bsrc\b|\basync\b|data-[a-z-]+|\bwindow\b|\bconst\b|\bfunction\b)/g;
const COMMENT_PATTERN = /(\/\/[^\n]*)/g;

const HTML_ESCAPES: Record<string, string> = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;",
};

/**
 * Escapes `code` exactly like PHP's `htmlspecialchars($code, ENT_QUOTES | ENT_SUBSTITUTE,
 * 'UTF-8')` (a single pass, so no character is ever escaped twice).
 */
function escapeHtml(code: string): string {
    return code.replace(/[&<>"']/g, (char) => HTML_ESCAPES[char] ?? char);
}

/**
 * Ported from `CodeHighlightExtension::highlight()`. Escapes first, then wraps strings, then
 * keywords, then line comments — in that exact order, since a later pass can (and, for the
 * tracker snippet's `https://`, does) match inside a span an earlier pass already opened.
 */
export function highlight(code: string): string {
    const escaped = escapeHtml(code);
    const withStrings = escaped.replace(
        STRING_PATTERN,
        '<span class="s">$1</span>',
    );
    const withKeywords = withStrings.replace(
        KEYWORD_PATTERN,
        '<span class="k">$1</span>',
    );
    return withKeywords.replace(COMMENT_PATTERN, '<span class="c">$1</span>');
}
