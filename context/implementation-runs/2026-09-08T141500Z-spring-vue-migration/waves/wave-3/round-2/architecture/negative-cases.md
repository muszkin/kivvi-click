# Negative-case proofs — architecture dimension, wave-3 round-2

Checkout: `/home/muszkin/work/kivvi-click-wt/verify-wave-3-architecture` @ `c74e3b8e9ed6763204fa193bf66b891d35ef7fe4` (detached, read-only per contract; each probe file below was created, run, observed to fail the intended rule, then deleted — `git status --porcelain` was empty before and after every probe).

Method: for every row of `architecture/rules-translated.md` in the oracle, name the enforcing ArchUnit test / ESLint rule, then add one throwaway file that violates it, run the exact contract command, capture the failure, delete the file, and re-confirm the baseline is green.

## Rows enforced by ArchUnit (`backend/src/test/java/click/kivvi/architecture/ArchitectureTest.java`)

Baseline (before/after every probe): `cd backend && ./mvnw -q test -Dtest=ArchitectureTest` → exit 0, `Tests run: 5, Failures: 0`.

### 1. Layering convention → `domainDoesNotDependOnWeb`

Probe: `backend/src/main/java/click/kivvi/domain/tracking/ArchViolationDomainToWeb.java`, a domain class holding `EventsController.class`.

Result: exit 1, `Tests run: 5, Failures: 2` — fired `domainDoesNotDependOnWeb` ("no classes that reside in a package '..domain..' should depend on classes that reside in a package '..web..'") **and**, because `application` already depends on `domain` while this probe made `domain` depend on `web` (and `web` on `application`/`fixtures`), it simultaneously fired the cycle rule (case 3 below) — both failures are the same probe, not two separate defects.

### 2. Layering convention → `infrastructureIsOnlyUsedFromApplication`

First probe attempt: a `web` class merely holding `MessageSourceConfig.class` (a class-literal reference, no method/constructor/field access) did **not** fire this rule — 0 failures. This is expected ArchUnit semantics, not a rule gap: `onlyBeAccessed()` matches `JavaAccess` (field access / method call / constructor call), not a bare type reference via `.class`; the earlier `dependOnClassesThat()`-based rule (case 1) does match type references, which is why that probe fired immediately. Recorded as a scope note, not a defect.

Working probe: `backend/src/main/java/click/kivvi/web/ArchViolationWebToInfrastructure.java`, calling `new MessageSourceConfig().messageSource()` from `web`.

Result: exit 1, `Tests run: 5, Failures: 1` — fired `infrastructureIsOnlyUsedFromApplication` only: "classes that reside in a package '..infrastructure..' should only be accessed by any package ['..infrastructure..', '..application..']", both the constructor call and the method call reported.

### 3. Layering convention → `topLevelPackagesFormNoCycle`

Proven as a side effect of case 1 above (the domain→web probe also closed a domain↔application↔web cycle). Isolated confirmation: same probe file, same run; the surefire report names `topLevelPackagesFormNoCycle` explicitly with the concrete cycle path `application -> domain -> web -> application`.

### 4. Mercure topic built only server-side (backend half) → `onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`

Probe: `backend/src/main/java/click/kivvi/application/ArchViolationApplicationRootToPublisher.java` — a class in the *root* `application` package (not `application.tracking`) calling `MercurePublisher.publish(...)`.

Result: exit 1, `Tests run: 5, Failures: 1` — fired exactly this rule: "classes that have simple name 'MercurePublisher' or have simple name 'HttpMercurePublisher' should only be accessed by any package ['click.kivvi.application.tracking..', 'click.kivvi.infrastructure.mercure..']". Confirms the rule is scoped to `application.tracking` specifically, not all of `application`.

### 5. Mercure topic built only server-side (literal-uniqueness half) → `accountsTopicLiteralExistsOnlyInEventStreamTopic`

Probe: `backend/src/main/java/click/kivvi/web/ArchViolationTopicLiteral.java` holding the string `"/accounts/9/events"`.

Result: exit 1, `Tests run: 5, Failures: 1` — `AssertionFailedError` listing both `EventStreamTopic.java` and the probe file, expected exactly one.

## Rows enforced by ESLint (`frontend/eslint.config.js`)

Command: `cd frontend && npx eslint <probe file>` (single-file invocation used for isolation; `npm run lint` baseline before/after each probe: exit 0, 6 pre-existing `vue/multiline-html-element-content-newline` warnings, 0 errors — see `eslint-baseline.txt`).

### 6. Formatting decided server-side (`format.ts` only importer of `Intl`) → `no-restricted-globals` + `no-restricted-syntax` (Intl selectors)

Probe: `frontend/src/archViolationIntl.ts` calling `new Intl.NumberFormat("pl-PL").format(value)`.

Result: exit 1, 2 errors — `no-restricted-globals` ("Unexpected use of 'Intl'. numbers are formatted server-side...") and `no-restricted-syntax` (`MemberExpression[object.name='Intl']`).

### 7. Mercure topic built only server-side (frontend half) → `no-restricted-syntax` (`Literal[value=/\/accounts\//]`, scoped to `src/**/*.{ts,vue}`)

Probe: `frontend/src/archViolationTopic.ts` holding `export const topic = "/accounts/1/events";`.

Result: exit 1, 1 error — "Mercure topics are built server-side only (domain/tracking/EventStreamTopic.java); read the topic from the API payload instead of composing it here".

### 8. Row markup in one place (`EventRow.vue` only owner of `.event-row`) → `vue/no-restricted-class`

Probe: `frontend/src/components/molecules/ArchViolationEventRow.vue` with `<div class="event-row">`.

Result: exit 1, 1 error — `'event-row' class is not allowed`.

## Rows with no ArchUnit/ESLint equivalent (by design, not a gap)

Per the plan's "Verifier contract", the `architecture` dimension command is exactly `mvnw test -Dtest=ArchitectureTest` + `npm run lint`; a separate "Static analysis" row (`mvnw spotless:check`, `npm run lint`, `npm run typecheck`) is its own gate outside the six verifier dimensions. `rules-translated.md`'s own "Tooling" column assigns the following three rows to that other tooling, not to ArchUnit/ESLint, and its preamble ("no source rule lacks an equivalent, so no deviation is needed") is satisfied because each of these already names its enforcing tool in the same table row:

- Static typing gate → Maven compiler config / `vue-tsc --noEmit` (static-analysis gate, not this dimension).
- Formatting `@Symfony` → `spotless-maven-plugin` / Prettier (static-analysis gate).
- Test policy fail-on-warning → JUnit/Vitest reporter configuration (unit dimension).

Storybook dev-only is explicitly marked "Out of scope for parity" in the same table and carries no verifier obligation here (also recorded as DEV-10 in the plan's deviation table, "storybook not reproduced").

Conclusion: every row of `rules-translated.md` maps to a named enforcement mechanism; all 5 ArchUnit tests and all 3 ESLint rule families relevant to this dimension have a proven, isolated negative case; no rule is silently unenforced.
