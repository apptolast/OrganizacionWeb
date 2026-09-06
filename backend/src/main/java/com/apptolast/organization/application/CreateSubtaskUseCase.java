package com.apptolast.organization.application;

import com.apptolast.organization.domain.Task;
import java.util.UUID;

public interface CreateSubtaskUseCase {
  Task execute(
      String ownerId,
      UUID projectId,
      UUID parentId,
      String title,
      String criterion,
      Integer minutes);
}
