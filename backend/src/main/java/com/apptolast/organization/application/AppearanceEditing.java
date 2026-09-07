package com.apptolast.organization.application;

import com.apptolast.organization.domain.Appearance;
import java.util.Optional;
import java.util.function.Function;

public interface AppearanceEditing {
  Appearance save(String owner, Function<Optional<Appearance>, Appearance> operation);
}
