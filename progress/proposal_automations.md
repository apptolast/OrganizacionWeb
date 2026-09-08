# Propuesta normativa — Feature 30 `automations`

Redactada el 8 de septiembre de 2026 por el `spec_partner` bajo la autorización global del 5 de septiembre. Sección lista para pegar en `project-spec.md`. `progress/proposal_webhooks.md` no existía al redactar; se asume que 25 define un cursor por consumidor sobre `outbox_events` leído por un worker con `app.webhooks.enabled`, sin tocar los adaptadores de commit. Los puntos que dependen de 25 quedan marcados como PREGUNTA ABIERTA.

## Feature 30: automations — Reglas auditables con simulación

### Propósito y frontera

Permitir que el propietario declare reglas «cuando ocurra este evento, haz esta acción» sobre sus propios datos, con vista previa sin efectos y registro de cada ejecución. Cumple la línea «plantillas y automatizaciones auditables» de personalización. No es un motor de flujos: no hay condiciones compuestas, temporizadores, acciones destructivas ni edición de eventos. Una automatización nunca sustituye el gesto humano de completar, cerrar o replanificar.

### Modelo de regla

Una regla tiene `id` UUID de servidor, `name` (1–80 puntos de código tras recortar Unicode White_Space), `enabled`, `trigger`, `condition`, `action`, `version` y fechas. El propietario procede del principal; ningún campo del cuerpo lo fija.

- `trigger.eventType`: exactamente uno de los doce tipos publicados por `OutboxMessage`: `ProjectCreated.v1`, `ProjectUpdated.v1`, `ProjectStatusChanged.v1`, `TaskCreated.v1`, `SubtaskCreated.v1`, `TaskStatusChanged.v1`, `BlockPlanned.v1`, `BlockChanged.v1`, `WorkSessionStarted.v1`, `WorkSessionStateChanged.v1`, `WorkSessionExtended.v1`, `WorkSessionClosed.v1`. Otro valor: 400 `UNKNOWN_EVENT_TYPE`. Añadir un tipo a `OutboxMessage` no lo habilita solo: la lista de 30 es un catálogo cerrado propio que se amplía por contrato.
- `condition`: `null` o `{ projectId }`. Coincide cuando el proyecto resuelto del evento es ese proyecto propio. Resolución por tipo: `Project*`, `TaskCreated`, `SubtaskCreated` y `TaskStatusChanged` usan `aggregateId`; `WorkSessionStarted` usa `payload.projectId`; `BlockPlanned` y `BlockChanged` resuelven `payload.taskId → tasks.project_id`; `WorkSessionStateChanged`, `WorkSessionExtended` y `WorkSessionClosed` resuelven `aggregateId → work_sessions → tasks.project_id`. Un `projectId` ajeno o inexistente al guardar: 422 `TARGET_NOT_FOUND`, mismo mensaje en ambos casos.
- `action` es una de dos formas cerradas:
  - `{ type: "CREATE_TASK", projectId, titleTemplate, criterionTemplate, estimatedMinutes }`. Crea una tarea raíz en un proyecto propio mediante el caso de uso existente `CreateTask`, con sus mismas reglas (proyecto propio y no `completed`, título 1–160, criterio ≤ 2000, minutos 1–1440 o `null`). La tarea no tiene campo descripción; la «descripción» de la regla es `criterionTemplate`, que alimenta `completionCriterion` (vacío si `null`). `estimatedMinutes` es un valor fijo opcional, nunca calculado.
  - `{ type: "NOTIFY_WEBHOOK", endpointId }`. Encola en el mecanismo de entregas de 25 el evento original, sin transformar, hacia un endpoint propio de 25. Endpoint ajeno, inexistente o desactivado al guardar: 422 `ENDPOINT_NOT_FOUND`.
