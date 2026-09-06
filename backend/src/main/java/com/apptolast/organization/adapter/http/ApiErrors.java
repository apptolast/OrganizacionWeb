package com.apptolast.organization.adapter.http;

import com.apptolast.organization.domain.ValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class ApiErrors {
  @ExceptionHandler(com.apptolast.organization.application.BlockNotFoundException.class)
  ResponseEntity<Map<String, Object>> missingBlock() {
    return ResponseEntity.status(404)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem(404, "BLOCK_NOT_FOUND", "No se ha encontrado el bloque."));
  }
  @ExceptionHandler(com.apptolast.organization.application.StorageUnavailableException.class)
  ResponseEntity<Map<String, Object>> storage() {
    return ResponseEntity.status(503)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            problem(
                503,
                "STORAGE_UNAVAILABLE",
                "El almacenamiento no está disponible. Inténtalo más tarde."));
  }

  public static Map<String, Object> problem(int status, String code, String title) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "urn:organization:problem:" + code.toLowerCase(java.util.Locale.ROOT));
    body.put("title", title);
    body.put("status", status);
    body.put("code", code);
    return body;
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  ResponseEntity<Map<String, Object>> malformed() {
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem(400, "MALFORMED_JSON", "No se puede leer el JSON enviado."));
  }

  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  ResponseEntity<Map<String, Object>> unsupported() {
    return ResponseEntity.status(415)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem(415, "UNSUPPORTED_MEDIA_TYPE", "Envía los datos como JSON."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, Object>> internal(Exception error) {
    String correlation = java.util.UUID.randomUUID().toString();
    org.slf4j.LoggerFactory.getLogger(ApiErrors.class)
        .error(
            "Project operation failed; correlationId={} category={}",
            correlation,
            error.getClass().getSimpleName());
    var body =
        problem(
            500,
            "INTERNAL_ERROR",
            "No se ha podido completar la operación. Usa la referencia al solicitar ayuda.");
    body.put("correlationId", correlation);
    return ResponseEntity.internalServerError()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  @ExceptionHandler(ValidationException.class)
  ResponseEntity<Map<String, Object>> validation(ValidationException error) {
    var body = problem(400, "VALIDATION_ERROR", error.getMessage());
    body.put("errors", error.errors());
    return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
  }

  @ExceptionHandler(com.apptolast.organization.application.ProjectNotFoundException.class)
  ResponseEntity<Map<String, Object>> notFound() {
    return ResponseEntity.status(404)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem(404, "PROJECT_NOT_FOUND", "Proyecto no encontrado"));
  }

  @ExceptionHandler(com.apptolast.organization.application.AvailabilityConflictException.class)
  ResponseEntity<Map<String, Object>> availabilityConflict() {
    return ResponseEntity.status(412)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            problem(
                412, "AVAILABILITY_CONFLICT", "La disponibilidad tiene una versión más reciente."));
  }

  @ExceptionHandler(com.apptolast.organization.application.TaskConflictException.class)
  ResponseEntity<Map<String, Object>> taskConflict() {
    return ResponseEntity.status(412)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem(412, "TASK_CONFLICT", "La tarea tiene una versión más reciente."));
  }

  @ExceptionHandler(com.apptolast.organization.application.ProjectConflictException.class)
  ResponseEntity<Map<String, Object>> conflict() {
    return ResponseEntity.status(412)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem(412, "PROJECT_CONFLICT", "El proyecto tiene una versión más reciente."));
  }

  @ExceptionHandler(com.apptolast.organization.application.InvalidProjectTransitionException.class)
  ResponseEntity<Map<String, Object>> invalidTransition() {
    return ResponseEntity.status(409)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            problem(
                409, "INVALID_PROJECT_TRANSITION", "La transición solicitada no está permitida."));
  }

  @ExceptionHandler(com.apptolast.organization.application.ActiveProjectLimitException.class)
  ResponseEntity<Map<String, Object>> activeLimit(
      com.apptolast.organization.application.ActiveProjectLimitException error) {
    var body =
        problem(
            409,
            "ACTIVE_PROJECT_LIMIT",
            "No hay plazas activas disponibles. Pausa o termina otro proyecto.");
    body.put("activeCount", error.activeCount());
    body.put("limit", error.limit());
    return ResponseEntity.status(409).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
  }

  @ExceptionHandler(com.apptolast.organization.application.ResourceNotFoundException.class)
  ResponseEntity<Map<String, Object>> resourceNotFound() {
    return ResponseEntity.status(404)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem(404, "RESOURCE_NOT_FOUND", "No se ha encontrado el recurso."));
  }

  @ExceptionHandler(com.apptolast.organization.application.ProjectCompletedException.class)
  ResponseEntity<Map<String, Object>> completedProject() {
    return ResponseEntity.status(409)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem(409, "PROJECT_COMPLETED", "Reabre el proyecto en pausa para añadir tareas."));
  }
}
