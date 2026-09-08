# ESLint negative/positive probe — wave-1 row "SPA `format.ts` is the only importer of Intl.NumberFormat"

Temp copy outside the checkout: `<scratchpad>/eslint-negative-probe` (frontend/ copied, `node_modules`
symlinked back to the checkout's install — no tracked files touched).

## Negative probe — src/composables/probeViolation.ts using Intl.NumberFormat directly

```ts
export function probeFormat(value: number): string {
  return new Intl.NumberFormat("pl-PL").format(value);
}
```

`npm run lint` result: exit 1 (FAILS as required)
```
src/composables/probeViolation.ts
  2:14  error  Unexpected use of 'Intl'. numbers are formatted server-side (domain/Format.java); see rules-translated.md  no-restricted-globals
  2:14  error  numbers are formatted server-side (domain/Format.java); see rules-translated.md                            no-restricted-syntax
```

## Exception probe — same code moved to src/format.ts (the one permitted importer)

`npm run lint` result: exit 0 (0 errors; 2 pre-existing unrelated warnings in FeedCard.vue,
identical to the checkout's own clean `npm run lint` run — see frontend-lint.log)
