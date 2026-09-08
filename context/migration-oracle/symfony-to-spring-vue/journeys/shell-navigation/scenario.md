# Journey: shell-navigation

The store owner can move between every panel section and keep sidebar, theme and locale choices across reloads

Source tests: tests/e2e/specs/navigation.spec.ts, tests/Controller/PanelPagesTest.php, tests/Controller/PreferencesControllerTest.php

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

1. `{"goto":"/pl/dashboard"}`
2. `{"click":".nav-item[data-route=\"events\"]","waitUrl":"/pl/events"}`
3. `{"click":".nav-item[data-route=\"customers\"]","waitUrl":"/pl/customers"}`
4. `{"click":".nav-item[data-route=\"automations\"]","waitUrl":"/pl/automations"}`
5. `{"click":".nav-item[data-route=\"campaigns\"]","waitUrl":"/pl/campaigns"}`
6. `{"click":".nav-item[data-route=\"popups\"]","waitUrl":"/pl/popups"}`
7. `{"click":".nav-item[data-route=\"feeds\"]","waitUrl":"/pl/feeds"}`
8. `{"click":".nav-item[data-route=\"import\"]","waitUrl":"/pl/import"}`
9. `{"click":".nav-item[data-route=\"settings\"]","waitUrl":"/pl/settings"}`
10. `{"click":".nav-item[data-route=\"dashboard\"]","waitUrl":"/pl/dashboard"}`
11. `{"click":"[data-action=\"toggle-sidebar\"]","waitAttr":[".app","data-sidebar","collapsed"]}`
12. `{"reload":true,"waitAttr":[".app","data-sidebar","collapsed"]}`
13. `{"click":"[data-action=\"toggle-sidebar\"]","waitAttr":[".app","data-sidebar","expanded"]}`
14. `{"click":"[data-action=\"set-theme\"]","waitAttr":["html","data-theme","dark"]}`
15. `{"reload":true,"waitAttr":["html","data-theme","dark"]}`
16. `{"click":"[data-action=\"set-theme\"]","waitAttr":["html","data-theme","light"]}`
17. `{"goto":"/pl/customers"}`
18. `{"click":".tb-btn","hasText":"PL","waitUrl":"/en/customers"}`
19. `{"goto":"/en/dashboard"}`
20. `{"goto":"/pl/feeds"}`
21. `{"goto":"/de/dashboard","expectStatus":404}`
