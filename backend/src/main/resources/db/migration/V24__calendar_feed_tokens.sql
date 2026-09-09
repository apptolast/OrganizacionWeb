CREATE TABLE calendar_feed_tokens (
 owner_id TEXT PRIMARY KEY,
 token_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(token_hash) = 32),
 created_at TIMESTAMPTZ NOT NULL
);
