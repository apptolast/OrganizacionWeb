ALTER TABLE block_projections ADD CONSTRAINT block_projection_version_positive CHECK (version > 0);
ALTER TABLE block_projections ADD CONSTRAINT block_projection_status CHECK (status IN ('planned','cancelled'));
ALTER TABLE block_projections ADD CONSTRAINT block_projection_complete_interval
 CHECK (num_nonnulls(start_local,end_local,zone_id,start_offset,end_offset,start_at,end_at,duration_minutes) IN (0,8));
ALTER TABLE block_projections ADD CONSTRAINT block_projection_duration_matches
 CHECK (end_at - start_at = make_interval(mins => duration_minutes));
ALTER TABLE block_projections ADD CONSTRAINT block_projection_duration_range
 CHECK (duration_minutes BETWEEN 1 AND 1440);
ALTER TABLE block_projections ADD CONSTRAINT block_projection_whole_seconds
 CHECK (date_trunc('second',start_at)=start_at AND date_trunc('second',end_at)=end_at);
ALTER TABLE block_projections ADD CONSTRAINT block_projection_instant_range
 CHECK (start_at >= TIMESTAMPTZ '0001-01-01 00:00:00+00' AND end_at < TIMESTAMPTZ '10000-01-01 00:00:00+00');
ALTER TABLE block_projections ADD CONSTRAINT block_projection_local_minutes
 CHECK (date_trunc('minute',start_local)=start_local AND date_trunc('minute',end_local)=end_local);
ALTER TABLE block_projections ADD CONSTRAINT block_projection_local_range
 CHECK (start_local >= TIMESTAMP '0001-01-01 00:00:00' AND start_local < TIMESTAMP '10000-01-01 00:00:00'
        AND end_local >= TIMESTAMP '0001-01-01 00:00:00' AND end_local < TIMESTAMP '10000-01-01 00:00:00');
ALTER TABLE block_projections ADD CONSTRAINT block_projection_nonempty_zone CHECK (btrim(zone_id) <> '');
ALTER TABLE block_changes ADD CONSTRAINT block_change_version_positive CHECK (version > 0);
ALTER TABLE block_changes ADD CONSTRAINT block_change_kind CHECK (kind IN ('RESCHEDULED','CANCELLED'));
ALTER TABLE block_changes ADD CONSTRAINT block_change_block_version UNIQUE (block_id,version);
ALTER TABLE planned_blocks ADD CONSTRAINT planned_block_context UNIQUE (project_id,task_id,id);
ALTER TABLE block_changes ADD CONSTRAINT block_change_context
 FOREIGN KEY (project_id,task_id,block_id) REFERENCES planned_blocks(project_id,task_id,id);
ALTER TABLE block_changes ADD CONSTRAINT block_change_receipt_object CHECK (jsonb_typeof(receipt) = 'object');
