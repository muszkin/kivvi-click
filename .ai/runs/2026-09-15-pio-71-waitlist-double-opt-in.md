# Execution plan — PIO-71 waitlist double opt-in

Source doc: context/plans/2026-09-15-pio-71-waitlist-double-opt-in.md
Issue: PIO-71 (Linear, team PIO)
Research: context/research/2026-09-15-transactional-email-provider.md

## Goal

A waitlist address is confirmed by the person who owns the mailbox: signing up sends a mail with a
link that lives for 7 days, following it flips the row to `confirmed` and records when, from which
IP and with which user agent. A spent or lapsed link offers a fresh one. Signing up again with an
unconfirmed address re-sends the link instead of creating a second row. Every mail carries a
working, login-free unsubscribe link.

## Scope

Sending is turned on for the first time in this repository, so the change reaches from the schema
to the landing copy: a `mail_outbox` table with retry and backoff, a Thymeleaf mail template in
both languages, an SMTP transport configured entirely from environment variables (Brevo), a
filesystem transport for the `dev` profile, three public routes with their Vue views, and the
privacy policy naming the provider it promised to name before the first message goes out.

## Non-goals

- Bounce handling and provider webhooks (P2 on the ticket).
- A panel screen listing sent mail (PIO-72 lists subscribers, not a mailbox).
- Marketing sends and campaigns.
- A general-purpose queue: `mail_outbox` is an outbox for mail, not an event broker.
- Re-opening an `unsubscribed` row when that address signs up again — unsubscribe stays terminal
  here; a rejoin path is follow-up work.

## Risks

- R4 from the spec: the single heartbeat thread. The sender gets its own pool, registered outside
  `@Scheduled`, and a test pins that.
- R5: the dev-only filesystem transport reaching production. `@Profile("dev")` plus a startup guard
  outside that profile, with a test for each half.
- The startup guard must not break the existing `*IT` suite, which boots the full context under the
  default profile — test resources supply a dummy host.
- Schema change: `risk-high` by default per CODE_REVIEW.md. `V3` only adds nullable columns and one
  new table, so existing rows are untouched.

## Validation gate

The 8 commands in `.ai/agentic.config.json`, in order, from the repository root. The backend step
needs `JAVA_HOME=$HOME/.cache/kivvi-toolchains/jdk-25`, and the frontend build must be copied into
`backend/src/main/resources/static` before `./mvnw verify` or every Spring integration test fails to
start. E2E runs against the dev stack on `-p kivvi-dev` (ports 8544/8543 — 8080 and 8443 are taken
on this host by unrelated containers) with `--workers=1`.

## Implementation Plan

### Phase 1: schema and outbox
1.1 `V3__waitlist_confirmation.sql`: unsubscribe/confirmation-proof columns, two partial unique
    token indexes, the `mail_outbox` table.
1.2 `MailOutboxStore` on `JdbcTemplate` with `MailOutboxStoreIT` (Testcontainers): insert,
    dedup by `dedup_key`, lease-based claim, mark sent, mark failed with backoff, and two
    concurrent claims never getting the same row.

### Phase 2: the confirmation token
2.1 `Sha256`, `OpaqueToken` and `ConfirmationToken` in `domain`, with their unit tests.

### Phase 3: the mail template
3.1 Add `spring-boot-starter-thymeleaf`, `spring-boot-starter-mail` and `greenmail-junit5`.
3.2 `templates/email/waitlist-confirmation.html` plus the `mail.waitlist.*` message keys in both
    bundles.
3.3 `WaitlistMailComposer` and `WaitlistMailComposerTest`.

### Phase 4: the sender
4.1 `MailTransport`, `MimeMailComposer`, `SmtpMailTransport`, `FilesystemMailTransport`,
    `MailConfigurationGuard`.
4.2 `MailOutboxSenderJob` and `MailSchedulingConfig` (own single-thread pool), with unit tests for
    the backoff, the attempt ceiling and the pool separation.
4.3 `spring.mail.*` from environment variables, the test-resources override, and
    `MailOutboxSenderIT` on GreenMail.

