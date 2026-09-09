package com.apptolast.organization.application;

/**
 * Puerto de salida para guardar secretos de terceros. El propietario es dato autenticado adicional:
 * una fila movida a otro propietario deja de descifrar.
 */
public interface SecretCipher {
  byte[] encrypt(String ownerId, String plaintext);

  String decrypt(String ownerId, byte[] ciphertext);

  boolean enabled();
}
