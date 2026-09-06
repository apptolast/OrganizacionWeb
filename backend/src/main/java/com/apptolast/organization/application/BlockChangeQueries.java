package com.apptolast.organization.application;

import com.apptolast.organization.domain.BlockChangePosition;
import com.apptolast.organization.domain.BlockChangeReceipt;
import java.util.List;
import java.util.UUID;

public interface BlockChangeQueries {
  List<BlockChangeReceipt> list(String owner, UUID project, UUID task, BlockChangePosition after);

  com.apptolast.organization.domain.BlockChangeReceipt detail(
      String owner, UUID project, UUID task, UUID id);

  com.apptolast.organization.domain.BlockChangeReceipt byRequest(
      String owner, UUID project, UUID task, UUID key);
}
