# Architecture verifier — negative-fires probes (round 2, final cohort)

All probes were transient files/edits created in the checkout `/home/muszkin/work/kivvi-click-wt/verify-final-architecture`,
run once to confirm the enforcing gate fails, then deleted. `git status --porcelain` was empty
before the first probe, after every probe's cleanup, and at the end of the run.

## rules-translated.md row 1 — Static typing gate

- Backend (`-Xlint:all -Werror`, maven-compiler-plugin): added
  `backend/src/main/java/click/kivvi/web/_verifierprobe/ProbeRawType.java` using a raw `List`
  type. `cd backend && ./mvnw -q -o compile` → `[ERROR] COMPILATION ERROR` /
  `warnings found and -Werror specified`. Deleted; `mvnw -q -o compile` on the real tree
  succeeds as part of every ArchitectureTest run below.
- Frontend (`vue-tsc --noEmit` strict): added `frontend/src/_verifier_probe_type.ts` assigning a
  `string` to a `number`. `npx vue-tsc --noEmit` → exit 2,
  `error TS2322: Type 'string' is not assignable to type 'number'.` Deleted; clean run recorded
  in `frontend-typecheck.log` (no output = clean, exit 0).

## rules-translated.md row 2 — Formatting

- Backend (Spotless/google-java-format, bound to the `verify` phase — see `backend/pom.xml`
  `check-formatting` execution): added a badly-indented `ProbeBadFormat.java`.
  `./mvnw -q -o spotless:check` → exit 1, diff printed, `Run 'mvn spotless:apply' to fix these
  violations.` Deleted; clean run recorded in `backend-spotless-check.log`.
- Frontend (Prettier): wrote `frontend/src/_verifier_probe_prettier.ts` with mis-spaced tokens.
  `npx prettier --check src/_verifier_probe_prettier.ts` → exit 1, `[warn] Code style issues
  found`. Deleted; clean run recorded in `frontend-format-check.log`.

## rules-translated.md row 3 — Test policy fail-on-warning

- Backend (`FailOnWarnLogExtension`, auto-registered via
  `src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` +
  `junit.jupiter.extensions.autodetection.enabled=true`): ran the shipped
  `FailOnWarnLogExtensionTest` (7/7 pass, proves the detection logic reports WARN/ERROR events
  and ignores INFO/DEBUG and known JVM noise). Additionally added a throwaway
  `backend/src/test/java/click/kivvi/_verifierprobe/ProbeFailOnWarnEndToEndTest.java` that calls
  `LOG.warn(...)` inside a real `@Test`. `./mvnw -q -o test -Dtest=ProbeFailOnWarnEndToEndTest` →
  `Tests run: 1, Failures: 1`, `AssertionError: click.kivvi.* logged at WARN or above during
  deliberateWarnShouldFailThisTest() (fail-on-warning test policy, architecture/rules-translated.md
  row 3)`. Deleted.
- Frontend (`frontend/test/setup.ts`): added a throwaway spec mounting a component with a missing
  required prop (`Vue warning: Missing required prop`) — `npx vitest run <spec>` → 1 test failed,
  `Error: Test policy: console.warn/console.error/Vue warning logged during the test
  (fail-on-warning, architecture/rules-translated.md row 3)`. A first probe using a bare
  `console.warn(...)` called directly from the spec file passed (as designed —
  `originatesFromFirstPartySource` only fires for a call stack touching `/src/`, and the Vue
  runtime-warning path is unconditional), which is why the second probe used a real component
  mount instead. Also probed the "fails on unhandled errors" half of the same row (Vitest,
  `dangerouslyIgnoreUnhandledErrors: false` in `vite.config.ts`): a throwaway spec doing
  `Promise.reject(new Error(...))` — the individual test itself reports "passed" (the rejection
  surfaces asynchronously after the test body returns) but the process exit code is 1
  (`Errors 1 error`), which is what the `npm run test -- --run` CI gate checks. Deleted both.

## rules-translated.md row 4 — Layering convention (ArchUnit)

- `domain` → `web`: added `ProbeDomainClass` in `domain._verifierprobe` with a constructor
  parameter of type `web.CollectController`. `ArchitectureTest#domainDoesNotDependOnWeb` →
  1 failure, violation text names the constructor and the dependency. Deleted.
