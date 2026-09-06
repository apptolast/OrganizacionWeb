package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ReadTodayUseCase;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/today")
public final class TodayController {
  private final ReadTodayUseCase today;

  public TodayController(ReadTodayUseCase today) {
    this.today = today;
  }

  @GetMapping
  public Map<String, Object> get(
      Principal principal,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty())
      throw new com.apptolast.organization.domain.ValidationException(
          parameters.keySet().stream()
              .sorted()
              .map(
                  field ->
                      new com.apptolast.organization.domain.FieldError(
                          field, "INVALID_VALUE", "Hoy no admite parámetros de consulta."))
              .toList());
    var agenda = today.get(principal.getName());
    var day = agenda.window();
    var body = new LinkedHashMap<String, Object>();
    body.put("serverNow", day.serverNow());
    body.put("date", day.date());
    body.put("zoneId", day.zoneId());
    body.put("zoneSource", day.zoneSource());
    body.put("availabilityZoneId", day.availabilityZoneId());
    body.put("dayStartAt", day.dayStartAt());
    body.put("dayEndAt", day.dayEndAt());
    body.put("budgetMinutes", day.budgetMinutes());
    body.put("plannedSeconds", agenda.plannedSeconds());
    body.put("remainingSeconds", agenda.remainingSeconds());
    body.put("excessSeconds", agenda.excessSeconds());
    body.put("currentBlockId", agenda.currentBlockId());
    body.put("nextBlockId", agenda.nextBlockId());
    body.put("closingAt", agenda.closingAt());
    body.put(
        "items",
        agenda.items().stream()
            .map(
                item ->
                    Map.of(
                        "block",
                        BlockController.BlockResponse.from(item.block()),
                        "projectName",
                        item.projectName(),
                        "taskTitle",
                        item.taskTitle()))
            .toList());
    return body;
  }
}
