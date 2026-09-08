package com.apptolast.organization.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportPreview(
    String fileSha256,
    long byteLength,
    String owner,
    Instant exportedAt,
    ImportCounts counts,
    ImportCounts insertCounts,
    ImportCounts identicalCounts,
    List<RunningSession> runningSessions) {
  public record RunningSession(UUID sessionId, Instant runningSince) {}
}
