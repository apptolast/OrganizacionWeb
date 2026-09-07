package com.apptolast.organization.application;

import com.apptolast.organization.domain.SessionStart;
import java.util.Optional;

public interface WorkSessionQueries {
  Optional<SessionStart> active(String owner);

  Optional<SessionStart> detail(String owner, java.util.UUID id);

  Optional<SessionStart> byRequest(String owner, java.util.UUID key);
}
