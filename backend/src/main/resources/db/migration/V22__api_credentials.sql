CREATE TABLE api_credentials (
  id UUID PRIMARY KEY,
  owner_id TEXT NOT NULL,
  name TEXT NOT NULL,
  scopes TEXT[] NOT NULL,
  expires_in_days INTEGER NOT NULL,
  verifier BYTEA NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ
);
CREATE INDEX api_credentials_owner_history ON api_credentials (owner_id, created_at DESC, id DESC);
