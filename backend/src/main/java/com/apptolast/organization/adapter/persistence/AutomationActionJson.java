package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.domain.AutomationAction;
import com.apptolast.organization.domain.CreateTaskAction;
import com.apptolast.organization.domain.NotifyWebhookAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** The JSONB column keeps the closed action shape the application already validated. */
final class AutomationActionJson {
  private AutomationActionJson() {}

  static String encode(ObjectMapper json, AutomationAction action) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("type", action.type());
    switch (action) {
      case CreateTaskAction task -> {
        fields.put("projectId", task.projectId().toString());
        fields.put("titleTemplate", task.titleTemplate());
        fields.put("criterionTemplate", task.criterionTemplate());
        fields.put("estimatedMinutes", task.estimatedMinutes());
      }
      case NotifyWebhookAction webhook -> fields.put("endpointId", webhook.endpointId().toString());
    }
    try {
      return json.writeValueAsString(fields);
    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
      throw new IllegalStateException("La acción validada no se pudo serializar.", error);
    }
  }

  static AutomationAction decode(ObjectMapper json, String raw) {
    try {
      var node = json.readTree(raw);
      return node.get("type").asText().equals("CREATE_TASK")
          ? new CreateTaskAction(
              UUID.fromString(node.get("projectId").asText()),
              node.get("titleTemplate").asText(),
              text(node.get("criterionTemplate")),
              number(node.get("estimatedMinutes")))
          : new NotifyWebhookAction(UUID.fromString(node.get("endpointId").asText()));
    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
      throw new IllegalStateException("La acción almacenada no se pudo leer.", error);
    }
  }

  private static String text(JsonNode node) {
    return node == null || node.isNull() ? null : node.asText();
  }

  private static Integer number(JsonNode node) {
    return node == null || node.isNull() ? null : node.intValue();
  }
}
