# Cross-journey imports and upload-directory default — wave-4 round 2

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-4-architecture` at
`71d884d2ed96a7a7caad18147e76570c9e113d4d`.

## Cross-journey imports beyond shared components

Audited every `import ... from "@/components/..."` in the three wave-4 journeys' views
(`DashboardView.vue`, `ImportView.vue`, `PopupsView.vue`, `PopupEditorView.vue`) and every
consumer of the components each journey owns (`components/import/**`, `ListCard.vue`,
`Cardiogram.vue`, `PopupStage.vue`, `PopupWidget.vue`, `PwTypeList.vue`, `EventStream.vue`,
`KpiGrid.vue`, `BlockLibrary.vue`, `EditorShell.vue`) via
`grep -rn 'from "@/components/...'` over `frontend/src/{views,components}`.

Findings:

- `components/import/*` (Stepper.vue excluded, see below) is consumed only by `views/ImportView.vue`.
  Not imported from any other view or component.
- `components/organisms/ListCard.vue` and `components/organisms/Cardiogram.vue` are consumed only
  by `views/DashboardView.vue`.
- `components/organisms/PopupStage.vue` and `components/organisms/PopupWidget.vue` are consumed
  only by `views/PopupsView.vue` and `views/PopupEditorView.vue` (both part of the
  popups-widget-editor journey).
- `components/organisms/PwTypeList.vue`, `molecules/TriggerRow.vue`, `molecules/SwatchGrid.vue`,
  `molecules/PositionGrid.vue`, `molecules/AddSlot.vue` are consumed only by `PopupEditorView.vue`.
- `components/organisms/EventStream.vue` (owned by the event-stream journey, wave-2) is consumed
  by `views/EventsView.vue` (its owner) **and** `views/DashboardView.vue` — this is the declared
  read-only cross-journey consumption from the plan's dependency audit ("J11 consumes J4's
  EventStream read-only").
- `components/organisms/KpiGrid.vue` (owned by the feeds journey, wave-1) is consumed by
  `CampaignsView.vue`, `DashboardView.vue`, `FeedsView.vue`, `ImportView.vue`, `CustomerView.vue`,
  and `components/import/StepRun.vue` — a deliberately shared KPI-tile organism, consistent with
  the plan's "both consume KpiGrid (J2) read-only" note for wave-2 journeys, extended in practice
  to every journey that shows KPI tiles.
- `components/organisms/BlockLibrary.vue` (owned by campaigns-email-editor, wave-3) is consumed by
  `EmailEditorView.vue` (its owner) **and** `PopupEditorView.vue` — the component's own header
  comment already documents this: "2-column grid of draggable block sources for the e-mail (and,
  from wave-4, popup) editor". Matches the plan's Group D note for `EditorShell`/`BlockLibrary`
  reuse into the popup editor.
- `components/organisms/EditorShell.vue` (owned by campaigns-email-editor, wave-3) is consumed by
  `EmailEditorView.vue` and `PopupEditorView.vue` — matches the plan verbatim: "EditorShell owned
  by J8 and consumed by J9 later."
- `components/atoms/*` and generic `components/molecules/{Card,Chip,...}.vue` are consumed broadly
  across nearly every view — ordinary shared design-system primitives, not journey-owned.

One notable non-violation worth recording: `components/molecules/Stepper.vue` (owned by
import-wizard; its own header comment says "First (and only, so far) owner: import-wizard") reaches
*into* `components/import/importRoute.ts` for a URL-building helper — a shared-tree molecule
depending on a journey-scoped helper module, the reverse of the usual shared->journey direction.
Because `Stepper.vue` itself is consumed only by `ImportView.vue` today, this creates no actual
cross-journey leak (no other journey imports `Stepper.vue`), so it is not a rule violation, but the
direction is architecturally backwards and worth a maintainability note if `Stepper.vue` ever gains
a second owner.

**Conclusion:** every cross-journey import found (`EventStream`, `KpiGrid`, `BlockLibrary`,
`EditorShell`, plus the generic atoms/molecules) is a declared, plan-consistent shared component.
No wave-4 journey privately reaches into another wave-4 (or earlier-wave) journey's own
non-shared component tree.

## Upload directory default

`backend/src/main/resources/application.yml:81`:
```yaml
upload-directory: ${KIVVI_IMPORT_UPLOAD_DIRECTORY:./var/import}
```
Relative path `./var/import`, resolved against the process working directory inside the `api`
container — no environment variable is set for it in `compose.next.yaml`, and that file's `api`
service declares no volume mount over `./var/import` or any parent of it (only `mercure_data`,
`mercure_config`, and the Postgres `database_data` volume are mounted, none of which cover the
app's own working directory). Uploaded files therefore live inside the container's writable layer
by default, never on a host bind mount — matching the plan's "Worktree resource isolation" table
row: "Test accounts / fixtures: none (fixtures are code); upload directory inside the api
container."
