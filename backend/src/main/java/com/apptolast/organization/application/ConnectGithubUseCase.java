package com.apptolast.organization.application;

public interface ConnectGithubUseCase {
  ConnectionView execute(String ownerId, String repository, String token);
}