- Plantillas: texto plano con marcadores `{{event.type}}`, `{{task.title}}`, `{{project.name}}` y `{{occurredAt}}` (ISO-8601 UTC del evento). Cualquier otro marcador, un `{{` sin cierre o `{{task.title}}` en un trigger `Project*` (no hay tarea) produce 400 `INVALID_TEMPLATE` con `errors[]` que señala el campo. Los valores se insertan sin escapar: son texto que la interfaz muestra como texto. `task.title` y `project.name` se resuelven con el estado vigente en el instante de ejecución, no con el histórico. Longitud de plantilla cruda: título ≤ 160, criterio ≤ 2000 puntos de código; el resultado resuelto que supere esos límites no se trunca, falla la ejecución con `TITLE_TOO_LONG` o `CRITERION_TOO_LONG`.
- Máximo 20 reglas por propietario contando desactivadas; la 21.ª devuelve 409 `RULE_LIMIT` sin escritura. Borrar libera cupo.
- `version` BIGINT desde 1; `ETag: "<version>"` en cada representación. `PUT` y `DELETE` exigen `If-Match`: ausente 428 `PRECONDITION_REQUIRED`, mal formado `VALIDATION_ERROR` sobre `If-Match`, distinto de la versión vigente 412 `AUTOMATION_CONFLICT`. Activar/desactivar se hace con `PUT` completo cambiando `enabled`; no hay endpoint aparte.

### Persistencia (migración reservada V28__automations.sql)

- `automation_rules(id UUID PK, owner_id TEXT NOT NULL, name, enabled BOOLEAN, event_type TEXT, condition_project_id UUID NULL, action JSONB CHECK jsonb_typeof='object', version BIGINT CHECK ≥ 1, created_at, updated_at)`, índice `(owner_id, created_at, id)`. `action` guarda la forma cerrada validada en aplicación; la migración no interpreta plantillas.
- `automation_runs(id UUID PK, rule_id UUID NULL REFERENCES automation_rules ON DELETE SET NULL, owner_id, event_id UUID, event_type, occurred_at, attempt INTEGER CHECK 1..3, status TEXT CHECK IN ('succeeded','retry','failed'), created_task_id UUID NULL, delivery_id UUID NULL, error_code TEXT NULL, executed_at TIMESTAMPTZ, UNIQUE (rule_id, event_id))`, índices `(rule_id, executed_at DESC, id DESC)` y `(created_task_id)`. La restricción única es la idempotencia: una regla ejecuta como máximo una vez por evento aunque el cursor relea.
- Cursor de ejecución: una fila por propietario `(owner_id PK, occurred_at, event_id)` bajo el nombre de consumidor `automations`. PREGUNTA ABIERTA (depende de 25): si V23 crea una tabla de cursores por consumidor, V28 no crea otra y usa `consumer = 'automations'`; si no, V28 crea `automation_cursors`. La destilación fija la opción leyendo V23 antes del primer escenario de persistencia.

### Ejecución

Worker programado propio (`app.automations.enabled`, por defecto `false`, `fixedDelay` 1 s) que consume el puerto genérico de cola de eventos de 25 (`EventTail`/`OwnerEventCursor`): «dame hasta N eventos de este propietario posteriores al cursor, ordenados por `occurred_at, event_id`». No modifica adaptadores de commit ni `OutboxMessage`. Lee `outbox_events` con independencia del estado de publicación en RabbitMQ; omite filas `blocked`. La misma semántica de cursor que 25 aplica: un evento que confirma tarde con `occurred_at` anterior al cursor no se procesa; entrega de mejor esfuerzo, sin promesa de exhaustividad ante relojes atrasados.

Por ciclo y propietario con al menos una regla activa: primero reintentos pendientes (`status = 'retry'`, `attempt < 3`), después hasta 100 eventos nuevos. Por evento, una transacción PostgreSQL: evaluar las reglas activas en orden `created_at, id`; para cada coincidencia insertar la fila de `automation_runs` con `ON CONFLICT (rule_id, event_id) DO NOTHING` (si ya existe, no se ejecuta), ejecutar la acción y avanzar el cursor. `CREATE_TASK` invoca `CreateTask` dentro de esa transacción (propagación REQUIRED, el adaptador se une): fila de `automation_runs`, tarea y `TaskCreated.v1` confirman juntos o ninguno. `NOTIFY_WEBHOOK` inserta la entrega de 25 en la misma transacción y guarda `delivery_id`.

