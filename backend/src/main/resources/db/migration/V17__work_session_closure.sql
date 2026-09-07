CREATE UNIQUE INDEX work_session_one_closure
 ON work_session_changes(session_id) WHERE action='CLOSE';
