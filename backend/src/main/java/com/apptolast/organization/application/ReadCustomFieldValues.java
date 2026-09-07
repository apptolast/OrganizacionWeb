package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;

public final class ReadCustomFieldValues implements ReadCustomFieldValuesUseCase {
  private final CustomFieldValuesQueries queries;

  public ReadCustomFieldValues(CustomFieldValuesQueries queries) {
    this.queries = queries;
  }

  public CustomFieldValues get(
      String owner, CustomizationScope scope, UUID projectId, UUID entityId) {
    return queries.find(owner, scope, projectId, entityId);
  }
}
