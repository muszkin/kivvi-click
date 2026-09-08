# Rules translated for the target stack (Spring Boot 4.1.x + Vue 3 SPA)

The source stack has no enforced boundary rule, so every row below is **introduced** by the migration plan and proven by the `architecture` verifier. No source rule lacks an equivalent, so no deviation is needed for this file.

| Source rule / convention | Target rule | Tooling (introduced) | Wave |
| --- | --- | --- | --- |
| Static typing gate (PHPStan 5) | Compile with `-Werror`-equivalent lint: Error Prone or `-Xlint:all` warnings as errors; `vue-tsc --noEmit` strict on the SPA | Maven compiler config; `vue-tsc` | wave-0 |
| Formatting `@Symfony` | Spotless with google-java-format on backend; Prettier on SPA (existing config) | spotless-maven-plugin; prettier 3.8.x | wave-0 |
| Test policy fail-on-warning | JUnit run with `-Djunit.jupiter.execution.parallel` off and logging at WARN treated as failure via Logback test appender assertion; Vitest `--reporter` fails on unhandled errors | JUnit 6 / Vitest 5 | wave-0 |
| Layering convention (controller → application → domain → infrastructure) | ArchUnit: `..web..` may not be accessed by `..domain..`; `..infrastructure..` only from `..application..`; no cycles between top-level packages | ArchUnit 1.5.0 `@AnalyzeClasses` test | wave-0 |
| Mercure topic built only server-side | ArchUnit: only `..tracking.stream..` may reference the `MercurePublisher`; SPA never composes topic strings (ESLint `no-restricted-syntax` on `/accounts/` literals) | ArchUnit; eslint | wave-2 |
| Row markup in one place | SPA: `EventRow.vue` is the only component rendering `.event-row`; backend publishes JSON event, not HTML (see DEV-3) | ESLint rule `vue/no-restricted-class` for `event-row` outside `EventRow.vue` | wave-2 |
| Formatting decided server-side (`Panel\Format`) | SPA `format.ts` module is the only importer of `Intl.NumberFormat`; ESLint `no-restricted-imports` | eslint | wave-1 |
| Storybook dev-only | Vue storybook (if any) lives outside the production bundle: Vite `build` excludes `stories/`; verified by bundle manifest | Vite config | Out of scope for parity |
