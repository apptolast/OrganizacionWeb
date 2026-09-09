package com.apptolast.organization.application;

public interface ReadGitlabConnectionUseCase {
  GitlabConnectionView execute(String ownerId);
}
