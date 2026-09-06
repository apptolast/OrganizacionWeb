CREATE TABLE tasks (
 id UUID PRIMARY KEY,
 project_id UUID NOT NULL REFERENCES projects(id),
 title TEXT NOT NULL CHECK (char_length(title) BETWEEN 1 AND 160),
 completion_criterion TEXT NOT NULL CHECK (char_length(completion_criterion)<=2000),
 estimated_minutes INTEGER CHECK (estimated_minutes BETWEEN 1 AND 1440),
 status TEXT NOT NULL CHECK (status='pending'),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX tasks_project_created_id ON tasks(project_id,created_at DESC,id DESC);
