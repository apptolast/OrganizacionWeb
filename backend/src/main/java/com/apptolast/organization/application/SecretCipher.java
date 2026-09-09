package com.apptolast.organization.application;

import java.util.Optional;

/**
 * Puerto de salida único para guardar secretos de terceros en reposo: la dirección iCal de la
 * feature 28 y el token personal de la feature 27 son el mismo concepto. El propietario es dato
 * autenticado adicional, así que una fila movida a otro propietario deja de descifrar.
 *
 * <p>{@code decrypt} devuelve vacío en lugar de lanzar o devolver nulo para obligar a quien llama a
 * decidir qué hacer cuando el secreto ya no se puede leer: la feature 28 lo traduce a {@code
 * SECRET_UNREADABLE} y vuelve a pedir la dirección.
 */
public interface SecretCipher {
  byte[] encrypt(String ownerId, String plaintext);

  Optional<String> decrypt(String ownerId, byte[] ciphertext);

  /** Falso cuando no hay clave configurada: los conectores quedan deshabilitados. */
  boolean enabled();
}
