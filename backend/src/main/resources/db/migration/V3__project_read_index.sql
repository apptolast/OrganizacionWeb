CREATE INDEX projects_owner_created_id_idx
    ON projects (owner_id, created_at DESC, id DESC);
