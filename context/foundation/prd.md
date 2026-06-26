---
project: "kivvi-click"
version: 1
status: draft
created: 2026-06-25
context_type: brownfield
product_type: web-app
target_scale:
  users: small
  qps: low
  data_volume: small
timeline_budget:
  delivery_weeks: 2
  hard_deadline: null
  after_hours_only: false
---

# kivvi-click PRD

## Current System Overview

- System purpose: `kivvi-click` is a from-scratch rebuild of a marketing automation platform for ecommerce sites and consumer websites.
- Key architecture: early single full-stack web application rebuild.
- Tech stack: PHP/Symfony/PostgreSQL stack per repository guidance; integrations are expected through lightweight pure JavaScript snippets and backend scripts/plugins on stores.
- Current user base: no production users stated for the rebuild yet.
- Core functionality today: localized homepage exists; the target product functionality is not yet built.
- Must preserve: multi-tenancy, lightweight dependency-free tracking script, event idempotency, Polish-first UI, and the product intent from the original kivvi-click.

## Problem Statement & Motivation

Store owners need a marketing automation platform that can react to ecommerce events with emails, coupons, popups, web layers, social proof elements, newsletters, product feed integrations, and subscriber imports. Existing platforms are perceived as unreliable: automations fail, delivery certainty is unclear, audit trails are missing, newsletter recipient counts are not predictable, and users lack queue visibility or control.

The product insight is that store owners do not only need more ready-made scenarios; they need reliable, inspectable automation. The platform-wide buildout should prioritize auditability, queue visibility, previewability, and clear estimates of who will receive each campaign or automation before actions are sent.

## User & Persona

Primary persona: store owner.

The store owner runs an ecommerce business and reaches for kivvi-click when they want to automate customer follow-up, recover abandoned carts, send newsletters, show on-site promotional widgets, and understand exactly what the system plans to do or has already done.

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

## Scope of Change

### Account and Website Setup

- [new] FR-001: Store owner can create an account and log in. Priority: must-have.
  > Socrates: Counter-argument considered: "Without auth, multi-tenancy cannot be proven." Resolution: kept; account/login is required to prove tenant isolation.
- [new] FR-002: Store owner can configure one website with URL, name, and sender email. Priority: must-have.
  > Socrates: Counter-argument considered: "Website identity is required to prove tenant isolation and tracking." Resolution: kept; website configuration is required for the first tracking slice.
- [new] FR-003: Store owner can get a custom JavaScript tracking script and installation instructions. Priority: must-have.
  > Socrates: Counter-argument considered: "JS install is the core customer integration moment and must be tested early." Resolution: kept; custom JS integration is part of the first proof.

### Event Tracking and Audit

- [new] FR-004: System can receive and deduplicate the first tracking event. Priority: must-have.
  > Socrates: Counter-argument considered: "Deduplication is core product correctness and must be in the first slice." Resolution: kept; idempotency is a first-slice correctness requirement.
- [new] FR-005: Store owner can see received events in an audit/event log. Priority: must-have.
  > Socrates: Counter-argument considered: "Auditability is the core competitor weakness, so it must be user-visible immediately." Resolution: kept; event audit is part of product differentiation.

### Automation and Queue Visibility

- [new] FR-006: Store owner can create a manual test email action from a received event. Priority: must-have.
  > Socrates: Counter-argument considered: "Automation is too much; queueing a manual test email would prove enough." Resolution: revised; the first slice proves queueing with a manual test action instead of full automation rules.
- [new] FR-007: System can queue the planned email send. Priority: must-have.
  > Socrates: Counter-argument considered: "Queueing is the minimum proof of controlled delivery." Resolution: kept; queue creation is required even if provider sending is deferred.
- [new] FR-008: Store owner can inspect the email queue and audit trail. Priority: must-have.
  > Socrates: Counter-argument considered: "Queue visibility is a key differentiator and must be present early." Resolution: kept; queue inspection is required in the first slice.

## Constraints & Compatibility

- A custom integration script must be available regardless of ecommerce platform.
- Docker-first development workflow must be preserved.
- Dependencies should stay as low as possible.
- The tracking integration should remain lightweight and dependency-free.
- Event ingestion must preserve idempotency through deduplication.

## Business Logic Changes

For each store account, kivvi-click accepts idempotent tracking events from configured websites, records an auditable event trail, and lets the store owner turn a selected event into a visible queued email action.

The rule consumes the configured website identity, the incoming tracking event, and the event's idempotency identity. Its output is an auditable event record plus a store-owner-visible queued email action when the owner chooses to create one.

## Access Control Changes

MVP authentication should support email/password plus Google OAuth and Facebook OAuth.

The first useful version can use flat account-level permissions: a logged-in user manages one store account without role separation. Future expansion should allow multiple users under one account with roles such as owner, store manager, and employee, but that role matrix is not required in the first useful version.

## Non-Goals

- No PrestaShop, Magento, Shoper, WooCommerce, or Shoplo plugin in v1; the first slice uses custom JavaScript integration.
- No platform autodetect in v1; the first slice does not attempt to infer the ecommerce engine.
- No newsletter builder in v1; the first slice may provide a simple textarea where the store owner can paste HTML.

## Open Questions

No open questions recorded.
