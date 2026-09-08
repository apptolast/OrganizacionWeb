package com.apptolast.organization.application;

import java.util.Optional;
import java.util.UUID;

public interface ReadImportReceiptUseCase {
  Optional<ImportReceipt> find(String owner, UUID requestKey);
}
