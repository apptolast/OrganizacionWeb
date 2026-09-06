package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;

public interface ReadTaskHistoryUseCase {
  TaskHistoryPage list(String owner, UUID project, UUID task, TaskHistoryPosition after);
}
