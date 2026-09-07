ALTER TABLE work_sessions
 ADD COLUMN effective_end_at TIMESTAMPTZ,
 ADD COLUMN last_decision_at TIMESTAMPTZ;
