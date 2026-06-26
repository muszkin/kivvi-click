---
project: kivvi-click
assessed_at: 2026-06-25T21:34:41+02:00
agent_readiness: ready
context_type: brownfield
stack_components:
  language: "PHP 8.5 plus vanilla TypeScript assets"
  framework: "Symfony 8"
  build_tool: "Composer scripts, Symfony AssetMapper, Docker multi-stage build"
  test_runner: "PHPUnit 13.1"
  package_manager: "Composer plus Yarn 4"
  ci_provider: "GitHub Actions"
  deployment_target: "FrankenPHP/Caddy container via Dockerfile and compose.yaml"
gates_passed: 14
gates_failed: 0
---

## Stack Components

The backend is a PHP 8.5 Symfony 8 application. Evidence: `composer.json` requires `php >=8.5`, Symfony 8 packages, Doctrine ORM and Messenger packages, and maps `App\\` to `src/`.

The frontend is intentionally small: server-rendered Twig, CSS, and vanilla TypeScript assets. Evidence: `package.json` defines Yarn 4, `yarn format`, and `yarn typecheck`; `tsconfig.json` enforces strict TypeScript checking for `assets/**/*.ts`; `assets/global.d.ts` supports AssetMapper CSS imports.

The runtime/deployment shape is Docker-first with FrankenPHP/Caddy and PostgreSQL. Evidence: `Dockerfile` uses `dunglas/frankenphp:1-php8.5`, installs Composer and PHP extensions, and builds dev/prod targets; `compose.yaml` defines `php`, `worker`, and `database` services.

Testing is PHPUnit-based. Evidence: `composer.json` requires `phpunit/phpunit ^13.1`, `symfony/browser-kit`, and `symfony/css-selector`; tests exist under `tests/`; `phpunit.dist.xml` configures the test suite; `composer test` prepares the test database and TypeScript assets before running PHPUnit.

Formatting and static analysis are configured. Evidence: `.php-cs-fixer.dist.php` applies the Symfony rule set; `phpstan.neon.dist` configures PHPStan with the Symfony extension; `composer phpstan` warms the Symfony container and runs analysis; `yarn typecheck` validates frontend assets.

CI/CD exists with intentionally narrow scope. Evidence: `.github/workflows/docker-build.yml` builds the production Docker image on push to `main`.

Instruction coverage is strong for product and workflow context. Evidence: `AGENTS.md` and `CLAUDE.md` document product context, stack constraints, Docker-first commands, architectural constraints, and workflow conventions.

## Quality Gate Assessment

| Component | Typed | Convention | Training Data | Documented | Verdict |
|---|---|---|---|---|---|
| PHP project | yes | - | - | - | pass |
| Symfony 8 | - | yes | yes | yes | pass |
| Composer / Symfony tooling | - | yes | yes | yes | pass |
| PHPUnit | - | - | yes | yes | pass |
| Vanilla TypeScript assets | yes | yes | yes | yes | pass |
| Docker / FrankenPHP runtime | - | partial | partial | yes | pass |

Legend: `yes` = pass, `partial` = acceptable with repo instructions, `-` = not applicable.

### Gate Details

Type safety is strong enough for current scope. PHP files use strict types and PHPStan with PHPStan Symfony is configured through `phpstan.neon.dist`. Frontend TypeScript now has a strict `tsconfig.json` and a passing `yarn typecheck` command.

Framework conventions are strong. Symfony gives predictable locations for controllers, entities, config, templates, translations, services, console commands, Messenger handlers, and tests. The repository already follows this shape with `src/Controller`, `src/Message`, `src/MessageHandler`, `config/packages`, `templates`, `translations`, and `tests`.

Training-data fit is strong for the core application. Symfony, Doctrine, Twig, Composer, PHPUnit, Docker, and vanilla TypeScript are mainstream within their ecosystems. FrankenPHP is newer than traditional PHP-FPM/Apache/Nginx deployments, so agents should continue following the Docker-first instructions in `AGENTS.md` and `CLAUDE.md`.

Documentation quality is strong. Symfony, Doctrine, Twig, Composer, PHPUnit, Docker, Caddy, and FrankenPHP have official documentation. Version-specific dependencies in `composer.json` and the Docker image tag provide enough anchors for agents to look up current docs when needed.

## Remaining Notes

The current GitHub Actions workflow only builds the production image after push to `main`. That matches the requested scope. When the project moves closer to collaboration through pull requests, CI should be expanded to run Composer validation, Composer audit, PHPStan, PHPUnit, Yarn install, TypeScript type checking, Yarn audit, and formatting checks.

FrankenPHP remains a good runtime fit for the project, but it is newer and less common in training data than classic PHP-FPM setups. Keep the Docker-first rule explicit and do not introduce Redis, RabbitMQ, Memcached, PHP-FPM/Nginx, or a separate WebSocket service unless the product architecture is deliberately changed.

## Recommended Instruction File Additions

The repository instruction files now cover the important operating rules. Keep these expectations current:

```markdown
## Type Safety

- Use `declare(strict_types=1);` in every new PHP file.
- Add native PHP type declarations at every function and method boundary, including return types.
- Treat `docker compose exec php composer phpstan` and `yarn typecheck` as required before completing relevant changes.
```

```markdown
## Runtime Assumptions

- Run commands inside the Docker `php` container unless the user explicitly asks otherwise.
- Do not replace Postgres-backed sessions, cache, Messenger, or scheduling with Redis, RabbitMQ, Memcached, or another backing service.
- Do not assume PHP-FPM/Nginx. The runtime is FrankenPHP with Caddy, and Mercure is the real-time path.
```

## Summary

Overall verdict: ready.

The stack is a strong fit for agent-assisted development because Symfony, Doctrine, Twig, Composer, PHPUnit, TypeScript, and Docker give agents recognizable conventions, mainstream patterns, explicit validation commands, and good documentation. The previous validation gaps are now closed for the current scope.
