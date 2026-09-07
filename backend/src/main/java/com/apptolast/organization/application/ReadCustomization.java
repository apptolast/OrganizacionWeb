package com.apptolast.organization.application;

import com.apptolast.organization.domain.Customization;
import com.apptolast.organization.domain.CustomizationScope;
import java.util.Optional;

public final class ReadCustomization implements ReadCustomizationUseCase {
  private final CustomizationQueries queries;

  public ReadCustomization(CustomizationQueries queries) {
    this.queries = queries;
  }

  public Optional<Customization> get(String owner, CustomizationScope scope) {
    return queries.find(owner, scope);
  }
}
