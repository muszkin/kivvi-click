-- Waitlist double opt-in (PIO-71): the proof a confirmation actually happened, the never-expiring
-- unsubscribe token, and the outbox the confirmation mail travels in.
--
-- V1__baseline.sql is the frozen cutover baseline and V2__waitlist.sql already holds production
-- rows, so both stay untouched — new schema always arrives as a new versioned migration
-- (BACKWARD_COMPATIBILITY.md, "Session and storage").
--
-- Every column added here is nullable and every existing row keeps working unchanged: a subscriber
-- captured by PIO-70 is simply one that has not confirmed yet, which is exactly what it was.

ALTER TABLE waitlist_subscriber
    -- The unsubscribe token is deliberately separate from the confirmation one. The confirmation
    -- token is single-use and lapses after 7 days; this one travels in the footer of every mail we
    -- will ever send and has to still work a year later, or the only way out of the list becomes
    -- writing to support.
    ADD COLUMN unsubscribe_token_hash CHAR(64),
    ADD COLUMN unsubscribed_at        TIMESTAMPTZ,
    -- The consent proof's other half. consent_ip/consent_user_agent (V2) record who asked to join;
    -- these record who proved they own the mailbox. Widths mirror their V2 counterparts exactly.
    ADD COLUMN confirmed_ip           VARCHAR(45),
    ADD COLUMN confirmed_user_agent   VARCHAR(512);

-- Partial unique indexes: a token hash identifies exactly one subscriber, but the overwhelming
-- majority of rows have none (confirmed rows have their confirmation token cleared, and rows
-- captured before this migration have neither). A plain unique index would have to treat every one
-- of those NULLs as distinct anyway; WHERE ... IS NOT NULL says so out loud and keeps the index
-- small enough to stay in cache.
CREATE UNIQUE INDEX waitlist_subscriber_confirmation_token_idx
    ON waitlist_subscriber (confirmation_token_hash)
    WHERE confirmation_token_hash IS NOT NULL;

CREATE UNIQUE INDEX waitlist_subscriber_unsubscribe_token_idx
    ON waitlist_subscriber (unsubscribe_token_hash)
    WHERE unsubscribe_token_hash IS NOT NULL;

-- The mail outbox. Postgres backs everything in this stack — there is no broker to hand a message
-- to (CLAUDE.md, "Architecture decisions") — so a row here is the handoff: the request inserts it
-- in the same transaction that changes the subscriber, and a scheduled sender drains it. Either we
-- have both the state change and the mail, or neither, which is the whole point of an outbox.
--
-- This is an outbox for MAIL, not a general-purpose queue. Nothing but e-mail belongs in it.
CREATE TABLE mail_outbox (
    id              BIGSERIAL PRIMARY KEY,
    -- Guards against two concurrent requests queueing the same message twice. Nullable because a
    -- caller that genuinely wants two identical mails (a resend the visitor asked for) supplies no
    -- key, and Postgres treats every NULL in a unique index as distinct.
    dedup_key       VARCHAR(160) UNIQUE,
    recipient       VARCHAR(320) NOT NULL,
    subject         VARCHAR(512) NOT NULL,
    html_body       TEXT         NOT NULL,
    -- Not nullable on purpose: a message with no plain-text alternative scores worse with spam
    -- filters and is unreadable in a text client.
    text_body       TEXT         NOT NULL,
    -- pending | sending | sent | failed.
    status          VARCHAR(16)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    -- Doubles as the claim lease: claiming a row pushes this into the future, so a row abandoned by
    -- a process that died mid-send becomes due again on its own, with no separate recovery job.
    next_attempt_at TIMESTAMPTZ  NOT NULL,
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    sent_at         TIMESTAMPTZ
);

-- The sender's only query is "what is due now", in that column order.
CREATE INDEX mail_outbox_due_idx ON mail_outbox (status, next_attempt_at);
