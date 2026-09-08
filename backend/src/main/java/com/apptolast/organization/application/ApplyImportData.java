package com.apptolast.organization.application;

import java.io.InputStream;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class ApplyImportData implements ApplyImportDataUseCase {
  private final ImportDataCommands commands;
  private final Clock clock;

  public ApplyImportData(ImportDataCommands commands, Clock clock) {
    this.commands = commands;
    this.clock = clock;
  }

  public ImportReceipt apply(String owner, UUID key, String expectedSha256, InputStream body) {
    return commands.apply(
        owner, key, expectedSha256, body, () -> clock.instant().truncatedTo(ChronoUnit.MICROS));
  }
}
