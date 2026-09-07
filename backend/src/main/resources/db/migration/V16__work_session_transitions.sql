ALTER TABLE work_sessions
 ADD COLUMN revision BIGINT NOT NULL DEFAULT 1,
 ADD COLUMN changed_at TIMESTAMPTZ,
 ADD COLUMN worked_microseconds BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN running_since TIMESTAMPTZ;

ALTER TABLE work_sessions ADD CONSTRAINT work_sessions_work_nonnegative CHECK(worked_microseconds >= 0);

DROP INDEX work_sessions_one_running_owner;
CREATE UNIQUE INDEX work_sessions_one_open_owner ON work_sessions(owner_id) WHERE status IN ('running','paused');

CREATE TABLE work_session_intervals (
 session_id UUID NOT NULL REFERENCES work_sessions(id),
 revision BIGINT NOT NULL,
 start_at TIMESTAMPTZ NOT NULL,
 end_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(session_id, revision)
);

CREATE TABLE work_session_changes (
 id UUID PRIMARY KEY,
 owner_id TEXT NOT NULL,
 session_id UUID NOT NULL REFERENCES work_sessions(id),
 request_key UUID NOT NULL,
 action TEXT NOT NULL,
 expected_revision BIGINT NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL,
 receipt JSONB NOT NULL,
 UNIQUE(owner_id,request_key)
);
