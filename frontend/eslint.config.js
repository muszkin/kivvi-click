import js from "@eslint/js";
import eslintPluginVue from "eslint-plugin-vue";
import globals from "globals";
import tseslint from "typescript-eslint";

export default tseslint.config(
    { ignores: ["dist/**", "coverage/**", "node_modules/**"] },
    js.configs.recommended,
    ...tseslint.configs.recommended,
    ...eslintPluginVue.configs["flat/recommended"],
    {
        languageOptions: {
            ecmaVersion: "latest",
            sourceType: "module",
            globals: { ...globals.browser, ...globals.node },
            parserOptions: {
                parser: tseslint.parser,
            },
        },
        rules: {
            // Formatting decisions belong to Panel\Format on the old stack, format.ts on this
            // one — mirrors architecture/rules-translated.md's wave-1 row (introduced early
            // since the rule is cheap to enforce from wave-0 on).
            "no-restricted-imports": [
                "error",
                {
                    patterns: [
                        {
                            group: ["*Intl*"],
                            message:
                                "Format numbers/money through src/format.ts, not Intl.NumberFormat directly.",
                        },
                    ],
                },
            ],
            // `no-restricted-imports` above only catches an `import` naming "Intl" — it cannot
            // see a bare reference to the global (`Intl.NumberFormat(...)`, no import needed).
            // These two rules close that gap: numbers are formatted server-side
            // (domain/Format.java), never client-side, per rules-translated.md's wave-1 row.
            "no-restricted-globals": [
                "error",
                {
                    name: "Intl",
                    message:
                        "numbers are formatted server-side (domain/Format.java); see rules-translated.md",
                },
            ],
            "no-restricted-syntax": [
                "error",
                {
                    selector: "MemberExpression[object.name='Intl']",
                    message:
                        "numbers are formatted server-side (domain/Format.java); see rules-translated.md",
                },
                {
                    selector:
                        "CallExpression[callee.type='MemberExpression'][callee.property.name='toLocaleString']",
                    message:
                        "numbers are formatted server-side (domain/Format.java); see rules-translated.md",
                },
                {
                    selector:
                        "CallExpression[callee.type='MemberExpression'][callee.property.name='toFixed']",
                    message:
                        "numbers are formatted server-side (domain/Format.java); see rules-translated.md",
                },
            ],
            "vue/multi-word-component-names": "off",
            "vue/require-default-prop": "off",
            // v-html is used only for developer-authored, fixed strings (i18n copy, the two
            // literal server-validated login error messages) — never for reflected user input.
            "vue/no-v-html": "off",
            // Formatting is Prettier's job (yarn format convention on the old stack, no ESLint
            // config file there either); eslint-plugin-vue's "recommended" preset bundles pure
            // layout rules (attribute wrapping, self-closing tags, indentation) that fight
            // Prettier's own opinions on the same markup.
            "vue/max-attributes-per-line": "off",
            "vue/html-self-closing": "off",
            "vue/singleline-html-element-content-newline": "off",
            "vue/html-closing-bracket-newline": "off",
            "vue/html-indent": "off",
            "vue/html-closing-bracket-spacing": "off",
            "vue/attributes-order": "off",
        },
    },
    {
        // The one module allowed to touch Intl directly — not created by this repair (test-only
        // change), kept ready for whichever slice introduces client-side formatting first.
        files: ["src/format.ts"],
        rules: {
            "no-restricted-imports": "off",
            "no-restricted-globals": "off",
            "no-restricted-syntax": "off",
        },
    },
    {
        // wave-2 architecture rule ("Mercure topic built only server-side",
        // rules-translated.md): the SPA never composes a topic string itself — EventsView
        // fetches it from the API payload (`mercureTopic`) and EventStream just forwards
        // whatever it was given as a prop, so a literal containing the account-topic prefix
        // anywhere in production `src/` code (a plain string or a template-literal segment) is
        // always a regression, never a legitimate use. Scoped to `src/` only (not `test/**`):
        // fixture data in a test asserting the SPA renders whatever topic it's handed needs the
        // realistic value, and that is not "composing a topic" in the sense this rule guards
        // against — `no-restricted-syntax`'s value replaces rather than merges across cascading
        // config blocks, so the base rules block's Intl selectors are repeated here to stay
        // active for `src/**` too (this block would otherwise silently drop them there).
        //
        // Repair-3: also hardens wave-2's "Row markup in one place" rule (see the
        // `vue/no-restricted-class` block below) against the exact bypass
        // `RecentImports.vue` attempted — `const recentImportRowClass = "event-row"` bound
        // through `:class`, which `vue/no-restricted-class` cannot see because it only
        // inspects the template's own `class="..."` attribute, never a script-side string
        // that ends up bound to it. These two selectors catch "event-row" as a plain string
        // literal or a template-literal segment anywhere in a file's `<script>`, so a future
        // `:class` constant anywhere in `src/` fails lint for the string itself, not only for
        // the rendered attribute. The one narrower block after `vue/no-restricted-class`'s own
        // override pair (further down this file) re-declares this same Intl/Mercure-topic set
        // *without* these two selectors, for the three files that are allowed to render
        // `.event-row` — `no-restricted-syntax`'s array-replaces-not-merges behaviour (this
        // comment's own point above) makes that the only way to lift just this one restriction
        // there without also losing Intl/Mercure-topic coverage for those files.
        files: ["src/**/*.{ts,vue}"],
        ignores: ["src/format.ts"],
        rules: {
            "no-restricted-syntax": [
                "error",
                {
                    selector: "MemberExpression[object.name='Intl']",
                    message:
                        "numbers are formatted server-side (domain/Format.java); see rules-translated.md",
                },
                {
                    selector:
                        "CallExpression[callee.type='MemberExpression'][callee.property.name='toLocaleString']",
                    message:
                        "numbers are formatted server-side (domain/Format.java); see rules-translated.md",
                },
                {
                    selector:
                        "CallExpression[callee.type='MemberExpression'][callee.property.name='toFixed']",
                    message:
                        "numbers are formatted server-side (domain/Format.java); see rules-translated.md",
                },
                {
                    selector: "Literal[value=/\\/accounts\\//]",
                    message:
                        "Mercure topics are built server-side only (domain/tracking/EventStreamTopic.java); read the topic from the API payload instead of composing it here.",
                },
                {
                    selector: "TemplateElement[value.raw=/\\/accounts\\//]",
                    message:
                        "Mercure topics are built server-side only (domain/tracking/EventStreamTopic.java); read the topic from the API payload instead of composing it here.",
                },
                {
                    selector: "Literal[value=/event-row/]",
                    message:
                        '"event-row" is a restricted class (wave-2 "Row markup in one place"): a script-side constant bound through :class is exactly the bypass repair-3 closed. Only EventRow.vue, ListCard.vue and RecentImports.vue may render it, each through its own governed vue/no-restricted-class override below — add a matching pair there instead of reaching for this string here.',
                },
                {
                    selector: "TemplateElement[value.raw=/event-row/]",
                    message:
                        '"event-row" is a restricted class (wave-2 "Row markup in one place"): a script-side constant bound through :class is exactly the bypass repair-3 closed. Only EventRow.vue, ListCard.vue and RecentImports.vue may render it, each through its own governed vue/no-restricted-class override below — add a matching pair there instead of reaching for this string here.',
                },
            ],
        },
    },
    {
        files: ["test/**/*.ts"],
        languageOptions: {
            globals: { ...globals.browser, ...globals.node, ...globals.es2021 },
        },
    },
    {
        // wave-2 architecture rule ("Row markup in one place", rules-translated.md):
        // EventRow.vue is the only component that may render `.event-row` — every event row on
        // the page (server-rendered or Mercure-arrived) goes through that one template, never a
        // second copy drifting out of sync with it.
        files: ["src/**/*.vue"],
        ignores: ["src/components/molecules/EventRow.vue"],
        rules: {
            "vue/no-restricted-class": ["error", "event-row"],
        },
    },
    {
        // wave-4 dashboard journey: ListCard.vue is the one other component allowed to render
        // `.event-row`. The dashboard's recent-customers and top-automations partials
        // (pages/dashboard/{recent-customers,top-automations}.html.twig on the old stack) wrap
        // each row in `class="event-row"` purely to reuse its compact grid styling — no time/type
        // icon/detail columns, no Mercure "new" flash-in class, nothing that overlaps the live
        // event stream's own row semantics the rule above guards. See ListCard.vue's own comment.
        files: ["src/components/organisms/ListCard.vue"],
        rules: {
            "vue/no-restricted-class": "off",
        },
    },
    {
        // wave-4 import-wizard journey (repair-3): RecentImports.vue is the third component
        // allowed to render `.event-row` — pages/import/recent-imports.html.twig:14 does the
        // same styling reuse as the dashboard's ListCard.vue partials above (see
        // RecentImports.vue's own comment). An earlier version of this component bypassed the
        // rule instead of adding this governed override: a `const recentImportRowClass =
        // "event-row"` bound through `:class`. Fixed to use the literal, exactly like the Twig
        // partial and like ListCard.vue already does.
        files: ["src/components/import/RecentImports.vue"],
        rules: {
            "vue/no-restricted-class": "off",
        },
    },
    {
        // Repair-3: the three files the two `vue/no-restricted-class` overrides above allow to
        // render `.event-row` literally are also exempted from the `no-restricted-syntax`
        // "event-row" string hardening added to the `src/**/*.{ts,vue}` block earlier in this
        // file — re-declaring that block's Intl/Mercure-topic selectors here, unchanged, minus
        // the two event-row ones (see that block's own comment for why a later, narrower block
        // is the only way to lift just one restriction without losing the others). None of the
        // three files' own <script> blocks currently contain the string "event-row" at all —
        // every occurrence in each is either a template `class="event-row"` attribute (a
        // separate AST `no-restricted-syntax` never traverses) or a comment — so this block is
        // presently a no-op in practice; it exists so the exemption is explicit and discoverable
        // rather than incidental, and so a future edit to one of these three files' scripts
        // cannot be blocked by the very restriction their template-level override already
        // authorizes.
        files: [
            "src/components/molecules/EventRow.vue",
            "src/components/organisms/ListCard.vue",
            "src/components/import/RecentImports.vue",
        ],
        rules: {
            "no-restricted-syntax": [
                "error",
                {
                    selector: "MemberExpression[object.name='Intl']",
                    message:
                        "numbers are formatted server-side (domain/Format.java); see rules-translated.md",
                },
                {
                    selector:
                        "CallExpression[callee.type='MemberExpression'][callee.property.name='toLocaleString']",
                    message:
                        "numbers are formatted server-side (domain/Format.java); see rules-translated.md",
                },
                {
                    selector:
                        "CallExpression[callee.type='MemberExpression'][callee.property.name='toFixed']",
                    message:
                        "numbers are formatted server-side (domain/Format.java); see rules-translated.md",
                },
                {
                    selector: "Literal[value=/\\/accounts\\//]",
                    message:
                        "Mercure topics are built server-side only (domain/tracking/EventStreamTopic.java); read the topic from the API payload instead of composing it here.",
                },
                {
                    selector: "TemplateElement[value.raw=/\\/accounts\\//]",
                    message:
                        "Mercure topics are built server-side only (domain/tracking/EventStreamTopic.java); read the topic from the API payload instead of composing it here.",
                },
            ],
        },
    },
);
