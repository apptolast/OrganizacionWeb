package com.apptolast.organization.application;

public interface ReadGithubConnectionUseCase {
  ConnectionView execute(String ownerId);
}
