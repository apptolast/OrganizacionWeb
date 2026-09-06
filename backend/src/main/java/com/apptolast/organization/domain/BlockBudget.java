package com.apptolast.organization.domain;

import java.time.*;
import java.util.*;

public final class BlockBudget {
  private BlockBudget() {}

  public static List<BudgetDay> calculate(
      Availability availability, Instant start, Instant end, List<ResolvedBlockTime> planned) {
    var zone = ZoneId.of(availability.zoneId());
    var result = new ArrayList<BudgetDay>();
    var date = start.atZone(zone).toLocalDate();
    if (date.getYear() < 1 || date.getYear() > 9999)
      throw new ValidationException(
          List.of(
              new FieldError(
                  "startLocal",
                  "OUT_OF_RANGE",
                  "La fecha de presupuesto debe permanecer entre los años 0001 y 9999.")));
    while (date.atStartOfDay(zone).toInstant().isBefore(end)) {
      var dayStart = date.atStartOfDay(zone).toInstant();
      var dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
      var from = start.isAfter(dayStart) ? start : dayStart;
      var to = end.isBefore(dayEnd) ? end : dayEnd;
      long seconds = Duration.between(from, to).getSeconds();
      long plannedSeconds = 0;
      for (var block : planned) {
        var occupiedFrom = block.startAt().isAfter(dayStart) ? block.startAt() : dayStart;
        var occupiedTo = block.endAt().isBefore(dayEnd) ? block.endAt() : dayEnd;
        plannedSeconds += Math.max(0, Duration.between(occupiedFrom, occupiedTo).getSeconds());
      }
      int budget = availability.dailyMinutes().get(date.getDayOfWeek());
      if (seconds > 0 && (date.getYear() < 1 || date.getYear() > 9999))
        throw new ValidationException(
            List.of(
                new FieldError(
                    "endLocal",
                    "OUT_OF_RANGE",
                    "La fecha de presupuesto debe permanecer entre los años 0001 y 9999.")));
      if (seconds > 0)
        result.add(
            new BudgetDay(
                date,
                budget,
                plannedSeconds,
                seconds,
                Math.max(0, plannedSeconds + seconds - budget * 60L)));
      date = date.plusDays(1);
    }
    return List.copyOf(result);
  }
}
