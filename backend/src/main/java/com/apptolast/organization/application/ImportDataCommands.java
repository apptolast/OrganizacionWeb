package com.apptolast.organization.application;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

public interface ImportDataCommands {
  ImportReceipt apply(
      String owner,
      UUID requestKey,
      String expectedSha256,
      InputStream body,
      Supplier<Instant> recordedAt);
}
