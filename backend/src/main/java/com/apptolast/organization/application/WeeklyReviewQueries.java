package com.apptolast.organization.application;

import com.apptolast.organization.domain.Availability;
import com.apptolast.organization.domain.WeeklyReview;
import com.apptolast.organization.domain.WeeklyReviewWindow;
import java.util.Optional;
import java.util.function.Function;

public interface WeeklyReviewQueries {
  WeeklyReview read(String owner, Function<Optional<Availability>, WeeklyReviewWindow> window);
}
