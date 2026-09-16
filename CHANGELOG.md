# Changelog

Notable changes to kivvi·click. Dates are the merge date on `main`.

## v0.1.0 — 2026-09-16

The first tagged release, cut as the project goes open source under the MIT licence.

### The stack

- Spring Boot 4.1 on Java 25 serving a Vue 3.5 single-page application from its own jar, as one
  deployable image. PostgreSQL 18 is the only backing service: sessions, scheduler locks, event
  deduplication, rate limiting and the mail outbox all live there, with no Redis, queue or broker.
- The Mercure hub is the public edge and carries the live event stream over Server-Sent Events.
- Migrated from a Symfony/Twig stack in September 2026; the old stack was removed once the new one
  was proven at parity against a recorded oracle. Two decisions from that migration are written up
  in `docs/adr/`.

### The waiting list

- A signup form on the landing page: a native form POST that stores a normalised address with a
  consent record — the time, IP, user agent and the exact clause the visitor saw. A repeat signup
  is idempotent, a honeypot and a Postgres-backed rate limiter turn away bots, and a refused submit
  redisplays the form without losing what was typed.
- Double opt-in: a confirmation link, valid seven days, sent through a Postgres outbox drained by a
  scheduled sender rather than inside the request. A spent link says the address is already
  confirmed, an expired one offers a fresh link, and every message carries an unsubscribe link that
  works without signing in and never expires.
- Mail renders from Thymeleaf templates in Polish and English, inline-styled with no remote assets.
  Outside the development profile the application refuses to start without a relay it can
  authenticate to, rather than accepting signups it cannot confirm.
- A privacy policy page in both languages, linked from the consent clause and the footer.

### Operations

- Production is managed by Portainer and deployed by GitHub Actions: merging to `main` builds the
  image, pushes it to GHCR and redeploys the stack. Rolling back is pinning an earlier commit SHA.
- Pull requests build on GitHub's runners; only pushes to `main` and the deploy job use the
  self-hosted runner.

### Known limitations

- `consent_ip` records the Docker bridge gateway rather than the visitor's address, because the
  proxy chain in front of the stack does not pass `X-Forwarded-For` through to the application. The
  consent proof's timestamp, user agent and clause text are accurate; its IP is not, and the
  per-IP rate limit is in practice a site-wide one.
- The English landing page still serves some Polish copy: the feature cards and trust badges come
  from backend fixtures that were never translated.
- The deploy job verifies that the site still serves after a redeploy, not that the newly built
  commit is the one serving. Confirming a deployment means checking the stack's revision.
