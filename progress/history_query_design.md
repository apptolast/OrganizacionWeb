# Diseño de consulta de historial18 — propuesta documental

Lectura del esquema y adaptadores actuales, sin migración, SQL ejecutado, endpoint ni contrato nuevo. Destinado a proposal_history.md. La selección funcional final corresponde a la propuesta18; este diseño muestra cómo consultar las cinco fuentes solicitadas sin añadir una tabla de eventos, secuencia global ni transacción persistente entre peticiones.

## Hechos y claves disponibles

| Fuente durable | Identidad / tiempo de orden | Contenido histórico y límite |
| --- | --- | --- |
| work_sessions (V14–V18) | id / started_at | SessionStart7: IDs, inicio, minutos/fin originales y zona inmutables. status/revision/changed_at/running_since/worked_microseconds/effective_end_at/last_decision_at son proyección mutable: no representan el hecho START ni ordenan su fila histórica. |
| work_session_changes (V16) | id / occurred_at | owner_id, session_id, action PAUSE/RESUME/CLOSE/EXTEND y receipt JSONB durable. WorkSessionTransitionReceipt interno admite variantes6/7 y campos closure/extension opcionales; HTTP debe conservar sus formas discriminadas, no serializar nulls nuevos. CLOSE aporta notas, workDate y closeZoneId persistidos; no recalcularlos con zona actual. |
| task_status_history (V9) | id / occurred_at | project/task, task_version, from_status/to_status. Versión sólo total dentro de una tarea; no orden global. No contiene título ni nombre histórico. |
| planned_blocks (V11) | id / created_at | Hecho de reserva original con objective, locales, offsets, zona, duración e instantes. start_at es tiempo planificado, no instante de creación. La proyección block_projections no sustituye ni añade el hecho original. |
| block_changes (V12–V13) | id / occurred_at | kind, version y receipt BlockChangeReceipt con before/after durables. No reconstruir desde la reserva/proyección actual. |

Cada tabla tiene PK UUID propia; el mismo UUID puede existir en tablas distintas. Usar (sourceRank,id) como identidad de fila y desempate. Los cinco rangos propuestos son constantes de contrato de cursor, no orden cronológico de negocio. No afirmar causalidad entre entidades por UUID ni timestamp empatado. Para una sesión, revisión explica la secuencia causal aunque dos hechos compartan microsegundo; si el producto quiere desempate por revisión deberá fijarlo en el contrato antes de implementar, no inferirlo del UUID.

projects.name y tasks.title son etiquetas actuales mutables, no snapshots presentes en estos hechos. Se pueden mostrar como contexto actual leído en el mismo snapshot de página y enlazar por IDs. Un cambio de nombre entre páginas es posible y no reescribe el hecho. No inventar nombre al momento del evento ni completar tiempos trabajados desde reservas. El inicio no contiene trabajo neto final; sólo CLOSE lo aporta históricamente. Outbox es transporte con retención/publicación, no fuente de historial.

## Reutilización concreta

- PostgresBlockChangeQueries: plantilla propia readOnly REPEATABLE_READ, guardia owner/contexto, captura TransactionException/DataAccessException fuera de execute (incluye fallo al finalizar). Es mejor base de política18 que copiar PostgresTaskHistoryQueries, que actualmente usa dos SELECT sin plantilla propia.
- ReadBlockChanges y páginas existentes: pedir21, devolver20, nextCursor sólo si existe la21; cursor del último elemento entregado, no del lookahead. Tamaño20 es propuesta de reutilización, pendiente de norma18.
- BlockCursor: base64url sin padding y roundtrip canónico; JSON cerrado, rechazo de claves duplicadas/trailing tokens; UTC1–9999 y precisiónµs; UUID explícito. Su shape está ligado a project/task y sólo time/id: no reutilizar directamente para unión global ni alterar contrato13. Extraer únicamente validación común si el test futuro lo exige; evitar framework genérico anticipado.
- TaskHistoryController: cursor taskVersion funciona por tarea; no sirve para unión global. Su orden UUID antes de query tampoco fija precedencia18: reutilizar la convención reciente de seguridad→query→cursor, documentada por contrato propio.
- ApiErrors: ValidationException/FieldError para400 VALIDATION_ERROR, query/cursor INVALID_VALUE; StorageUnavailableException para503 STORAGE_UNAVAILABLE y problem+json. Principal.getName aporta owner, nunca parámetro owner aceptado del cliente. Seguridad/no-store existentes se conservan. Historial global propio vacío debe ser200 vacío, no404 ni «almacenamiento caído». Filtros project/task, si se añaden, necesitarían decisión de privacidad propia; no los presupone esta consulta.
- WorkSessionStore muestra wrappers read-only y lectura JSONB compatible, pero no mezclar comandos/locks con esta nueva consulta. No Clock necesario para listar hechos ya ocurridos; no sumar neto en vivo ni snapshot.serverNow por fila.

