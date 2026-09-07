ALTER TABLE work_sessions
 ADD COLUMN effective_end_at TIMESTAMPTZ,
 ADD COLUMN last_decision_at TIMESTAMPTZ;
ALTER TABLE work_sessions
 ADD CONSTRAINT work_sessions_end_not_before_plan CHECK (effective_end_at >= planned_end_at);
