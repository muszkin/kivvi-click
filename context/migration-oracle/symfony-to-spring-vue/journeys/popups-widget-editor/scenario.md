# Journey: popups-widget-editor

The store owner can preview on-site widgets and compose one for desktop or mobile

Source tests: tests/e2e/specs/lists.spec.ts (widgets), tests/e2e/specs/editors.spec.ts (popup editor)

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

1. `{"goto":"/pl/popups"}`
2. `{"click":".auto-card","hasText":"Pasek darmowej dostawy","waitUrl":"preview=p2"}`
3. `{"goto":"/pl/popups/p1"}`
4. `{"click":".pw-type","hasText":"Pasek","waitUrl":"type=banner"}`
5. `{"goto":"/pl/popups/p1"}`
6. `{"click":".seg [data-action=\"navigate\"]","hasText":"Mobile","waitUrl":"device=mobile"}`
7. `{"goto":"/pl/popups/new"}`
