package com.apptolast.organization.domain;

public final class IcsFeedMalformedException extends RuntimeException {
  public IcsFeedMalformedException() {
    super("El feed no es un calendario iCalendar.");
  }
}
