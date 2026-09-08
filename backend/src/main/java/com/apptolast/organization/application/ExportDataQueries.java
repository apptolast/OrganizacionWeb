package com.apptolast.organization.application;

import java.time.Instant;
import java.util.function.Supplier;

public interface ExportDataQueries {
  PreparedExport prepare(String owner, Supplier<Instant> timestamp);
}