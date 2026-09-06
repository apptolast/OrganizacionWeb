package com.apptolast.organization.application;

public interface AvailabilityQueries {
  java.util.Optional<com.apptolast.organization.domain.Availability> find(String owner);
}
