# Revisión del contrato de planificación de bloques

Estado: APPROVED. Especificación y Gherkin revisados antes de implementación, bajo autorización global vigente.

## Decisiones contrastadas

Los revisores de backend, interfaz e integración examinaron por separado la sección Feature 11 de project-spec.md. Las precisiones incorporadas cierran las siguientes fronteras:

- Los horarios se resuelven en el servidor, con selección explícita ante ambigüedad y rechazo de horas inexistentes. El cliente comprueba la correspondencia entre intención e instantes sin copiar TZDB.
- El presupuesto usa días de la zona vigente de disponibilidad e intersecciones reales en segundos. Una zona histórica no resoluble exige actualizar disponibilidad; no se sustituye silenciosamente.
- El permiso de exceso corresponde al bloque exacto y contempla que otras reservas aumenten el exceso antes de guardar. Nunca habilita solapes.
- La deduplicación se consulta antes de reglas actuales y de nuevo tras esperar el bloqueo de disponibilidad. El replay confirmado no depende del estado posterior de la tarea ni de la revisión actual.
- Ante resultado incierto se conserva la misma intención y key. Consultar una key ausente no demuestra que la petición original se haya cancelado. Un conflicto de idempotencia no genera otra key automáticamente.
- Las fechas expuestas conservan años 0001–9999. Errores de conversión son de campo, no errores internos.
- El estado confirmado de tarea se comparte sin provocar otra consulta silenciosa. Completar una tarea no elimina el acceso a recuperación de una petición anterior.

## Reutilización y límites

Se reutilizan Clock, ZoneCatalog, JdbcTemplate, TransactionTemplate, ObjectMapper, los errores existentes, el transporte de sesión/CSRF y los controles nativos. Los bloqueos nuevos requieren SELECT separados FOR SHARE de proyecto/tarea; no reutilizar un método FOR UPDATE sólo por conveniencia. La fila propia de disponibilidad coordina reservas entre proyectos.

El editor permanece inline dentro del detalle de tarea. No incluye calendario, sesiones, replanificación, cancelación ni acreditación de trabajo. La implementación tendrá pruebas reales de concurrencia y rollback; el razonamiento sobre bloqueos no sustituye esas pruebas.

## Ejemplos temporales contrastados

El revisor backend ejecutó jshell con Java 25 (salida 0), sin modificar producción ni tests. Europe/Paris, 1900-01-01 10:00 a 10:01, usa +00:09:21 en ambos extremos y produce 60 segundos. Africa/Monrovia, 1972-01-06 23:30 con -00:44:30 a 1972-01-07 01:15 con Z, produce 3630 segundos: 60,5 minutos, que el contrato rechaza. Ambos IDs pertenecen al catálogo. Los futuros tests usarán relojes históricos explícitos; estos experimentos no cuentan como tests implementados.

## Cierre de contrato

Contrato final: 62 escenarios y 325 casos expandidos. Revisión independiente de backend e interfaz, con precisiones finales incorporadas por el autor; el coordinador comprobó formas, precedencia, recuperación, carreras y esquema de evento. Se corrigieron casos estructurales agrupados, conflicto de contexto en una carrera, distinción entre errores JSON/validación, fallback de foco y publicación de zona histórica. El protocolo del publicador se contrastó con PublishOutbox y OutboxMessage existentes.

Se aprueba iniciar TDD. Esto no acredita implementación, pruebas ejecutadas del contrato ni cierre del MVP. La baseline de arranque fue verde y no ha cambiado producción durante la destilación.
