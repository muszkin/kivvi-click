# Architecture verifier — global implementation constraints (wave FINAL, all journeys, round 1)

Wave SHA: `7e6a66a77d4f97ecd2aaea8ad2eb3b6c66015194`. All commands run with `cwd =
/home/muszkin/work/kivvi-click-wt/verify-final-architecture` unless noted. All checks read-only
(`git diff`/`git log`/`find`/`grep`/a Node script) — no probe files needed for this section.

## 1. Layering (domain/application/infrastructure/web)

Covered mechanically by `architecture/rules-proof.md` row 4 (ArchUnit `domainDoesNotDependOnWeb`,
`infrastructureIsOnlyUsedFromApplication`, `topLevelPackagesFormNoCycle`, all proven with a firing
negative and a clean restore).

**Finding (not a violation):** `backend/src/main/java/click/kivvi/` has a fifth top-level
package, `fixtures/` (`AutomationsFixtures.java`, `CampaignsFixtures.java`, ... 10 files),
alongside `domain/application/infrastructure/web`. It is a DAG leaf: `fixtures` depends only on
`domain` (`Format`, `Identity`); `application`, `web` and one `infrastructure` class
(`SessionIdentityStore`) depend on `fixtures`; nothing in `domain` depends back on `fixtures`
(confirmed both by `grep` and by the `topLevelPackagesFormNoCycle` probe, which showed exactly
this edge set when a synthetic `domain -> fixtures` back-edge was added). It does not violate
`domainDoesNotDependOnWeb` or `infrastructureIsOnlyUsedFromApplication`, and the cycle-freedom
test already covers it (proven above).

