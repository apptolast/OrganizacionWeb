package com.apptolast.organization.application;

import java.time.Instant;
import java.util.Optional;

/**
 * Puerto por el que el caso de uso compartido de importación llega a la conexión de un gestor
 * concreto sin conocer su forma. Hay una implementación por gestor, y es lo único que hay que
 * escribir para que un tercer gestor entre por el mismo camino.
 */
public interface IssueConnections {
  /** Clave estable del gestor: {@code github}, {@code gitlab}. */
  String source();

  Optional<IssueConnection> find(String ownerId);

  /** El gestor rechazó el token: la conexión deja de servir hasta que se sustituya. */
  void invalidate(String ownerId, String errorCode, Instant at);

  /** El gestor falló sin que la conexión deje de valer: se anota y nada más. */
  void recordFailure(String ownerId, String errorCode, Instant at);
}
