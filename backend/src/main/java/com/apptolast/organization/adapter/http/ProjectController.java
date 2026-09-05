package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.CreateProjectUseCase;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.Project;
import com.apptolast.organization.domain.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public final class ProjectController {
  private final CreateProjectUseCase createProject;

  public ProjectController(CreateProjectUseCase createProject) {
    this.createProject = createProject;
  }

  @PostMapping(value = "/api/v1/projects", consumes = "application/json")
  public ResponseEntity<Project> create(@RequestBody JsonNode request, Principal principal) {
    request
        .fieldNames()
        .forEachRemaining(
            field -> {
              if (!field.equals("name") && !field.equals("description")) {
                throw new ValidationException(
                    List.of(
                        new FieldError(field, "UNKNOWN_FIELD", "Este campo no está permitido.")));
              }
            });
    Project project =
        createProject.execute(
            principal.getName(), string(request, "name"), string(request, "description"));
    return ResponseEntity.created(URI.create("/api/v1/projects/" + project.id())).body(project);
  }

  @GetMapping("/api/session")
  public ResponseEntity<Void> session() {
    return ResponseEntity.noContent().build();
  }

  private String string(JsonNode request, String field) {
    JsonNode value = request.get(field);
    if (value == null || value.isNull()) return null;
    if (!value.isTextual())
      throw new ValidationException(
          List.of(new FieldError(field, "INVALID_TYPE", "Introduce texto en este campo.")));
    return value.textValue();
  }
}
