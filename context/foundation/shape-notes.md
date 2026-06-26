---
project: "kivvi-click"
context_type: brownfield
created: 2026-06-25
updated: 2026-06-25
checkpoint:
  current_phase: 8
  phases_completed: [1, 2, 3, 4, 5, 6, 7]
  gray_areas_resolved:
    - topic: "change category"
      decision: "platform-wide product buildout; rebuilding the full platform from scratch, step by step"
    - topic: "primary persona"
      decision: "store owner"
    - topic: "product insight"
      decision: "competitors have bugs and technical debt; kivvi-click should emphasize reliable automation, auditability, queue visibility/control, and clear recipient estimates before sending"
    - topic: "access control"
      decision: "MVP uses email/password plus Google and Facebook OAuth; permissions are flat at first, with future expansion toward multiple users per account and roles such as owner, store manager, and employee"
    - topic: "mvp scope"
      decision: "scope down to a vertical slice: account, website setup, custom JS integration, first event received, event audit visible, one simple automation queues one email, queue/audit visible"
    - topic: "timeline"
      decision: "first shippable version should fit in 1-2 weeks; record delivery_weeks as 2"
    - topic: "mvp guardrails"
      decision: "preserve Docker-first workflow and keep dependencies as low as possible"
    - topic: "business logic"
      decision: "For each store account, kivvi-click accepts idempotent tracking events from configured websites, records an auditable event trail, and lets the store owner turn a selected event into a visible queued email action."
    - topic: "first-slice constraint"
      decision: "A custom integration script must be available regardless of ecommerce platform."
    - topic: "non-functional requirement"
      decision: "Event response time matters for v1."
    - topic: "product type"
      decision: "web app / SaaS platform with custom JavaScript integration as an external install surface"
    - topic: "target scale"
      decision: "small first target scale: one/few early store accounts and low event volume, enough to prove the slice"
    - topic: "timing"
      decision: "no hard deadline; main work, not after-hours"
    - topic: "non-goals"
      decision: "no platform plugins for PrestaShop, Magento, Shoper, WooCommerce, or Shoplo; no platform autodetect in v1; no newsletter builder beyond a textarea for HTML"
  frs_drafted: 8
  quality_check_status: accepted
timeline_budget:
  delivery_weeks: 2
  hard_deadline: null
  after_hours_only: false
product_type: web-app
target_scale:
  users: small
  qps: low
  data_volume: small
---

# Shape Notes

## Seed Idea

Chcę zbudować kompletną platformę do marketing automation, gdzie userzy (właściciele firm, sklepów internetowych) będą mogli integrować swoje systemy i reagować automatycznie na zdarzenia w ich systemach, jak np ktoś kupił produkt, to wyślij mu maila po 7 dniach z kuponem na następne zakupy. Zapisał się do newslettera, to wyślij mu kupon, porzucił koszyk to wyślij mu kupon, zaproś na specjalne promocje, zdefiniuj popup, weblayer, social proof elementy na swojej stronie. Zbieraj statystyki otwierania swoich newsletterów, też konfigurowanych w kivvi-click. Integracje poprzez wtyczki w pure JS lub przez skrypty backendowe na sklepach. Integracje z feedami produktowymi Google, Facebook. Możliwość śledzenia kolejki maili do wysyłki, podglądu treści wysłanych/zaplanowanych maili. Pełny audyt zdarzeń w systemie. Integracje ze systemami sklepów PrestaShop, Magento, Shoplo, Shoper, WooCommerce. Import subskrybentów z plików CSV, Excel.

## Research Notes

- Local product spec: `.claude/skills/product-spec/SKILL.md`.
- Competitor reference: SALESmanago positions itself as an AI Customer Engagement Platform for ecommerce, focused on customer data, touchpoints, revenue growth, personalization, and marketing automation.
- Competitor reference: edrone positions itself as ecommerce CRM and marketing automation, highlighting ready automation scenarios, real-time KPI tracking, newsletters, product recommendations, abandoned cart recovery, and customer intelligence.

## Current System

- System purpose: `kivvi-click` is a from-scratch rebuild of a marketing automation platform for ecommerce sites and consumer websites.
- Key architecture: early single full-stack web application rebuild.
- Tech stack: PHP/Symfony/PostgreSQL stack per repository guidance; integrations are expected through lightweight pure JavaScript snippets and backend scripts/plugins on stores.
- Current user base: no production users stated for the rebuild yet.
- Core functionality today: localized homepage exists; the target product functionality is not yet built.
- Must preserve: multi-tenancy, lightweight dependency-free tracking script, event idempotency, Polish-first UI, and the product intent from the original kivvi-click.

## Vision & Problem Statement

Store owners need a marketing automation platform that can react to ecommerce events with emails, coupons, popups, web layers, social proof elements, newsletters, product feed integrations, and subscriber imports. Existing platforms are perceived as unreliable: automations fail, delivery certainty is unclear, audit trails are missing, newsletter recipient counts are not predictable, and users lack queue visibility or control.

The product insight is that store owners do not only need more ready-made scenarios; they need reliable, inspectable automation. The platform-wide buildout should prioritize auditability, queue visibility, previewability, and clear estimates of who will receive each campaign or automation before actions are sent.

