package com.apptolast.organization.domain;

/** Códigos cerrados con los que se explica una sincronización fallida. */
public enum FeedError {
  /** La dirección resuelve a una dirección prohibida o ya no resuelve. */
  FEED_REJECTED,
  /** No se pudo abrir la conexión o el servidor no respondió a tiempo. */
  FEED_UNREACHABLE,
  /** El servidor respondió con algo distinto de 200, incluidas las redirecciones. */
  FEED_HTTP_ERROR,
  /** El cuerpo superó el tamaño máximo admitido. */
  FEED_TOO_LARGE,
  /** El tipo de contenido no era textual. */
  FEED_UNSUPPORTED_TYPE,
  /** El cuerpo no es un calendario iCalendar. */
  FEED_MALFORMED,
  /** La clave vigente ya no descifra la dirección guardada: hay que volver a pedirla. */
  SECRET_UNREADABLE
}
