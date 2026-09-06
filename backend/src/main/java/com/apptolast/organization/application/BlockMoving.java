package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;
import java.util.function.Function;

public interface BlockMoving {
  BlockPreview preview(
      String owner,
      UUID project,
      UUID task,
      UUID block,
      Function<MoveContext, BlockPreview> operation);

  BlockChangeConfirmation commit(
      String owner,
      UUID project,
      UUID task,
      UUID block,
      UUID key,
      BlockMoveRequest request,
      Function<MoveContext, BlockMutation> operation);
}
