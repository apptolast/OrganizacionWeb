# Backend 24: ciclos TDD

## Corte 1: emisión nominal (@s1, @s5)

`CreateApiCredentialTest.s1_s5_issuesOneSecretAndOnlyVerifierAcrossCommitBoundary`: RED real por los tipos ausentes; GREEN 1/1. Reloj truncado a microsegundos, caducidad de 30 días, una llamada SecureRandom de 32 bytes, formato de token y SHA-256. El puerto de salida recibe el generador diferido; el test comprueba su resultado, no una transacción PostgreSQL. No se acredita todavía persistencia, idempotencia, límites ni autenticación.

Durante el mínimo verde se movió la preparación MessageDigest fuera de la lambda del fixture para evitar una excepción checked ajena al comportamiento. Los logs originales RED/GREEN/formato y XML se preservan externamente en deployment-preparation/integration24-01-*.

Formato SpotlessApply/Check EXIT 0. Sin campaña global ni mutación nueva. Siguientes ciclos: valores y reloj, PostgreSQL real con fixture compartida por JVM y limpieza por propietario; después lectura/revocación/autenticación/cuotas.
