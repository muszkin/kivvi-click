-- Waitlist signup (PIO-70): the landing page's e-mail capture and its abuse counter.
--
-- V1__baseline.sql is the frozen cutover baseline and is never edited — new schema always
-- arrives as a new versioned migration (BACKWARD_COMPATIBILITY.md, "Session and storage").
--
-- waitlist_subscriber holds one row per normalized address. The confirmation_token_* and
-- confirmed_at columns are created nullable and stay NULL in this ticket: PIO-71 (double
-- opt-in) fills them, and creating them now means that ticket never has to reshape a table
-- that already holds production rows.
CREATE TABLE waitlist_subscriber (
    id                            BIGSERIAL PRIMARY KEY,
    email                         VARCHAR(320) NOT NULL UNIQUE,
    status                        VARCHAR(16)  NOT NULL,
    source                        VARCHAR(32)  NOT NULL,
    locale                        VARCHAR(2)   NOT NULL,
    signed_up_at                  TIMESTAMPTZ  NOT NULL,
    consent_at                    TIMESTAMPTZ  NOT NULL,
    consent_ip                    VARCHAR(45),
    consent_user_agent            VARCHAR(512),
    -- The exact clause the visitor saw, not a key pointing at it: a consent record that
    -- resolves through today's copy is worthless the first time that copy is edited.
    consent_text                  TEXT         NOT NULL,
    confirmation_token_hash       CHAR(64),
    confirmation_token_expires_at TIMESTAMPTZ,
    confirmed_at                  TIMESTAMPTZ
);

CREATE INDEX waitlist_subscriber_status_idx ON waitlist_subscriber (status);

-- Hourly request counter, one row per bucket ("ip:<address>", "email:<address>").
-- Postgres backs everything in this stack — there is no Redis to hold a rate limiter, and
-- this table is the same shape of solution event_dedup already is for idempotency.
CREATE TABLE waitlist_throttle (
    bucket_key        VARCHAR(160) PRIMARY KEY,
    window_started_at TIMESTAMPTZ  NOT NULL,
    hits              INT          NOT NULL
);
