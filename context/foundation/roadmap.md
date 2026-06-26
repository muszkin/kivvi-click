---
project: kivvi-click
version: 1
status: draft
created: 2026-06-25
updated: 2026-06-25
prd_version: 1
main_goal: market-feedback
top_blocker: none
---

# Roadmap: kivvi-click

> Derived from `context/foundation/prd.md` (v1) + auto-researched codebase baseline.
> Edit-in-place; archive when superseded.
> Slices below are listed in dependency order. The "At a glance" table is the index.

## Vision recap

`kivvi-click` ma odbudować platformę marketing automation dla właścicieli sklepów, ale pierwszy zakres nie próbuje jeszcze pokryć całej automatyzacji. Najważniejszy wyróżnik to niezawodność i wgląd: właściciel sklepu powinien widzieć, jakie zdarzenia system odebrał, co zostało zdeduplikowane i co trafiło do kolejki emaili.

Pierwsza wersja ma udowodnić ten kierunek przez customową integrację JavaScript, audyt eventów i kontrolowaną kolejkę emaili zamiast przez gotowe scenariusze automatyzacji.

## North star

**S-04: właściciel sklepu może utworzyć ręczną testową akcję email z eventu i zobaczyć ją w kolejce oraz audycie** — to jest gwiazda przewodnia, czyli najmniejszy pionowy fragment produktu, którego działanie potwierdza główne założenie: platforma nie tylko przyjmuje eventy, ale też pokazuje właścicielowi, co system planuje zrobić dalej.

## At a glance

| ID | Change ID | Outcome (user can …) | Prerequisites | PRD refs | Status |
|---|---|---|---|---|---|
| S-01 | account-login-and-tenant-shell | Właściciel sklepu może założyć konto, zalogować się i wejść do pustego konta sklepu | — | US-01, FR-001 | ready |
| S-02 | website-setup | Właściciel sklepu może skonfigurować jedną stronę/sklep z URL, nazwą i adresem nadawcy | S-01 | US-01, FR-002 | proposed |
| S-03 | custom-script-first-event-audit | Właściciel sklepu może pobrać customowy skrypt, wysłać pierwszy event, zobaczyć go w audycie i nie dostać duplikatu przy tym samym idempotency id | S-02 | US-01, FR-003, FR-004, FR-005 | proposed |
| S-04 | manual-email-queue-audit | Właściciel sklepu może utworzyć ręczną testową akcję email z odebranego eventu i sprawdzić kolejkę oraz audyt | S-03 | US-01, FR-006, FR-007, FR-008 | proposed |

## Baseline

What's already in place in the codebase as of `2026-06-25` (auto-researched).
Foundations below assume these are present and do NOT re-scaffold them.

- **Frontend:** partial — Twig layout/homepage and vanilla TypeScript assets exist; no product dashboard screens yet.
- **Backend / API:** partial — Symfony controller/routing exists for the localized homepage; product routes and ingestion endpoints are absent.
- **Data:** partial — Doctrine/PostgreSQL and migrations are configured; product entities for account, website, events, and email queue are absent.
- **Auth:** absent — no security configuration, user entity, login, OAuth, or route-level access control exists.
- **Deploy / infra:** present — Docker/FrankenPHP compose files exist and GitHub Actions builds the production image on push to `main`.
- **Observability:** absent — no dedicated application event metrics, error tracking, or audit dashboard beyond future product audit requirements.

## Foundations

No standalone foundation slice is needed yet. The absent product layers are introduced inside the first vertical slice that uses them, so the roadmap does not prebuild an auth, data, API, or UI layer without a user-visible outcome.

## Slices

### S-01: Konto i pusty tenant sklepu

- **Outcome:** Właściciel sklepu może założyć konto, zalogować się i wejść do pustego konta sklepu.
- **Change ID:** account-login-and-tenant-shell
- **PRD refs:** US-01, FR-001
- **Prerequisites:** —
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** To jest pierwszy punkt wielodzierżawowości; jeśli granica konta będzie niejasna, kolejne eventy i kolejki nie będą bezpiecznie przypisane do właściciela.
- **Status:** ready

### S-02: Konfiguracja jednej strony/sklepu

- **Outcome:** Właściciel sklepu może skonfigurować jedną stronę/sklep z URL, nazwą i adresem nadawcy.
- **Change ID:** website-setup
- **PRD refs:** US-01, FR-002
- **Prerequisites:** S-01
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Strona/sklep jest źródłem tożsamości dla integracji; bez tego pierwszy event nie ma wiarygodnego kontekstu właściciela.
- **Status:** proposed

### S-03: Customowy skrypt, pierwszy event i audyt

- **Outcome:** Właściciel sklepu może pobrać customowy skrypt, wysłać pierwszy event, zobaczyć go w audycie i nie dostać duplikatu przy tym samym idempotency id.
- **Change ID:** custom-script-first-event-audit
- **PRD refs:** US-01, FR-003, FR-004, FR-005
- **Prerequisites:** S-02
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** To pierwszy test wiarygodności produktu; jeśli deduplikacja lub audyt będą nieczytelne, platforma powieli główny problem konkurencji.
- **Status:** proposed

### S-04: Ręczna akcja email, kolejka i audyt

- **Outcome:** Właściciel sklepu może utworzyć ręczną testową akcję email z odebranego eventu i sprawdzić kolejkę oraz audyt.
- **Change ID:** manual-email-queue-audit
- **PRD refs:** US-01, FR-006, FR-007, FR-008
- **Prerequisites:** S-03
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Ten krok sprawdza obietnicę kontroli nad wysyłką; w v1 kolejka musi być inspectable nawet jeśli prawdziwe dostarczenie przez provider zostaje poza tym zakresem.
- **Status:** proposed

## Backlog Handoff

| Roadmap ID | Change ID | Suggested issue title | Ready for `/10x-plan` | Notes |
|---|---|---|---|---|
| S-01 | account-login-and-tenant-shell | Add account signup, login, and tenant shell | yes | Run `/10x-plan account-login-and-tenant-shell` |
| S-02 | website-setup | Add one-website setup for a logged-in store owner | no | Depends on S-01 |
| S-03 | custom-script-first-event-audit | Add custom JS integration, first event ingestion, deduplication, and audit log | no | Depends on S-02 |
| S-04 | manual-email-queue-audit | Add manual test email action with visible queue and audit | no | Depends on S-03 |

## Open Roadmap Questions

No open roadmap questions recorded.

## Parked

- **Platform plugins for PrestaShop, Magento, Shoper, WooCommerce, and Shoplo** — Why parked: PRD Non-Goals says v1 uses custom JavaScript integration instead.
- **Platform autodetect** — Why parked: PRD Non-Goals says v1 does not infer the ecommerce engine.
- **Newsletter builder** — Why parked: PRD Non-Goals says v1 avoids a builder; a simple HTML textarea can come later if needed.
- **Product feeds, subscriber imports, widgets, social proof, and full automation rules** — Why parked: these are part of the platform direction, but the first proof is event ingestion, auditability, and queue visibility.

## Done

