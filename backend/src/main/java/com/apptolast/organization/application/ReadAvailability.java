package com.apptolast.organization.application;

public final class ReadAvailability implements ReadAvailabilityUseCase {
  private final AvailabilityQueries queries;
  private final ZoneCatalog catalog;

  public ReadAvailability(AvailabilityQueries queries, ZoneCatalog catalog) {
    this.queries = queries;
    this.catalog = catalog;
  }

  public java.util.Optional<com.apptolast.organization.domain.Availability> get(String owner) {
    return queries.find(owner);
  }

  public java.util.List<String> zones() {
    return catalog.zones().stream().sorted().toList();
  }
}
