package com.apptolast.organization.application;

import com.apptolast.organization.domain.WeeklyReview;
import java.time.LocalDate;

public interface ReadWeeklyReviewUseCase {
  WeeklyReview get(String owner, LocalDate date, String zoneId);
}
