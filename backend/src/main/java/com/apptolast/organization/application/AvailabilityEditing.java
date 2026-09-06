package com.apptolast.organization.application;

import com.apptolast.organization.domain.Availability;
import java.util.function.Function;

public interface AvailabilityEditing {
  Availability save(
      String owner, Function<java.util.Optional<Availability>, Availability> operation);
}
