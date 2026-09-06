package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;
import java.util.function.Function;

public interface BlockEditing {
  BlockChangeConfirmation cancel(
      String owner,
      UUID project,
      UUID task,
      UUID block,
      UUID key,
      Function<BlockState, BlockMutation> operation);
}
