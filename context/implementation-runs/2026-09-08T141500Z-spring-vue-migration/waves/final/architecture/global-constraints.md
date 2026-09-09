# Architecture verifier — global implementation constraints (round 2, final cohort)

Checked at wave SHA `dae169614a52532c130bd34435994d6a914165c0` in
`/home/muszkin/work/kivvi-click-wt/verify-final-architecture` (git status clean throughout).

## Layering

See `negative-probes.md` row 4 and `backend-archunit-test.log`/`backend-archunit-surefire.txt`:
`ArchitectureTest` (5/5) covers domain→web, infrastructure-access and the no-cycle slice rule;
each was independently probed to a real failure and reverted.

## Flyway baseline unchanged

`find backend/src/main/resources -iname "*.sql"` → exactly one file,
`backend/src/main/resources/db/migration/V1__baseline.sql`. `git log --oneline --follow` on the
whole `db/migration/` directory → one commit only, `3b17c07 feat: stand up the Spring Boot +
Vue 3 login walking skeleton (wave-0)`. No later commit touches it and no `V2__*`/`V3__*` file
was ever added — the baseline is unchanged since its introduction.

## compose.next*.yaml service scope

`grep -n "^  [a-z]" compose.next.yaml` → `api`, `mercure`, `database` (plus the `volumes:` block,
not a service). `compose.next.prod.yaml` → the same three service keys as overrides, no new
service added. No fourth backing service (no Redis/RabbitMQ/etc.) in either file — consistent
with CLAUDE.md's "Postgres is the only datastore; Mercure hub is the only additional container."

## Pinned versions vs. the plan's Target stack table

| Target stack row | Plan pin | Found in checkout | Verdict |
| --- | --- | --- | --- |
| Spring Boot | 4.1.1 | `backend/pom.xml` parent version `4.1.1` | match |
| Vue 3 SPA | 3.5.42 | `frontend/package.json` `"vue": "3.5.42"` | match |
| Java | Temurin 25.0.4.1+1 | build `java.version=25` (pom); `JAVA_HOME` toolchain used by every command in this run is `Temurin-25.0.4.1+1` (`java -version`); **but** `backend/Dockerfile` pins the runtime image only by major version (`maven:3.9-eclipse-temurin-25`, `eclipse-temurin:25-jre`), not the exact patch `25.0.4.1_1`, and `backend/README.md` says only "Java 25 (Temurin)" | partial — exact patch not pinned in the shipped container image tags (pre-existing since wave-0, not a round-2 regression; does not affect any journey's runtime behaviour) |
| Maven wrapper | 3.9.x | `backend/.mvn/wrapper/maven-wrapper.properties`: `wrapperVersion=3.3.4`, `distributionUrl=…/apache-maven-3.9.11-bin.zip` | match (3.9.x range) |
| Mercure hub | 0.24.2 | `compose.next.yaml`: `image: dunglas/mercure:v0.24.2` | match |

Only the Java Docker-image tag is a soft (major-version) pin rather than the table's exact patch;
every other row matches exactly. This is an existing wave-0 characteristic of the Dockerfile, not
something round 2's shell-navigation label repair touched (that commit only added test files —
see the summary's "Round-2 scope" section) — noted as an observation, not scored as a FAIL on its
own.

## No Intl outside format.ts

`frontend/src/format.ts` does not exist yet on this SHA (no journey has introduced client-side
number/money formatting; `domain/Format.java` remains the sole formatter, per rules-translated.md
row "Formatting decided server-side"). `eslint.config.js` bans `Intl` (global, import pattern,
and `no-restricted-syntax` member-expression/`toLocaleString`/`toFixed` selectors) everywhere
under `src/**/*.{ts,vue}` except a pre-registered (currently unused) exemption for
`src/format.ts`. Probed in `negative-probes.md` row 7 — fires on a bare `Intl.NumberFormat` call.

## `/accounts/` only in EventStreamTopic

Backend: `ArchitectureTest#accountsTopicLiteralExistsOnlyInEventStreamTopic` source-scans every
`.java` file under `src/main/java` for the quote-anchored literal `"/accounts/` and asserts the
only match is `domain/tracking/EventStreamTopic.java` — passed (see
`backend-archunit-surefire.txt`, 5/5) and probed to fail in `negative-probes.md` row 5. Frontend:
`grep -rl "/accounts/" src --include=*.ts --include=*.vue` → no matches (topic is only ever read
from the API payload, never composed client-side), and `eslint.config.js`'s
`no-restricted-syntax` literal/template-element selectors ban the substring in `src/**/*.{ts,vue}`
— probed to fail in the same section.

## `event-row` only in the three allowed files

`grep -rln "event-row" src --include=*.vue` (real tree) → exactly `EventRow.vue`,
`ListCard.vue`, `RecentImports.vue`. `eslint.config.js`'s `vue/no-restricted-class` blocks the
class everywhere else, with `RecentImports.vue`'s history (repair-3, see the file's own git log)
recording an earlier bypass (`const recentImportRowClass = "event-row"` bound through `:class`)
that a later `no-restricted-syntax` string/template-literal hardening now also catches. Probed to
fail on a fourth file in `negative-probes.md` row 6.

## i18n pl/en key parity

`frontend/test/unit/i18n.spec.ts` (shipped, ran clean — 3/3) checks the base
`src/i18n/pl.ts`/`en.ts` catalogues carry the same key set. That test does **not** cover the ten
per-journey `src/i18n/messages/<journey>.{pl,en}.ts` pairs merged in by `src/i18n/index.ts`'s
`import.meta.glob`. Wrote a throwaway Vitest spec importing all ten pairs
(automations, campaigns, customers, dashboard, events, feeds, import, landing, popups, settings)
and asserting `keys(en).sort() === keys(pl).sort()` on each (recursive key-path comparison,
mirroring `i18n.spec.ts`'s own `keys()` helper) — **10/10 passed**, i.e. every journey catalogue
does carry pl/en key parity on this SHA, even though no automated gate currently exercises the
per-journey pairs (only the base catalogue has a standing test). Deleted after running; not a
regression to flag, but worth noting to the run owner that per-journey i18n parity currently has
no standing gate — only manual/verifier spot-checking establishes it.

## Protected old-stack paths unmodified since `5b806ac` except the compose.yaml restart-policy fix

`git log --oneline 5b806ac..HEAD -- src/ templates/ assets/ composer.json composer.lock
compose.yaml Dockerfile frankenphp/` → exactly one commit, `33b3f8d fix: restart the database
container automatically and approve the migration plan`. `git show --stat 33b3f8d` touches
`compose.yaml` plus context/plan docs only (no `src/`, `templates/`, `assets/`, `composer.*`,
`Dockerfile`, or `frankenphp/` changes). `git show 33b3f8d -- compose.yaml` is exactly the
documented fix: adds `restart: unless-stopped` to the `database` service (R17 in
`context/map/risks-and-unknowns.md`) — no other line changed. Per-path empty-diff confirmation:
`git diff --stat 5b806ac..HEAD -- <path>` returned empty for `src`, `templates`, `assets`,
`composer.json`, `composer.lock`, `Dockerfile`, `frankenphp`.
