package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.CreateAutomationUseCase;
import com.apptolast.organization.application.DeleteAutomationUseCase;
import com.apptolast.organization.application.ReadAutomationRunsUseCase;
import com.apptolast.organization.application.ReadAutomationsUseCase;
import com.apptolast.organization.application.ReplaceAutomationUseCase;
import com.apptolast.organization.application.SimulateAutomationUseCase;
import com.apptolast.organization.domain.AutomationRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** The private rule API: closed bodies, version as ETag and no effect until the worker runs. */
@RestController
@RequestMapping("/api/v1/me/automations")
public final class AutomationController {
  private static final String UUID_PATTERN =
      "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
  private static final String VERSION_PATTERN = "\"(0|[1-9][0-9]*)\"";

  private final CreateAutomationUseCase create;
  private final ReadAutomationsUseCase read;
  private final ReplaceAutomationUseCase replace;
  private final DeleteAutomationUseCase delete;
  private final SimulateAutomationUseCase simulate;
  private final ReadAutomationRunsUseCase runs;
  private final ObjectMapper json;

  public AutomationController(
      CreateAutomationUseCase create,
      ReadAutomationsUseCase read,
      ReplaceAutomationUseCase replace,
      DeleteAutomationUseCase delete,
      SimulateAutomationUseCase simulate,
      ReadAutomationRunsUseCase runs,
      ObjectMapper json) {
    this.create = create;
    this.read = read;
    this.replace = replace;
    this.delete = delete;
    this.simulate = simulate;
    this.runs = runs;
    this.json = json;
  }

  @GetMapping
  public AutomationView.Rules list(Principal principal) {
    return new AutomationView.Rules(
        read.list(principal.getName()).stream().map(AutomationView::of).toList());
  }

  @GetMapping("/{id}")
  public ResponseEntity<AutomationView.Rule> one(@PathVariable String id, Principal principal) {
    return tagged(read.get(principal.getName(), ruleId(id)));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AutomationView.Rule> create(@RequestBody String raw, Principal principal) {
    var rule = create.create(principal.getName(), AutomationBody.read(json, raw));
    return ResponseEntity.status(201)
        .eTag("\"" + rule.version() + "\"")
        .header("Location", "/api/v1/me/automations/" + rule.id())
        .body(AutomationView.of(rule));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AutomationView.Rule> replace(
      @PathVariable String id,
      @RequestHeader("If-Match") List<String> matches,
      @RequestBody String raw,
      Principal principal) {
    var expected = precondition(matches);
    var draft = AutomationBody.read(json, raw);
    return tagged(replace.replace(principal.getName(), ruleId(id), expected, draft));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable String id,
      @RequestHeader("If-Match") List<String> matches,
      Principal principal) {
    delete.delete(principal.getName(), ruleId(id), precondition(matches));
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/simulate", consumes = MediaType.APPLICATION_JSON_VALUE)
  public AutomationView.Simulation simulate(@RequestBody String raw, Principal principal) {
    return AutomationView.of(
        simulate.simulate(principal.getName(), AutomationBody.read(json, raw)));
  }

  @GetMapping("/{id}/runs")
  public AutomationView.Runs runs(
      @PathVariable String id,
      @RequestParam(value = "cursor", required = false) String cursor,
      Principal principal) {
    var page = runs.read(principal.getName(), ruleId(id), AutomationRunCursorCodec.decode(cursor));
    return new AutomationView.Runs(
        page.items().stream().map(AutomationView::of).toList(),
        AutomationRunCursorCodec.encode(page.nextCursor()));
  }

  @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
  public ResponseEntity<Map<String, Object>> missingPrecondition() {
    return ResponseEntity.status(428)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                428, "PRECONDITION_REQUIRED", "Recarga la regla antes de guardar los cambios."));
  }

  /** A rule is replaced whole; there is no partial edit, so PATCH is answered, not dispatched. */
  @RequestMapping(value = "/{id}", method = RequestMethod.PATCH)
  public ResponseEntity<Map<String, Object>> patchIsNotAllowed() {
    return ResponseEntity.status(405)
        .header("Allow", "GET, PUT, DELETE")
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                405, "METHOD_NOT_ALLOWED", "Usa PUT completo para cambiar una regla."));
  }

  private static ResponseEntity<AutomationView.Rule> tagged(AutomationRule rule) {
    return ResponseEntity.ok().eTag("\"" + rule.version() + "\"").body(AutomationView.of(rule));
  }

  private static UUID ruleId(String id) {
    if (!id.matches(UUID_PATTERN)) throw AutomationBody.invalid("id");
    return UUID.fromString(id);
  }

  private static long precondition(List<String> matches) {
    if (matches.size() != 1 || !matches.getFirst().matches(VERSION_PATTERN))
      throw AutomationBody.invalid("If-Match");
    return Long.parseLong(matches.getFirst().replace("\"", ""));
  }
}
