package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.List;

public interface SaveCustomizationViewUseCase {
  Customization save(
      String owner,
      CustomizationScope scope,
      CustomizationRevision expected,
      List<String> visibleFields);
}
