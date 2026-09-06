package com.apptolast.organization.application;
import com.apptolast.organization.domain.BlockChangeConfirmation;
import java.util.UUID;
public interface CancelBlockUseCase {
  BlockChangeConfirmation cancel(String owner, UUID project, UUID task, UUID block, UUID key, long expected);
}
