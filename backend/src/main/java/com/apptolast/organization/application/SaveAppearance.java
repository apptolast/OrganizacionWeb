package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

public final class SaveAppearance implements SaveAppearanceUseCase {
  private final AppearanceEditing store;
  private final Clock clock;

  public SaveAppearance(AppearanceEditing store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public Appearance execute(String owner, AppearanceRevision expected, String theme, String accentLight, String accentDark) {
    return store.save(owner, prior -> {
      if (prior.isPresent() && !prior.get().id().equals(expected.id())) throw new AppearanceConflictException();
      return new Appearance(UUID.randomUUID(), owner, theme,
        accentLight.toUpperCase(Locale.ROOT), accentDark.toUpperCase(Locale.ROOT), 0,
        clock.instant().truncatedTo(ChronoUnit.MICROS));
    });
  }
}
