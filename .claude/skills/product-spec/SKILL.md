---
name: product-spec
description: Product vision, features, domain entities, and assumptions for kivvi-click-10x. Use when planning or building any feature, deciding scope, or answering "what should this do / what is the domain model". Carries the product intent from the original kivvi-click — NOT its tech stack.
---

# kivvi-click-10x — Product Spec

A multi-tenant SaaS **marketing-automation platform for e-commerce sites and websites**.
It bridges real-time visitor tracking with automated responses: **see** what visitors do,
**decide** what to do (rules), **act** instantly (emails / popups / coupons / recommendations).

Build the new app to these goals. The original used a different stack — ignore that; the
stack for this rebuild is in `CLAUDE.md`.

## Goals / value proposition

- Track visitor behavior across client websites and build customer profiles.
- Automate marketing responses triggered by user actions.
- Increase conversions by showing the right message at the right time.
- Full transparency into tracked events and triggered deliveries.
- ML-powered personalization (product recommendations) to raise average order value.
- Multi-channel: email, popups, web layers/banners, coupons, recommendations.

## Core features

- **Tracking script** — lightweight (~2KB minified, no external deps) JS loader installed on
  client sites. Assigns a unique customer id; captures pageview, add-to-cart, purchase, login,
  signup, search, and custom events. Events are idempotent (idempotency id, dedup required).
- **Live event dashboard** — real-time stream of events as they occur (Mercure-driven).
- **Event history & analytics** — searchable event log with per-customer detail.
- **Email automation** — behavior-triggered sends with delivery tracking; multiple providers (SMTP/SES/SendGrid).
- **Email template builder** — WYSIWYG editor with placeholders, categories, HTML output.
- **Popups** — modal / slide-in / full-screen, shown on behavioral triggers (WYSIWYG).
- **Web layers / banners** — persistent banners for promotions, discounts, social proof.
- **Coupon delivery** — show discount codes at optimal conversion moments.
- **Product recommendations** — collaborative filtering, similar products, frequently-bought-together, trending.
- **Subscriber management** — subscribers with unsubscribe handling.
- **Account management** — multi-user accounts, company-level org, authenticated access.
- **Multiple websites** — track several domains/URLs per account.

## Domain concepts (entities)

- **Account** — a company using the platform (company_name, address, tax id).
- **User** — a user within an account (email/password auth).
- **Webpage** — a tracked URL/site under an account (url, name, tracking enabled).
- **Tracking event / AccountEvent** — raw event from the script (event_type, customer_id,
  event_time, custom_data, idempotency_id).
- **Customer / Visitor** — identified by customer_id; profile (first_seen, last_seen, totals, recent_urls).
- **Email template** — design + html + placeholder schema + category.
- **Email campaign** — rule/event-triggered sends.
- **Widget** — popups, banners, recommendation carousels.
- **Coupon** — discount codes shown at conversion moments.
- **Rule / Automation** — conditional logic mapping event types to actions.

## Assumptions / constraints

- Target audience: e-commerce stores and consumer-facing websites.
- Multi-tenancy: many independent accounts on one platform.
- Scale: event ingestion is high-throughput; processing is real-time.
- Pricing: two tiers — Free (usage with email-send limits, no ML) and Pro (unlimited email, full ML).
- Privacy/security: authenticated access; HTTPS in production.
- i18n: default language **Polish**; English is an optional toggle (build PL-first, EN translations).
- Idempotency: event dedup via idempotency id.
- Tracking script must stay minimal (~2KB, no external deps).

## What to ignore from the original

The original kivvi-click was Java/Spring + Go + Vue + Kafka/Redpanda, monorepo. The rebuild is a
single Symfony 8 / PHP 8.5 full-stack app with Postgres for everything (see `CLAUDE.md`). Reuse
the **product** intent above, not those technology choices or the multi-service split.
