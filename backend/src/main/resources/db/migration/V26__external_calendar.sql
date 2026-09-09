-- Feature 28: suscripción de solo lectura a un calendario iCalendar externo.
-- Una suscripción como máximo por propietario; la instantánea vive solo en la tabla de eventos.

CREATE TABLE external_calendar_subscriptions (
    owner_id TEXT PRIMARY KEY,
    id UUID NOT NULL UNIQUE,
    label TEXT NOT NULL CHECK (char_length(label) BETWEEN 1 AND 40),
    url_ciphertext BYTEA NOT NULL CHECK (octet_length(url_ciphertext) > 12),
    url_host TEXT NOT NULL CHECK (char_length(url_host) BETWEEN 1 AND 253),
    url_tail TEXT NOT NULL CHECK (char_length(url_tail) = 4),
    version BIGINT NOT NULL CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_attempt_at TIMESTAMPTZ,
    last_sync_at TIMESTAMPTZ,
    last_status TEXT CHECK (last_status IN ('OK', 'FAILED')),
    last_error TEXT CHECK (last_error IN (
        'FEED_REJECTED', 'FEED_UNREACHABLE', 'FEED_HTTP_ERROR', 'FEED_TOO_LARGE',
        'FEED_UNSUPPORTED_TYPE', 'FEED_MALFORMED', 'SECRET_UNREADABLE')),
    snapshot_zone_id TEXT,
    imported INTEGER NOT NULL DEFAULT 0 CHECK (imported >= 0),
    skipped_recurring INTEGER NOT NULL DEFAULT 0 CHECK (skipped_recurring >= 0),
    skipped_cancelled INTEGER NOT NULL DEFAULT 0 CHECK (skipped_cancelled >= 0),
    skipped_invalid INTEGER NOT NULL DEFAULT 0 CHECK (skipped_invalid >= 0),
    truncated BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT external_calendar_error_needs_failure
        CHECK (last_error IS NULL OR last_status = 'FAILED')
);

CREATE TABLE external_calendar_events (
    owner_id TEXT NOT NULL
        REFERENCES external_calendar_subscriptions (owner_id) ON DELETE CASCADE,
    uid TEXT NOT NULL,
    summary TEXT NOT NULL CHECK (char_length(summary) <= 500),
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    all_day BOOLEAN NOT NULL,
    PRIMARY KEY (owner_id, uid),
    CONSTRAINT external_calendar_event_ends_after_it_starts CHECK (end_at > start_at)
);

CREATE INDEX external_calendar_events_by_start
    ON external_calendar_events (owner_id, start_at, uid);
