package com.apptolast.organization.application;

import com.apptolast.organization.domain.IssueImportReceipt;
import java.util.UUID;

public interface ReadIssueImportUseCase {
  IssueImportReceipt execute(String ownerId, String source, UUID importId);
}
