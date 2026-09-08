# Journey: customers

The store owner can browse customers, open a 360 profile and return to the list

Source tests: tests/e2e/specs/customers.spec.ts, tests/Controller/PanelPagesTest.php (customers)

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

1. `{"goto":"/pl/customers"}`
2. `{"goto":"/pl/customers?page=2"}`
3. `{"goto":"/pl/customers"}`
4. `{"click":".table tbody tr","waitUrl":"/pl/customers/c_"}`
5. `{"click":".page-head a.btn.ghost","waitUrl":"/pl/customers"}`
6. `{"goto":"/pl/customers/c_9999","expectStatus":404}`