## SQL ilustrativo de sólo lectura

Es pseudoconsulta parametrizada para documentación, no script ejecutado. Usa el owner autenticado en **cada rama**, y además lo contrasta con el contexto actual en la unión final. La doble comprobación en cambios de sesión evita confiar sólo en su owner_id desnormalizado. Los hechos originales devuelven identificadores y datos escalares seleccionables; receipt NULL no significa inexistencia, sino que esas fuentes no guardan JSONB de recibo. Un mapper puede seleccionar los campos originales necesarios en esta misma consulta sin un GET por fila; el DTO mínimo aún corresponde a la propuesta18.

```sql
WITH owned_tasks AS (
  SELECT t.id AS task_id, t.project_id, t.title AS current_task_title,
         p.name AS current_project_name
  FROM tasks t JOIN projects p ON p.id=t.project_id
  WHERE p.owner_id=:owner
), facts AS (
  SELECT s.started_at AS occurred_at, 1 AS source_rank, s.id AS fact_id,
         'WORK_STARTED'::text AS kind, s.project_id, s.task_id,
         s.id AS resource_id, NULL::jsonb AS receipt
  FROM work_sessions s JOIN owned_tasks o
    ON (o.project_id,o.task_id)=(s.project_id,s.task_id)
  WHERE s.owner_id=:owner
  UNION ALL
  SELECT c.occurred_at, 2, c.id, c.action, s.project_id, s.task_id,
         c.session_id, c.receipt
  FROM work_session_changes c
  JOIN work_sessions s ON s.id=c.session_id
  JOIN owned_tasks o ON (o.project_id,o.task_id)=(s.project_id,s.task_id)
  WHERE c.owner_id=:owner AND s.owner_id=:owner
  UNION ALL
  SELECT h.occurred_at, 3, h.id, 'TASK_STATUS_CHANGED',
         h.project_id,h.task_id,h.task_id,NULL::jsonb
  FROM task_status_history h JOIN owned_tasks o
    ON (o.project_id,o.task_id)=(h.project_id,h.task_id)
  UNION ALL
  SELECT b.created_at, 4, b.id, 'BLOCK_PLANNED',
         b.project_id,b.task_id,b.id,NULL::jsonb
  FROM planned_blocks b JOIN owned_tasks o
    ON (o.project_id,o.task_id)=(b.project_id,b.task_id)
  UNION ALL
  SELECT c.occurred_at, 5, c.id,c.kind,
         c.project_id,c.task_id,c.block_id,c.receipt
  FROM block_changes c JOIN owned_tasks o
    ON (o.project_id,o.task_id)=(c.project_id,c.task_id)
)
SELECT f.*, o.current_project_name, o.current_task_title
FROM facts f JOIN owned_tasks o
  ON (o.project_id,o.task_id)=(f.project_id,f.task_id)
WHERE (:after_time::timestamptz IS NULL OR
       (f.occurred_at,f.source_rank,f.fact_id) <
       (:after_time::timestamptz,:after_source::integer,:after_id::uuid))
ORDER BY f.occurred_at DESC,f.source_rank DESC,f.fact_id DESC
LIMIT 21;
```

