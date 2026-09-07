CREATE UNIQUE INDEX work_sessions_owner_key ON work_sessions(owner_id,request_key);
CREATE UNIQUE INDEX work_sessions_one_running_owner ON work_sessions(owner_id) WHERE status='running';
ALTER TABLE work_sessions ADD CONSTRAINT work_sessions_task_project
 FOREIGN KEY(project_id,task_id) REFERENCES tasks(project_id,id);
