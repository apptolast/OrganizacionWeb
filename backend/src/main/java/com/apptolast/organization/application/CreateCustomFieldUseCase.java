package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;

public interface CreateCustomFieldUseCase {
  Customization create(
      String owner,
      CustomizationScope scope,
      CustomizationRevision expected,
      String label,
      CustomFieldType type);
}
