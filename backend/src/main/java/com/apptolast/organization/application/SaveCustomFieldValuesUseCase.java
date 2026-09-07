package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.List;
import java.util.UUID;

public interface SaveCustomFieldValuesUseCase {
  CustomFieldValues save(
      String owner,
      CustomizationScope scope,
      UUID projectId,
      UUID entityId,
      CustomFieldValuesRevision expected,
      List<CustomFieldInput> values);
}
