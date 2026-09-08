# Slice packet w3-settings — wave-3, journey J7 "settings"

Read `../../common-journey-rules.md` first. **Worktree:** /home/muszkin/work/kivvi-click-wt/w3-settings · branch `migration/wave-3/settings` · parent SHA b87a701244f5316e1a53bace7a1e41facdffc277. **Lease:** compose project `kivvi-w-settings`, HTTP_PORT=19070, HTTPS_PORT=19071. **Model:** sonnet, effort HIGH.

**Capability:** The store owner can open each of the eight settings tabs at its own URL.
**Oracle:** journeys/settings/steps/1..10 (`/pl/settings/account`, then clicking each `.settings-nav a` for sites, team, providers, api, notifications, billing, gdpr; `/pl/settings` default tab; `/pl/settings/nonexistent` → 404). Behaviours: B06, B32, B01 (8 settings rows).
**Old-stack sources:** `src/Controller/SettingsController.php`, `src/Panel/Content/SettingsCatalog.php` (incl. `TRACKER_SNIPPET`, `DEFAULT_TAB`), `src/Twig/CodeHighlightExtension.php` (port to TS: same regexes, same `span.s/.k/.c` markup, HTML-escaping first), `templates/pages/settings.html.twig` + the 30 partials under `templates/pages/settings/`, organisms `settings-nav`, molecules `toggle-row`, `dns-row`, `hook-row`, `table` (from customers), `callout`, atoms `code-block`, `field`, `kbd`, `chip`, `bar`; `tests/e2e/specs/settings.spec.ts`.

## In scope
- Backend: `GET /api/v1/{locale}/settings/{tab}` → `{ tab, tabs, tabSubtitle, settings: {trackedSites, automaticEvents, team, roles, emailProviders, dnsRecords, apiKeys, webhooks, apiLimits, notificationMatrix, planUsage, invoices, dataSubjectRequests, retentionPolicies}, trackerSnippet }` (the Twig page receives the whole catalog object — serialize every method's output); unknown tab → 404 (API and document); `/{locale}/settings` = default tab `account`.
- Frontend: `SettingsView.vue` with one partial component per tab (30 partials → components under `components/settings/`), `SettingsNav` (`.settings-nav a[aria-current=true]`), `ToggleRow`, `DnsRow`, `HookRow`, `CodeBlock` (client-side port of `kivvi_highlight` producing identical HTML — unit-test against the oracle's step 2 a11y/texts and the snippet markup captured in the oracle html? (texts only; the `span.s/.c` presence is asserted by e2e), `Field` variants; tab URLs are real navigations.
- Tests: Vitest for the highlight port (same output as the PHP regexes for the tracker snippet), `SettingsNav` aria-current; JUnit for `isKnownTab`/404 and the payload; e2e `settings.spec.ts` (5 tests incl. 8 tabs).

## Out of scope
Any persistence of settings; forms that POST (none exist today).

## Deviations in scope
DEV-4, DEV-12 (step 10 is a 404 document).

## Specs to run
`cd tests/e2e && E2E_BASE_URL=https://localhost:19071 npx playwright test settings.spec.ts`

## Parallel-safety obligations
Shared wave with `automations` and `campaigns-email-editor`. Touch only: `backend/src/main/java/click/kivvi/{web/SettingsController.java,web/dto/Settings*.java,application/SettingsViewService.java,fixtures/SettingsFixtures.java,domain/CodeHighlight? (no — highlighting is client-side; keep the snippet raw in the payload)}`, `backend/src/test/**/settings*`, `frontend/src/views/SettingsView.vue`, `frontend/src/components/settings/**`, `frontend/src/components/**/{SettingsNav,ToggleRow,DnsRow,HookRow,CodeBlock}.vue`, `frontend/src/highlight.ts`, `frontend/src/composables/useIntents.ts` (add `copy-dns`, `copy-api-key`, `copy-snippet` only if missing), `frontend/src/i18n/messages/settings.*.ts`, `frontend/src/router/routes.ts` (your one line + import), `frontend/test/**/{settings,highlight}*`, your report/evidence.
