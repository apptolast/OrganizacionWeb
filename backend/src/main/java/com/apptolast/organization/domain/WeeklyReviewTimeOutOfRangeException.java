package com.apptolast.organization.domain;

public final class WeeklyReviewTimeOutOfRangeException extends RuntimeException {
  public WeeklyReviewTimeOutOfRangeException() {
    super("La semana no puede representarse con el reloj y la zona actuales.");
  }
}
