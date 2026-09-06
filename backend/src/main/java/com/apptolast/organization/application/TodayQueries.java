package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.Optional;
import java.util.function.Function;

public interface TodayQueries {
  TodayWindow.Agenda read(String owner, Function<Optional<Availability>, TodayWindow> window);
}
