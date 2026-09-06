package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;

public interface ChangeTaskStatusUseCase {
  TaskSnapshot execute(String owner, UUID project, UUID id, TaskRevision expected, String target);
}
