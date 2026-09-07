package com.apptolast.organization.application;

import java.util.UUID;

public interface ReadWorkSessionEndUseCase {
  WorkSessionEndSnapshot read(String owner, UUID session);
}
