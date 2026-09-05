package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;

public interface EditProjectUseCase {
  ProjectSnapshot execute(
      String ownerId, UUID id, ProjectRevision expected, String name, String description);
}
