package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;

public interface MoveBlockUseCase {
  BlockPreview preview(
      String owner, UUID project, UUID task, UUID block, long expected, BlockMoveRequest request);

  BlockChangeConfirmation move(
      String owner,
      UUID project,
      UUID task,
      UUID block,
      UUID key,
      long expected,
      AvailabilityRevision availability,
      BlockMoveRequest request);
}