### Phase 5: confirm, resend, unsubscribe
5.1 Token operations on `WaitlistSubscriberStore`, covered by `WaitlistSubscriberStoreIT`.
5.2 `MailQueue`, `ConfirmationOutcome`, `UnsubscribeOutcome`, `WaitlistConfirmationService` and its
    unit test; wire the signup path to it.
5.3 `WaitlistConfirmationController`, the three `RouteTable` entries, and their tests.
5.4 `WaitlistConfirmationApiIT` end to end on a real database; extend `ExpiredRowsCleanupJob` with
    the sent-row sweep.

### Phase 6: the pages
6.1 `waitlist.{pl,en}.ts`, `WaitlistConfirmView.vue`, `WaitlistUnsubscribeView.vue` and the three
    router entries.
6.2 Frontend integration specs for both views; correct the landing thank-you copy and the tests
    that quote it.
6.3 Name Brevo in the privacy policy, both languages.

### Phase 7: end to end
7.1 `compose.yaml`: `SPRING_PROFILES_ACTIVE` and the `./var/mail` maildrop mount, with the
    production overlay pinned to `prod`; documentation and `.gitignore`.
7.2 Extend `tests/e2e/specs/waitlist.spec.ts` with the full loop and run the suite headless against
    the dev stack.
7.3 Full validation gate, green, from the repository root.

## Progress

PR: #2

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: schema and outbox

- [x] 1.1 V3 migration — 8e6e456
- [x] 1.2 MailOutboxStore and its integration test — 8e6e456

### Phase 2: the confirmation token

- [x] 2.1 Sha256, OpaqueToken, ConfirmationToken — 6da193f

### Phase 3: the mail template

- [x] 3.1 Thymeleaf, mail and GreenMail dependencies — bd3fc62
- [x] 3.2 Thymeleaf template and message keys — bd3fc62
- [x] 3.3 WaitlistMailComposer — bd3fc62

### Phase 4: the sender

- [x] 4.1 Mail transports and the configuration guard — f3413f1
- [x] 4.2 MailOutboxSenderJob on its own pool — f3413f1
- [x] 4.3 SMTP configuration and the GreenMail integration test — f3413f1

### Phase 5: confirm, resend, unsubscribe

- [x] 5.1 Token operations on the subscriber store — 5d8994a
- [x] 5.2 WaitlistConfirmationService and the signup wiring — 5d8994a
- [x] 5.3 WaitlistConfirmationController and the route table — 5d8994a
- [x] 5.4 End-to-end API test and the outbox cleanup sweep — 5d8994a

### Phase 6: the pages

- [x] 6.1 Confirmation and unsubscribe views — dbf1aa8
- [x] 6.2 Frontend integration specs and the landing copy — dbf1aa8
- [x] 6.3 The privacy policy names Brevo — dbf1aa8

### Phase 7: end to end

- [x] 7.1 Dev maildrop in compose — ab92fe5
- [x] 7.2 Playwright loop against the dev stack — b491d2a
- [x] 7.3 Full validation gate — b491d2a

## Outcome

Status: complete. All 8 validation-gate commands green from the repository root, and the full
Playwright suite green at 75/75 against the dev stack (`-p kivvi-dev`, ports 8544/8543,
`--workers=1`).

Two defects the run found and fixed, neither of them in the plan:

- Boot's mail health indicator opens an SMTP connection on every probe, so a relay having a bad
  minute reported the whole application as DOWN and took the compose healthcheck with it. Under
  `dev`, where there is deliberately no relay, it failed on the first probe and the stack never
  came up. Disabled: delivery problems belong in `mail_outbox`'s `failed` rows, not in liveness.
- Docker creates any missing parent of a bind-mount point as root, so mounting the maildrop at
  `/app/var/mail` left `/app/var` root-owned and the unprivileged runtime user could no longer
  create `/app/var/import` — every customer-import upload became an `AccessDeniedException`. The
  image now creates that tree itself. The e2e suite is what caught it.

Deliberately left out: re-opening an `unsubscribed` row when that address signs up again.
Unsubscribe stays terminal here; a rejoin path is follow-up work.