- `infrastructure` accessed from outside `infrastructure`/`application`: added `ProbeWebClass` in
  `web._verifierprobe` that *calls* `EventDedupStore.claim(...)` (a mere constructor-parameter
  reference, tried first, did not trip `onlyBeAccessed()` — ArchUnit's "accessed" condition needs
  an actual field/method/constructor call, not just a type reference in a signature; corrected the
  probe to add a real method call). `ArchitectureTest#infrastructureIsOnlyUsedFromApplication` →
  1 failure, `Method ProbeWebClass.probe() calls method EventDedupStore.claim(...)`. Deleted.
- No cycle among `{domain, application, infrastructure, web}`: added `ProbeCycle` in
  `domain._verifierprobe` depending on `application.CustomersViewService` (application already
  depends on domain via `Format`), closing a 2-node cycle without touching web or infrastructure
  access. `ArchitectureTest#topLevelPackagesFormNoCycle` → 1 failure, ArchUnit reports multiple
  detected cycles including `application -> infrastructure -> fixtures -> domain -> application`
  driven by the probe edge. Deleted.

## rules-translated.md row 5 — Mercure topic built only server-side

- Backend (`ArchitectureTest#onlyTrackingApplicationAndMercureInfrastructureDependOnThePublisher`):
  added `ProbeMercureCaller` in `web._verifierprobe` calling
  `MercurePublisher.publish("/accounts/1/events", "{}")`. → 1 failure, `Method
  ProbeMercureCaller.probe() calls method MercurePublisher.publish(...)`. Deleted.
- Backend source-scan (`ArchitectureTest#accountsTopicLiteralExistsOnlyInEventStreamTopic`): added
  `ProbeTopicLeak.java` with a `"/accounts/1/events"` string literal outside
  `domain/tracking/EventStreamTopic.java`. → `AssertionFailedError`, actual file list contains
  both `EventStreamTopic.java` and the probe file, expected only the former. Deleted.
- Frontend (`eslint.config.js` `no-restricted-syntax` `/accounts\//` literal/template-element
  selectors, scoped to `src/**/*.{ts,vue}`): added `frontend/src/_verifier_probe_topic.ts` with
  `"/accounts/" + "123" + "/events"`. `npx eslint` → 1 error, `Mercure topics are built
  server-side only (domain/tracking/EventStreamTopic.java); read the topic from the API payload
  instead of composing it here`. Deleted. Confirmed no `/accounts/` literal exists anywhere in
  the real `frontend/src` (`grep -rl "/accounts/" src --include=*.ts --include=*.vue` → no
  matches).

## rules-translated.md row 6 — Row markup in one place

- Frontend (`vue/no-restricted-class` on `event-row`, exempting only `EventRow.vue`,
  `ListCard.vue`, `RecentImports.vue`): added
  `frontend/src/components/molecules/_VerifierProbeEventRow.vue` with `class="event-row"`.
  `npx eslint` → 1 error, `'event-row' class is not allowed`. Deleted. Confirmed
  `grep -rl "event-row" src --include=*.vue` on the real tree returns exactly the three allowed
  files, nothing else.

## rules-translated.md row 7 — Formatting decided server-side (Intl only in format.ts)

- Frontend: added `frontend/src/_verifier_probe_intl.ts` calling
  `Intl.NumberFormat("pl-PL").format(n)`. `npx eslint` → 2 errors,
  `no-restricted-globals` (`Unexpected use of 'Intl'`) and `no-restricted-syntax`
  (`numbers are formatted server-side (domain/Format.java)`). Deleted. Note: `src/format.ts`
  itself does not exist yet on this SHA (no client-side formatting has been introduced by any
  journey so far) — consistent with `domain/Format.java` still being the sole formatter and with
  the ESLint override block that pre-emptively exempts that path once created.

## rules-translated.md row 8 — Storybook dev-only

- No `stories/` directory or storybook tooling exists in `frontend/` on this SHA — matches
  DEV-10 ("storybook not reproduced; out of scope, recorded for completeness only" —
  `tools/migration-verify/deviations.json`). No violation is possible to probe; row is
  out-of-scope-for-parity per the plan's own Wave column, not a gap.
