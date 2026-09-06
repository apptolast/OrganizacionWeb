package com.apptolast.organization.application;

import com.apptolast.organization.domain.TaskSnapshot;
import java.util.UUID;

public interface ReadTaskStatusUseCase {
  TaskSnapshot status(String owner, UUID project, UUID id);
}
