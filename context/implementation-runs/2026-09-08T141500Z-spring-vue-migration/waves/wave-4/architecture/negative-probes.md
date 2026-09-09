# Negative-fire probes — architecture dimension, wave-4 round 2

All probes below were throwaway files created in the detached checkout
`/home/muszkin/work/kivvi-click-wt/verify-wave-4-architecture` (SHA `71d884d2ed96a7a7caad18147e76570c9e113d4d`),
run once to confirm the rule fires, then deleted. After every probe `git status --porcelain`
was empty. No tracked file was permanently edited.

## rules-translated.md coverage

| Row | Target rule | Tooling | Negative probe | Result |
| --- | --- | --- | --- | --- |
| Static typing gate (PHPStan 5) | `-Xlint:all -Werror` (backend); `vue-tsc --noEmit` strict (SPA) | `backend/pom.xml` compiler plugin; `frontend/package.json` `typecheck` | see "Compiler -Werror" and "vue-tsc" below | fires |
| Formatting `@Symfony` | Spotless (google-java-format) backend; Prettier frontend | `backend/pom.xml` spotless-maven-plugin bound to `verify`; `frontend/package.json` `format:check` | see "Spotless" and "Prettier" below | fires |
| Test policy fail-on-warning | `FailOnWarnLogExtension` (JUnit, auto-registered); Vitest `dangerouslyIgnoreUnhandledErrors: false` | `backend/src/test/java/click/kivvi/testsupport/FailOnWarnLogExtension.java` + its own `FailOnWarnLogExtensionTest` | `FailOnWarnLogExtensionTest` is itself a self-contained negative proof (asserts a deliberate WARN/ERROR event is reported) — re-ran it, 0 failures, confirming the detection logic still reports positively | fires (proof already in suite; re-run green) |
| Layering convention | ArchUnit `ArchitectureTest` (`domainDoesNotDependOnWeb`, `infrastructureIsOnlyUsedFromApplication`, `topLevelPackagesFormNoCycle`) | `ArchitectureTest.java` | see "ArchUnit layering" below | fires |
| Mercure topic built only server-side | ArchUnit (`onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`, `accountsTopicLiteralExistsOnlyInEventStreamTopic`) + ESLint `no-restricted-syntax` on `/accounts/` literals | `ArchitectureTest.java`; `frontend/eslint.config.js` | see "ArchUnit Mercure publisher", "ArchUnit accounts-topic literal", "ESLint Mercure topic literal" below | fires |
| Row markup in one place (`event-row`) | ESLint `vue/no-restricted-class` (template) + `no-restricted-syntax` (script-side literal, repair-3 hardening) | `frontend/eslint.config.js` | see "event-row rule" section below — (a)/(b)/(c)/(d) | fires; 3-file override is file-scoped |
| Formatting decided server-side (`Panel\Format`) | ESLint `no-restricted-imports`/`no-restricted-globals`/`no-restricted-syntax` on `Intl` | `frontend/eslint.config.js` | see "ESLint Intl ban" below | fires |
| Storybook dev-only | Vite build excludes `stories/` | — | out of scope for parity (rules-translated.md marks this row "Out of scope for parity"); not exercised | n/a |

## ArchUnit layering / boundary rules

Four throwaway classes were added under `backend/src/main/java/click/kivvi/`:

- `domain/ZZZProbeDomainDependsOnWeb.java` — field of type `click.kivvi.web.AutomationsController`
- `web/ZZZProbeInfraFromWeb.java` — method that *calls* `click.kivvi.infrastructure.tracking.EventDedupLedger.claim(...)`
  (a bare unused field declaration does **not** count as an ArchUnit "access" for `onlyBeAccessed()` —
  had to actually invoke the method for the rule to see the dependency)
