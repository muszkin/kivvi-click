# CON-1 — "tests/e2e unchanged and green" exit condition

**Verdict: PASS**

Verified 2026-09-09T16:04Z–16:07Z against production `https://kivvi.click` (compose project
`kivvi-click`, `compose.yaml` + `compose.prod.yaml`, host `/home/muszkin/work/kivvi-click`,
branch `main` @ `4539828` — the CON-1 commits that deleted the old Symfony stack and dropped the
PHP test suite). Read-only run: no compose stack or container was started, stopped, or restarted;
no tracked file was edited or committed. The only filesystem writes were `npm install` in
`tests/e2e/` and `frontend/` (gitignored `node_modules/`) and a local `frontend/dist` production
build required by `performance.mjs`, both removed afterward (see Cleanup). `git status --porcelain`
was `?? context/implementation-runs/.../cutover/con1/` and the pre-existing untracked `.ai/` only —
no tracked file touched.

## 1. `tests/e2e/specs` unchanged over the CON-1 window

```
$ git diff --stat HEAD~10 -- tests/e2e/specs
(no output — no changes)
```

`HEAD~10..HEAD` spans `24e0327`..`4539828`, i.e. the RR-1/CUT-1 docs commits, the migration squash
(`8b224c5`), and all four CON-1 commits (`7287390` Symfony removal, `25329ac` PHP test suite drop,
`8c37b92` plan errata, `0b46320` workflow path filters). None touched `tests/e2e/specs`.

