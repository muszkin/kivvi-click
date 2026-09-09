# Common rules for every journey packet (waves 1–5)

Read this file, then your journey packet. The wave-0 packet (slices/w0-login/packet.md) §3 (context list), §4 (pinned stack), §10 (commit discipline) and §11 (report format) apply unchanged; where this file and the wave-0 packet differ, this file wins.

## Stack produced by wave-0 (read, reuse, do not restructure)

- Backend `backend/` — Spring Boot 4.1.1, Java 25 (`export JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25; export PATH=$JAVA_HOME/bin:$PATH`), packages `click.kivvi.{web,application,domain,infrastructure,fixtures}`, ArchUnit layering test in `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java`, Flyway `V1__baseline.sql` (schema frozen: add NO migration), `RouteTable` (all SPA routes already listed), `SpaDocumentController` (serves the SPA document), `ShellController` (`GET /api/v1/{locale}/shell`). Pattern for a page: `web/<Page>Controller` → `application/<Page>ViewService` → `fixtures/<Page>Fixtures` (+ domain types), returning the exact arrays the Twig page received from its Symfony controller (`render()` parameters), pre-formatted with the ported `Format` rules (PL number/money/percent/timeAgo — port `src/Panel/Format.php` to `domain/Format.java` if it is not there yet; the SPA never formats numbers).
- Frontend `frontend/` — Vue 3.5.42 / Vite 8.2.2 / vue-router 5.3.1 / Pinia 4.0.3 / vue-i18n 11.4.10 / TS 6.0.3; `src/components/{atoms,molecules,organisms}`, `src/layouts`, `src/views`, `src/router/routes.ts` (replace ONLY your route's `component: EmptyPageView` with your view; touch no other line), `src/stores`, `src/composables/useIntents.ts` (register new `data-action` intents there only if your journey needs them), `src/i18n/{index,pl,en}.ts`. Scripts: `npm run test` (test/unit), `npm run test:integration` (test/integration), `npm run lint`, `npm run typecheck`, `npm run build`.
- i18n rule for parallel safety: do NOT edit `src/i18n/pl.ts` or `src/i18n/en.ts`. Put your keys in `src/i18n/messages/<journey>.pl.ts` and `<journey>.en.ts` (default-export plain objects, top-level key = your journey name, never colliding with existing keys). `src/i18n/index.ts` is CANONICAL after wave-1 (loader merging every `./messages/*.{pl,en}.ts` into copies of the base catalogues, plus `warnHtmlMessage: false` — authorized by the orchestrator on 2026-09-08 because several catalogue strings carry developer-authored markup ported from Twig `|raw` and are bound only with v-html): do not edit `index.ts` at all; if it is missing the loader in your parent SHA, you are on the wrong parent — stop and report.
- Compose: `compose.next.yaml` (+ `compose.next.prod.yaml`); `mercure` (dunglas/mercure v0.24.2, `mercure/Caddyfile`) is the edge; `api` built from `backend/Dockerfile` (node → maven → temurin-jre); `database` postgres:18-alpine. Do not change the edge or compose unless your packet says so.
- Verification tooling: `tools/migration-verify/compare.mjs --journey <id> --base https://localhost:<https-port> --out <dir>`, `performance.mjs --base <url>`, `deviations.json` (extend ONLY with rows for your journey's DEV ids if the mechanism is missing), `budget.json`.
- CSS is byte-identical to `assets/styles/*.css` and complete for every page — never edit CSS; if a class seems missing, you are reading the wrong Twig template.
- Markup parity method: open the oracle step's `a11y.json` and `texts.json`, the Twig template and its partials, and reproduce the DOM (elements, classes, data-* attributes, ids, text nodes). Watch Vue's whitespace condensing between inline siblings (wave-0 found it drops a whitespace-only text node with a newline: use `{{ " " }}`). Screenshots must match at ≤0.5 % after masks; texts/a11y exactly.
- Navigation stays a real document request (plain `<a href>` or the `navigate` intent), never `router.push`, unless the oracle `http.jsonl` of that step shows no document request.
- Query-string state (`?status=`, `?filter=`, `?view=`, `?preview=`, `?type=`, `?device=`, `?page=`) is read by the backend view service and reflected in the payload, exactly as the Symfony controller did.
- Old stack is read-only. Oracle is read-only. Run ledger: write only `slices/<your-id>/worker-report.md` and `slices/<your-id>/evidence/`.

## Behaviour tests

Name tests after `behaviours.json` ids for your journey (JUnit `@DisplayName("Bnn …")`, Vitest `describe("Bnn …")`); every listed behaviour must have at least one test that fails when the behaviour is removed.

## Gates (all on the final candidate SHA; record command, cwd, exit, evidence path)

1. `cd backend && ./mvnw -q test`; `cd frontend && npm run test -- --run`
2. `cd backend && ./mvnw -q verify`; `cd frontend && npm run test:integration -- --run`
3. ArchUnit (inside mvnw test) + `npm run lint` + `npm run typecheck`
4. `cd backend && ./mvnw -q spotless:check`; `cd frontend && npm run build`
5. `node tools/migration-verify/compare.mjs --journey <your-journey> --base https://localhost:<https-port> --out <run-dir>/slices/<id>/evidence/compare` → 0 regressions
6. `cd tests/e2e && E2E_BASE_URL=https://localhost:<https-port> npx playwright test <your specs>` green, specs unchanged (K8 otherwise: stop and report). Playwright keeps only the LAST `-g` flag on the command line: run one invocation per filter (or per spec file without `-g`) and keep each log — never one combined command with several `-g`. `events.spec.ts` (and any spec that asserts exact row counts on the shared Mercure topic) runs with `--workers=1`.
7. `node tools/migration-verify/performance.mjs --base https://localhost:<https-port>` within budget
Sonar: NOT_APPLICABLE (no config). List introduced dependencies with version + license; grep your diff for secrets.

## Expected initial RED

Before implementing: bring your stack up from your worktree (`docker compose -p <lease> -f compose.next.yaml up -d --build --wait` with your HTTP_PORT/HTTPS_PORT), run compare.mjs for your journey and your Playwright specs → they must FAIL (empty page / missing API), and record the failure excerpts under `evidence/red-*.txt`. Then implement. Tear the stack down (`down -v`) and remove the built image before reporting.

## Stack build hint

The `api` image builds the whole SPA and jar (several minutes). For fast iteration you may run the backend locally (`./mvnw spring-boot:run` with a dev profile pointing at the compose database) and `npm run dev` — but every gate in §Gates must run against the compose stack built from your candidate SHA.

- Contract verifier isolation (from wave-3): the contract dimension runs ALONE on the shared verification stack (or on its own stack) — other verifiers' `/collect` traffic (performance budgets, e2e) contaminates `db.json` deltas. The orchestrator sequences it after e2e/visual finish.

- Adjacent atom includes carry a trailing newline (wave-3 settings, step 5): `templates/components/atoms/button.html.twig` (and likely other atoms) ends with `\n`, so two `include()`s concatenated with `~` inside a plain `<td>`/inline container render a whitespace text node between them (one visible space, ~3px). Vue strips whitespace between sibling components — add `{{ " " }}` between adjacent `<Button>`/atom components wherever the Twig parent is not a flex/gap container. A sub-pixel `table-layout: auto` knife-edge can turn that 3px into a 7% screenshot diff, so a whole-table wrap difference with identical texts is a whitespace-node bug until proven otherwise (diff DOM text nodes against the old stack, not screenshots).

- A follow-up commit must re-run the guard that proved the fix it touches (wave-3 campaigns: the reload-type gate follow-up skipped the compare visual guard and broke step 6 mobile again). Test-only follow-ups may skip stack gates; behaviour-touching ones never.
