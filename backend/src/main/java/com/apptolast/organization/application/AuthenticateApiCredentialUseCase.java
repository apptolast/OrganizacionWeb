package com.apptolast.organization.application;

public interface AuthenticateApiCredentialUseCase {
  ApiCredentialAccess authenticate(String token);
}