## User & Persona

Primary persona: store owner.

The store owner runs an ecommerce business and reaches for kivvi-click when they want to automate customer follow-up, recover abandoned carts, send newsletters, show on-site promotional widgets, and understand exactly what the system plans to do or has already done.

## Access Control

MVP authentication should support email/password plus Google OAuth and Facebook OAuth.

The first useful version can use flat account-level permissions: a logged-in user manages one store account without role separation. Future expansion should allow multiple users under one account with roles such as owner, store manager, and employee, but that role matrix is not required in the first useful version.

## Success Criteria

### Primary

- First shippable slice works end to end: store owner creates an account, configures one website, installs a custom JavaScript integration, sends the first event, sees that event in an audit view, creates a manual test email action from that event, and can inspect the resulting email queue/audit entry.

### Secondary

- The full platform direction remains visible after setup: future platform plugins, product feeds, imports, newsletters, and broader automation surfaces are framed as later steps, not part of the first slice.

### Guardrails

- Docker-first development workflow must be preserved.
- Dependencies should stay as low as possible.

## User Stories

### US-01: Store owner proves the first integration works

- **Given** a store owner has created an account
- **When** they configure one website, install the custom JavaScript tracking script, open their store, and trigger the first event
- **Then** kivvi-click receives the event, deduplicates it, shows it in the audit/event log, and allows the owner to create a manual test email action whose queued action is visible.

#### Acceptance Criteria

- The website setup captures URL, name, and sender email.
- The custom JavaScript setup provides installation instructions.
- The first received event is visible to the store owner.
- Re-sending the same event with the same idempotency id does not create a duplicate audit event.
- A manual test email action can create a queued email action from the received event.
- The queued email action is inspectable by the store owner.

## Functional Requirements

### Account and Website Setup

- FR-001: Store owner can create an account and log in. Priority: must-have. Change: new
  > Socrates: Counter-argument considered: "Without auth, multi-tenancy cannot be proven." Resolution: kept; account/login is required to prove tenant isolation.
- FR-002: Store owner can configure one website with URL, name, and sender email. Priority: must-have. Change: new
  > Socrates: Counter-argument considered: "Website identity is required to prove tenant isolation and tracking." Resolution: kept; website configuration is required for the first tracking slice.
- FR-003: Store owner can get a custom JavaScript tracking script and installation instructions. Priority: must-have. Change: new
  > Socrates: Counter-argument considered: "JS install is the core customer integration moment and must be tested early." Resolution: kept; custom JS integration is part of the first proof.

### Event Tracking and Audit

- FR-004: System can receive and deduplicate the first tracking event. Priority: must-have. Change: new
  > Socrates: Counter-argument considered: "Deduplication is core product correctness and must be in the first slice." Resolution: kept; idempotency is a first-slice correctness requirement.
- FR-005: Store owner can see received events in an audit/event log. Priority: must-have. Change: new
  > Socrates: Counter-argument considered: "Auditability is the core competitor weakness, so it must be user-visible immediately." Resolution: kept; event audit is part of product differentiation.

### Automation and Queue Visibility

- FR-006: Store owner can create a manual test email action from a received event. Priority: must-have. Change: new
  > Socrates: Counter-argument considered: "Automation is too much; queueing a manual test email would prove enough." Resolution: revised; the first slice proves queueing with a manual test action instead of full automation rules.
- FR-007: System can queue the planned email send. Priority: must-have. Change: new
  > Socrates: Counter-argument considered: "Queueing is the minimum proof of controlled delivery." Resolution: kept; queue creation is required even if provider sending is deferred.
- FR-008: Store owner can inspect the email queue and audit trail. Priority: must-have. Change: new
  > Socrates: Counter-argument considered: "Queue visibility is a key differentiator and must be present early." Resolution: kept; queue inspection is required in the first slice.

## Constraints & Preserved Behavior

- A custom integration script must be available regardless of ecommerce platform.
- Docker-first development workflow must be preserved.
- Dependencies should stay as low as possible.
- The tracking integration should remain lightweight and dependency-free.
- Event ingestion must preserve idempotency through deduplication.

## Business Logic

For each store account, kivvi-click accepts idempotent tracking events from configured websites, records an auditable event trail, and lets the store owner turn a selected event into a visible queued email action.

The rule consumes the configured website identity, the incoming tracking event, and the event's idempotency identity. Its output is an auditable event record plus a store-owner-visible queued email action when the owner chooses to create one.

## Non-Functional Requirements

- A tracking event submission should receive a success or duplicate response within 500 ms at p95 for the first-slice expected load.

## Non-Goals

- No PrestaShop, Magento, Shoper, WooCommerce, or Shoplo plugin in v1; the first slice uses custom JavaScript integration.
- No platform autodetect in v1; the first slice does not attempt to infer the ecommerce engine.
- No newsletter builder in v1; the first slice may provide a simple textarea where the store owner can paste HTML.

## Quality cross-check

- Access Control: present.
- Business Logic: present.
- Project artifacts: present.
- Timeline-cost acknowledgment: present; delivery is less than or equal to three weeks.
- Non-Goals: present.
- Preserved behavior: present.
