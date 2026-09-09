package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalIssue;
import java.util.UUID;
import java.util.function.Function;

/**
 * Puerto de salida que confirma tarea, evento de la outbox y enlace externo en la misma
 * transacción, o revierte los tres. Devuelve {@code false} cuando el enlace ya existía: el
 * propietario, el origen y el identificador externo son únicos, así que reimportar no duplica.
 *
 * <p>La unicidad es por origen, de modo que la misma issue número 42 de GitHub y de GitLab son dos
 * enlaces distintos hacia dos tareas distintas.
 */
public interface ImportedTaskCommit {
  boolean save(
      String ownerId,
      UUID projectId,
      String source,
      ExternalIssue issue,
      Function<String, TaskCreation> operation);
}
