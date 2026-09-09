-- Recalcula la cota de longitud del token cifrado del conector de GitHub (feature 27).
--
-- V25 escribió `BETWEEN 30 AND 284` para un formato en reposo que ya no existe: entonces era
-- 1 byte de versión de clave + 12 de nonce + texto cifrado + 16 de etiqueta. Al unificar
-- `SecretCipher` entre las features 27 y 28 se retiró el byte de versión, porque @s1 de la 27 exige
-- «octet_length 12 + 14 + 16» y el byte lo contradecía. El formato de hoy es, por tanto,
-- 12 de nonce + texto cifrado + 16 de etiqueta.
--
-- Con `PersonalAccessToken` acotando el token a 1..255 caracteres ASCII, el rango real pasa a ser:
--
--   mínimo: 12 + 1   + 16 = 29   (antes 30)
--   máximo: 12 + 255 + 16 = 283  (antes 284)
--
-- La cota alta sobraba y era inofensiva, pero la baja **rechazaba una fila legítima**: un token de
-- un solo carácter, que el dominio admite, lo tumbaba la base de datos con una violación de
-- restricción —que sale como 500— en lugar de dejar decidir al dominio con el error del contrato.
-- La comprobación se mantiene, con su propósito intacto: descartar que alguien guarde texto en
-- claro por error. Sólo se le ponen los números del formato vigente.
ALTER TABLE connector_connections
    DROP CONSTRAINT connector_connections_token_ciphertext_check;

ALTER TABLE connector_connections
    ADD CONSTRAINT connector_connections_token_ciphertext_check
    CHECK (octet_length(token_ciphertext) BETWEEN 29 AND 283);
