# Boundary rules enforced on the pre-migration stack

Observed on `91f8f85` (product code) / `5b806ac` (docs), 2026-09-08.

| Rule | Tooling | Evidence | Status |
| --- | --- | --- | --- |
| No dependency-boundary tool (no deptrac, no phpat, no custom PHPStan rules) | — | `composer.json` require-dev, `phpstan.neon.dist` (level 5, no custom rules) | no boundary rule exists |
| Static typing gate: PHPStan level 5 over `src` and `tests` | phpstan/phpstan 2.2.2 + phpstan-symfony | `phpstan.neon.dist`, `composer phpstan` | quality gate, not a boundary rule |
| Formatting: `@Symfony` rule set | php-cs-fixer 3.95.2 | `.php-cs-fixer.dist.php` | style gate |
| TypeScript strict (`strict`, `noUncheckedIndexedAccess`, `noImplicitReturns`) | typescript 6.0.3 `tsc --noEmit` | `tsconfig.json` | quality gate |
| Test policy: fail on deprecation/notice/warning | phpunit 13.1.10 | `phpunit.dist.xml` | quality gate |
| Convention (docblocks, not enforced): Twig never assembles Mercure topics; row markup lives in one template; formatting decided in `Panel\Format`; storybook only in dev/test | none | `src/Panel/EventStreamTopic.php`, `src/Tracking/EventIngestion.php`, `src/Panel/Format.php`, `src/Controller/StorybookController.php` | convention only |
