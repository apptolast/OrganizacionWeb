package com.apptolast.organization.application;

import java.io.InputStream;
import java.util.UUID;

public interface ApplyImportDataUseCase {
  ImportReceipt apply(String owner, UUID requestKey, String expectedSha256, InputStream body);
}
