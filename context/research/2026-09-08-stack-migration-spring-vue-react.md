# Research: Should kivvi-click migrate from Symfony 8 / Twig to Spring Boot + Vue 3 or React (SSR or SPA)?

**Status:** decided — flip condition confirmed by the owner on 2026-09-08 ("nie chcę utrzymywać PHP długoterminowo, Java to język, w którym to będzie żyć"; Vue 3 SPA, no SSR). Effective verdict: **adopt-with-constraints (b)**. Sections below are kept as written for re-evaluation.
**Date:** 2026-09-08
**Decision unblocked:** whether the 46 Linear tasks (PIO-70…PIO-115) are executed on the current Symfony stack or after a platform migration; whether `CLAUDE.md`/`AGENTS.md` stack rules stay in force (risk R1 in `context/map/risks-and-unknowns.md`).
**Decision owner:** Piotr Mucha (sole maintainer).
**Budget spent:** one desk-research session (~1 h); no spike run; budget not exhausted.
**Path note:** this repository has no research-artifact convention; `context/research/` is a proposal consistent with the existing `context/` layout.

## Verdict (first screen)

**Verdict: reject** the migration *now*; keep Symfony 8 / Twig / vanilla TypeScript and execute the backlog on it. The strongest evidence: the whole product surface (13.9k lines in `bdf120e`, 10 Playwright specs, 46 AI-ready Linear tasks whose "Punkt startu w repo" names Symfony file paths) is built on the incumbent, while the domain model does not exist yet — so a migration would rewrite a prototype and invalidate the backlog without buying a single product capability. No technical constraint observed in the repository (load, data shape, deployment target, Postgres-only rule) requires the JVM or a client-side framework.

The verdict flips to **adopt-with-constraints (Spring Boot + Vue 3 SPA)** only if the owner confirms a non-technical driver that outweighs the rewrite: long-term maintenance in Java is the only option the maintainer will sustain, or hiring/partner constraints require the JVM. That is a `User-confirmed` input this research could not obtain; see residual unknowns.

One currency finding independent of the verdict: the lockfile pins Symfony **8.0.11**, and the 8.0 branch is **unmaintained since 2026-07-31**. Bump to 8.1.x (current 8.1.6, supported to 2027-01-31) or to 7.4 LTS regardless of the migration decision.

## Question

Does moving the backend to Spring Boot and the panel to Vue 3 or React (over an HTTP API, SSR or SPA) deliver enough value for kivvi-click's actual constraints to justify rewriting the existing Symfony/Twig prototype and re-pointing the backlog?

- **Success criterion (declared before research):** at least one constraint from the repository (performance target, deployment target, integration, licensing, team capability) that the incumbent cannot meet and an alternative can, with primary-source evidence.
- **Kill criterion (declared before research):** if no such constraint exists and the only drivers are preference or familiarity, reject and keep the incumbent, per the "incumbent wins ties" rule.

## Constraints this answer must satisfy

| Constraint | Source | Provenance |
| --- | --- | --- |
| Single full-stack app, Postgres as the only backing service (sessions, cache, queue, scheduler); no Redis/RabbitMQ/Memcached; Mercure hub built into Caddy | `CLAUDE.md`, `AGENTS.md`, `compose.yaml`, `config/packages/*.yaml` | Observed |
| PL-first UI with EN toggle; locale-prefixed routes | `config/packages/translation.yaml`, `src/Controller/*` | Observed |
| Deployment: one Ubuntu host, Docker Compose, prod on port 23456 behind an external TLS proxy; image built by GitHub Actions | `compose.prod.yaml`, `README.md`, `.github/workflows/docker-build.yml` | Observed |
| Scale target: small (one/few accounts, low QPS); tracking event p95 < 500 ms | `context/foundation/prd.md`, `shape-notes.md` | Observed (documented intent) |
| Tracking script ~2 KB, no deps; idempotent event ingestion | product-spec, `src/Tracking/*` | Observed |
| Team: one developer, tasks executed by AI subagent sessions that see only the Linear issue body | memory `kivvi-linear-backlog`, git history (single author) | Observed / Inferred |
| Backlog: 46 issues, all `Backlog`; P0 = waitlist, registration, auth + firewall, panel lock + Mercure auth, password reset, error pages; P1 = k.js, ingest to Postgres, tenant isolation, domain model, real-data screens | Linear project "kivvi", queried 2026-09-08 | Observed |
| Host toolchain present: OpenJDK 21.0.12, Node 26.8.1, Docker | `java -version`, `node --version` on this host | Observed |

