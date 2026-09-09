package com.apptolast.organization.application;

import java.util.UUID;

/**
 * Bitácora del ejecutor de automatizaciones.
 *
 * <p>Existe como puerto y no como un {@code Logger} dentro del caso de uso porque la capa de
 * aplicación sólo puede depender de {@code java..}, {@code ..domain..} y {@code ..application..}, y
 * {@code ArchitectureTest.hexagonalBoundariesAndInputPort()} lo comprueba. Es el mismo patrón que
 * {@link ConnectorAudit}.
 *
 * <p>La firma admite <b>sólo identificadores</b>. El título renderizado de una tarea y el nombre de
 * un proyecto son contenido del propietario, y la bitácora de un trabajador de fondo no es el sitio
 * para ellos: que no quepan es una propiedad del tipo, no de la disciplina de quien escribe la
 * línea.
 */
public interface AutomationAudit {
  void runFinished(UUID ruleId, UUID eventId, String status, int attempt, String errorCode);
}
