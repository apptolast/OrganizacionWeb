package com.apptolast.organization.application;

import java.util.UUID;

public interface ReadWorkSessionStateUseCase {
  WorkSessionSnapshot read(String owner, UUID session);
}
