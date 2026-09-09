# Architecture verifier — rule-by-rule proof (wave FINAL, all journeys, round 1)

Wave SHA: `7e6a66a77d4f97ecd2aaea8ad2eb3b6c66015194`
Checkout: `/home/muszkin/work/kivvi-click-wt/verify-final-architecture` (detached, untouched — every
edit below was a throwaway probe, deleted immediately after capturing the failure; `git status
--porcelain` was verified empty after each probe and again at the end of the run).

Environment: `JAVA_HOME=/home/muszkin/.cache/kivvi-toolchains/jdk-25`; `java -version` →
`openjdk 25.0.4.1 2026-08-18 LTS, Temurin-25.0.4.1+1` (matches plan's pinned Temurin
25.0.4.1+1); `./mvnw -v` → Apache Maven 3.9.11 (matches plan's "Maven 3.9 wrapper", wrapper
config `apache-maven-3.9.11`); `node --version` → v26.8.1.

Method: for every row of `architecture/rules-translated.md` (oracle path
`context/migration-oracle/symfony-to-spring-vue/architecture/rules-translated.md`), the tooling
is named, a baseline (rule-compliant) run is shown green, then a throwaway probe that breaks the
rule is added, the exact gate command is re-run and shown red (the rule's own error message
quoted), and the probe is deleted and `git status --porcelain` re-confirmed empty.

## Row 1 — Static typing gate (PHPStan 5) → javac `-Werror -Xlint:all` (backend) / `vue-tsc --noEmit` strict (frontend)

**Backend.** Gate: `maven-compiler-plugin` in `backend/pom.xml` (`<arg>-Xlint:all</arg>
<arg>-Werror</arg>`), run via `cd backend && ./mvnw -q -o compile`.

- Baseline: part of every `mvnw test` run below (all exit 0).
- Negative probe: added `backend/src/main/java/click/kivvi/domain/ProbeCompilerWarning.java`
  calling the deprecated `new Integer(5)` constructor.
- `cd backend && ./mvnw -q -o compile` → **exit 1**:
  `[ERROR] .../ProbeCompilerWarning.java: warnings found and -Werror specified`
- Probe deleted; `git status --porcelain` empty.

**Frontend.** Gate: `frontend/package.json` script `"typecheck": "vue-tsc --noEmit"`, run via
`cd frontend && npx vue-tsc --noEmit`.

- Baseline: `cd frontend && npx vue-tsc --noEmit` → **exit 0** (after `npm ci`, 259 packages).
- Negative probe: added `frontend/src/ProbeTypecheck.ts` with `const leak: number = "not a
  number";`.
- `cd frontend && npx vue-tsc --noEmit` → **exit 2**:
  `src/ProbeTypecheck.ts(2,7): error TS2322: Type 'string' is not assignable to type 'number'.`
- Probe deleted; re-run → exit 0 again (restored).

## Row 2 — Formatting `@Symfony` → Spotless/google-java-format (backend) / Prettier (frontend)

**Backend.** Gate: `spotless-maven-plugin` bound to the `verify` phase (`check-formatting`
execution), run directly via `cd backend && ./mvnw -q -o spotless:check`.

- Negative probe: temporarily mis-indented one line of
  `backend/src/main/java/click/kivvi/domain/SupportedLocale.java` (`EN("en");` indented 8 spaces
  instead of 2).
- `cd backend && ./mvnw -q -o spotless:check` → **exit 1**:
  ```
  [ERROR] Failed to execute goal com.diffplug.spotless:spotless-maven-plugin:3.10.2:check ...
  [ERROR]     src/main/java/click/kivvi/domain/SupportedLocale.java
  [ERROR]         @@ -10,7 +10,7 @@
  [ERROR]         -········EN("en");
  [ERROR]         +··EN("en");
  ```
- File restored from a pre-edit copy; `git status --porcelain` empty.

**Frontend.** Gate: `frontend/package.json` script `"format:check": "prettier --check ."`, run
via `cd frontend && npx prettier --check <file>`.

- Negative probe: temporarily changed the import line of `frontend/src/router/localeHref.ts`
  from double quotes + semicolon to single quotes + no semicolon.
- `cd frontend && npx prettier --check src/router/localeHref.ts` → **exit 1**:
  `[warn] src/router/localeHref.ts` / `[warn] Code style issues found ...`
