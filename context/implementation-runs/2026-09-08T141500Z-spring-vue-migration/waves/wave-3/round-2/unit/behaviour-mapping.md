# Wave-3 unit — behaviour → named test mapping (round 2, independent)

Source: oracle `behaviours.json` filtered to journeys `automations`, `settings`,
`campaigns-email-editor` (directly or via `also`). Only `*Test.java` (Surefire) and
`frontend/test/unit/*.spec.ts` files count for the unit dimension; `*IT.java` and
`frontend/test/integration/*` do not (they belong to the integration dimension).

| Behaviour | Journey | Named unit test(s) | Result |
| --- | --- | --- | --- |
| B01 | shell-navigation (also: automations, settings, campaigns-email-editor, …) | `click.kivvi.domain.RouteTableTest` — "B01 every panel/public URL in the route table is known"; `click.kivvi.web.SpaDocumentControllerTest::everyKnownRouteRenders200` — "B01 every panel/public URL renders 200 and carries the SPA document" (parametrized over `/pl/automations`, `/pl/automations/new`, `/pl/automations/a1`, `/pl/campaigns`, `/pl/emails/new`, `/pl/emails/k1`, `/pl/settings`, plus other journeys' routes); `frontend/test/unit/routes.spec.ts` — "B01 every panel/public route in the table resolves" (same route set client-side) | green |
| B06 | settings | `click.kivvi.fixtures.SettingsFixturesTest` — "B06 isKnownTab is true for every seeded tab and false for anything else"; `click.kivvi.application.SettingsViewServiceTest` — "B06 an unknown tab throws, mirroring SettingsController's 404"; `click.kivvi.web.SettingsControllerTest` — "B06 GET /api/v1/pl/settings/nonexistent is not found" and "B06/DEV-12 GET /pl/settings/nonexistent is a 404 document, not the 200 SPA shell" | green |
| B26 | automations | `click.kivvi.fixtures.AutomationsFixturesTest`, `click.kivvi.application.AutomationsViewServiceTest`, `click.kivvi.web.AutomationsControllerTest` (all `@DisplayName("B26 …")`, backend); `frontend/test/unit/automationsComponents.spec.ts` — `describe("B26 FlowCanvas …")`, `describe("B26 RulePipeline …")`, `describe("B26 AutoCard …")` | green |
| B27 | campaigns-email-editor | `click.kivvi.application.CampaignsViewServiceTest`, `click.kivvi.web.CampaignsControllerTest` (all `@DisplayName("B27 …")`, backend) | green |
| B28 | campaigns-email-editor | `click.kivvi.application.CampaignsViewServiceTest`, `click.kivvi.web.CampaignsControllerTest` (`@DisplayName("B28 …")`, backend); `frontend/test/unit/campaignsComponents.spec.ts` — `describe("B28 BlockLibrary …")`, `describe("B28 CouponCode …")`; `frontend/test/unit/emailDocument.spec.ts` — `describe("B28 EmailDocument keeps a literal white background regardless of theme")` | green |
| B32 | settings | `click.kivvi.fixtures.SettingsFixturesTest`, `click.kivvi.application.SettingsViewServiceTest`, `click.kivvi.web.SettingsControllerTest` (`@DisplayName("B32 …")`, backend); `frontend/test/unit/settingsComponents.spec.ts` — `describe("B32 SettingsNav/ToggleRow/DnsRow/HookRow/CodeBlock …")`; `frontend/test/unit/highlight.spec.ts` — `describe("B32 highlight() reproduces CodeHighlightExtension::highlight() byte-for-byte")` | green |

No behaviour for these three journeys is left without a named unit-dimension test, and
no mapped test is red (see `backend-surefire-summary.csv`: 0 failures/errors across 225
tests; `frontend-vitest-unit-verbose.log`: 126/126 passed).

## Wave-3 shell regression guard (unit dimension)

Shell changes landed in this wave that fall under the unit dimension's regression-guard
duty:

- vue-router `scrollBehavior` + reload scroll restoration: `frontend/test/unit/scrollRestoration.spec.ts`
  (14 tests: `waitForStableLayout`, `isReloadOfAnAlreadyVisitedEntry`, `scrollBehavior`,
  `saveScrollPositionForReload`/`consumeScrollPositionForReload`, and the fallback-path
  describe block) — all green.
- Generic locale toggle via `route.meta.defaultParams`: `frontend/test/unit/localeHref.spec.ts`
  — `describe("B32/repair-1 buildLocaleHref — generic Symfony-style default-param omission")`
  (7 tests) — all green.

Both files are inside `frontend/test/unit` and ran as part of the `npm run test -- --run`
invocation reported above.