Fallos deterministas (`PROJECT_COMPLETED`, `TARGET_NOT_FOUND`, `ENDPOINT_NOT_FOUND`, `TITLE_TOO_LONG`, `CRITERION_TOO_LONG`) registran `failed` en el primer intento con `error_code`, sin tarea ni entrega, y no detienen las demás reglas ni el cursor. Fallos de almacenamiento o excepción inesperada revierten toda la transacción del evento y, en una transacción corta aparte, registran o incrementan la fila `retry` con `attempt + 1` y `error_code` (`STORAGE_UNAVAILABLE`, `INTERNAL_ERROR`); el cursor no avanza. El tercer intento fallido pasa a `failed`. Si ni siquiera puede escribirse el registro de reintento, el evento se vuelve a leer en el ciclo siguiente sin contar intento: no hay bucle infinito porque cada intento registrado consume cupo y un almacenamiento caído no ejecuta nada. No se emite ningún evento nuevo de tipo automatización; el único evento producido es el `TaskCreated.v1` ordinario de la tarea creada.

Una regla solo ve eventos que el cursor lee mientras está activa. Al crear la primera regla del propietario, el cursor se inicializa en el último evento existente de ese propietario: el historial anterior no se ejecuta. Reactivar una regla no procesa lo ocurrido mientras estuvo desactivada.

### Protección contra bucles

Los eventos `TaskCreated.v1` y `SubtaskCreated.v1` cuyo `payload.taskId` aparezca en `automation_runs.created_task_id` no evalúan ninguna regla; el evento se salta y el cursor avanza. Profundidad 1: la tarea creada por una automatización no dispara otra creación, pero acciones humanas posteriores sobre esa tarea (completar, planificar bloque) sí disparan reglas. `NOTIFY_WEBHOOK` no genera eventos y no necesita guarda.

Mecanismo elegido: la columna `created_task_id` de `automation_runs` con índice, consultada solo para esos dos tipos. Justificación: el dato ya existe para auditoría, no exige tabla nueva ni tocar `PostgresTaskCommit`, y `ON DELETE SET NULL` conserva la guarda cuando se borra la regla. Descartadas: marcar el origen en el payload del evento (rompe los esquemas cerrados validados por `OutboxMessage`), y una tabla `task_external_links` con `source = 'automation'` (no existe todavía, obligaría a un segundo insert en el commit de tareas y 27 podría definirla con otra semántica).

### Simulación

`POST /api/v1/me/automations/simulate` recibe el mismo cuerpo que `POST /api/v1/me/automations` (regla sin `id`), lo valida íntegramente con los mismos errores, y evalúa el trigger y la condición contra los últimos 100 eventos del propietario en `outbox_events` (orden `occurred_at, event_id` descendente, excluidas filas `blocked`). Respuesta 200 cerrada: `{ evaluatedEvents, matches: [{ eventId, eventType, occurredAt, preview }] }`, donde `preview` es `{ type: "CREATE_TASK", projectId, title, completionCriterion, estimatedMinutes, wouldFail: null | código }` o `{ type: "NOTIFY_WEBHOOK", endpointId, eventId }`. Las plantillas se resuelven con los valores vigentes; `wouldFail` anticipa `PROJECT_COMPLETED`, `TITLE_TOO_LONG`, etc. La simulación no escribe, no encola, no mueve el cursor, no aplica la guarda de bucles (informa `loopGuarded: true` en la coincidencia) y no queda en `automation_runs`. Cero coincidencias es una respuesta válida con `matches: []`. La simulación no promete que una regla guardada después coincida con los mismos eventos: el cursor arranca en el presente.

Elegida la variante con la regla en el cuerpo y descartada `POST .../{id}/simulate`: la primera sirve antes y después de guardar con un solo evaluador; la segunda obligaría a dos endpoints o a guardar para probar.

### Auditoría

`GET /api/v1/me/automations/{id}/runs?cursor=` devuelve exactamente `{ items, nextCursor }`, 20 por página, orden `executed_at, id` descendente. Cada item: `{ id, eventId, eventType, occurredAt, attempt, status, createdTaskId, deliveryId, errorCode, executedAt }` con `null` donde no aplique. Cursor opaco `(executedAt, id)` vinculado a la regla; cursor de otra regla o mal formado: `VALIDATION_ERROR`. Regla inexistente o ajena: 404 `RESOURCE_NOT_FOUND`. Los runs de reglas borradas dejan de ser consultables; las tareas creadas siguen en sus proyectos.

### API completa

