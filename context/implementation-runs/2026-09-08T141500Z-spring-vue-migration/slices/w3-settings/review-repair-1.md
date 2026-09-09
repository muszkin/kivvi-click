# Independent review — w3-settings repair-1

**Diff:** `bddc537551fe06ba480037c968ee06ffa9627d31...4bb3b83459e0734dfc4bfc7c3ac7b3e19f1afa75` (one commit, `4bb3b83`)

PASS

## 1. R1-B generic locale toggle

`frontend/src/router/localeHref.ts:19-34` (`buildLocaleHref`) reads `route.meta.defaultParams`
generically, drops any param equal to its declared default, and calls `router.resolve({name,
params}).path`. `Topbar.vue:32-33` calls it with no route-name branch (the old `route.name ===
"settings"` special case is gone — diff confirms). `routes.ts:130-138` adds `meta.defaultParams:
{ tab: "account" }` to the settings route only; `meta.d.ts:4-19` types the new field.

Semantics verified live (`npx vitest run` + one throwaway probe, deleted after):
- settings account → `/en/settings`; settings billing → `/en/settings/billing`; bare `/pl/settings`
  → `/en/settings`; customers detail unaffected; dashboard unaffected; import route (`no
  defaultParams` yet) keeps its step segment (`/en/import/1`) — all match `localeHref.spec.ts` and
  independently re-ran green (95/95).
- Query string: old-stack `templates/components/organisms/topbar.html.twig:32` builds the href from
  `path($route, $route_params)` only — query is never part of `_route_params`, so it's dropped.
  Verified against the oracle: `journeys/customers/steps/2/a11y.json` (`/pl/customers?page=2` → PL
  link still `/en/customers`, no `page`) and `journeys/event-stream/steps/7/a11y.json`
  (`?range=24h&site=aurea&type=purchase` → `/en/events`). `buildLocaleHref` uses `router.resolve().path`,
  which likewise never carries `route.query` — matches.
- Unknown/404 route: probed live — `router.resolve("/pl/this-does-not-exist")` gives
  `matched.length === 0`, `name: undefined`, `meta: {}`; `buildLocaleHref` returns `/en` (falls back
  to the locale-root route) with no exception. Graceful, untested but correct; not a defect.
- **Mutation test performed:** flipped `defaults[key] === value` to `!==` in `localeHref.ts:30` →
  5/7 `localeHref.spec.ts` cases failed as expected (import + round-trip cases too); reverted, tree
  clean, 95/95 restored. The helper is genuinely load-bearing for the tests.

**Verdict: PASS.** Fully generic, no journey-specific branch, matches old-stack semantics on every
checked case including the query-string edge the packet flagged as open.

## 2. Tests

`frontend/test/unit/localeHref.spec.ts:8-54` — exactly 7 cases as claimed. Re-ran
`npm run test -- --run`: **95/95** (88 pre-existing + 7 new), matching the report. No route name is
hardcoded as a literal list to iterate — the spec drives real `routes.ts` through `router.resolve`,
so it can't rot when new routes are added; it would need updating only if `defaultParams` semantics
themselves change. **PASS.**

## 3. Scope

`git diff --name-only` shows exactly 5 files: `Topbar.vue`, `routes.ts` (meta only, one route),
`localeHref.ts` (new), `localeHref.spec.ts` (new), `meta.d.ts` (new, undisclosed-by-name in the
packet but a necessary, disclosed TS companion to the `defaultParams` meta field — reported
explicitly in worker-report.md "Files changed in this repair"). `tools/migration-verify/deviations.json`
confirmed **not** in the diff (`git diff --name-only ... -- .../deviations.json` empty). **PASS.**

## 4. R1-A evidence quality

Evidence files exist under `evidence/repair-1/01`–`10` and support the numeric claims:
- `10-threshold-sensitivity.txt`: `extraPx:0` → `headerHeight:37`; `extraPx:1` → `headerHeight:53.5`
  — exact match to the report's "37px → 53.5px" claim; oracle's own screenshot
  (`journeys/settings/steps/5/desktop.png`, visually inspected) shows a genuine two-line header
  ("OSTATNIE"/"UŻYCIE") — consistent with ~53px.