**Last commit that ever touched `tests/e2e/specs`:** `812db3f` "fix: gate live-row prepends on an
open Mercure subscription and honour pause" (2026-08-26 13:07:01 +0000) — the only other commit is
`bdf120e` (2026-08-26 13:04:49 +0000, the suite's original creation). Both predate the migration by
two weeks; the spec files are byte-for-byte what they were before the migration started.

Evidence: `con1/step-git-diff-specs.txt`.

## 2. Full Playwright suite, green (`E2E_BASE_URL=https://kivvi.click`)

`cd tests/e2e && npm install` — exit 0, "up to date, audited 4 packages, found 0 vulnerabilities"
(`con1/step0-npm-install.txt`). One spec file per invocation; `--workers=1` for `events`,
`dashboard`, `navigation` (navigation run twice) per instructions; default parallel workers for
the rest.

| Spec file | Tests | Passed | Failed | Workers | Duration |
| --- | --- | --- | --- | --- | --- |
| `public.spec.ts` | 5 | 5 | 0 | 5 | 2.2s |
| `navigation.spec.ts` (run 1) | 14 | 14 | 0 | 1 | 9.5s |
| `navigation.spec.ts` (run 2) | 14 | 14 | 0 | 1 | 9.5s |
| `events.spec.ts` | 4 | 4 | 0 | 1 | 3.8s |
| `dashboard.spec.ts` | 6 | 6 | 0 | 1 | 4.0s |
| `customers.spec.ts` | 4 | 4 | 0 | 4 | 1.6s |
| `automations.spec.ts` | 4 | 4 | 0 | 4 | 1.7s |
| `editors.spec.ts` | 6 | 6 | 0 | 6 | 2.1s |
| `import.spec.ts` | 6 | 6 | 0 | 6 | 2.5s |
| `lists.spec.ts` | 5 | 5 | 0 | 5 | 2.0s |
| `settings.spec.ts` | 12 | 12 | 0 | 8 | 3.4s |
| **Total (66 unique tests, navigation counted once)** | **66** | **66** | **0** | | |

All 11 invocations exited 0. No failing test names, no retries, no flaky output. `import.spec.ts`
exercised the live import wizard demo upload; `performance.mjs` (below) posted 50 real `purchase`
events to `/collect` — both pre-approved as this is a demo app. Full stdout per file/run:
`con1/e2e/<file>[-run{1,2}].log`.

## 3. Performance budgets (DEV-8)

`frontend/dist` did not exist in this checkout — `initialJsGzipBytes` is computed from a local
build, not fetched from the server. Ran `npm install && npm run build` in `frontend/`
(`con1/step1-frontend-npm-install.txt`, `con1/step1-frontend-build.txt`, both exit 0; `dist/` is
gitignored). Then:

```
node tools/migration-verify/performance.mjs --base https://kivvi.click
```

Exit 0 (`con1/step2-performance.txt`):

| Metric | Value | Budget | Verdict |
| --- | --- | --- | --- |
| `initialJsGzipBytes` | 101,990 B | ≤ 307,200 B | PASS |
| `lcpMillis` (`/pl/login`) | 200 ms | ≤ 2000 ms | PASS |
| `ttiMillis` | 57.8 ms | ≤ 2500 ms | PASS |
| `collectP95Millis` (50 sequential `/collect` POSTs) | 39.7 ms | ≤ 500 ms | PASS |

All four budgets pass, comfortably.

## 4. Compose stack status (read-only)

```
docker compose -p kivvi-click --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml ps
```

(`con1/step3-compose-ps.txt`) — three services, all healthy:

| Service | Container | Status | Started |
| --- | --- | --- | --- |
| `api` | `kivvi-click-api-1` | Up, healthy | 2026-09-09T16:02:27Z |
| `mercure` | `kivvi-click-mercure-1` | Up, healthy | 2026-09-09T16:01:17Z |
| `database` | `kivvi-click-database-1` | Up, healthy | 2026-09-09T14:18:25Z (continuous since CUT-1) |

**Note (not an anomaly, but material to the log windows below):** `api` and `mercure` had been
recreated by someone/something outside this verification run only ~4–5 minutes before these checks
started (16:04–16:07Z) — image `kivvi-click-api` was built 2026-09-09T13:57:43Z, i.e. after the
CON-1 code commits. This verifier never ran `up`/`down`/`restart`/`stop` — only `ps` and `logs` —
so the recreation predates this session and is consistent with the operator redeploying the
CON-1-cleaned stack. Consequence: the `--since 30m` / `--since 60m` log windows below only contain
~4–5 minutes of real log history (the full container log, no truncation), not a full 30/60-minute
trailing window. `database` was untouched and has run continuously since the CUT-1 cutover
yesterday.

## 5. Container error logs

`docker logs --since 30m kivvi-click-api-1 2>&1 | grep -iE " ERROR |exception"`:
0 matches (31 total log lines captured — `con1/step4-api-logs-full.txt`, `con1/step4-api-grep.txt`).

`docker logs --since 30m kivvi-click-mercure-1 2>&1 | grep -iE " ERROR |exception"`:
0 matches (660 total log lines — `con1/step4-mercure-logs-full.txt`, `con1/step4-mercure-grep.txt`).

**5xx check (Mercure/Caddy JSON access log, `"status"` field) over the same window:** status-code
distribution was 492× `200`, 52× `202`, 3× `302`, **0× 5xx** (`con1/step4-mercure-5xx.txt` is
empty; full breakdown in the log). Clean.

## 6. Edge access log — requests to old-only paths

```
docker logs --since 60m kivvi-click-mercure-1 2>&1 | grep -E '"uri":"[^"]*(\.php|_storybook)'
```

0 matches over 661 captured log lines (`con1/step5-mercure-logs-60m-full.txt`,
`con1/step5-old-only-paths.txt` empty). No client hit a PHP-only route or `/_storybook` during the
window. `/collect` traffic present and healthy (202s, shared endpoint, unaffected by the cutover).

## Cleanup

- `frontend/dist` deleted after `performance.mjs` ran (confirmed absent).
- `tests/e2e/node_modules` and `frontend/node_modules` deleted at the end of this run.
- No tracked file modified; no commit made; no container/stack started, stopped, or restarted by
  this verifier.

## Commands (cwd → exit code)

| Command | cwd | Exit |
| --- | --- | --- |
| `git diff --stat HEAD~10 -- tests/e2e/specs` | `/home/muszkin/work/kivvi-click` | 0 |
| `git log --format='%H %ad %s' --date=iso -- tests/e2e/specs` | `/home/muszkin/work/kivvi-click` | 0 |
| `npm install` | `tests/e2e/` | 0 |
| `npx playwright test specs/public.spec.ts` | `tests/e2e/` | 0 |
| `npx playwright test specs/navigation.spec.ts --workers=1` (×2) | `tests/e2e/` | 0, 0 |
| `npx playwright test specs/events.spec.ts --workers=1` | `tests/e2e/` | 0 |
| `npx playwright test specs/dashboard.spec.ts --workers=1` | `tests/e2e/` | 0 |
| `npx playwright test specs/customers.spec.ts` | `tests/e2e/` | 0 |
| `npx playwright test specs/automations.spec.ts` | `tests/e2e/` | 0 |
| `npx playwright test specs/editors.spec.ts` | `tests/e2e/` | 0 |
| `npx playwright test specs/import.spec.ts` | `tests/e2e/` | 0 |
| `npx playwright test specs/lists.spec.ts` | `tests/e2e/` | 0 |
| `npx playwright test specs/settings.spec.ts` | `tests/e2e/` | 0 |
| `npm install` | `frontend/` | 0 |
| `npm run build` | `frontend/` | 0 |
| `node tools/migration-verify/performance.mjs --base https://kivvi.click` | `/home/muszkin/work/kivvi-click` | 0 |
| `rm -rf frontend/dist` | `/home/muszkin/work/kivvi-click` | — (cleanup) |
| `docker compose -p kivvi-click --env-file .env.prod.docker -f compose.yaml -f compose.prod.yaml ps` | `/home/muszkin/work/kivvi-click` | 0 |
| `docker logs --since 30m kivvi-click-api-1` \| `grep -iE " ERROR \|exception"` | — | 0 (0 matches) |
| `docker logs --since 30m kivvi-click-mercure-1` \| `grep -iE " ERROR \|exception"` | — | 0 (0 matches) |
| `docker logs --since 60m kivvi-click-mercure-1` \| `grep -E '"uri":"[^"]*(\.php\|_storybook)'` | — | 0 (0 matches) |
| `rm -rf tests/e2e/node_modules frontend/node_modules` | `/home/muszkin/work/kivvi-click` | — (cleanup) |

## Conclusion

CON-1's exit condition — "contract holds; `tests/e2e` unchanged and green" plus "zero requests to
old-only paths during the window (edge access log); full e2e green after deletion" — is **met**:
`tests/e2e/specs` has no diff over the last 10 commits (last real change 2026-08-26, before the
migration), all 66 e2e tests pass against production `https://kivvi.click` (80 executions counting
navigation twice, 0 failures), all 4 DEV-8 performance budgets pass, `api`/`mercure` logs are clean
of errors/exceptions/5xx, and the edge access log carries zero requests to `.php` or `_storybook`
paths.
