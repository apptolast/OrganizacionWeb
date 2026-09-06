ALTER TABLE tasks ADD COLUMN version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0);
ALTER TABLE tasks ADD COLUMN completed_at TIMESTAMPTZ;
ALTER TABLE tasks DROP CONSTRAINT tasks_status_check;
ALTER TABLE tasks ADD CONSTRAINT tasks_status_check CHECK (status IN ('pending','completed'));
ALTER TABLE tasks ADD CONSTRAINT tasks_completion_consistent CHECK ((status='pending' AND completed_at IS NULL) OR (status='completed' AND completed_at IS NOT NULL AND completed_at=updated_at));
CREATE TABLE task_status_history (
 id UUID PRIMARY KEY,
 project_id UUID NOT NULL,
 task_id UUID NOT NULL,
 task_version BIGINT NOT NULL CHECK (task_version > 0),
 from_status TEXT NOT NULL CHECK (from_status IN ('pending','completed')),
 to_status TEXT NOT NULL CHECK (to_status IN ('pending','completed')),
 occurred_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT task_history_real_transition CHECK (from_status <> to_status),
 CONSTRAINT task_history_task FOREIGN KEY (project_id,task_id) REFERENCES tasks(project_id,id),
 UNIQUE(task_id,task_version)
);
