# Journey: landing

A visitor can read the public landing page in Polish or English and enter the panel demo

Source tests: tests/e2e/specs/public.spec.ts (landing), tests/Controller/PanelPagesTest.php (landing)

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

1. `{"goto":"/pl"}`
2. `{"goto":"/en"}`
3. `{"goto":"/"}`
4. `{"goto":"/pl"}`
5. `{"click":".hero-cta a","hasText":"demo","waitUrl":"/pl/dashboard"}`
