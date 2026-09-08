package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record ImportReceipt(
    UUID requestKey,
    String fileSha256,
    long byteLength,
    Instant recordedAt,
    String outcome,
    ImportCounts insertedCounts,
    ImportCounts identicalCounts) {}
