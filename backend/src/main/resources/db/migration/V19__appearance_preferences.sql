CREATE TABLE appearance_preferences (
    id UUID PRIMARY KEY,
    owner_id TEXT NOT NULL UNIQUE,
    theme TEXT NOT NULL CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM')),
    accent_light TEXT NOT NULL CHECK (accent_light ~ '^#[0-9A-F]{6}$'),
    accent_dark TEXT NOT NULL CHECK (accent_dark ~ '^#[0-9A-F]{6}$'),
    version BIGINT NOT NULL CHECK (version >= 0),
    updated_at TIMESTAMPTZ NOT NULL
);
