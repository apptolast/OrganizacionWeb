package com.apptolast.organization.adapter.http;

import com.apptolast.organization.domain.AutomationAction;
import com.apptolast.organization.domain.AutomationDraft;
import com.apptolast.organization.domain.CreateTaskAction;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.NotifyWebhookAction;
import com.apptolast.organization.domain.ValidationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Reads the closed rule body: exact key sets, no unknown or duplicated properties. */
final class AutomationBody {
  private static final Set<String> RULE_KEYS =
      Set.of("name", "enabled", "trigger", "condition", "action");
  private static final Set<String> CREATE_TASK_KEYS =
      Set.of("type", "projectId", "titleTemplate", "criterionTemplate", "estimatedMinutes");
  private static final Set<String> NOTIFY_WEBHOOK_KEYS = Set.of("type", "endpointId");

  private AutomationBody() {}

  static AutomationDraft read(ObjectMapper json, String raw) {
    var body = tree(json, raw);
    onlyKnown(body, RULE_KEYS, "body");
    for (var key : List.of("name", "enabled", "trigger", "condition", "action"))
      if (!body.has(key)) throw invalid(key);
    var trigger = body.get("trigger");
    if (!trigger.isObject() || trigger.size() != 1) throw invalid("trigger");
    if (!trigger.path("eventType").isTextual()) throw invalid("trigger.eventType");
    if (!body.get("enabled").isBoolean()) throw invalid("enabled");
    if (!body.get("name").isTextual()) throw invalid("name");
    return new AutomationDraft(
        body.get("name").asText(),
        body.get("enabled").asBoolean(),
        trigger.get("eventType").asText(),
        condition(body.get("condition")),
        action(body.get("action")));
  }

  private static UUID condition(JsonNode node) {
    if (node.isNull()) return null;
    if (!node.isObject() || node.size() != 1 || !node.has("projectId"))
      throw invalid("condition.projectId");
    return uuid(node.get("projectId"), "condition.projectId");
  }

  private static AutomationAction action(JsonNode node) {
    if (!node.isObject() || !node.path("type").isTextual()) throw invalid("action");
    return switch (node.get("type").asText()) {
      case "CREATE_TASK" -> {
        exactly(node, CREATE_TASK_KEYS);
        yield new CreateTaskAction(
            uuid(node.get("projectId"), "action.projectId"),
            text(node.get("titleTemplate"), "action.titleTemplate", false),
            text(node.get("criterionTemplate"), "action.criterionTemplate", true),
            minutes(node.get("estimatedMinutes")));
      }
      case "NOTIFY_WEBHOOK" -> {
        exactly(node, NOTIFY_WEBHOOK_KEYS);
        yield new NotifyWebhookAction(uuid(node.get("endpointId"), "action.endpointId"));
      }
      default -> throw invalid("action.type");
    };
  }

  private static Integer minutes(JsonNode node) {
    if (node.isNull()) return null;
    if (!node.isIntegralNumber() || !node.canConvertToInt())
      throw invalid("action.estimatedMinutes");
    return node.intValue();
  }

  private static String text(JsonNode node, String field, boolean nullable) {
    if (nullable && node.isNull()) return null;
    if (!node.isTextual()) throw invalid(field);
    return node.asText();
  }

  private static UUID uuid(JsonNode node, String field) {
    if (!node.isTextual()
        || !node.asText()
            .matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      throw invalid(field);
    return UUID.fromString(node.asText());
  }

  /** A wrong key set means the object is not that action at all, so the error names `action`. */
  private static void exactly(JsonNode node, Set<String> keys) {
    if (node.size() != keys.size()) throw invalid("action");
    onlyKnown(node, keys, "action");
  }

  private static void onlyKnown(JsonNode node, Set<String> keys, String field) {
    node.fieldNames()
        .forEachRemaining(
            name -> {
              if (!keys.contains(name)) throw invalid(field);
            });
  }

  private static JsonNode tree(ObjectMapper json, String raw) {
    try {
      var body =
          json.reader()
              .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
              .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .readTree(raw);
      if (body == null || !body.isObject()) throw invalid("body");
      return body;
    } catch (java.io.IOException error) {
      throw invalid("body");
    }
  }

  static ValidationException invalid(String field) {
    return new ValidationException(
        List.of(new FieldError(field, "INVALID_VALUE", "Revisa el valor de este campo.")));
  }
}
