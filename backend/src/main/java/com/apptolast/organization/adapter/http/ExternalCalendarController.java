package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ExternalCalendarNotConfiguredException;
import com.apptolast.organization.application.ExternalCalendarUseCases;
import com.apptolast.organization.application.ExternalEventsView;
import com.apptolast.organization.domain.ExternalCalendarSubscription;
import com.apptolast.organization.domain.ExternalEventsRange;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Las cinco rutas privadas del calendario externo. Ninguna respuesta contiene la dirección. */
@RestController
public final class ExternalCalendarController {
  private static final String ROUTE = "/api/v1/me/external-calendar";
  private static final String NO_STORE = "no-store";
  private static final Set<String> SUBSCRIPTION_FIELDS = Set.of("label", "url");
  private static final Set<String> SYNC_FIELDS = Set.of("onlyIfStale");

  private final ExternalCalendarUseCases.Read read;
  private final ExternalCalendarUseCases.Save save;
  private final ExternalCalendarUseCases.Delete remove;
  private final ExternalCalendarUseCases.Sync sync;
  private final ExternalCalendarUseCases.ReadEvents events;
  private final ObjectMapper json;

  public ExternalCalendarController(
      ExternalCalendarUseCases.Read read,
      ExternalCalendarUseCases.Save save,
      ExternalCalendarUseCases.Delete remove,
      ExternalCalendarUseCases.Sync sync,
      ExternalCalendarUseCases.ReadEvents events,
      ObjectMapper json) {
    this.read = read;
    this.save = save;
    this.remove = remove;
    this.sync = sync;
    this.events = events;
    this.json = json;
  }

  public record SubscriptionResponse(
      boolean configured, ExternalCalendarSubscription subscription) {}

  public record SyncResponse(boolean performed, ExternalCalendarSubscription subscription) {}

  @GetMapping(ROUTE)
  public ResponseEntity<SubscriptionResponse> get(
      Principal principal, @RequestParam MultiValueMap<String, String> parameters) {
    rejectAnyParameter(parameters);
    return noStore(
        read.execute(principal.getName())
            .map(subscription -> new SubscriptionResponse(true, subscription))
            .orElseGet(() -> new SubscriptionResponse(false, null)));
  }

  @PutMapping(value = ROUTE, consumes = "application/json")
  public ResponseEntity<SubscriptionResponse> put(
      Principal principal,
      @RequestBody(required = false) String raw,
      @RequestParam MultiValueMap<String, String> parameters)
      throws JsonProcessingException {
    rejectAnyParameter(parameters);
    var body = object(raw, SUBSCRIPTION_FIELDS);
    var label = text(body, "label");
    var url = text(body, "url");
    return noStore(new SubscriptionResponse(true, save.execute(principal.getName(), label, url)));
  }

  @DeleteMapping(ROUTE)
  public ResponseEntity<Void> delete(
      Principal principal, @RequestParam MultiValueMap<String, String> parameters) {
    rejectAnyParameter(parameters);
    remove.execute(principal.getName());
    return ResponseEntity.noContent().header("Cache-Control", NO_STORE).build();
  }

  @PostMapping(value = ROUTE + "/sync", consumes = "application/json")
  public ResponseEntity<SyncResponse> sync(
      Principal principal,
      @RequestBody(required = false) String raw,
      @RequestParam MultiValueMap<String, String> parameters)
      throws JsonProcessingException {
    rejectAnyParameter(parameters);
    var body = object(raw, SYNC_FIELDS);
    var flag = body.get("onlyIfStale");
    if (flag == null || flag.isNull()) throw invalid("onlyIfStale", "REQUIRED");
    if (!flag.isBoolean()) throw invalid("onlyIfStale", "INVALID_TYPE");
    var outcome = sync.execute(principal.getName(), flag.booleanValue());
    return noStore(new SyncResponse(outcome.performed(), outcome.subscription()));
  }

  @GetMapping(ROUTE + "/events")
  public ResponseEntity<ExternalEventsView> events(
      Principal principal, @RequestParam MultiValueMap<String, String> parameters) {
    rejectParametersOtherThan(parameters, Set.of("from", "to"));
    var range = ExternalEventsRange.of(single(parameters, "from"), single(parameters, "to"));
    return noStore(events.execute(principal.getName(), range));
  }

  @ExceptionHandler(ExternalCalendarNotConfiguredException.class)
  ResponseEntity<Map<String, Object>> notConfigured() {
    return ResponseEntity.status(404)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .header("Cache-Control", NO_STORE)
        .body(
            ApiErrors.problem(
                404,
                "EXTERNAL_CALENDAR_NOT_CONFIGURED",
                "No hay ningún calendario externo configurado."));
  }

  private static <T> ResponseEntity<T> noStore(T body) {
    return ResponseEntity.ok().header("Cache-Control", NO_STORE).body(body);
  }

  private JsonNode object(String raw, Set<String> allowed) throws JsonProcessingException {
    if (raw == null || raw.isBlank()) throw invalid("body", "REQUIRED");
    var body =
        json.reader()
            .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(raw);
    if (body == null || !body.isObject()) throw invalid("body", "INVALID_TYPE");
    var extras = new TreeSet<String>();
    body.fieldNames()
        .forEachRemaining(
            field -> {
              if (!allowed.contains(field)) extras.add(field);
            });
    if (!extras.isEmpty()) throw invalid(extras.first(), "UNKNOWN_FIELD");
    return body;
  }

  private static String text(JsonNode body, String field) {
    var value = body.get(field);
    if (value == null || value.isNull()) throw invalid(field, "REQUIRED");
    if (!value.isTextual()) throw invalid(field, "INVALID_TYPE");
    return value.textValue();
  }

  private static String single(MultiValueMap<String, String> parameters, String name) {
    var values = parameters.get(name);
    if (values == null || values.isEmpty()) return null;
    if (values.size() > 1) throw invalid(name, "INVALID_FORMAT");
    return values.getFirst();
  }

  private static void rejectAnyParameter(MultiValueMap<String, String> parameters) {
    rejectParametersOtherThan(parameters, Set.of());
  }

  private static void rejectParametersOtherThan(
      MultiValueMap<String, String> parameters, Set<String> allowed) {
    var extras = new TreeSet<>(parameters.keySet());
    extras.removeAll(allowed);
    if (!extras.isEmpty()) throw invalid(extras.first(), "UNKNOWN_PARAMETER");
  }

  private static ValidationException invalid(String field, String code) {
    return new ValidationException(
        List.of(new FieldError(field, code, "Revisa el valor de este campo.")));
  }
}
