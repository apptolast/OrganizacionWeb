package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

public interface CustomFieldValuesEditing {
  CustomFieldValues changeValues(
      String owner,
      CustomizationScope scope,
      UUID projectId,
      UUID entityId,
      BiFunction<
              Optional<Customization>,
              Optional<CustomFieldValuesCollection>,
              CustomFieldValuesCollection>
          operation);
}
