# Journey: import-wizard

The store owner can walk the four-step customer import and upload a file that advances the wizard

Source tests: tests/e2e/specs/import.spec.ts, tests/Controller/PanelPagesTest.php (import)

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

1. `{"goto":"/pl/import/1"}`
2. `{"goto":"/pl/import/2"}`
3. `{"goto":"/pl/import/3"}`
4. `{"goto":"/pl/import/4"}`
5. `{"goto":"/pl/import/1"}`
6. `{"click":".step[data-payload=\"3\"]","waitUrl":"/pl/import/3"}`
7. `{"click":"a.btn","hasText":"← Wstecz","waitUrl":"/pl/import/2"}`
8. `{"goto":"/pl/import/1"}`
9. `{"setFile":".dropzone input[type=\"file\"]","name":"klienci-oracle.csv","content":"email;imie\nhania.k@aurea.pl;Hania\n","waitUrl":"/pl/import/2"}`
10. `{"goto":"/pl/import/5","expectStatus":404}`
