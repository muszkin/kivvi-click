# Journey: settings

The store owner can open each of the eight settings tabs at its own URL

Source tests: tests/e2e/specs/settings.spec.ts, tests/Controller/PanelPagesTest.php (settings)

Run id placeholder: `__RUN__` (per-run value normalized to `<RUN-ID>`).

## Steps

1. `{"goto":"/pl/settings/account"}`
2. `{"click":".settings-nav a","hasText":"Śledzone strony","waitUrl":"/pl/settings/sites"}`
3. `{"click":".settings-nav a","hasText":"Zespół","waitUrl":"/pl/settings/team"}`
4. `{"click":".settings-nav a","hasText":"Dostawcy email","waitUrl":"/pl/settings/providers"}`
5. `{"click":".settings-nav a","hasText":"Webhooks i API","waitUrl":"/pl/settings/api"}`
6. `{"click":".settings-nav a","hasText":"Powiadomienia","waitUrl":"/pl/settings/notifications"}`
7. `{"click":".settings-nav a","hasText":"Plan i płatności","waitUrl":"/pl/settings/billing"}`
8. `{"click":".settings-nav a","hasText":"RODO / DPA","waitUrl":"/pl/settings/gdpr"}`
9. `{"goto":"/pl/settings"}`
10. `{"goto":"/pl/settings/nonexistent","expectStatus":404}`
