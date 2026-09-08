# Journey: automations

The store owner can review automation rules and edit one as a list or a diagram with its simulation

Source tests: tests/e2e/specs/automations.spec.ts, tests/Controller/PanelPagesTest.php (automations)

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

1. `{"goto":"/pl/automations"}`
2. `{"goto":"/pl/automations?status=active"}`
3. `{"goto":"/pl/automations"}`
4. `{"click":".auto-card","waitUrl":"/pl/automations/a1"}`
5. `{"click":".seg [data-action=\"navigate\"]","hasText":"Diagram","waitUrl":"view=flow"}`
6. `{"click":".seg [data-action=\"navigate\"]","hasText":"Lista","waitUrl":"view=list"}`
7. `{"goto":"/pl/automations/new"}`
