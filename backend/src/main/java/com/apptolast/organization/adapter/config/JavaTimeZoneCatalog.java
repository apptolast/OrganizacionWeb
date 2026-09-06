package com.apptolast.organization.adapter.config;

@org.springframework.stereotype.Component
public final class JavaTimeZoneCatalog
    implements com.apptolast.organization.application.ZoneCatalog {
  public java.util.Set<String> zones() {
    var ids = new java.util.HashSet<>(java.time.ZoneId.getAvailableZoneIds());
    ids.add("UTC");
    return java.util.Set.copyOf(ids);
  }
}
