package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ReadCustomizationUseCase;
import com.apptolast.organization.domain.CustomFieldDefinition;
import com.apptolast.organization.domain.CustomizationScope;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.ValidationException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class CustomizationController {
  private final ReadCustomizationUseCase read;

  public CustomizationController(ReadCustomizationUseCase read) {
    this.read = read;
  }

  public record CustomizationResponse(
      boolean configured,
      List<String> visibleFields,
      List<CustomFieldDefinition> customFields,
      Instant updatedAt) {}

  @GetMapping("/api/v1/me/customization/{scope}")
  public ResponseEntity<CustomizationResponse> get(
      Principal principal,
      @PathVariable String scope,
      @RequestParam MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty())
      throw new ValidationException(
          List.of(
              new FieldError("query", "INVALID_VALUE", "No se admiten parámetros de consulta.")));
    CustomizationScope parsed;
    try {
      parsed = CustomizationScope.valueOf(scope);
    } catch (IllegalArgumentException error) {
      throw new ValidationException(
          List.of(
              new FieldError("scope", "INVALID_VALUE", "Revisa el ámbito de personalización.")));
    }
    return read.get(principal.getName(), parsed)
        .map(
            value ->
                ResponseEntity.ok()
                    .eTag(
                        "\"customization:"
                            + scope
                            + ":"
                            + value.id()
                            + ":"
                            + value.version()
                            + "\"")
                    .body(
                        new CustomizationResponse(
                            true, value.visibleFields(), value.customFields(), value.updatedAt())))
        .orElseGet(
            () ->
                ResponseEntity.ok()
                    .eTag("\"customization:" + scope + ":unconfigured\"")
                    .body(
                        new CustomizationResponse(
                            false,
                            parsed == CustomizationScope.PROJECT
                                ? List.of("createdAt")
                                : List.of("completionCriterion", "estimatedMinutes"),
                            List.of(),
                            null)));
  }
}
