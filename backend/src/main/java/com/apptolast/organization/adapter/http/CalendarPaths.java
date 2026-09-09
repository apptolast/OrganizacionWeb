package com.apptolast.organization.adapter.http;

import jakarta.servlet.http.HttpServletRequest;

/** The three addresses of the calendar, in one place so security and routing cannot drift apart. */
public final class CalendarPaths {
  public static final String PUBLIC_PREFIX = "/calendar/";
  public static final String FEED = "/api/v1/me/calendar-feed";
  public static final String DOWNLOAD = "/api/v1/me/calendar.ics";
  public static final String SUFFIX = ".ics";

  private CalendarPaths() {}

  public static String of(HttpServletRequest request) {
    return request.getRequestURI().substring(request.getContextPath().length());
  }

  public static boolean isPublicFeed(HttpServletRequest request) {
    return of(request).startsWith(PUBLIC_PREFIX);
  }

  /** The feed is a capability in the path: no browser session and no bearer credential apply. */
  public static boolean isCalendar(HttpServletRequest request) {
    var path = of(request);
    return path.startsWith(PUBLIC_PREFIX) || path.equals(FEED) || path.equals(DOWNLOAD);
  }
}
