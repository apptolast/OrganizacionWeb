# Preparación de revisión de integración 14

Lectura acotada de componentes existentes de 11–13 y contrato 14; no se inspeccionó implementación nueva de los autores. No constituye dictamen del primer corte ni evidencia de pruebas. Sin suites, Git o modificaciones de producto. Ponytail full y Caveman lite.

## Reutilización y riesgos concretos

1. **Contexto y locks.** `PostgresBlockStore` ya consulta proyecto propio y después tarea con `FOR SHARE` (líneas 248/254 y 378/388). Esa secuencia encaja con 14. `PostgresTaskStatusStore:70` usa `FOR NO KEY UPDATE OF t`; `PostgresProjectStatusEditing:39` conserva su advisory heredado. No hay motivo para modificar esas transiciones. No copiar el mutex de disponibilidad de planificación: 14 debe admitir ausencia y resolver unicidad entre proyectos por propietario, aun sin esa fila.

2. **Rollback y ganador durable.** El wrapper de `PostgresBlockStore:423` captura errores alrededor de `TransactionTemplate.execute`, incluyendo finalización. Es un patrón útil, pero su traducción genérica a 503 no basta para una colisión esperada de 14. Primero debe terminar la transacción fallida; después se consulta key propia y se compara intención antes de activa. Las comprobaciones de una fila en `persistChange` son reutilizables como criterio: INSERT suprimido sin ganador sigue siendo 503, sin sesión/outbox parcial. No importar el ámbito task/key de 13 al nuevo ámbito owner/key.

3. **Lecturas.** `PostgresTodayQueries` crea su propia plantilla read-only y captura también errores de finalización; sirve como patrón de aislamiento de configuración. Su REPEATABLE_READ responde al snapshot de agenda y no debe copiarse a la lectura simple de 14, que admite READ_COMMITTED. Recuperar por ID/key debe filtrar propietario sin exigir contexto pending ni leer el outbox. La consulta activa tampoco debe tomar locks de comando ni materializar filas.

4. **Reloj y precisión.** `PlanBlock.create` reutiliza una captura y trunca a MICROS antes de construir hecho/evento. Para 14 ambos extremos deben derivar de esa captura truncada, con suma exacta y validación de años antes de escribir. `schedule-block-api.ts:301,458` calcula diferencias con `Date.parse`: eso pierde microsegundos y no satisface por sí solo 14 @s30. Puede reutilizarse la validación léxica `instant` (:368), pero la relación temporal requiere conservar la fracción completa. Tampoco deben heredarse de PlanBlock el requisito de disponibilidad, presupuesto o resolución de horas locales.

5. **Publicación.** `PublishOutbox.publishBatch` ya valida antes de entregar y clasifica blocked/retry; el transporte existente declara quorum durable y mensajes persistentes con confirms. La incorporación debe ampliar la lista cerrada de `OutboxMessage` y el mapeo explícito de `RabbitBrokerPublisher:42–78` con la novena ruta. El fallback actual construye nombres de proyecto: agregar sólo un tipo al switch enviaría a un destino incorrecto. No hace falta otro worker ni cambiar `PublisherConfiguration`. Validar los once campos y su relación sin resolver TZDB histórico; aggregateId será sesión, no proyecto como en eventos de bloques.

Estos puntos bastan para revisar el primer corte. El contrato ya delimita replay, activa de otra tarea y recuperación incierta; no se propone otra capa, tabla de mutex, matriz combinatoria ni ampliación a pausa/cierre.

Evidencia de lectura: `7f189b`, `7dfed8`, `af4e81`. Dos nombres de almacén supuestos no existían; se localizaron los nombres reales con rg, sin ejecutar ni alterar nada.

## Delta de diseño: FK del agregado outbox

**Conforme con retirar únicamente la FK a projects mediante V14**, conservando V1 intacta y `aggregate_id UUID NOT NULL`. V1:12 declara esa referencia sin nombre explícito; el nombre PostgreSQL esperado es `outbox_events_aggregate_id_fkey`, que debe verificarse en el catálogo al comprobar la migración. La búsqueda en V1–V13 no encontró otra modificación de esa referencia. No se leyó V14 en desarrollo.

Una FK ordinaria no puede apuntar alternativamente a projects o sesiones según event_type. Mantener projectId como aggregateId contradice el evento14; añadir un registro universal de agregados, columnas alternativas o triggers introduce estructura adicional innecesaria. Retirar la FK específica es el cambio estándar mínimo para este outbox de varios agregados. La integridad de contexto de la sesión permanece en su tabla y su evento se registra en la misma transacción. Se pierde deliberadamente la garantía referencial SQL de aggregate_id para eventos de proyecto; no se afirma que permanezca intacta esa restricción.

La comprobación acotada de upgrade debe conservar filas existentes del outbox, incluidos payload, aggregate_id y estado de publicación, y verificar que siguen PK/NOT NULL y los campos de publicación de V2. Un evento14 debe admitir un sessionId que no exista en projects; un agregado null debe seguir rechazándose. El flujo heredado de publicación debe mantener sus destinos y payload sin reescritura. Un fallo de la migración transaccional debe dejar el esquema anterior, sin eliminación de datos. No hace falta backfill ni reidentificar eventos históricos. Evidencia de lectura: `554bad`, `c3121c` y lectura directa de V2.
