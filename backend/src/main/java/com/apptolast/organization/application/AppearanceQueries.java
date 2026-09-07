package com.apptolast.organization.application;

import com.apptolast.organization.domain.Appearance;
import java.util.Optional;

public interface AppearanceQueries {
  Optional<Appearance> find(String owner);
}
