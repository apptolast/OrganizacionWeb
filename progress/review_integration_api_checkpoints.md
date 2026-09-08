# Revisión de cortes de API24

## Emisión nominal f485486 — aprobado para integrar, no cierre funcional

Root leyó las ocho fuentes y el test de emisión, comprobó las 17 huellas del freeze y el XML original de una prueba sin fallos. RED real por tipos ausentes y GREEN/formato preservados. La emisión diferida separa decisión transaccional de generación del secreto; el adaptador todavía no existe y este corte no demuestra commit, cupo, replay ni persistencia. El cálculo nominal conserva microsegundos, 30 días UTC, 32 bytes SecureRandom y SHA-256; los toString de resultados redactan secreto/verificador.

C puede integrar el commit en HTTP tras su baseline. A continúa validación y PostgreSQL. Al fijar invariantes se requieren scopes inmutables/canonicalizados y ownership claro del byte[] verificador; no se atribuyen al corte nominal validaciones que todavía no implementa. Root no escribió producto ni tests. Ponytail full/Caveman lite: puerto explícito requerido por hexagonal, sin segunda implementación de negocio.

## Cliente/intención inicial — revisión con corrección requerida

Freeze integration24_frontend_first_freeze.json: cinco huellas coincidentes y log 16/16 en dos suites, cuatro fuentes/test leídos. Confirmación ligada a id/intención, secreto canónico, replay sin secreto y cancelación antes/después de awaits están cubiertos nominalmente. El helper de intención sólo persiste owner/id y no toca otros borradores; la integración de retirada/logout todavía no existe.

Hallazgo: timestamp exige exactamente seis decimales aunque el contrato sólo exige precisión de microsegundos; una serialización UTC válida con cero o tres decimales no debe rechazarse. Reutilizar validación/conversión de instantes ya existente y comparar duración en microsegundos, sin comparaciones de strings de longitud variable. Tampoco está contratado rechazar revokedAt anterior a createdAt por retroceso del reloj. B añadirá caso rojo de respuesta válida y corregirá esas restricciones; no se pide modificar backend para satisfacer el decoder. Freeze inicial conservado, no aprobación completa todavía.
