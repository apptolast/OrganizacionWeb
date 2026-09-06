ALTER TABLE tasks ADD COLUMN parent_id UUID;
ALTER TABLE tasks ADD CONSTRAINT tasks_project_identity UNIQUE (project_id, id);
ALTER TABLE tasks ADD CONSTRAINT tasks_parent_same_project FOREIGN KEY (project_id, parent_id) REFERENCES tasks(project_id, id);
ALTER TABLE tasks ADD CONSTRAINT tasks_parent_not_self CHECK (parent_id <> id);
CREATE INDEX tasks_direct_children ON tasks(project_id, parent_id, created_at DESC, id DESC);
