package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;

public interface SaveAvailabilityUseCase {
  Availability execute(
      String owner,
      AvailabilityRevision expected,
      String zone,
      java.util.Map<java.time.DayOfWeek, Integer> days);
}
