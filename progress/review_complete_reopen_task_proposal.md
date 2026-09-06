# Revisión de propuesta — completar y reabrir tareas

Sólo preparación documental de feature 9. Split_task sigue activa. Leídos la propuesta, Task, migraciones V7/V8 y las restricciones actuales de DTO8. Ponytail full y Caveman lite: mantener dos estados, una versión por tarea y un historial explícito; sin propagaciones, event sourcing ni reutilización del outbox como archivo del producto.

## Opción mínima recomendada

**Estado actual:** añadir a tasks `version BIGINT NOT NULL DEFAULT 0` y `completed_at TIMESTAMPTZ NULL`. Admitir pending/completed. Una restricción exige completed_at null en pending y no null en completed. Completar fija el instante UTC del servidor; reabrir lo limpia. Ese campo describe la finalización vigente, no todas las finalizaciones anteriores. createdAt, título, criterio, estimación, padre y proyecto permanecen intactos; updatedAt cambia sólo ante transición real.

**Concurrencia:** versión interna de tarea, sin campo público version. ETag fuerte con UUID canónico y versión, distinto del ETag del proyecto. GET del recurso de estado y PUT devuelven ETag del mismo snapshot que el cuerpo. PUT requiere If-Match; copiar reglas de edición de proyecto: 428 ausente, 400 formato inválido y 412 versión antigua. Comparar versión antes de decidir no-op. Estado ya satisfecho con versión vigente devuelve 200 sin incrementar versión, tocar fechas ni añadir historial/evento. Una versión antigua sigue siendo 412 aunque coincida la intención actual: evita ocultar completar/reabrir entre lecturas. La futura edición de contenido usará esta misma versión de tarea.

**API y compatibilidad:** conservar los ocho campos de lista plana, detalle, padre e hijos; ampliar explícitamente sus valores de status a pending/completed en el contrato 9. Añadir GET/PUT `/api/v1/projects/{projectId}/tasks/{id}/status` con cuerpo de lectura/confirmación cerrado `{status, completedAt, updatedAt}`. PUT recibe sólo `{status}`. Esta proyección evita añadir un noveno campo silenciosamente a cuatro representaciones y permite mostrar la fecha y actualizar el estado visible tras guardar. Coste: una lectura específica al abrir el control; no es necesario pedirla para cada fila de lista. El control usa su propia respuesta/ETag como snapshot, sin mezclarlo con una versión leída antes del detalle.

## Historial duradero independiente

Añadir `task_status_history` con id UUID, project_id, task_id, task_version, from_status, to_status y occurred_at. FK compuesta a la tarea del mismo proyecto y unicidad `(task_id, task_version)`. Sólo se añade una fila ante transición real; no existen edición ni borrado de historia en este corte. La retención es indefinida por defecto del producto. Un futuro borrado/exportación requerirá contrato propio; la limpieza del outbox nunca toca esta tabla.

GET `/api/v1/projects/{projectId}/tasks/{id}/history` devuelve `{items,nextCursor}`, veinte transiciones por versión descendente. Cada item expone `{id,fromStatus,toStatus,occurredAt}`; la versión puede permanecer dentro del cursor opaco, vinculado a proyecto/tarea. Ordenar por versión evita depender de empates o ajustes del reloj. Una tarea sin cambios devuelve lista vacía, y ninguna tarea existente se presenta como completada antes de tener una transición confirmada.

Actualizar tarea, insertar historia e insertar outbox ocurre en una misma transacción. Bloquear únicamente la fila de tarea propia mediante la consulta autorizada basta si las transiciones no dependen del estado del proyecto; no hace falta bloqueo global de capacidad. Comprobar una fila por escritura y rollback de las tres ante excepción o cero filas. Historia y evento pueden compartir UUID para correlación, pero son registros con responsabilidades y retención distintas.

Evento propuesto: `TaskStatusChanged.v1`, esquema cerrado con eventId, aggregateId (proyecto), ownerId, occurredAt, schemaVersion, type, taskId, fromStatus y toStatus. Sin criterio, estimación ni título necesario. Publicación por ruta/cola específica; los eventos de creación anteriores no cambian. Broker detenido no impide confirmar estado e historia.

## Decisiones y riesgos que debe cerrar el contrato

- Recomiendo completar/reabrir de forma independiente del estado del proyecto: un proyecto terminado ya puede contener tareas pending. No completar padres/hijos ni reabrir el proyecto implícitamente. Añadir tareas nuevas conserva la restricción existente de proyecto abierto.
- Reabrir limpia completedAt actual, pero conserva la fila previa de finalización. El texto UI debe distinguir «finalización actual» del historial; no mostrar cero actividad al reabrir.
- Las cuatro vistas usan hoy guardas que aceptan sólo pending. Aunque DTO8 se conserve, deben actualizarse juntas para no rechazar tareas completed válidas. No ocultar este cambio semántico como compatibilidad total.
- Probar completar, reabrir y completar otra vez: tres transiciones duraderas; no-op sin cuarta fila; dos cambios concurrentes con un único ganador; fallo de cada escritura con rollback; 404 privado y sesión/CSRF; broker caído con historia consultable.
- No introducir edición de contenido aquí. Su ausencia en el roadmap sigue siendo una necesidad separada; compartir la versión desde ahora evita dos modelos de concurrencia incompatibles.

No se modifica el contrato aprobado ni se implementa esta propuesta. La decisión final de campos, códigos y retención deberá incorporarse explícitamente al contrato 9 después del cierre de split_task.
