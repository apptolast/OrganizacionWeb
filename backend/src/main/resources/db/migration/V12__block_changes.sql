CREATE TABLE block_projections (
 block_id UUID PRIMARY KEY REFERENCES planned_blocks(id),
 version BIGINT NOT NULL,
 status TEXT NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 start_local TIMESTAMP,
 end_local TIMESTAMP,
 zone_id TEXT,
 start_offset TEXT,
 end_offset TEXT,
 start_at TIMESTAMPTZ,
 end_at TIMESTAMPTZ,
 duration_minutes INTEGER
);
CREATE TABLE block_changes (
 id UUID PRIMARY KEY,
 project_id UUID NOT NULL,
 task_id UUID NOT NULL,
 block_id UUID NOT NULL REFERENCES planned_blocks(id),
 request_key UUID NOT NULL,
 kind TEXT NOT NULL,
 version BIGINT NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL,
 receipt JSONB NOT NULL,
 FOREIGN KEY(project_id,task_id) REFERENCES tasks(project_id,id),
 UNIQUE(task_id,request_key)
);
