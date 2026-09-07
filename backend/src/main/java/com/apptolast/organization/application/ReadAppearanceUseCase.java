package com.apptolast.organization.application;

import com.apptolast.organization.domain.Appearance;
import java.util.Optional;

public interface ReadAppearanceUseCase {
  Optional<Appearance> get(String owner);
}
