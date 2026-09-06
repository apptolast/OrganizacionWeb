package com.apptolast.organization.application;

import com.apptolast.organization.domain.PlannedBlock;
import java.util.UUID;

public interface BlockQueries {
  java.util.List<PlannedBlock> list(
      String owner, UUID project, UUID task, com.apptolast.organization.domain.BlockPosition after);

  PlannedBlock detail(String owner, UUID project, UUID task, UUID block);

  PlannedBlock byRequest(String owner, UUID project, UUID task, UUID key);
}
