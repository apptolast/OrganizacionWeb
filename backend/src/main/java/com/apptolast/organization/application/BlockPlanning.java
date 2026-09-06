package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;
import java.util.function.Function;

public interface BlockPlanning {
  BlockPreview preview(
      String owner,
      UUID projectId,
      UUID taskId,
      Function<BlockPlanningContext, BlockPreview> operation);
}
