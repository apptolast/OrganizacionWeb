package com.apptolast.organization.application;

import com.apptolast.organization.domain.Appearance;
import java.util.Optional;

public final class ReadAppearance implements ReadAppearanceUseCase {
  private final AppearanceQueries queries;

  public ReadAppearance(AppearanceQueries queries) {
    this.queries = queries;
  }

  public Optional<Appearance> get(String owner) {
    return queries.find(owner);
  }
}
