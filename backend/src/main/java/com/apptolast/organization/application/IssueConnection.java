package com.apptolast.organization.application;

/**
 * Conexión vista por el caso de uso de importación, que es lo mínimo que necesita para trabajar:
 * cómo se llama el proyecto en el recibo, cómo lo nombra el gestor al pedirle issues, el token
 * todavía cifrado y si la conexión sirve. No hay hueco para el token en claro.
 */
public record IssueConnection(
    String projectPath, String reference, byte[] tokenCiphertext, boolean valid) {}