It is, however, a drift from the plan's own decision ledger: the plan's "Decision and assumption
ledger" (line 111) and the settings journey packet's evidence row (line 127) both say *"fixtures
are JSON resources under `backend/src/main/resources/fixtures/`"* / *"Sample catalogues become
seed fixtures in the Java API — fixtures are JSON resources under
backend/src/main/resources/fixtures/"*. On this SHA, `backend/src/main/resources/fixtures/` does
not exist (`find backend/src/main/resources -iname '*fixture*'` → no result); the fixtures are
compiled Java classes returning object literals, not JSON resource files. This is plausibly a
reasonable implementation choice (`DashboardFixtures`/`FeedsFixtures`/etc. call
`domain.Format.number(...)`/`.money(...)` at runtime, which a static JSON file cannot do — the
old stack's `EventFeed::rows()` needed a clock-derived relative time the same way, per plan line
127), but it is a real deviation from the written plan text with no corresponding `DEV-n` entry
and no rule in `rules-translated.md` governing it either way. **Not scored as an architecture-
dimension FAIL** (the dimension's contract is "ArchUnit + ESLint boundary rules against
rules-translated.md", and no such rule exists for this), but flagged here as an unrecorded plan
deviation for the run owner's attention.

## 2. Flyway baseline unchanged since V1

```
$ find backend/src/main/resources/db/migration -type f
backend/src/main/resources/db/migration/V1__baseline.sql   (only migration file)

$ git log -p --follow -- backend/src/main/resources/db/migration/V1__baseline.sql \
    | grep -E '^commit|^diff --git|^\+\+\+|^---'
commit 3b17c07e23354e493edbab572e4090f69d2c0c71
diff --git a/backend/src/main/resources/db/migration/V1__baseline.sql b/... (new file, /dev/null -> file)
```

Only one commit ever touches the file's content (its creation in wave-0, `3b17c07`); a second
commit that appears in `git log --format=%H -- <path>` (`f4ac025`, same wave-0 feat commit
message from a different branch/merge) produces **no diff** for this path under `git show`,
confirming the file's content has never changed since it was introduced. No `V2__*.sql` or later
migration exists. **PASS — baseline unchanged since V1, still the only migration.**

## 3. No Redis/RabbitMQ/other backing services in compose.next*.yaml (Postgres + Mercure only)

```
$ grep -n '^  [a-z]' compose.next.yaml
6:  api:
49:  mercure:
82:  database:

$ grep -n 'image:' compose.next.yaml compose.next.prod.yaml
compose.next.prod.yaml:7:    image: ${IMAGES_PREFIX:-}kivvi-click-api
compose.next.yaml:50:    image: dunglas/mercure:v0.24.2
compose.next.yaml:83:    image: postgres:18-alpine
```

`compose.next.prod.yaml` is a pure overlay on the same three services (api/mercure/database, plus
volumes) — no new service block. `dunglas/mercure:v0.24.2` matches the plan's pinned Mercure
version exactly; `postgres:18-alpine` matches "PostgreSQL 18 (existing)". **PASS — Postgres +
Mercure only, no Redis/RabbitMQ/Memcached/other backing service anywhere in either compose file.**

## 4. Pinned versions in pom.xml/package.json vs the plan's "Target stack" / "Technology decisions" tables

| Dependency | Plan-pinned | Found in this checkout | Source | Drift |
| --- | --- | --- | --- | --- |
| Java | Temurin 25.0.4.1+1 | `java -version` → `Temurin-25.0.4.1+1` (JAVA_HOME toolchain); `pom.xml` `<java.version>25</java.version>` | env + pom.xml | none |
| Spring Boot | 4.1.1 | `<artifactId>spring-boot-starter-parent</artifactId><version>4.1.1</version>` | backend/pom.xml:10 | none |
| ArchUnit | 1.5.0 | `<archunit.version>1.5.0</archunit.version>` | backend/pom.xml:23 | none |
| ShedLock | 7.10.0 | `shedlock-spring` / `shedlock-provider-jdbc-template` both `<version>7.10.0</version>` | backend/pom.xml:94,99 | none |
| Maven wrapper | 3.9.x | `distributionUrl=...apache-maven-3.9.11-bin.zip`, `wrapperVersion=3.3.4` | backend/.mvn/wrapper/maven-wrapper.properties | none |
| Testcontainers | 2.0.5 | resolved `testcontainers`/`testcontainers-postgresql` jars both `2.0.5` in `~/.m2` (managed by the Boot 4.1.1 BOM, not pinned directly in pom.xml — expected) | `~/.m2/repository/org/testcontainers/...` | none |
| JUnit | 6.0.3 | resolved `junit-jupiter` jar `6.0.3` (BOM-managed) | `~/.m2/repository/org/junit/jupiter/junit-jupiter/6.0.3` | none |
| Hibernate/JPA | n/a (wave-0 has no JPA entities per pom.xml comment) | absent from `~/.m2` entirely | — | none (confirms the comment's claim) |
| Mercure hub | 0.24.2 | `dunglas/mercure:v0.24.2` | compose.next.yaml:50 | none |
| PostgreSQL | 18 (existing) | `postgres:18-alpine` | compose.next.yaml:83 | none |
| Vue | 3.5.42 | `"vue": "3.5.42"` | frontend/package.json | none |
| vue-router | 5.3.1 | `"vue-router": "5.3.1"` | frontend/package.json | none |
| Pinia | 4.0.3 | `"pinia": "4.0.3"` | frontend/package.json | none |
| vue-i18n | 11.4.10 | `"vue-i18n": "11.4.10"` | frontend/package.json | none |
| Vite | 8.2.2 | `"vite": "8.2.2"` | frontend/package.json | none |
| @vitejs/plugin-vue | 6.0.8 | `"@vitejs/plugin-vue": "6.0.8"` | frontend/package.json | none |
| Vitest | 5.0.0 | `"vitest": "5.0.0"` | frontend/package.json | none |
| @vue/test-utils | 2.5.0 | `"@vue/test-utils": "2.5.0"` | frontend/package.json | none |
| vue-tsc | 3.3.11 | `"vue-tsc": "3.3.11"` | frontend/package.json | none |
| TypeScript | 6.0.3 | `"typescript": "6.0.3"` | frontend/package.json | none |
| Prettier | 3.8.4 | `"prettier": "3.8.4"` | frontend/package.json | none |
| Playwright (e2e, existing) | 1.62.1 | not re-verified here (not a pom.xml/package.json dependency of this checkout's backend/frontend; owned by `tests/e2e/package.json`, out of this dimension's file scope) | — | not checked |

**PASS — zero version drift found** across every pinned dependency the plan's Target stack table
and Technology decisions table name that lives in `backend/pom.xml` or `frontend/package.json`
(including BOM-managed transitive pins verified against the resolved jars in `~/.m2`).

Extra frontend devDependencies not named in the plan's table (`@eslint/js`, `eslint`,
`eslint-plugin-vue`, `globals`, `jsdom`, `typescript-eslint`, `@types/node`) are lint/test tooling
required by the architecture verifier's own "ESLint boundary rules" contract row and the frontend
unit-test harness — not a drift from a pin the plan made, since the plan never pinned a specific
ESLint toolchain version.

## 5. No `Intl` outside `format.ts`

Covered by `architecture/rules-proof.md` row 7. Additional read-only confirmation: `grep -rn
'Intl\.' frontend/src --include='*.ts' --include='*.vue'` → no matches anywhere (not even in
`format.ts`, which does not exist yet on this SHA — see the finding in row 7). `grep -rn
'toLocaleString\|toFixed' frontend/src` → one match, a **comment** in `FilterChip.vue` describing
the ban, not a usage. **PASS.**

## 6. `/accounts/` literal only in `EventStreamTopic`

Covered by `architecture/rules-proof.md` row 5, both the ArchUnit source-scan test
(`accountsTopicLiteralExistsOnlyInEventStreamTopic`, baseline green, probe-proven) and the
frontend ESLint `no-restricted-syntax` rule (probe-proven). **PASS.**

## 7. `event-row` only in the three allowed files

Covered by `architecture/rules-proof.md` row 6. Read-only confirmation: `grep -rln
'class="event-row"' frontend/src --include='*.vue'` returns exactly `EventRow.vue`,
`ListCard.vue`, `RecentImports.vue` — the same three files the `eslint.config.js` overrides name.
**PASS.**

## 8. i18n PL-first with EN present for every message key

Script: a Node ESM script (Node 26's built-in TypeScript type-stripping, no build step) imported
`frontend/src/i18n/pl.ts`/`en.ts` plus every `frontend/src/i18n/messages/*.{pl,en}.ts` pair,
replicated the app's own merge order (`src/i18n/index.ts`'s `import.meta.glob` +
`Object.assign`), flattened both catalogues to dotted keys, and diffed the key sets — both at the
whole-catalogue level and per journey-file pair.

```
PL total flattened keys: 427
EN total flattened keys: 427
Keys only in PL (missing EN): 0
Keys only in EN (missing PL): 0

Per-journey-file key parity:
  automations: pl=14 en=14 -> PARITY
  campaigns:   pl=30 en=30 -> PARITY
  customers:   pl=19 en=19 -> PARITY
  dashboard:   pl=20 en=20 -> PARITY
  events:      pl=14 en=14 -> PARITY
  feeds:       pl=13 en=13 -> PARITY
  import:      pl=91 en=91 -> PARITY
  landing:     pl=10 en=10 -> PARITY
  popups:      pl=38 en=38 -> PARITY
  settings:    pl=136 en=136 -> PARITY
```

Verified the merge is collision-safe: every journey message file's single top-level key
(`automations`, `campaigns`, `customers`, `dashboard`, `events`, `feeds`, `import`,
`landingPage`, `popups`, `settings`) is unique, so the app's shallow `Object.assign` per file
never silently drops sibling keys the way a shallow merge would if two files shared a top-level
namespace. `DEFAULT_LOCALE = "pl"` and `fallbackLocale: DEFAULT_LOCALE` in `src/i18n/index.ts`
confirm PL-first/EN-toggle. **PASS — 427/427 keys present in both locales, zero mismatches.**

## 9. Protected old-stack paths unmodified since 5b806ac

```
$ git diff --stat 5b806ac..HEAD -- src templates assets translations migrations \
    tests/e2e/specs composer.json composer.lock compose.yaml compose.override.yaml \
    compose.prod.yaml Dockerfile frankenphp config public
 compose.yaml | 3 +++
 1 file changed, 3 insertions(+)

$ git diff 5b806ac..HEAD -- compose.yaml
+    # Without a restart policy the database stayed down after a host-level stop
+    # while php/worker (unless-stopped) came back and crash-looped waiting for it.
+    restart: unless-stopped

$ git log --format='%H %ci %s' 5b806ac..HEAD -- compose.yaml
33b3f8de3585f12e776e7dbd9c439cac233d8ceb 2026-09-08 14:02:48 +0000 fix: restart the database
container automatically and approve the migration plan
```

One file changed: `compose.yaml` gained a `restart: unless-stopped` policy on the `database`
service. This is the operator's fix for the R17 production outage recorded in
`context/map/risks-and-unknowns.md` ("Production was down 2026-09-07 11:00 UTC → 2026-09-08 ...
no restart policy; repaired, policy added"), bundled into the **same commit that approved the
migration plan** (`33b3f8d`, 2026-09-08 14:02:48Z — before the run's `2026-09-08T141500Z` start
timestamp). It predates every journey worker's work and is not something any wave touched; the
global constraint ("read-only for every journey worker") is about journey workers during
execution, and no journey commit touches this path. All other protected paths (`src`, `templates`,
`assets`, `translations`, `migrations`, `tests/e2e/specs`, `composer.json`, `composer.lock`,
`compose.override.yaml`, `compose.prod.yaml`, `Dockerfile`, `frankenphp`, `config`, `public`) show
zero diff. **PASS with one pre-run, out-of-band, already-documented exception** (not a regression
introduced by the migration wave).

## Disk hygiene

`backend/target` (2.4M) and `frontend/node_modules` (161M) were deleted immediately after their
respective probe sequences finished; `git status --porcelain` is empty in the checkout at the end
of this run.
