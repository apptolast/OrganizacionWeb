package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalIssue;
import java.util.UUID;
import java.util.function.Function;

/**
 * Puerto de salida que confirma tarea, evento de la outbox y enlace externo en la misma
 * transacción, o revierte los tres. Devuelve {@code false} cuando el enlace ya existía: el
 * propietario, el origen y el identificador externo son únicos, así que reimportar no duplica.
 */
public interface ImportedTaskCommit {
  boolean save(
      String ownerId,
      UUID projectId,
      ExternalIssue issue,
      Function<String, TaskCreation> operation);
}
