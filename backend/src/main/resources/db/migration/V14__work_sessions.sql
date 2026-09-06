CREATE TABLE work_sessions (
 id UUID PRIMARY KEY,
 owner_id TEXT NOT NULL,
 project_id UUID NOT NULL REFERENCES projects(id),
 task_id UUID NOT NULL REFERENCES tasks(id),
 request_key UUID NOT NULL,
 started_at TIMESTAMPTZ NOT NULL,
 planned_minutes INTEGER NOT NULL,
 planned_end_at TIMESTAMPTZ NOT NULL,
 zone_id TEXT NOT NULL,
 status TEXT NOT NULL
);
ALTER TABLE outbox_events DROP CONSTRAINT outbox_events_aggregate_id_fkey;