- `07-final-margins.json`: margin `0.15625` — matches report and the original review's independent
  figure exactly.
- `01-font-falsify.txt`: `document.fonts.ready`/`.check("500 14px Geist")` both report
  `true`/`"loaded"`, and `wraps:true, height:49.5` is identical before and after — the font-loading
  test was actually run and does falsify the race hypothesis as claimed.
- `06-hscan-table-edges.txt`: table left/right edges pixel-identical (x=521/1411) oracle vs
  candidate — supports "not a container-width shift."
- Visually confirmed the qualitative claim (candidate screenshot vs oracle screenshot, both read
  directly): candidate's "Produkcja — backend" does not wrap, oracle's does; candidate renders the
  copy/trash action icons side-by-side per row, oracle stacks them vertically per row — direction
  matches the report.
- **Minor gap:** `09-icon-scan-full.txt` contains **only** the oracle's pixel scan (no `CANDIDATE`
  section, confirmed via `grep`), and only covers y=283–355 — too narrow a window to independently
  derive the report's specific "73px apart" figure for the two icon bands. The general "stacked
  vertically" claim is visually true; the specific distance number is not fully backed by the
  supplied evidence file. Not fatal (not one of the two required numbers), but worth a note for the
  next repair to include a matching CANDIDATE scan at the same coordinates.

No code fix was made (correctly — the packet's step 4 only requires a fix if the cause is in the
port); the DEV-14 row is proposed in prose in worker-report.md and **not** added to
`deviations.json`, per instruction. **PASS**, with one LOW note above.

## 5. Commit hygiene

Single commit `4bb3b83`: `fix: generalize the locale-toggle default-param omission via route meta
(#settings)` — Conventional Commits, English, references the journey, no trailers, no AI mentions.
`git status --porcelain` clean. **PASS.**

## 6. Report accuracy — gates re-run

| Gate | Report | Independently re-run | Match |
| --- | --- | --- | --- |
| `frontend && npm ci` | — | clean install, 0 vulnerabilities | — |
| `npm run test -- --run` | 95/95 | **95/95** | ✓ |
| `npm run test:integration -- --run` | 57/57 | **57/57** | ✓ |
| `npm run lint` | 0 errors / 6 warnings (pre-existing class) | **0 errors / 6 warnings**, same 3 files/lines (`HookRow.vue:45-46`, `FeedCard.vue:164,169`, `ApiTab.vue:78,80`) | ✓ |
| `npm run typecheck` | clean | **clean** | ✓ |
| `npm run format:check` | clean | **clean** | ✓ |
| `backend && ./mvnw -q test` | exit 0 (unchanged backend) | **exit 0**, no BUILD FAILURE / no failed test in log | ✓ |

`npm run build`, `mvnw verify`, `compare.mjs`, and e2e specs were not re-run here (no stack, per
this review's read-only/no-Docker constraint) — judged from `evidence/repair-1-gates/*.log` and
`compare/*` artifacts, which are internally consistent with the claimed numbers (step 5 desktop is
the one visual regression, steps 1–4/6–10 parity; login/customers visual guards 0 regressions).

## Findings by severity

**LOW-1** — `evidence/repair-1/09-icon-scan-full.txt` has no `CANDIDATE` section and its y-range
(283–355) is too narrow to derive the "73px apart" figure used in the R1-A narrative and the
proposed DEV-14 note independently; the qualitative "stacked vertically" claim is visually
confirmed, the specific distance number is not. Fix (next touch of this evidence, non-blocking):
re-run the icon scan with matching CANDIDATE coordinates over a wider y-range.

**LOW-2** — No unit test exercises `buildLocaleHref` against an unmatched/404 route
(`route.name === undefined`). Verified live it degrades gracefully to `/{locale}` with no
exception, so this is a coverage gap, not a defect. Add a case if this helper grows further.

No HIGH or MEDIUM findings. Both HIGH items from the prior review (missing deviation entry for the
step-5 regression, and the Topbar special case) are addressed: R1-B is now fully generic and
tested; R1-A's root cause is pinned down with numbers and correctly escalated as a proposed
deviation rather than self-approved.
