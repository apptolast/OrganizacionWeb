package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class SaveAppearance implements SaveAppearanceUseCase {
  private final AppearanceEditing store;
  private final Clock clock;

  public SaveAppearance(AppearanceEditing store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public Appearance execute(
      String owner,
      AppearanceRevision expected,
      String theme,
      String accentLight,
      String accentDark) {
    var values = new AppearanceValues(theme, accentLight, accentDark);
    return store.save(
        owner,
        prior -> {
          if (prior.isEmpty()
              ? expected.id() != null
              : (!prior.get().id().equals(expected.id())
                  || prior.get().version() != expected.version()))
            throw new AppearanceConflictException();
          if (prior.isPresent()
              && prior.get().theme().equals(values.theme())
              && prior.get().accentLight().equals(values.accentLight())
              && prior.get().accentDark().equals(values.accentDark())) return prior.get();
          if (prior.isPresent() && prior.get().version() == Long.MAX_VALUE)
            throw new StorageUnavailableException(
                new ArithmeticException("Appearance revision exhausted"));
          var now = timestamp();
          if (prior.isPresent()) {
            var old = prior.get();
            return new Appearance(
                old.id(),
                owner,
                values.theme(),
                values.accentLight(),
                values.accentDark(),
                old.version() + 1,
                now.isBefore(old.updatedAt()) ? old.updatedAt() : now);
          }
          return new Appearance(
              UUID.randomUUID(),
              owner,
              values.theme(),
              values.accentLight(),
              values.accentDark(),
              0,
              now);
        });
  }

  private java.time.Instant timestamp() {
    try {
      var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
      int year = now.atOffset(java.time.ZoneOffset.UTC).getYear();
      if (year < 1 || year > 9999)
        throw new IllegalStateException("Appearance timestamp outside public range");
      return now;
    } catch (RuntimeException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
