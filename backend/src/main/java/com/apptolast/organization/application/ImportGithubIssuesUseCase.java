package com.apptolast.organization.application;

import com.apptolast.organization.domain.IssueImportReceipt;
import java.util.UUID;

public interface ImportGithubIssuesUseCase {
  IssueImportReceipt execute(String ownerId, UUID projectId);
}