## Prior art

| Source | What it establishes | Provenance |
| --- | --- | --- |
| `.claude/skills/product-spec/SKILL.md` §"What to ignore from the original" | The original kivvi-click *was* Java/Spring + Go + Vue + Kafka/Redpanda in a monorepo; the rebuild deliberately chose one Symfony app with Postgres for everything | Observed |
| `CLAUDE.md`, `AGENTS.md` | Explicit rules: no React/Vue/Svelte, no CSS frameworks, no extra backing services | Observed |
| `context/foundation/stack-assessment.md` (2026-06-25) | Stack judged "ready" for agent-assisted development; FrankenPHP flagged as less common but acceptable | Observed (stale on test counts, not on stack) |
| `context/foundation/prd.md` guardrails | "Docker-first workflow must be preserved; dependencies as low as possible" | Observed |
| `src/**`, `templates/**`, `assets/**`, `tests/e2e/**` (commit `bdf120e`, 215 files, +13,965 lines) | Complete design system and every panel screen already implemented server-side; real ingestion → Mercure flow; e2e oracle exists | Observed |
| `src/Entity/`, `src/Repository/` empty; no `security.yaml` | Domain model and auth not started — the part a migration would *not* have to redo is small | Observed |
| Conversation 2026-09-08 | Owner "wahа się" (hesitates) between Vue 3 and React, asks about SSR; no driver stated | User-confirmed (hesitation), Unknown (driver) |

The original monorepo's stack was rejected once already, with the reason recorded in the product spec ("multi-service split"). That rejection is not binding, but the constraint it rested on (single developer, low scale, minimal ops) is unchanged.

## Options

| Option | Current version and release date | License | Operational surface added | Migration and exit cost | Source and retrieval date |
| --- | --- | --- | --- | --- | --- |
| **(a) Incumbent: Symfony 8 + Twig + vanilla TS on FrankenPHP** (bump 8.0→8.1) | Symfony 8.1.6, 2026-08-30 (8.0 EOL 2026-07-31; 7.4 LTS bug-fix to 2028-11-30, security to 2029-11-30); PHP 8.5.10 active support to 2027-12-31; FrankenPHP 1.12.7, 2026-08-07 | MIT (Symfony, FrankenPHP) | none new; 6-monthly minor upgrades (or 7.4 LTS) | none; bump `symfony/*` to `8.1.*` | GitHub releases API `symfony/symfony`, `php/frankenphp`; endoflife.date `symfony`, `php`; php.net supported-versions — 2026-09-08 |
| **(b) Spring Boot backend + Vue 3 frontend** (SPA, or Nuxt 4 SSR) | Spring Boot 4.1.1, 2026-08-20 (requires Java 17–26; 4.1 OSS support to 2027-07-31, 4.0 to 2026-12-31); Vue 3.5.42, 2026-08-27 (3.6 at rc.7); Nuxt 4.5.2, 2026-08-05 (Nuxt 3 EOL 2026-07-31; Nuxt 5 in development) | Apache-2.0 (Spring), MIT (Vue, Nuxt) | JVM container + Maven/Gradle toolchain; separate frontend build; for SSR a Node runtime container in prod; Mercure becomes a separate hub container (no Caddy-embedded PHP worker); two test stacks (JUnit + Vitest/Playwright); ~13-month Spring Boot minor OSS windows | rewrite all controllers/view-models (`src/Panel/Content` ≈ 1.7k lines, 14 controllers), all 58 Twig components + ~90 page templates into SFCs, i18n, session/preferences, ingestion; rewrite 46 Linear tasks' repo starting points; e2e selectors partially reusable; exit = another rewrite | GitHub releases API `spring-projects/spring-boot`, `vuejs/core`, `nuxt/nuxt`; docs.spring.io system-requirements (4.1.1); endoflife.date `spring-boot`, `nuxt` — 2026-09-08 |
| **(c) Spring Boot backend + React frontend** (SPA, or Next.js 16 SSR) | Spring Boot as above; React 19.2.8, 2026-07-21; Next.js 16.3.4, 2026-08-31 (Node ≥ 20.9; Next 15 EOL 2026-10-21) | Apache-2.0 (Spring), MIT (React, Next.js) | as (b); Next.js SSR additionally couples the front to Vercel-style deployment conventions and React canary builds in the App Router | as (b) | GitHub releases API `facebook/react`, `vercel/next.js`; nextjs.org installation docs (v16.3.4, updated 2026-07-21); endoflife.date `react`, `nextjs` — 2026-09-08 |

