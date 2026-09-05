ALTER TABLE outbox_events
    ADD COLUMN attempts BIGINT NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT '-infinity',
    ADD COLUMN published_at TIMESTAMPTZ,
    ADD COLUMN last_error_code TEXT;
