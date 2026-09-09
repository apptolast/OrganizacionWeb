package com.apptolast.organization.application;

public interface ConnectGitlabUseCase {
  GitlabConnectionView execute(String ownerId, String token, String projectPath);
}