### Trade-offs

**(a) Incumbent.** Makes easier: shipping P0/P1 immediately (SecurityBundle, Doctrine entities, Messenger, Scheduler are all already wired or one `composer require` away); one container, one language, one test runner; Twig *is* SSR so SEO for the landing and no-flash theming come free; e2e suite stays a valid oracle. Makes harder: rich client interactions (rule-flow editor, drag-and-drop email composer) stay in hand-written TS with server-rendered fragments — workable (the pattern is already in place) but more bespoke than a component framework; ML/recommendation work (PIO-101/102) may want a separate service later. Wrong choice when: the maintainer will not sustain PHP long-term, or the product needs a heavy client-side editor beyond what fragment-swapping can carry.

**(b) Spring Boot + Vue 3.** Makes easier: strong typing, mature JPA/Modulith/Scheduling, large hiring pool, Vue SFCs for the editors; Vue 3 is closest to the Twig component model (templates + props), so component translation is mechanical. Makes harder: everything in the "migration cost" column; Postgres-only rule must be re-implemented (Spring Session JDBC, `@Scheduled` with ShedLock, a DB-backed outbox instead of Messenger), Mercure needs its own hub container, i18n moves to two places (backend messages + frontend). SSR via Nuxt adds a Node runtime in prod for pages that sit behind a login — negligible SEO value; SSR is worth it only for the public landing, which could equally be a static build. Wrong choice when: there is no driver beyond preference; a single developer with AI subagents loses the "one repo, one mental model" advantage.

**(c) Spring Boot + React.** As (b), plus: React's JSX model is farther from Twig than Vue SFCs (higher translation effort), Next.js App Router ships React canary builds and steers toward Vercel conventions (self-hosting on Compose is supported but is the less-trodden path); React 19's server components mainly pay off with a Node backend, which this option does not have. Wrong choice when: the backend is Java (double SSR runtime) and the team is one person.

**SSR vs SPA, independent of framework.** The panel is behind a login and is an internal tool for store owners: no SEO, first paint dominated by data, theme already persisted server-side. SSR buys nothing there and costs a Node process in prod. The public landing (`/`, `/pl`, `/en`) is the only page that benefits from SSR/static rendering. Under (a) this is already the case. Under (b)/(c) the sane split is: SPA panel + statically generated landing (Nuxt `generate` / Next static export), or keep the landing in Spring templates. **Recommendation if migrating: SPA, not SSR.**

## Evidence

