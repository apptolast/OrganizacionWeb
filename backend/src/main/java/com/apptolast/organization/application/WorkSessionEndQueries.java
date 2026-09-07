package com.apptolast.organization.application;

import java.util.UUID;
import java.util.function.Function;

public interface WorkSessionEndQueries {
  WorkSessionEndSnapshot readEnd(
      String owner, UUID session, Function<WorkSessionEnd, WorkSessionEndSnapshot> snapshot);
}