Rutas privadas, autenticadas por sesión o credencial de 24 con alcance de escritura, `Cache-Control: no-store`, CSRF y `OriginGuard` vigentes, JSON estricto sin campos desconocidos. Representación de regla: `{ id, name, enabled, trigger, condition, action, version, createdAt, updatedAt }`.

| Método y ruta | Éxito | Errores propios |
| --- | --- | --- |
| `GET /api/v1/me/automations` | 200 `{ items }` (máx. 20, orden `createdAt, id`) | — |
| `POST /api/v1/me/automations` | 201, `Location`, `ETag` | 400 `VALIDATION_ERROR`/`UNKNOWN_EVENT_TYPE`/`INVALID_TEMPLATE`, 409 `RULE_LIMIT`, 422 `TARGET_NOT_FOUND`/`ENDPOINT_NOT_FOUND` |
| `GET /api/v1/me/automations/{id}` | 200 + `ETag` | 404 `RESOURCE_NOT_FOUND` |
| `PUT /api/v1/me/automations/{id}` | 200 + nuevo `ETag`, `version + 1` | los de POST salvo `RULE_LIMIT`, 428, 412 `AUTOMATION_CONFLICT`, 404 |
| `DELETE /api/v1/me/automations/{id}` | 204 | 428, 412, 404 |
| `POST /api/v1/me/automations/simulate` | 200 | los de validación de POST |
| `GET /api/v1/me/automations/{id}/runs` | 200 | 400, 404 |

401, 403 CSRF/origen, 415, 405 y 503 `STORAGE_UNAVAILABLE` siguen los filtros y códigos globales. `PUT` que no cambia nada sigue incrementando `version` (misma regla que edición de proyecto). Un `PUT` cambiando `trigger` o `action` no borra runs anteriores.

### Seguridad y privacidad

Toda lectura y escritura filtra por `owner_id`; proyectos y endpoints referenciados deben pertenecer al mismo propietario y las respuestas para ajeno e inexistente son idénticas. Las plantillas son texto: la interfaz nunca las interpreta como HTML y el servidor no evalúa expresiones. Los logs del worker registran `ruleId`, `eventId`, `outcome`, `attempt` y `code`; nunca plantillas resueltas, títulos, nombres ni payloads. `NOTIFY_WEBHOOK` no expone datos nuevos: envía el mismo evento que 25 ya puede entregar, con las mismas firma y redacción de 25. No hay acciones sobre datos de terceros ni llamadas salientes fuera del mecanismo de 25.

### Concurrencia y recuperación

Dos réplicas del worker no ejecutan la misma regla dos veces para un evento: la fila de `automation_runs` se inserta antes de actuar y `UNIQUE (rule_id, event_id)` falla la segunda dentro de su transacción. El avance del cursor comparte transacción con las ejecuciones del evento; una caída deja el cursor en el evento anterior y la relectura es idempotente. Borrar o desactivar una regla mientras el worker evalúa: la transacción del evento bloquea la fila de la regla (`FOR SHARE`) y respeta el estado que lea; un `PUT` concurrente espera o el evento se ejecuta con la versión anterior, nunca con una mezcla. Reinicio del backend no pierde reglas, runs ni cursor. Con `app.automations.enabled=false` no se lee ni escribe nada y los eventos no se acumulan como deuda: al habilitar, el cursor sigue desde donde quedó, salvo la inicialización descrita para propietarios sin cursor.

### Interfaz

Ruta `/automatizaciones`, encabezado «Automatizaciones», entrada de navegación tras Integraciones. Lista de reglas con nombre, disparador legible, destino y estado activa/inactiva, con estados vacío, cargando y error independientes. Editor con controles nativos: nombre, `<select>` de tipo de evento con etiqueta en español, proyecto opcional de la condición, tipo de acción y sus campos; ayuda inline de los cuatro marcadores; validaciones del servidor asociadas a su campo. Botón «Simular» que muestra en la misma página el número de eventos evaluados y una lista de coincidencias con la vista previa resuelta y el posible fallo anticipado; no guarda. Botón «Guardar» separado, deshabilitado durante la petición, con conflicto 412 explicado y recarga deliberada de la versión vigente conservando el borrador. Historial de ejecuciones por regla con paginación «Cargar más», estado no solo por color y enlace a la tarea creada. Activar/desactivar mediante interruptor accesible que envía el `PUT` y refleja solo la respuesta confirmada. Sin `localStorage`; el borrador vive en memoria del componente. Matriz de `docs/ux-requirements.md`: 320/768/1440, zoom 200 %, teclado, foco, 44 px, axe en Playwright; principios Tesler (la resolución del proyecto del evento la hace el sistema), Modelo mental (la tarea creada es pendiente, no trabajo hecho) y Fin de pico (simular antes de guardar) revisados con evidencia.

