package com.apptolast.organization.application;

import com.apptolast.organization.domain.CalendarSnapshot;
import java.time.Instant;

public interface CalendarQueries {
  /** Reads the availability zone and every current block of the window from a single snapshot. */
  CalendarSnapshot read(String owner, Instant from, Instant to);
}
