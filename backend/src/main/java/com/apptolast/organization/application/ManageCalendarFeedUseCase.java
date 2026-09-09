package com.apptolast.organization.application;

public interface ManageCalendarFeedUseCase {
  CalendarFeedLink generate(String owner);

  CalendarFeedStatus status(String owner);

  void revoke(String owner);
}