- File restored from a pre-edit copy; `git status --porcelain` empty.

## Row 3 — Test policy fail-on-warning (PHPUnit `failOnWarning`/`failOnNotice`, restricted to `src`)

**Backend.** Gate: `FailOnWarnLogExtension` (`backend/src/test/java/click/kivvi/testsupport/`),
auto-registered for every test via
`src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` +
`junit.jupiter.extensions.autodetection.enabled=true`.

- Existing codified proof: `FailOnWarnLogExtensionTest` (7 tests) exercises the detection logic
  directly — `warnEventIsReported`, `errorEventIsReported` prove a WARN/ERROR event is reported;
  `infoAndDebugAreIgnored`, `noEventsIsNotReported` prove the non-failing paths; stderr noise
  allowlist covered by 3 more tests. All 7 pass as part of the normal suite.
- End-to-end negative probe (this run): added
  `backend/src/test/java/click/kivvi/testsupport/ProbeFailOnWarnEndToEndTest.java`, a real test
  that logs `LOG.warn(...)` on a `click.kivvi.*` logger.
- `cd backend && ./mvnw -q -o test -Dtest=ProbeFailOnWarnEndToEndTest` → **exit 1**:
  `click.kivvi.* logged at WARN or above during ... (fail-on-warning test policy,
  architecture/rules-translated.md row 3)`
- Probe deleted; `git status --porcelain` empty.

**Frontend.** Gate: `frontend/test/setup.ts` (`console.warn`/`console.error` interception
restricted to call stacks touching `/src/`, plus an unconditional Vue `warnHandler`) +
`vite.config.ts`'s `test.dangerouslyIgnoreUnhandledErrors: false` (Vitest fails a run on any
unhandled error/rejection).

- Negative probe 1 (first-party console.warn): added `frontend/src/probeWarnHelper.ts`
  (`console.warn(...)`) called from `frontend/test/unit/probeFailOnWarn.spec.ts`.
  `cd frontend && npx vitest run test/unit/probeFailOnWarn.spec.ts` → **exit 1**: `Error: Test
  policy: console.warn/console.error/Vue warning logged during the test (fail-on-warning,
  architecture/rules-translated.md row 3)`. Both probe files deleted.
  - Control check: the same `console.warn` called directly from *inside* the `.spec.ts` file
    (not through `/src/**`) does **not** fail — the policy is deliberately restricted to
    first-party `src/` code, mirroring PHPUnit's restriction to `src`. Confirmed
    (`test/unit/probeFailOnWarn.spec.ts` calling `console.warn` inline → exit 0), then corrected
    to call through a `src/` helper as above.
- Negative probe 2 (unhandled rejection): added
  `frontend/test/unit/probeUnhandledRejection.spec.ts` with an unawaited `Promise.reject(...)`.
  `cd frontend && npx vitest run test/unit/probeUnhandledRejection.spec.ts` → **exit 1** (`Errors
  1 error`, "Unhandled Rejection" reported) even though the individual test itself is marked
  passed — the run-level exit code is what a CI gate consumes, and it is non-zero.
- Probes deleted; `git status --porcelain` empty.

## Row 4 — Layering convention (controller → application → domain → infrastructure)

Gate: `backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java`
(`com.tngtech.archunit:archunit-junit5:1.5.0`), run via `cd backend && ./mvnw -q -o test
-Dtest=ArchitectureTest`.

- Baseline: `cd backend && ./mvnw -q -o test -Dtest=ArchitectureTest` → **exit 0** (4 tests).

- **`domainDoesNotDependOnWeb`** — negative probe: added
  `backend/src/main/java/click/kivvi/domain/ProbeWebLeak.java` with a field of type
  `click.kivvi.web.dto.CampaignEmailResponse`.
  `./mvnw -q -o test -Dtest=ArchitectureTest#domainDoesNotDependOnWeb` → **exit 1**:
  `Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package
  '..domain..' should depend on classes that reside in a package '..web..'' was violated (1
  times): Field <click.kivvi.domain.ProbeWebLeak.leak> has type
  <click.kivvi.web.dto.CampaignEmailResponse>`. Probe deleted.

