# Independent review — w2-customers candidate 0cb6baa (2026-09-08)

Verdict: **PASS**. Fixture formulas byte-exact vs CustomerDirectory; document route for `c_\d+` proven non-shadowing via CustomersApiIT; Topbar locale-link fix is a genuine parity correction (Twig `path($route,$routeParams)` drops the query string; no earlier oracle step affected); Card slot fix correct; DEV-12 customers:[6] additive and matches the plan. All gates green (105 unit, 25 IT, 57/31 frontend).
