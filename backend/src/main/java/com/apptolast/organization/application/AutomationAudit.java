package com.apptolast.organization.application;

import java.util.UUID;

/**
 * Bitácora del ejecutor de automatizaciones.
 *
 * <p>Existe como puerto y no como un {@code Logger} dentro del caso de uso porque la capa de
 * aplicación sólo puede depender de {@code java..}, {@code ..domain..} y {@code ..application..}, y
 * {@code ArchitectureTest.hexagonalBoundariesAndInputPort()} lo comprueba.
 *
 * <p>La firma admite <b>sólo identificadores</b>. El título renderizado de una tarea y el nombre de
 * un proyecto son contenido del propietario, y la bitácora de un trabajador de fondo no es el sitio
 * para ellos: que no quepan es una propiedad del tipo, no de la disciplina de quien escribe la
 * línea.
 */
public interface AutomationAudit {
  void runFinished(UUID ruleId, UUID eventId, String status, int attempt, String errorCode);

  /**
   * Un fallo atribuible a un propietario, que detiene su recorrido y el de nadie más. {@code
   * eventId} nombra la fila de outbox en la que se quedó, o es null cuando el fallo no es de un
   * evento concreto. {@code category} es el nombre de la clase del fallo: ni el mensaje ni el
   * payload, que son contenido del propietario.
   */
  void cycleFailed(String ownerId, UUID eventId, String category);
}