- **`infrastructureIsOnlyUsedFromApplication`** — negative probe: added
  `backend/src/main/java/click/kivvi/web/ProbeInfraLeak.java` whose method actually **calls**
  `new MessageSourceConfig().messageSource()` (an `infrastructure.config` class).
  `./mvnw -q -o test -Dtest=ArchitectureTest#infrastructureIsOnlyUsedFromApplication` → **exit
  1**: `Rule 'classes that reside in a package '..infrastructure..' should only be accessed by
  any package ['..infrastructure..', '..application..']' was violated (2 times)`. Probe deleted.
  - **Finding, not a violation:** a first attempt that only *declared a field* of the
    infrastructure type (no constructor/method call) did **not** fail — ArchUnit's
    `onlyBeAccessed()` predicate counts field/method/constructor access, not a bare type
    reference such as a field declaration. `dependOnClassesThat()` (used by row 1's rule above)
    does count type references; `onlyBeAccessed()` does not. This is expected ArchUnit semantics,
    not a gap in the test, but worth recording: a `web`/`domain` class that merely *imports and
    declares a field of* an infrastructure type without calling it would slip past this
    particular rule. No such declaration exists anywhere in the current codebase (see the
    baseline pass), so this is a latent gap, not an active violation.

- **`topLevelPackagesFormNoCycle`** — negative probe: added
  `backend/src/main/java/click/kivvi/domain/ProbeCycleLeak.java` calling
  `click.kivvi.fixtures.AutomationsFixtures.activeForCustomer()` (fixtures already depends on
  domain via `Format`/`Identity`, so this closes a 2-node cycle).
  `./mvnw -q -o test -Dtest=ArchitectureTest#topLevelPackagesFormNoCycle` → **exit 1**: `Rule
  'slices matching 'click.kivvi.(*)..' should be free of cycles' was violated (1 times): Cycle
  detected: Slice domain -> Slice fixtures -> Slice domain`. Probe deleted.

- Full `ArchitectureTest` re-run after all three probes removed → **exit 0** (restored).

## Row 5 — Mercure topic built only server-side

**Backend** (ArchUnit, same file/command as row 4):

- **`onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`** — negative probe:
  added `backend/src/main/java/click/kivvi/web/ProbeMercureLeak.java` calling
  `publisher.publish("/accounts/1/events", "{}")` on an injected `MercurePublisher`.
  `./mvnw -q -o test -Dtest=ArchitectureTest#onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`
  → **exit 1**: `Rule 'classes that have simple name 'MercurePublisher' or have simple name
  'HttpMercurePublisher' should only be accessed by any package
  ['click.kivvi.application.tracking..', 'click.kivvi.infrastructure.mercure..']' was violated (1
  times)`. Probe deleted.

- **`accountsTopicLiteralExistsOnlyInEventStreamTopic`** — negative probe: added
  `backend/src/main/java/click/kivvi/web/ProbeAccountsLiteral.java` with
  `static final String LEAK = "/accounts/1/events";`.
  `./mvnw -q -o test -Dtest=ArchitectureTest#accountsTopicLiteralExistsOnlyInEventStreamTopic` →
  **exit 1**: `Expecting actual: [".../EventStreamTopic.java", ".../ProbeAccountsLiteral.java"]
  to contain exactly (and in same order): [".../EventStreamTopic.java"]`. Probe deleted.

**Frontend** (ESLint `no-restricted-syntax`, `frontend/eslint.config.js`, block scoped to
`src/**/*.{ts,vue}` excluding `src/format.ts`):

- Negative probe: added `frontend/src/ProbeArchitecture.ts` containing
  `const topic = "/accounts/1/events";` (plus the row-7 and row-6 violations below, all in one
  file — see combined run).
  `cd frontend && npx eslint src/ProbeArchitecture.ts` → **exit 1**, includes: `Mercure topics
  are built server-side only (domain/tracking/EventStreamTopic.java); read the topic from the
  API payload instead of composing it here  no-restricted-syntax`. Probe deleted.

## Row 6 — Row markup in one place (`.event-row` only in `EventRow.vue`)

Gate: ESLint `vue/no-restricted-class` (template-level, `files: ["src/**/*.vue"]`, `ignores:
["src/components/molecules/EventRow.vue"]`, with governed `"off"` overrides for
`ListCard.vue`/`RecentImports.vue`) + `no-restricted-syntax` `Literal`/`TemplateElement`
selectors matching `/event-row/` (script-level, closes the `:class="constant"` bypass a prior
repair found in `RecentImports.vue`).

- Negative probe (template): added `frontend/src/components/molecules/ProbeEventRow.vue` with
  `<div class="event-row">probe</div>`.
  `cd frontend && npx eslint src/components/molecules/ProbeEventRow.vue` → **exit 1**: `'event-
  row' class is not allowed  vue/no-restricted-class`. Probe deleted.
- Negative probe (script-side string, the `RecentImports.vue` bypass class): `const rowClass =
  "event-row";` inside `frontend/src/ProbeArchitecture.ts` (see combined run in row 7) → fired
  `no-restricted-syntax` with the message naming the three allowed files. Probe deleted.
- Confirmed the three allowed files (`EventRow.vue`, `ListCard.vue`, `RecentImports.vue`) exist
  and are the only ones exempted in `eslint.config.js`; `git grep -n 'class="event-row"'
  frontend/src` (read-only, no probe) returns matches only inside those three files' templates.

## Row 7 — Formatting decided server-side (`Panel\Format` → `format.ts`, `Intl` banned elsewhere)

Gate: ESLint `no-restricted-imports` (pattern `*Intl*`), `no-restricted-globals` (`Intl`),
`no-restricted-syntax` (`Intl` member access, `.toLocaleString()`, `.toFixed()`), all in
`frontend/eslint.config.js`, exempted only for `files: ["src/format.ts"]`.

- **Finding:** `frontend/src/format.ts` does not exist yet on this SHA (`find frontend/src
  -iname format.ts` → no result) and no production file references `Intl`, `.toLocaleString()`
  or `.toFixed()` (`grep -rn` over `frontend/src` → no matches outside a single comment in
  `FilterChip.vue` that mentions the ban, not a usage). All number/money formatting currently
  happens server-side per `backend/src/main/java/click/kivvi/domain/Format.java`, consistent with
  the rule's intent — the exemption file is simply unused so far, which is correct, not a gap.
- Negative probe: `frontend/src/ProbeArchitecture.ts` —
  `import { helper } from "./IntlHelper";` (path pattern match),
  `Intl.NumberFormat("pl-PL").format(5)` (global + member-expression),
  `(5).toFixed(2)`, `(5).toLocaleString()`.
  `cd frontend && npx eslint src/ProbeArchitecture.ts` → **exit 1**, 7 errors total (this file
  combined rows 5–7's probes in one run):
  ```
  2:1   error  './IntlHelper' import is restricted ...                    no-restricted-imports
  6:23  error  Unexpected use of 'Intl'. ...                              no-restricted-globals
  6:23  error  numbers are formatted server-side ...                      no-restricted-syntax
  7:21  error  numbers are formatted server-side ...                      no-restricted-syntax
  8:23  error  numbers are formatted server-side ...                      no-restricted-syntax
  9:19  error  Mercure topics are built server-side only ...              no-restricted-syntax
  10:22 error  "event-row" is a restricted class ...                      no-restricted-syntax
  ✖ 7 problems (7 errors, 0 warnings)
  ```
- Probe deleted; `git status --porcelain` empty.

## Row 8 — Storybook dev-only (Out of scope for parity, DEV-10)

Gate (per `rules-translated.md`): Vite `build` config excludes `stories/`. Wave column says "Out
of scope for parity"; plan's DEV-10 records storybook as not reproduced at all.

- Verified (read-only, no probe needed): `find frontend -iname '*stor*' -not -path
  '*/node_modules/*'` returns no `stories/` directory and no storybook config anywhere in
  `frontend/`. `frontend/vite.config.ts`'s `build` block has no story-related include/exclude
  because there is nothing to exclude — the row is vacuously satisfied, matching DEV-10 exactly
  (no equivalent needed, no deviation needed, because there is no storybook surface to police).

## Summary

All 8 rows of `rules-translated.md` have a named tool and a proven-firing negative. One
ArchUnit-semantics nuance is recorded under row 4 (`onlyBeAccessed()` vs `dependOnClassesThat()`)
as a latent-gap finding, not an active violation — no code in the current tree exploits it.
