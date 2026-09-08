# Revisión de cortes de API24

## Emisión nominal f485486 — aprobado para integrar, no cierre funcional

Root leyó las ocho fuentes y el test de emisión, comprobó las 17 huellas del freeze y el XML original de una prueba sin fallos. RED real por tipos ausentes y GREEN/formato preservados. La emisión diferida separa decisión transaccional de generación del secreto; el adaptador todavía no existe y este corte no demuestra commit, cupo, replay ni persistencia. El cálculo nominal conserva microsegundos, 30 días UTC, 32 bytes SecureRandom y SHA-256; los toString de resultados redactan secreto/verificador.

C puede integrar el commit en HTTP tras su baseline. A continúa validación y PostgreSQL. Al fijar invariantes se requieren scopes inmutables/canonicalizados y ownership claro del byte[] verificador; no se atribuyen al corte nominal validaciones que todavía no implementa. Root no escribió producto ni tests. Ponytail full/Caveman lite: puerto explícito requerido por hexagonal, sin segunda implementación de negocio.

## Cliente/intención inicial — revisión con corrección requerida

Freeze integration24_frontend_first_freeze.json: cinco huellas coincidentes y log 16/16 en dos suites, cuatro fuentes/test leídos. Confirmación ligada a id/intención, secreto canónico, replay sin secreto y cancelación antes/después de awaits están cubiertos nominalmente. El helper de intención sólo persiste owner/id y no toca otros borradores; la integración de retirada/logout todavía no existe.

Hallazgo: timestamp exige exactamente seis decimales aunque el contrato sólo exige precisión de microsegundos; una serialización UTC válida con cero o tres decimales no debe rechazarse. Reutilizar validación/conversión de instantes ya existente y comparar duración en microsegundos, sin comparaciones de strings de longitud variable. Tampoco está contratado rechazar revokedAt anterior a createdAt por retroceso del reloj. B añadirá caso rojo de respuesta válida y corregirá esas restricciones; no se pide modificar backend para satisfacer el decoder. Freeze inicial conservado, no aprobación completa todavía.

## HTTP nominal de C — aprobado para continuar, integración espera wiring

Root verificó nueve huellas de integration_http_nominal_freeze.json en checkout HTTP, leyó controlador/test y XML original 2/2. RED real de replay esperaba200 frente a201; respuesta distingue primera emisión y replay por secret:null, conserva Location inicial y no-store, y delega owner del Principal. No se acredita persistencia, autenticación Bearer, límites ni JSON cerrado: RequestBody y UUID convertido aún son provisionales del mínimo verde. C continúa esas fronteras en aislado; la integración al checkout principal espera wiring real para no introducir un bean de aplicación ausente en pruebas de contexto.

La precisión de fechas se aclaró en project-spec de9b062; A confirma que retroceso de Clock no invalida la primera fecha de revocación. B informó corrección con RED019/020 y helpers existentes, pendiente revisión del próximo corte fijo.

## Cliente completo de transporte y estructura HTTP — aprobados para continuar

Cliente: cinco huellas del segundo freeze coinciden, log32/32 (26 cliente, cuatro intención y dos UI nominal). Root leyó decoder, transporte y pruebas; casos019/020 conservan sus RED y corrigen UTC sin fracción/seis dígitos, duración exacta con microseconds existente y revocación con reloj retrasado. GET/list/revoke conservan identidad, campos cerrados, errores y abort. No atribuye recuperación/logout/UX a helpers puros. Quedan vista e integración completa.

HTTP: cinco huellas del corte estructural y XML16/16 verificados. Lectura de4097bytes antes de construir árbol,4096inclusive, duplicados/trailing/campos extra y tipos sin coacciones comprobados. UUID canónico se comprueba explícitamente; entero exacto usa BigDecimal, sin redondeo binario. Resto de gestión, handlers, canal Bearer y seguridad integrada siguen pendientes.

## Creación durable c58bad5 — aprobada para integrar con dos precisiones de oráculos

Root verificó18archivos y51evidencias originales, incluidos tres XML/29pruebas sin fallos/errores/omisiones. Leyó persistencia, dominio, emisor, migraciónV22, wiring y tests. Locks transaccionales owner→id serializan comparación/alta; replay no genera secreto ni escribe; INSERT requiere una fila y sólo retorna después de TransactionTemplate.execute. Cupo usa el mismo instante capturado por emisión. Secretos no se persisten; lista y verificador tienen copias defensivas. V22 sólo añade tabla/índice; no acredita aún rollback final ni cuotas.

Se pidió fortalecer la prueba de carrera exigiendo que observe realmente bloqueo antes de liberar, y modelar replay caducado con fechas coherentes en lugar de alterar expiresAt a un intervalo incompatible. Son precisiones de fixture/oráculo, no un RED productivo inventado. C puede incorporar el wiring y errores reales mientras A las resuelve. No se atribuyen todavía lectura, revocación, autenticación, cuotas, filas corruptas o COMMIT de resultado incierto.
