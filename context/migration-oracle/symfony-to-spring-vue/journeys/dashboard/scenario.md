# Journey: dashboard

The store owner can see the operational overview and jump from it into a customer profile or a rule

Source tests: tests/e2e/specs/dashboard.spec.ts, tests/Controller/PanelPagesTest.php (dashboard)

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

1. `{"goto":"/pl/dashboard"}`
2. `{"click":"a.event-row[href*=\"/customers/c_\"]","waitUrl":"/pl/customers/c_"}`
3. `{"back":true,"waitUrl":"/pl/dashboard"}`
4. `{"click":"a.event-row[href*=\"/automations/a\"]","waitUrl":"/pl/automations/a"}`
