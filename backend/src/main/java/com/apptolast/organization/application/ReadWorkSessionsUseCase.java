package com.apptolast.organization.application;

import com.apptolast.organization.domain.SessionStart;
import java.util.Optional;

public interface ReadWorkSessionsUseCase {
  Optional<SessionStart> active(String owner);

  SessionStart detail(String owner, java.util.UUID id);

  SessionStart byRequest(String owner, java.util.UUID key);
}
