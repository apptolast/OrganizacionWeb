CREATE TABLE automation_rules (
 id UUID PRIMARY KEY,
 owner_id TEXT NOT NULL,
 name TEXT NOT NULL CHECK (char_length(name) BETWEEN 1 AND 80),
 enabled BOOLEAN NOT NULL,
 event_type TEXT NOT NULL,
 condition_project_id UUID NULL,
 action JSONB NOT NULL CHECK (jsonb_typeof(action) = 'object'),
 version BIGINT NOT NULL CHECK (version >= 1),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX automation_rules_owner_created_id ON automation_rules(owner_id, created_at, id);

-- rule_id survives as NULL so the loop guard keeps seeing created_task_id after a rule is deleted.
CREATE TABLE automation_runs (
 id UUID PRIMARY KEY,
 rule_id UUID NULL REFERENCES automation_rules(id) ON DELETE SET NULL,
 owner_id TEXT NOT NULL,
 event_id UUID NOT NULL,
 event_type TEXT NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL,
 attempt INTEGER NOT NULL CHECK (attempt BETWEEN 1 AND 3),
 status TEXT NOT NULL CHECK (status IN ('succeeded', 'retry', 'failed')),
 created_task_id UUID NULL,
 delivery_id UUID NULL,
 error_code TEXT NULL,
 executed_at TIMESTAMPTZ NOT NULL,
 UNIQUE (rule_id, event_id)
);
CREATE INDEX automation_runs_rule_executed_id ON automation_runs(rule_id, executed_at DESC, id DESC);
CREATE INDEX automation_runs_created_task ON automation_runs(created_task_id);

-- Feature 25 has not shipped a per-consumer cursor table yet, so automations keeps its own.
CREATE TABLE automation_cursors (
 owner_id TEXT PRIMARY KEY,
 occurred_at TIMESTAMPTZ NOT NULL,
 event_id UUID NOT NULL
);
