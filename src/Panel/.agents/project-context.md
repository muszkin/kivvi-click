<!-- BEGIN project-context-initializer:context -->
# Context: `src/Panel/`

Source `91f8f85` (main), refreshed 2026-09-08. Index: `context/map/INDEX.md`, manifest: `context/map/manifest.json`. Coverage: `own`; `Content/` rolled-up.

## Purpose
View-model for the panel shell and **sample content for every screen**. This is where the prototype's data lives until Doctrine entities replace it.

## Files
- `PanelContext` — Twig global `panel`: theme, sidebar state, nav groups, current section, crumb, workspace card, user.
- `Navigation` — sidebar groups (`main`, `automate`, `data`, `config`), detail→index mapping (`customer_show`→`customers` …), label keys `nav.*`.
- `Workspace` — tenant name `aureashop.pl`, three sample sites with colours (drive stream dots). **Tenant is a constant.**
- `PanelPreferences` — session keys `panel.theme` (light|dark), `panel.sidebar` (expanded|collapsed).
- `PanelIdentity` — session key `panel.identity`; default `maciej@aureashop.pl`; explicitly not a security identity.
- `EventStreamTopic` — `/accounts/%s/events`, `CURRENT_ACCOUNT = '1'`. Build topics here, never in templates.
- `ImportUploadStorage` — stores uploads in `var/import` as `<32hex>.<ext>` (ext whitelisted `[a-z0-9]{1,8}`), session keys `import.file_name`, `import.file_path`.
- `Format` — PL number/money/percent/initials/timeAgo (narrow no-break space thousands separator).
- `Content/` (rolled-up): `AutomationCatalog`, `CampaignCatalog`, `CustomerDirectory` (`byId` throws `InvalidArgumentException` → 404), `DashboardMetrics`, `EventFeed` (`TYPES` const used by ingestion; deterministic sample rows), `ImportWizard` (`FIRST_STEP`), `LandingContent`, `ProductFeedCatalog`, `SettingsCatalog` (`DEFAULT_TAB`, tabs, `TRACKER_SNIPPET`), `WidgetCatalog`.

## Consumers
All `src/Controller` panel controllers, `Tracking\\EventIngestion` (topic, sites, event types), `templates/layout/app.html.twig` via global `panel`.

## State and trust
Session (Postgres) only. No persistence of catalogues. Replacing a catalogue with a repository must keep the array shapes documented in each method's `@return` (templates depend on keys).

## Invariants
- Row shape of `EventFeed::rows()` must match `components/molecules/event-row.html.twig` params exactly (ingestion renders the same template).
- Money/number formatting is decided here, never in Twig.

## Risks
Hard-coded tenant/account (R4), sample data indistinguishable from real data in the UI (R3), PL strings hard-coded (R10).

## Evidence
`src/Panel/*.php`, `src/Panel/Content/*.php`, `config/services.yaml` (`ImportUploadStorage` upload dir).
<!-- END project-context-initializer:context -->