| Claim | Source | Retrieved | Provenance |
| --- | --- | --- | --- |
| Symfony latest stable 8.1.6 (2026-08-30); 8.0.16 last 8.0 patch (2026-07-29) | `gh api repos/symfony/symfony/releases` | 2026-09-08 | Observed |
| Symfony 8.0 EOL 2026-07-31; 8.1 EOL 2027-01-31; 7.4 LTS support 2028-11-30 / EOL 2029-11-30 | endoflife.date/api/symfony.json; symfony.com/releases (fetched, matches) | 2026-09-08 | Observed (secondary + primary agree) |
| Repo pins `symfony/framework-bundle` v8.0.11 | `composer.lock` | 2026-09-08 | Observed |
| PHP 8.5 active support to 2027-12-31, security to 2029-12-31 | php.net/supported-versions.php | 2026-09-08 | Observed |
| FrankenPHP latest 1.12.7 (2026-08-07), MIT | `gh api repos/php/frankenphp` | 2026-09-08 | Observed |
| Spring Boot latest GA 4.1.1 (2026-08-20); 4.2.0-M1 milestone; 3.5.16 (2026-06-25) | `gh api repos/spring-projects/spring-boot/releases` | 2026-09-08 | Observed |
| Spring Boot 4.1.1 requires Java 17, compatible up to Java 26; Spring Framework 7.0.9+; Maven 3.6.3+/Gradle 8.14+ or 9.x; Tomcat 11 / Jetty 12.1 | docs.spring.io/spring-boot/system-requirements.html | 2026-09-08 | Observed |
| Spring Boot 4.1 OSS support ends 2027-07-31; 4.0 ends 2026-12-31; 3.5 ended 2026-06-30 (commercial to 2032) | endoflife.date/api/spring-boot.json | 2026-09-08 | Observed (secondary; spring.io support table could not be extracted — verify before pinning) |
| Spring Boot license Apache-2.0 | `gh api repos/spring-projects/spring-boot` | 2026-09-08 | Observed |
| Vue latest stable 3.5.42 (2026-08-27); 3.6.0-rc.7 (2026-09-04); MIT | `gh api repos/vuejs/core` | 2026-09-08 | Observed |
| Vue has no fixed release cycle; minors every 3–6 months with beta phase | vuejs.org/about/releases.html | 2026-09-08 | Observed |
| Nuxt latest 4.5.2 (2026-08-05); Nuxt 3 EOL 2026-07-31; Nuxt 5 not released (blog latest: 4.5, 2026-07-18) | `gh api repos/nuxt/nuxt`; endoflife.date/api/nuxt.json; nuxt.com/blog | 2026-09-08 | Observed |
| Nuxt Node.js requirement | nuxt.com/docs/getting-started/installation returned 404 | 2026-09-08 | Unknown |
| React latest 19.2.8 (2026-07-21); MIT | `gh api repos/facebook/react` | 2026-09-08 | Observed |
| Next.js latest stable 16.3.4 (2026-08-31); Node ≥ 20.9; App Router uses React canary builds; Next 15 EOL 2026-10-21 | `gh api repos/vercel/next.js`; nextjs.org installation docs; endoflife.date/api/nextjs.json | 2026-09-08 | Observed |
| Host has OpenJDK 21.0.12 (2026-07-21) and Node 26.8.1 | `java -version`, `node --version` | 2026-09-08 | Observed |
| Java LTS designation of 21/25 and Oracle support dates | oracle.com roadmap returned HTTP 403 | 2026-09-08 | Unknown — verify before choosing a JDK |
| 46 Linear issues, all in Backlog, labelled P0–P5 / obszar; P0 set is auth/registration/waitlist/error pages | Linear MCP `list_issues project=kivvi` | 2026-09-08 | Observed |
| Linear issues carry "Punkt startu w repo" with real Symfony paths | memory `kivvi-linear-backlog` (issue bodies not re-read in this run) | 2026-09-08 | Inferred |
| Prototype size: 215 files, +13,965 lines in one commit; 58 components, ~90 page templates, 14 controllers, 6 TS controllers | `git show --stat bdf120e`, `git ls-files` | 2026-09-08 | Observed |
| Original product stack was Java/Spring + Go + Vue + Kafka | `.claude/skills/product-spec/SKILL.md` | 2026-09-08 | Observed (author's own statement) |
| Owner's driver for migrating | not stated in conversation | 2026-09-08 | Unknown |

## Spike

Not run. Reading settled the question: no decision-relevant quantitative gap exists (scale target is small and documented; both stacks trivially meet p95 < 500 ms for a JSON insert + publish; licensing is permissive on all sides). The open input is the owner's driver and long-term language commitment, which no spike can measure. No worktree was created; the checkout is unchanged apart from this artifact.

## Verdict

**Verdict:** reject (keep the incumbent), with the flip condition stated below.

No repository constraint fails on Symfony and passes on Spring Boot. The kill criterion declared up front was met: the only visible drivers are preference and the maintainer's prior Java/Vue history. Meanwhile the incumbent already carries the complete UI, the e2e oracle and the entire backlog, and the missing pieces (entities, firewall, persistence of events) are the cheapest part of a Symfony app to add. The migration would spend the next several weeks reproducing a prototype instead of shipping P0.

### Flip condition (turns this into adopt-with-constraints for option b)

If the owner confirms one of: (1) "I will not maintain PHP long-term; Java is the language I want to own this in", or (2) a hiring/partner/customer constraint requiring the JVM — then adopt **Spring Boot 4.1.x + Vue 3 SPA**, with constraints:

- SPA for the panel, static/SSG for the landing; no SSR runtime in prod (evidence: panel is behind login; SSR adds a Node container for no SEO gain).
- Preserve the Postgres-only rule: Spring Session JDBC, DB-backed job/outbox tables, ShedLock or DB-scheduled jobs; Mercure as its own container (the Caddy-embedded hub is a FrankenPHP artefact).
- Capture the oracle first: freeze `tests/e2e` URLs and selectors as the acceptance contract (`migration-planning` requirement).
- Do it *before* PIO-74/78/81 (auth, domain model, ingest persistence) are built on Symfony — the window closes as soon as domain code exists.
- Verify Spring Boot OSS support dates and the JDK LTS line from primary sources before pinning (marked Unknown/secondary above).

### Why the alternatives lost

| Option | Why it lost |
| --- | --- |
| (b) Spring Boot + Vue 3 (SPA/Nuxt) | No constraint requires it; full rewrite of a working prototype plus 46 task rewrites; adds JVM + Node (if SSR) + separate Mercure container to a one-person Compose deployment; Spring Boot minors need yearly upgrades. Best of the migration options if the flip condition is confirmed — Vue SFCs map most directly onto the existing Twig components. |
| (c) Spring Boot + React (SPA/Next.js) | Everything in (b), plus a larger translation gap from Twig, React canary builds inside Next's App Router, and self-hosted Next.js being the less-trodden path; server components bring no benefit with a Java backend. |
| SSR (Nuxt/Next) in any migrated variant | Panel is authenticated and data-driven; SSR yields no SEO and costs a runtime. Only the landing benefits, and static generation covers that. |

### Residual unknowns

| Unknown | Impact if wrong | How it would be closed | Owner |
| --- | --- | --- | --- |
| ~~Owner's real driver for migrating~~ — closed 2026-09-08: long-term maintenance in Java (User-confirmed); Vue 3 SPA, no SSR (User-confirmed) | — | — | Piotr Mucha |
| Spring Boot OSS support windows (secondary source only) | Upgrade cadence estimate off by months | Read spring.io/projects/spring-boot#support in a browser | maintainer |
| Java LTS/ support dates (Oracle page blocked) | Wrong JDK pin | Read openjdk.org / Oracle roadmap | maintainer |
| Nuxt Node.js minimum | Only matters under (b) with SSR | nuxt.com installation docs | maintainer |
| Whether the rich editors (rule flow, email composer) will outgrow fragment-swapping TS | Could justify a client framework *for the editors only* (embedded Vue island) without a backend migration | Revisit when PIO-87/93 are planned | maintainer |

## Handoff

- **Consumer:** implementation-planning (for the incumbent path: first the Symfony 8.0→8.1 bump, then PIO P0 tasks) or migration-planning (only if the flip condition is confirmed).
- **What planning still owns:** version pinning, vertical slicing, gate contract, rollout and rollback.
- **Federation record:** path `context/research/2026-09-08-stack-migration-spring-vue-react.md`; source revision `f58d62c`; consumers: implementation-planning, migration-planning, project-context-initializer (INDEX + manifest `related_artifacts`). Content hash recorded in `context/map/manifest.json`.
- **This artifact authorizes nothing.**
