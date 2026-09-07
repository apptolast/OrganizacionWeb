package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;

public interface UpdateCustomFieldUseCase {
  Customization update(
      String owner,
      CustomizationScope scope,
      UUID fieldId,
      CustomizationRevision expected,
      String label,
      boolean active);
}
