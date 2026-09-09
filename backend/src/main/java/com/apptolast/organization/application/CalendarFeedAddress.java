package com.apptolast.organization.application;

/** The single place that knows how a token becomes a public address. */
public final class CalendarFeedAddress {
  public static final String PREFIX = "/calendar/";
  public static final String SUFFIX = ".ics";

  private CalendarFeedAddress() {}

  public static String of(String publicOrigin, String token) {
    return publicOrigin + PREFIX + token + SUFFIX;
  }
}
