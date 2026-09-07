package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.Optional;
import java.util.function.Function;

public interface CustomizationEditing {
  Customization change(
      String owner,
      CustomizationScope scope,
      Function<Optional<Customization>, Customization> operation);
}
