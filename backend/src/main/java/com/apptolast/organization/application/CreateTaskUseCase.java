package com.apptolast.organization.application;

import com.apptolast.organization.domain.Task;
import java.util.UUID;

public interface CreateTaskUseCase {
  Task execute(String ownerId, UUID projectId, String title, String criterion, Integer minutes);
}
