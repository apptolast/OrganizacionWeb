CREATE TABLE webhook_endpoints (
  id UUID PRIMARY KEY,
  owner_id TEXT NOT NULL,
  url TEXT NOT NULL CHECK (char_length(url) BETWEEN 1 AND 2048),
  description TEXT NOT NULL CHECK (char_length(description) <= 80),
  event_types TEXT[] NOT NULL CHECK (
    cardinality(event_types) BETWEEN 1 AND 12
    AND array_position(event_types, NULL) IS NULL
    AND event_types <@ ARRAY[
      'ProjectCreated.v1','ProjectUpdated.v1','ProjectStatusChanged.v1',
      'TaskCreated.v1','SubtaskCreated.v1','TaskStatusChanged.v1',
      'BlockPlanned.v1','BlockChanged.v1',
      'WorkSessionStarted.v1','WorkSessionStateChanged.v1',
      'WorkSessionExtended.v1','WorkSessionClosed.v1']::text[]),
  status TEXT NOT NULL CHECK (status IN ('active', 'disabled')),
  disabled_reason TEXT CHECK (disabled_reason IN ('MANUAL', 'DELIVERY_EXHAUSTED')),
  disabled_at TIMESTAMPTZ,
  secret_ciphertext BYTEA NOT NULL CHECK (octet_length(secret_ciphertext) > 0),
  cursor_occurred_at TIMESTAMPTZ NOT NULL,
  cursor_event_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  -- A disabled endpoint always carries both its reason and its instant; an active one carries neither.
  CHECK ((status = 'disabled') = (disabled_reason IS NOT NULL)),
  CHECK ((disabled_reason IS NULL) = (disabled_at IS NULL))
);
CREATE INDEX webhook_endpoints_owner_history
  ON webhook_endpoints (owner_id, created_at DESC, id DESC);

CREATE TABLE webhook_deliveries (
  id UUID PRIMARY KEY,
  endpoint_id UUID NOT NULL REFERENCES webhook_endpoints(id) ON DELETE CASCADE,
  owner_id TEXT NOT NULL,
  event_id UUID NOT NULL,
  event_type TEXT NOT NULL,
  body TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('pending', 'succeeded', 'exhausted')),
  attempt INTEGER NOT NULL CHECK (attempt BETWEEN 0 AND 6),
  http_status INTEGER CHECK (http_status BETWEEN 100 AND 599),
  latency_ms INTEGER CHECK (latency_ms >= 0),
  error_class TEXT CHECK (error_class IN (
    'HTTP_ERROR', 'REDIRECT', 'TIMEOUT', 'CONNECTION', 'TLS', 'DNS', 'BLOCKED_ADDRESS')),
  next_attempt_at TIMESTAMPTZ,
  -- Amendment B1: the claim is a lease, so the HTTP exchange happens outside any transaction.
  leased_until TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  -- Only a pending delivery is ever scheduled for another attempt.
  CHECK ((status = 'pending') = (next_attempt_at IS NOT NULL))
);
CREATE INDEX webhook_deliveries_endpoint_log
  ON webhook_deliveries (endpoint_id, updated_at DESC, id DESC);
CREATE INDEX webhook_deliveries_due
  ON webhook_deliveries (next_attempt_at, id) WHERE status = 'pending';
