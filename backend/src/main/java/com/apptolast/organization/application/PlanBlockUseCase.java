package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;

public interface PlanBlockUseCase {
  BlockCreation create(
      String owner,
      UUID projectId,
      UUID taskId,
      UUID key,
      AvailabilityRevision expected,
      BlockRequest request);

  BlockPreview preview(String owner, UUID projectId, UUID taskId, BlockRequest request);
}
