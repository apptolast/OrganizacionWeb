package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

public final class SaveCustomizationView implements SaveCustomizationViewUseCase {
  private final CustomizationEditing store;
  private final Clock clock;

  public SaveCustomizationView(CustomizationEditing store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public Customization save(
      String owner,
      CustomizationScope scope,
      CustomizationRevision expected,
      List<String> visibleFields) {
    new CustomizationView(scope, visibleFields);
    return store.change(
        owner,
        scope,
        prior -> {
          if (prior.isEmpty()
              ? expected.id() != null
              : !prior.get().id().equals(expected.id())
                  || prior.get().version() != expected.version())
            throw new CustomizationConflictException();
          if (prior.isPresent() && prior.get().visibleFields().equals(visibleFields))
            return prior.get();
          if (prior.isPresent() && prior.get().version() == Long.MAX_VALUE)
            throw new StorageUnavailableException(
                new ArithmeticException("Customization revision exhausted"));
          var now = CustomizationTime.capture(clock);
          if (prior.isPresent()) {
            var old = prior.get();
            return new Customization(
                old.id(),
                owner,
                scope,
                List.copyOf(visibleFields),
                old.customFields(),
                old.version() + 1,
                now.isBefore(old.updatedAt()) ? old.updatedAt() : now);
          }
          return new Customization(
              UUID.randomUUID(), owner, scope, List.copyOf(visibleFields), List.of(), 0, now);
        });
  }
}