### Límites explícitos

Sin condiciones compuestas, comparadores distintos de «proyecto igual a», ni filtros por texto. Sin programación temporal, cron, retrasos ni recurrencia. Sin acciones destructivas ni de estado: no completa, reabre, borra, replanifica ni cierra sesiones. Sin edición, supresión o reemisión de eventos. Sin encadenar automatizaciones (profundidad 1). Sin plantillas con lógica, formato de fechas ni expresiones. Sin acciones hacia terceros fuera de los endpoints de 25. Sin reintento manual de runs `failed` en este corte. Sin importación/exportación de reglas en 22/23.

### Decisiones y alternativas descartadas

| Decisión | Alternativa descartada | Motivo |
| --- | --- | --- |
| Consumir el puerto de cola de eventos de 25 | Cursor y lector propios de 30 | Un solo mecanismo de cola por consumidor; menos código y una sola semántica de orden |
| Cursor por propietario con nombre de consumidor `automations` | Cursor por regla | 20 cursores por propietario multiplican estados; la unicidad `(rule_id, event_id)` ya da idempotencia por regla |
| Guarda de bucles vía `created_task_id` | Marcar el payload o tabla `task_external_links` | No toca esquemas cerrados ni commits existentes; dato ya necesario para auditoría |
| Fallos deterministas sin reintento; 3 intentos solo para fallos transitorios | 3 intentos uniformes | Reintentar `PROJECT_COMPLETED` no cambia el resultado y ensucia el historial |
| Resultado resuelto demasiado largo falla | Truncar en silencio | Falla cerrado ante dato no confirmado; la simulación lo anticipa |
| Simulación con la regla en el cuerpo | `POST .../{id}/simulate` | Un evaluador y un endpoint; permite probar antes de guardar |
| `PUT` completo para activar/desactivar | `PATCH` o endpoint `/enable` | Reutiliza ETag y validación sin superficie extra |
| Borrado físico con `ON DELETE SET NULL` en runs | Borrado lógico de reglas | Libera cupo real y conserva la guarda de bucles sin estado «borrada» en la API |
| Cursor inicial en el presente | Procesar backlog al crear la primera regla | Evita crear cientos de tareas por historial antiguo; coherente con «una regla ve el futuro» |
| Entrega doble posible si el endpoint ya está suscrito al tipo en 25 | Prohibir la combinación | El receptor deduplica por `eventId` según el contrato de 25; prohibirlo acopla validaciones de dos features |

### Verificación prevista

Unit: motor de plantillas (marcadores válidos, desconocidos, sin cierre, por tipo de trigger, límites de longitud por puntos de código) y evaluador (trigger, condición, resolución de proyecto por tipo, guarda de bucles) con reloj inyectado y sin Spring. Integración PostgreSQL Testcontainers: V28, unicidad `(rule_id, event_id)` con dos ejecutores concurrentes, atomicidad run+tarea+evento con fallo inducido, cursor tras caída, reintentos hasta `failed`, `ON DELETE SET NULL`, límite de 20. MockMvc `AutomationsApiTest`: tabla completa de errores, ETag/If-Match, JSON estricto, simulación sin escrituras. Frontend Vitest: editor, simulación, historial, conflicto 412, estados vacío/error. E2E Playwright con worker habilitado: crear regla, provocar el evento, ver la tarea y su run; matriz responsive y axe. PIT scope `automations` (dominio, casos de uso, controlador, adaptador y worker) y `frontend/stryker.automations.config.json`, ambos con umbral 80 % y sin timeouts contados como muertos. Los escenarios que dependan del puerto de 25 usan un doble del puerto hasta que 25 esté implementada; ningún escenario de 30 se declara verde por vacuidad si 25 no existe.
