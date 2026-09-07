package com.apptolast.organization.application;

import com.apptolast.organization.domain.Customization;
import com.apptolast.organization.domain.CustomizationScope;
import java.util.Optional;

public interface ReadCustomizationUseCase {
  Optional<Customization> get(String owner, CustomizationScope scope);
}
