package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;

public interface SaveAppearanceUseCase {
  Appearance execute(String owner, AppearanceRevision expected, String theme, String accentLight, String accentDark);
}
