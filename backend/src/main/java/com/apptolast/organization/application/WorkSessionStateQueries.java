package com.apptolast.organization.application;

import com.apptolast.organization.domain.WorkSessionState;
import java.util.UUID;
import java.util.function.Function;

public interface WorkSessionStateQueries {
  WorkSessionSnapshot read(
      String owner, UUID session, Function<WorkSessionState, WorkSessionSnapshot> snapshot);
}
