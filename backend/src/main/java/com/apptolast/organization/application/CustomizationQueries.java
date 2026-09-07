package com.apptolast.organization.application;

import com.apptolast.organization.domain.Customization;
import com.apptolast.organization.domain.CustomizationScope;
import java.util.Optional;

public interface CustomizationQueries {
  Optional<Customization> find(String owner, CustomizationScope scope);
}
