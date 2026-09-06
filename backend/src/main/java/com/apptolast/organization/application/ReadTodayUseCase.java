package com.apptolast.organization.application;

import com.apptolast.organization.domain.TodayWindow;

public interface ReadTodayUseCase {
  TodayWindow.Agenda get(String owner);
}
