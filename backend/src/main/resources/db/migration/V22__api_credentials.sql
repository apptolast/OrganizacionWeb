CREATE TABLE api_credentials (
  id UUID PRIMARY KEY,
  owner_id TEXT NOT NULL,
  name TEXT NOT NULL,
  scopes TEXT[] NOT NULL CHECK (cardinality(scopes) BETWEEN 1 AND 6 AND array_position(scopes, NULL) IS NULL AND scopes <@ ARRAY['projects:read','projects:write','tasks:read','tasks:write','agenda:read','history:read']::text[]),
  expires_in_days INTEGER NOT NULL,
  verifier BYTEA NOT NULL CHECK (octet_length(verifier) = 32),
  created_at TIMESTAMPTZ NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ
);
CREATE INDEX api_credentials_owner_history ON api_credentials (owner_id, created_at DESC, id DESC);

CREATE TABLE api_owner_quotas (
  owner_id TEXT PRIMARY KEY,
  window_start TIMESTAMPTZ NOT NULL,
  used INTEGER NOT NULL CHECK (used BETWEEN 0 AND 120)
);
CREATE TABLE api_credential_quotas (
  credential_id UUID PRIMARY KEY REFERENCES api_credentials(id),
  window_start TIMESTAMPTZ NOT NULL,
  used INTEGER NOT NULL CHECK (used BETWEEN 0 AND 60)
);
