package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;

public interface ReadCustomFieldValuesUseCase {
  CustomFieldValues get(String owner, CustomizationScope scope, UUID projectId, UUID entityId);
}
