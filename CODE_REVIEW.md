# Code review rules — kivvi-click

Repo-local checklist, read by `om-code-review` in addition to its built-in rules. Everything here
is derived from this repository's own conventions and enforcement, not imported from elsewhere.
Findings are ranked: **blocker** stops the merge, **major** must be fixed or explicitly deferred on
the ticket, **minor** is advisory.

## Architecture

- **Layering (blocker).** `web` → `application` → `domain`; `infrastructure` is reachable only from
  `application`. `domain` must never reference `web`. Enforced by `ArchitectureTest` (ArchUnit) —
  a change that needs a new edge changes the test deliberately, never as a side effect.
- **No package cycles (blocker).** `web`, `application`, `domain`, `infrastructure` stay acyclic.
- **Postgres is the only backing service (blocker).** No Redis, RabbitMQ, Kafka, Memcached or any
  other datastore. Caching, locking, queueing and deduplication are solved on Postgres (see
  `spring_session`, `shedlock`, `event_dedup` for the existing precedents).
- **No JPA.** Persistence is `JdbcTemplate` with explicit SQL. A PR introducing Hibernate or
  `spring-boot-starter-data-jpa` needs its own decision record, not a review comment.
- **Mercure topics are built server-side only (blocker).** The `/accounts/` literal lives in
  `domain.tracking.EventStreamTopic` and nowhere else; the frontend has an ESLint rule mirroring it.

## Schema and migrations

- **`V1__baseline.sql` is frozen (blocker).** It is the cutover baseline. New schema arrives as a
  new versioned migration; an edit to the baseline breaks every existing environment.
- Migrations are forward-only and must run cleanly against an empty database — Testcontainers
  proves it on every `./mvnw verify`.
- A schema change is `risk-high` by default. Say in the PR body what happens to existing rows.

## Backend

- **Public responses that carry HTML must set `text/html; charset=UTF-8` explicitly (major).** The
  servlet default is ISO-8859-1 and silently mangles every Polish diacritic. See `LoginController`.
- **Forms are native document POSTs, not XHR.** Success redirects (PRG); failure re-renders the SPA
  document with `data-*` attributes on `<html>`. Deviating from that pattern needs a reason in the PR.
- **No `WARN` logs on an expected path (blocker).** `FailOnWarnLogExtension` fails the suite. A
  rejected form, a rate limit, a duplicate or a honeypot hit is INFO or DEBUG, never WARN.
- User-visible strings come from `MessageSource` (`messages_pl.properties` / `messages_en.properties`).
  A hard-coded Polish literal in new code is a major finding unless it mirrors a documented parity case.
- Concurrency and idempotency belong in one atomic SQL statement where possible (`ON CONFLICT ...`),
  not in a check-then-write pair that races.

## Frontend

- **No CSS frameworks and no new UI libraries (blocker).** No Tailwind, Bootstrap, React, Svelte.
  The dependency list in `frontend/package.json` does not grow without a decision.
- New styles go into the existing files under `frontend/src/styles/` and use the tokens from
  `01-tokens.css`. A raw colour literal in new CSS is a major finding.
- **Polish first, English second.** Every new string lands in `i18n/messages/*.pl.ts` and
  `*.en.ts` in the same PR. A missing EN translation is a major finding.
- New top-level i18n keys must not collide with existing ones (see the note in `landing.pl.ts`).
- `v-html` is allowed only on strings that come from the code (i18n catalogues), never on anything
  that originates in the database or from a request. This is a blocker.

## Tests

- **TDD order.** The failing test lands before or with the implementation, never after — a PR whose
  tests could not have failed before the change is a major finding.
- Naming is load-bearing: `*Test` runs in the fast, container-free subset; `*IT` runs under
  Testcontainers in `verify`. A container-dependent test named `*Test` breaks `./mvnw test`.
- Every acceptance criterion on the ticket maps to a named test. The PR body says which.
- E2E specs touching live/SSE state (`events`, `dashboard`, `navigation`) run with `--workers=1`;
  a new spec that shares that state says so.
- A change to a user-visible flow updates the Playwright suite in the same PR. A spec that had to be
  weakened (a loosened assertion, a removed count) is called out explicitly, not slipped in.

## Delivery

- **Never run the prod compose project from a worktree (blocker).** Production runs under
  `-p kivvi-click` from the repository directory; a different directory name starts a second,
  colliding stack. Dev is always `-p kivvi-dev`.
- Conventional Commits in English, referencing the Linear identifier, e.g.
  `feat: add waitlist signup (PIO-70)`.
- **No AI co-authorship trailers** — no `Co-Authored-By`, no "generated with" footers, anywhere.
- No secrets in code, config, comments or PR text. `.env.prod.docker` and the Linear credentials
  file stay out of the repository.

## Contracts

Changes to anything listed in `BACKWARD_COMPATIBILITY.md` are a blocker unless the PR body names the
surface, explains why it must change, and describes the migration path for existing callers.
