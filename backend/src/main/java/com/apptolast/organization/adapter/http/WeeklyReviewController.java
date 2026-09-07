package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ReadWeeklyReviewUseCase;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/weekly-review")
public final class WeeklyReviewController {
  private final ReadWeeklyReviewUseCase weekly;

  public WeeklyReviewController(ReadWeeklyReviewUseCase weekly) {
    this.weekly = weekly;
  }

  @GetMapping
  public Response get(
      Principal principal,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
    if (parameters.keySet().stream().anyMatch(key -> !List.of("date", "zoneId").contains(key)))
      throw BlockController.invalid("query", "INVALID_VALUE");
    parameters.forEach(
        (field, values) -> {
          if (values.size() != 1) throw BlockController.invalid("query", "INVALID_VALUE");
        });
    var review =
        weekly.get(
            principal.getName(),
            parameters.containsKey("date")
                ? HistoryController.date(parameters.getFirst("date"), "date")
                : null,
            parameters.getFirst("zoneId"));
    return new Response(
        review.weekStart(),
        review.weekEnd(),
        review.zoneId(),
        review.zoneSource(),
        review.availabilityZoneId(),
        review.serverNow(),
        review.startAt(),
        review.endAt(),
        review.days().stream()
            .map(
                day ->
                    new DayResponse(
                        day.date(),
                        day.startAt(),
                        day.endAt(),
                        Long.toString(day.plannedMicroseconds()),
                        Long.toString(day.workedMicroseconds()),
                        text(day.capacityMicroseconds())))
            .toList(),
        new TotalsResponse(
            Long.toString(review.totals().plannedMicroseconds()),
            Long.toString(review.totals().workedMicroseconds()),
            text(review.totals().capacityMicroseconds())),
        Long.toString(review.unquantifiedSessionCount()));
  }

  private static String text(Long value) {
    return value == null ? null : value.toString();
  }

  @ExceptionHandler(com.apptolast.organization.domain.WeeklyReviewTimeOutOfRangeException.class)
  org.springframework.http.ResponseEntity<java.util.Map<String, Object>> temporal() {
    return org.springframework.http.ResponseEntity.status(409)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                409,
                "WEEKLY_REVIEW_TIME_OUT_OF_RANGE",
                "La semana no puede representarse con el reloj y la zona actuales."));
  }

  public record Response(
      LocalDate weekStart,
      LocalDate weekEnd,
      String zoneId,
      String zoneSource,
      String availabilityZoneId,
      Instant serverNow,
      Instant startAt,
      Instant endAt,
      List<DayResponse> days,
      TotalsResponse totals,
      String unquantifiedSessionCount) {}

  public record DayResponse(
      LocalDate date,
      Instant startAt,
      Instant endAt,
      String plannedMicroseconds,
      String workedMicroseconds,
      String capacityMicroseconds) {}

  public record TotalsResponse(
      String plannedMicroseconds, String workedMicroseconds, String capacityMicroseconds) {}
}
