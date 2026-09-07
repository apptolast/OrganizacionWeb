package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;

public interface CustomFieldValuesQueries {
  CustomFieldValues find(String owner, CustomizationScope scope, UUID projectId, UUID entityId);
}
