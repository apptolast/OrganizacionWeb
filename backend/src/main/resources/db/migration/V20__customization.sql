CREATE TABLE customization_preferences (
    id UUID PRIMARY KEY,
    owner_id TEXT NOT NULL,
    scope TEXT NOT NULL CHECK (scope IN ('PROJECT', 'TASK')),
    visible_fields JSONB NOT NULL CHECK (jsonb_typeof(visible_fields) = 'array'),
    custom_fields JSONB NOT NULL CHECK (jsonb_typeof(custom_fields) = 'array' AND jsonb_array_length(custom_fields) <= 12),
    version BIGINT NOT NULL CHECK (version >= 0),
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (owner_id, scope)
);

CREATE TABLE project_custom_field_values (
    id UUID PRIMARY KEY,
    owner_id TEXT NOT NULL,
    project_id UUID NOT NULL REFERENCES projects(id),
    field_values JSONB NOT NULL CHECK (jsonb_typeof(field_values) = 'object'),
    version BIGINT NOT NULL CHECK (version >= 0),
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (owner_id, project_id)
);

CREATE TABLE task_custom_field_values (
    id UUID PRIMARY KEY,
    owner_id TEXT NOT NULL,
    task_id UUID NOT NULL REFERENCES tasks(id),
    field_values JSONB NOT NULL CHECK (jsonb_typeof(field_values) = 'object'),
    version BIGINT NOT NULL CHECK (version >= 0),
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (owner_id, task_id)
);
