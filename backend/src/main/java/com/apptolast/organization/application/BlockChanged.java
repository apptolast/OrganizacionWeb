package com.apptolast.organization.application;
import java.time.Instant;
import java.util.UUID;
import com.apptolast.organization.domain.PlannedBlock;
public record BlockChanged(UUID eventId, UUID aggregateId, String ownerId, Instant occurredAt, int schemaVersion, String type, UUID changeId, UUID blockId, UUID taskId, String kind, long revision, Interval before, Interval after) {
  public record Interval(Instant startAt, Instant endAt, String zoneId, int durationMinutes) {
    public static Interval from(PlannedBlock block) { return new Interval(block.time().startAt(),block.time().endAt(),block.request().zoneId(),block.time().durationMinutes()); }
  }
}
