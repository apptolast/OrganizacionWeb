package com.apptolast.organization.application;

public interface ReadAvailabilityUseCase {
  java.util.Optional<com.apptolast.organization.domain.Availability> get(String owner);

  java.util.List<String> zones();
}
