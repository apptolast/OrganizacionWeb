package com.apptolast.organization.application;

import com.apptolast.organization.domain.IssueImportReceipt;
import java.util.UUID;

public interface ImportIssuesUseCase {
  IssueImportReceipt execute(String ownerId, UUID projectId);
}
