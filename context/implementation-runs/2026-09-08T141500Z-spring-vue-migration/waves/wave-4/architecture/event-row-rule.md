# `event-row` rule — detailed proof (round 2)

rules-translated.md row "Row markup in one place": *"SPA: `EventRow.vue` is the only component
rendering `.event-row`; backend publishes JSON event, not HTML (see DEV-3)."* Tooling: ESLint
`vue/no-restricted-class` for `event-row` outside `EventRow.vue`.

By wave-4 this widened to three allowed files (`EventRow.vue`, `ListCard.vue`,
`RecentImports.vue`), each through its own file-scoped `frontend/eslint.config.js` override, and
repair-3 (this round) added a second layer: `no-restricted-syntax` selectors that catch the
literal `"event-row"` as a plain string or template-literal segment inside `<script>`, closing a
bypass an earlier `RecentImports.vue` draft used (`const recentImportRowClass = "event-row"` bound
through `:class`).

## (a) A literal `class="event-row"` outside the three allowed files fails lint

Probe `frontend/src/components/molecules/ZZZProbeEventRowLiteral.vue`:
```vue
<template>
  <div class="event-row">probe</div>
</template>
<script setup lang="ts"></script>
```
`npx eslint src/components/molecules/ZZZProbeEventRowLiteral.vue` →
```
2:14  error  'event-row' class is not allowed  vue/no-restricted-class
```
Exit 1. Probe deleted immediately after.

## (b) A script-side `const x = "event-row"` bound via `:class` outside the three allowed files fails lint

Probe `frontend/src/components/molecules/ZZZProbeEventRowScript.vue`:
```vue
<script setup lang="ts">
const probeRowClass = "event-row";
</script>
<template>
  <div :class="probeRowClass">probe</div>
</template>
```
`npx eslint src/components/molecules/ZZZProbeEventRowScript.vue` →
```
2:23  error  "event-row" is a restricted class (wave-2 "Row markup in one place"): a script-side
constant bound through :class is exactly the bypass repair-3 closed. Only EventRow.vue, ListCard.vue
and RecentImports.vue may render it, each through its own governed vue/no-restricted-class override
below — add a matching pair there instead of reaching for this string here  no-restricted-syntax
```
Exit 1. This is the hardened `no-restricted-syntax` selector
(`Literal[value=/event-row/]` / `TemplateElement[value.raw=/event-row/]`) added to the
`src/**/*.{ts,vue}` block in `frontend/eslint.config.js`, which `vue/no-restricted-class` alone
cannot see because that rule only inspects a template's own `class="..."` attribute, never a
script-side string that ends up bound to it. Probe deleted immediately after.

## (c) The three allowed files pass

`npx eslint src/components/molecules/EventRow.vue src/components/organisms/ListCard.vue
src/components/import/RecentImports.vue` → no output, exit 0.

Inspected each file's actual `.event-row` usage — all three render the literal directly in the
template (`class="event-row"`), none uses a script-side constant bound through `:class`:

```
src/components/organisms/ListCard.vue:20:            class="event-row"
src/components/import/RecentImports.vue:49:                class="event-row"
src/components/molecules/EventRow.vue:36:        class="event-row"
```

`RecentImports.vue`'s own header comment records that an earlier draft *did* use the
`const recentImportRowClass = "event-row"` / `:class` bypass and was fixed to the plain literal
during repair-3 — matching probe (b) exactly.

## (d) Each override is file-scoped and justified by an old-stack Twig partial using `class="event-row"`

`frontend/eslint.config.js` has three separate override blocks, one per allowed file
(`files: ["src/components/organisms/ListCard.vue"]`,
`files: ["src/components/import/RecentImports.vue"]`, and the base rule's
`ignores: ["src/components/molecules/EventRow.vue"]`) — each sets `"vue/no-restricted-class": "off"`
for exactly that one file, not for a directory or a glob covering more than the named component.
The `no-restricted-syntax` exemption block that follows is scoped to the same three explicit file
paths (an array of three literal paths, not a glob).

Old-stack Twig partials in the pre-migration checkout (`/home/muszkin/work/kivvi-click`, branch
`main`) confirmed to use the literal `class="event-row"`:

```
templates/pages/dashboard/recent-customers.html.twig:10:  <a class="event-row" href="...">
templates/pages/dashboard/top-automations.html.twig:10:  <a class="event-row" href="...">
templates/pages/import/recent-imports.html.twig:14:    <div class="event-row" style="...">
```

Mapping:

| SPA file (allowed override) | Justifying old-stack Twig partial(s) |
| --- | --- |
| `ListCard.vue` (dashboard organism, renders both "recent customers" and "top automations" list rows) | `templates/pages/dashboard/recent-customers.html.twig` and `templates/pages/dashboard/top-automations.html.twig` — both wrap each row in `class="event-row"` to reuse the compact grid styling |
| `RecentImports.vue` (import step 1 sidebar) | `templates/pages/import/recent-imports.html.twig:14` — wraps each row in `class="event-row"` for the same styling reuse |
| `EventRow.vue` (live event stream row) | the original, unwidened row of `rules-translated.md`; the canonical `.event-row` owner, ported from `components/molecules/event-row.html.twig` |

`ListCard.vue`'s and `RecentImports.vue`'s own header comments in the checkout independently state
the same justification ("Both old-stack partials wrap each row in a plain `<a class="event-row"
href="...">` — reusing…" / "recent-imports.html.twig:14 wraps each row in `class="event-row"` —
reusing the live event…"), matching the Twig evidence above verbatim.

**Conclusion:** the widened event-row allowance is file-scoped (three explicit files, not a
pattern), each file's exemption is independently justified by a real old-stack partial that used
the same class for the same styling-reuse reason, the literal-class bypass and the harder
script-side bypass both still fail lint everywhere else, and the three allowed files pass with 0
errors.
