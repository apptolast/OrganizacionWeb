package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;

public final class SaveAvailability implements SaveAvailabilityUseCase {
  private final AvailabilityEditing store;
  private final ZoneCatalog catalog;
  private final Clock clock;

  public SaveAvailability(AvailabilityEditing store, ZoneCatalog catalog, Clock clock) {
    this.store = store;
    this.catalog = catalog;
    this.clock = clock;
  }

  public Availability execute(
      String owner, AvailabilityRevision expected, String zone, Map<DayOfWeek, Integer> days) {
    if (!catalog.zones().contains(zone))
      throw new ValidationException(
          List.of(new FieldError("zoneId", "INVALID_VALUE", "Selecciona una zona disponible.")));
    return store.save(
        owner,
        previous -> {
          if (previous.isEmpty()
              ? expected.id() != null
              : !previous.get().id().equals(expected.id())
                  || previous.get().version() != expected.version())
            throw new AvailabilityConflictException();
          var now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
          if (previous.isPresent()) {
            var old = previous.get();
            if (old.zoneId().equals(zone) && old.dailyMinutes().equals(days)) return old;
            return new Availability(
                old.id(),
                owner,
                zone,
                days,
                old.version() + 1,
                old.createdAt(),
                now.isBefore(old.updatedAt()) ? old.updatedAt() : now);
          }
          return new Availability(UUID.randomUUID(), owner, zone, days, 0, now, now);
        });
  }
}