- `web/ZZZProbeMercurePublisherFromWeb.java` — method that *calls* `click.kivvi.infrastructure.mercure.MercurePublisher.publish(...)`
- `web/ZZZProbeAccountsTopicLiteral.java` — `static final String TOPIC = "/accounts/1/events";`

Command: `cd backend && ./mvnw -q -Dmaven.repo.local=<scratch> test -Dtest=ArchitectureTest`

Baseline (no probes): `Tests run: 5, Failures: 0` — clean.

With the four probes present: `Tests run: 5, Failures: 5` — **every** ArchUnit test method failed:

- `domainDoesNotDependOnWeb` — FAILURE (caught the domain->web field)
- `topLevelPackagesFormNoCycle` — FAILURE (caught the cycle introduced by all four probes)
- `accountsTopicLiteralExistsOnlyInEventStreamTopic` — FAILURE (`containsExactly` now sees two files)
- `infrastructureIsOnlyUsedFromApplication` — FAILURE (only after the probe was changed to actually
  *call* `EventDedupLedger.claim(...)` instead of merely declaring an unused field)
- `onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher` — FAILURE (only after the
  probe was changed to actually *call* `MercurePublisher.publish(...)`)

Probes deleted; re-ran `ArchitectureTest` — `Tests run: 5, Failures: 0` again (clean), `git status --porcelain` empty.

## ArchUnit Mercure publisher / accounts-topic literal

Covered by the same run above (probes 3 and 4).

## ESLint Intl ban

Probe `frontend/src/ZZZProbeIntl.ts`:
```ts
export function probe(value: number): string {
  return new Intl.NumberFormat("pl-PL").format(value);
}
```
`npx eslint src/ZZZProbeIntl.ts` → 2 errors (`no-restricted-globals`, `no-restricted-syntax`), exit 1.
Probe deleted; `git status --porcelain` empty.

## ESLint Mercure topic literal

Probe `frontend/src/ZZZProbeTopic.ts`:
```ts
export function probe(accountId: number): string {
  return `/accounts/${accountId}/events`;
}
```
`npx eslint src/ZZZProbeTopic.ts` → 1 error (`no-restricted-syntax`, "Mercure topics are built
server-side only"), exit 1. Probe deleted; `git status --porcelain` empty.

## vue-tsc (static typing gate, SPA half)

Probe `frontend/src/ZZZProbeTypeError.ts` with a deliberate `string`/`number` mismatch.
`npx vue-tsc --noEmit -p tsconfig.json` → 2 `TS2322` errors, exit 2. Probe deleted.

## Prettier (formatting, SPA half)

Probe `frontend/src/ZZZProbeFormat.ts` with unformatted code
(`export function probe(x:number,y:number){return x+y}`).
`npx prettier --check src/ZZZProbeFormat.ts` → `[warn]` + exit 1. Probe deleted.
Baseline `npm run format:check` on the real tree: "All matched files use Prettier code style!" (clean).

## Spotless (formatting, backend half)

Probe `backend/src/main/java/click/kivvi/domain/ZZZProbeSpotless.java` with deliberately
mis-indented/mis-spaced Java. `./mvnw -q -Dmaven.repo.local=<scratch> spotless:check` → build
FAILURE naming the file and printing the google-java-format diff. Probe deleted; re-ran
`spotless:check` (part of the combined `spotless:check compile` run) — clean, exit 0.

## Compiler `-Werror -Xlint:all` (static typing gate, backend half)

Probe `backend/src/main/java/click/kivvi/domain/ZZZProbeWerror.java` — raw-typed `List`/`ArrayList`
usage (unchecked-conversion warning) with no `@SuppressWarnings`.
`./mvnw -q -Dmaven.repo.local=<scratch> compile` → `COMPILATION ERROR: … warnings found and -Werror
specified`. Probe deleted; re-ran `spotless:check compile` together — clean, exit 0.

## `event-row` rule — the round-2 focus

See `event-row-rule.md` in this directory for the full (a)/(b)/(c)/(d) breakdown.
