package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;
import java.util.function.Function;

public interface BlockCommit {
  BlockCreation commit(
      String owner,
      UUID projectId,
      UUID taskId,
      UUID key,
      BlockRequest request,
      Function<BlockPlanningContext, BlockChange> operation);
}
