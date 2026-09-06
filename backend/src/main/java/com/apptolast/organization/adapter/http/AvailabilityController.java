package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ReadAvailabilityUseCase;
import com.apptolast.organization.application.SaveAvailabilityUseCase;
import com.apptolast.organization.domain.AvailabilityRevision;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.*;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
public final class AvailabilityController {
  private final ReadAvailabilityUseCase read;
  private final SaveAvailabilityUseCase save;
  private final ObjectMapper json;

  public AvailabilityController(
      ReadAvailabilityUseCase read, SaveAvailabilityUseCase save, ObjectMapper json) {
    this.read = read;
    this.save = save;
    this.json = json;
  }

  public record PreferenceResponse(
      boolean configured, String zoneId, Map<DayOfWeek, Integer> dailyMinutes, Instant updatedAt) {}

  @GetMapping("/api/v1/me/availability")
  public ResponseEntity<PreferenceResponse> get(
      Principal principal, @RequestParam MultiValueMap<String, String> parameters) {
    query(parameters);
    return read.get(principal.getName())
        .map(
            value ->
                ResponseEntity.ok()
                    .eTag("\"availability:" + value.id() + ":" + value.version() + "\"")
                    .body(
                        new PreferenceResponse(
                            true, value.zoneId(), value.dailyMinutes(), value.updatedAt())))
        .orElseGet(
            () ->
                ResponseEntity.ok()
                    .eTag("\"availability:unconfigured\"")
                    .body(new PreferenceResponse(false, null, null, null)));
  }

  @GetMapping("/api/v1/me/availability/zones")
  public Map<String, List<String>> zones(@RequestParam MultiValueMap<String, String> parameters) {
    query(parameters);
    return Map.of("items", read.zones());
  }

  @PutMapping(value = "/api/v1/me/availability", consumes = "application/json")
  public ResponseEntity<?> put(
      @RequestBody(required = false) String raw,
      @RequestHeader(value = "If-Match", required = false) List<String> matches,
      Principal principal,
      @RequestParam MultiValueMap<String, String> parameters)
      throws JsonProcessingException {
    query(parameters);
    if (matches == null)
      return ResponseEntity.status(428)
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(
              ApiErrors.problem(
                  428, "PRECONDITION_REQUIRED", "Envía la versión actual de disponibilidad."));
    var expected = precondition(matches);
    if (raw == null || raw.isBlank()) throw new MalformedBody();
    com.fasterxml.jackson.databind.JsonNode body =
        json.reader()
            .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(raw);
    if (body == null || !body.isObject()) throw invalid("body", "INVALID_TYPE");
    var extras = new TreeSet<String>();
    body.fieldNames()
        .forEachRemaining(
            field -> {
              if (!Set.of("zoneId", "dailyMinutes").contains(field)) extras.add(field);
            });
    if (!extras.isEmpty()) throw invalid(extras.first(), "UNKNOWN_FIELD");
    var zone = body.get("zoneId");
    if (zone == null || zone.isNull()) throw invalid("zoneId", "REQUIRED");
    if (!zone.isTextual()) throw invalid("zoneId", "INVALID_TYPE");
    if (!read.zones().contains(zone.textValue())) throw invalid("zoneId", "INVALID_VALUE");
    var daily = body.get("dailyMinutes");
    if (daily == null || daily.isNull()) throw invalid("dailyMinutes", "REQUIRED");
    if (!daily.isObject()) throw invalid("dailyMinutes", "INVALID_TYPE");
    var dayNames =
        Arrays.stream(DayOfWeek.values())
            .map(Enum::name)
            .collect(java.util.stream.Collectors.toSet());
    var dayExtras = new TreeSet<String>();
    daily
        .fieldNames()
        .forEachRemaining(
            field -> {
              if (!dayNames.contains(field)) dayExtras.add(field);
            });
    if (!dayExtras.isEmpty()) throw invalid("dailyMinutes." + dayExtras.first(), "UNKNOWN_FIELD");
    var days = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) {
      var value = daily.get(day.name());
      var field = "dailyMinutes." + day.name();
      if (value == null || value.isNull()) throw invalid(field, "REQUIRED");
      if (!value.isIntegralNumber()) throw invalid(field, "INVALID_TYPE");
      if (!value.canConvertToInt() || value.intValue() < 0 || value.intValue() > 1440)
        throw invalid(field, "OUT_OF_RANGE");
      days.put(day, value.intValue());
    }

    var next = save.execute(principal.getName(), expected, body.get("zoneId").textValue(), days);
    return ResponseEntity.ok()
        .eTag("\"availability:" + next.id() + ":" + next.version() + "\"")
        .body(new PreferenceResponse(true, next.zoneId(), next.dailyMinutes(), next.updatedAt()));
  }

  private static AvailabilityRevision precondition(List<String> matches) {
    if (matches.size() != 1) throw invalid("If-Match", "INVALID_VALUE");
    var raw = matches.getFirst();
    if (raw.equals("\"availability:unconfigured\"")) return new AvailabilityRevision(null, 0);
    if (!raw.matches(
        "\"availability:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:(0|[1-9][0-9]*)\""))
      throw invalid("If-Match", "INVALID_VALUE");
    var parts = raw.substring(1, raw.length() - 1).split(":");
    try {
      return new AvailabilityRevision(UUID.fromString(parts[1]), Long.parseLong(parts[2]));
    } catch (NumberFormatException error) {
      throw invalid("If-Match", "INVALID_VALUE");
    }
  }

  private static ValidationException invalid(String field, String code) {
    return new ValidationException(
        List.of(new FieldError(field, code, "Revisa el valor de este campo.")));
  }

  private static final class MalformedBody extends RuntimeException {}

  @ExceptionHandler({MalformedBody.class, JsonProcessingException.class})
  ResponseEntity<Map<String, Object>> malformed() {
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(400, "MALFORMED_JSON", "No se puede leer el JSON enviado."));
  }

  private static void query(MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw invalid("query", "INVALID_VALUE");
  }
}
