CREATE TABLE import_receipts (
    owner_id TEXT NOT NULL,
    request_key UUID NOT NULL,
    file_sha256 TEXT NOT NULL CHECK (file_sha256 ~ '^[0-9a-f]{64}$'),
    byte_length BIGINT NOT NULL CHECK (byte_length BETWEEN 0 AND 33554432),
    recorded_at TIMESTAMPTZ NOT NULL,
    outcome TEXT NOT NULL CHECK (outcome IN ('IMPORTED', 'NO_CHANGE')),
    inserted_counts JSONB NOT NULL CHECK (jsonb_typeof(inserted_counts) = 'object'),
    identical_counts JSONB NOT NULL CHECK (jsonb_typeof(identical_counts) = 'object'),
    PRIMARY KEY (owner_id, request_key)
);