El mapper debe incorporar task_version/from_status/to_status y los campos originales de SessionStart/PlannedBlock cuando el DTO los necesite: mediante columnas explícitas en cada rama o joins de las20 filas seleccionadas dentro de la misma transacción, nunca mediante una consulta HTTP por fila ni `to_jsonb(tabla)` que exponga request_key/owner/metadatos internos. Receipt interno se valida/convierte a su variante pública. No omitir silenciosamente JSON corrupto: concretar traducción a503 de integridad/lectura en el contrato si se incluye payload, sin convertirlo en página vacía.

## Cursor y estabilidad exacta

Propuesta mínima: JSON cerrado versionado con collection=`workHistory`, occurredAt UTCµs, sourceRank del enum fijo e id UUID; incluir filtros normalizados si el contrato incorpora filtros. El cursor no concede acceso: cada petición vuelve a aplicar owner autenticado en SQL. No incluir notas, nombres ni request keys; no necesita guardar owner si no se promete rechazar un cursor copiado entre propietarios. Si se exige ese rechazo además del aislamiento, requerirá vínculo verificable decidido explícitamente; base64 no es firma ni secreto.

Orden descendente total sobre claves inmutables y comparación estricta `<`: una fila ya entregada no reaparece al continuar correctamente. Las nuevas filas delante del límite anterior quedan excluidas del recorrido y aparecen al refrescar desde primera página. Un commit tardío con clave menor que el límite puede aparecer en una página posterior; si su clave pertenece al tramo ya recorrido, no aparecerá hasta refrescar. Un límite de tiempo inicial no soluciona commits tardíos con occurredAt antiguo. REPEATABLE_READ sólo congela cada petición, no todas las páginas. Los cambios de nombres también pueden ser visibles entre peticiones.

No afirmar snapshot global, exhaustividad de un recorrido frente a inserciones concurrentes ni ausencia de omisiones respecto a un instante inicial. «Actualizar historial» reinicia cursor para incorporar lo nuevo. No OFFSET, secuencia global, tabla auxiliar ni exportación de snapshot PostgreSQL sólo para fabricar esa garantía. En un snapshot sin nuevas inserciones, cada hecho aparece una vez con su identidad compuesta.

## Índices existentes y candidatos, sin DDL

Existentes: PK id en cinco fuentes; projects(owner_id,created_at DESC,id DESC) V3; tasks(project_id,created_at DESC,id DESC) V7 y UNIQUE(project_id,id) V8; task_status_history UNIQUE(task_id,task_version) V9; planned_blocks UNIQUE(task_id,request_key) V11 y UNIQUE(project_id,task_id,id) V13; block_changes UNIQUE(task_id,request_key), UNIQUE(block_id,version); work_sessions UNIQUE(owner_id,request_key), índice único parcial owner abierto; work_session_changes UNIQUE(owner_id,request_key) y parcial session_id WHERE action='CLOSE'. Ninguno de esos índices de idempotencia es por sí mismo un índice de orden global temporal.

Candidatos mínimos a validar con plan real en implementación: work_sessions(owner_id,started_at DESC,id DESC); work_session_changes(owner_id,occurred_at DESC,id DESC); para las tres fuentes que heredan owner por proyecto, (project_id,occurred_at DESC,id DESC) o (project_id,created_at DESC,id DESC) según tabla. No se prescribe un índice extra de nombre/estado, no se indexa JSONB sin consulta concreta ni se promete que esos índices eviten ordenar toda la unión. El planificador puede usar el índice existente de projects por su prefijo owner. Si el volumen exige empujar frontera/limit21 a cada rama, se conserva el mismo orden total y luego se toma el top21 global; medir antes de complejizar. No EXPLAIN ni benchmark realizado en esta tarea.

## Handoff y límites

Lecturas fae121/5bd137/d5a80a/535b40 sustentan tablas, recibos, páginas y excepciones. Un glob inicial incompatible con PowerShell y una ruta inicialmente buscada en domain en vez de application fallaron sólo en lectura; corregidos sin acciones de escritura. No hay migraciones, tests, Gherkin ni producción18 creados. C puede adoptar el diseño más pequeño compatible con la propuesta y fijar los detalles de DTO/orden causal antes del contrato.
