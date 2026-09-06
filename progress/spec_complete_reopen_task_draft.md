# Borrador de sección 9 — Completar y reabrir tareas

Preparación para incorporar a project-spec.md después de cerrar split_task. No activa feature 9 ni autoriza implementación. Contrato: [contract_complete_reopen_task_draft.feature](contract_complete_reopen_task_draft.feature), con 36 escenarios y 137 casos locales según el parser del coordinador, además de todas las variantes de las matrices referenciadas. Se incorporan [propuesta](proposal_complete_reopen_task.md), [revisión backend](review_complete_reopen_task_backend.md) y [revisión de contrato](review_complete_reopen_task_contract.md). Las tablas Gherkin son la referencia de entradas y errores; no se repiten aquí.

## Estado, API y compatibilidad

Completar y reabrir son decisiones explícitas entre pending y completed, independientes del estado del proyecto y de padres/hijos. No propagan estados ni registran tiempo trabajado. Un padre completed admite hijos nuevos mientras el proyecto esté abierto; el hijo nace pending y no reabre al padre. La restricción existente de no crear trabajo bajo proyectos completed se conserva.

La lista plana, detalle, padre y lista de hijos mantienen exactamente DTO8: id, projectId, title, completionCriterion, estimatedMinutes, status, createdAt y updatedAt. Sus validadores admiten ahora pending/completed; la creación sigue produciendo pending. No se añade completedAt a esas representaciones.

GET y PUT `/api/v1/projects/{projectId}/tasks/{id}/status` devuelven HTTP 200 con exactamente `{status,completedAt,updatedAt}` y ETag fuerte del mismo snapshot confirmado. PUT acepta sólo `{status}` con el literal exacto pending o completed, sin trim ni cambio de caja. El ETag tiene forma `"task:<UUID canónico minúsculo>:<versión decimal canónica>"`; versión BIGINT no negativa, representada como `0` o entero sin signo ni ceros iniciales. El prefijo diferencia tarea de proyecto. El cliente conserva el ETag opaco, sin convertir la versión en Number.

PUT exige If-Match. Ausencia: 428 PRECONDITION_REQUIRED; formato, identidad de otra tarea o tag no canónico: 400 VALIDATION_ERROR con campo If-Match y código INVALID_VALUE; versión antigua de tarea propia: 412 TASK_CONFLICT. Se compara versión antes del no-op. Una intención ya satisfecha con revisión vigente devuelve cuerpo/ETag anteriores sin escribir, incrementar versión ni tocar fechas, historia o outbox.

## Errores y acceso

Sesión JDBC, origen y CSRF conservan las reglas vigentes. Recursos ajenos, inexistentes o vinculados a otro proyecto devuelven el mismo cuerpo completo 404 RESOURCE_NOT_FOUND, sin revelar conflicto ni estado. Respuesta privada 401 no incluye desafío Basic. Cada nueva respuesta lleva Cache-Control no-store, incluidas las combinaciones de s31.

Después de filtros y con media type admitido: validar UUID de path antes de cursor/cuerpo/precondición; en PUT validar precondición antes de JSON. Sintaxis precede forma/tipos; propiedades extra preceden status y se selecciona la primera por orden lexical. Una vez válida la petición, comprobar propiedad/existencia antes de versión y versión antes de no-op. La selección HTTP del endpoint puede devolver 415 UNSUPPORTED_MEDIA_TYPE antes del handler; no se promete otra precedencia para ese caso. Cada rechazo de las tablas mantiene válido el resto de la petición.

s7 fija REQUIRED para status ausente/null, INVALID_TYPE para escalares o estructuras incorrectos, INVALID_VALUE para literales no admitidos y UNKNOWN_FIELD para extras. JSON truncado, vacío, duplicado o concatenado usa 400 MALFORMED_JSON. Indisponibilidad de almacenamiento usa 503 STORAGE_UNAVAILABLE; nunca fabrica pending, relación ni historial vacío. Las tablas s6/s7/s13/s14/s15/s30 completan las variantes obligatorias.

## Persistencia, tiempo y concurrencia

Añadir versión interna BIGINT con default 0 y completed_at nullable. Las tareas existentes permanecen pending, versión 0, sin historia inventada. Restricciones: versión no negativa; pending implica completed_at null; completed implica completed_at presente e igual a updated_at. Una transición real incrementa la versión una vez y usa el máximo entre reloj UTC truncado a microsegundos y updatedAt anterior. Historia, evento y updatedAt comparten ese instante. Completar fija completedAt; reabrir lo limpia sin borrar cierres previos. La versión, no la fecha, ordena las transiciones.

La consulta transaccional autorizada bloquea sólo la fila de tarea mediante `FOR NO KEY UPDATE OF t`, bajo READ_COMMITTED, antes de comparar revisión. No toma bloqueo global de capacidad ni bloquea todas las tablas del JOIN. Así se mantiene compatibilidad con KEY SHARE de las FK al crear hijos; s28 verifica ambos órdenes reales de creación de hijo y cambio de estado sin bloqueo circular.

UPDATE conserva identidad/proyecto/versión y exige una fila. Actualizar tarea, insertar historia e insertar outbox es una sola transacción; cada inserción exige una fila. Excepción o cero filas en cualquiera revierte las tres operaciones y devuelve 503 sin ETag de éxito. Respuesta sólo después del commit. Dos completed concurrentes desde pending y el mismo ETag producen un ganador 200 y un 412, una transición y un evento. No hay llamada RabbitMQ dentro de la transacción.

## Historial duradero

Tabla propia task_status_history: id UUID, project_id, task_id, task_version positiva, from_status, to_status y occurred_at. FK compuesta a la tarea del mismo proyecto, unicidad `(task_id,task_version)` y estados distintos del conjunto permitido. No se enlaza su retención al outbox. Conserva transiciones indefinidamente; no hay edición, borrado ni limpieza en cascada de historia en este corte. Puede compartir UUID con eventId para correlación sin compartir responsabilidad.

GET `/api/v1/projects/{projectId}/tasks/{id}/history` devuelve exactamente `{items,nextCursor}`. Cada entrada tiene exactamente id, fromStatus, toStatus y occurredAt; son identidades y fechas confirmadas, no derivadas de la cola. Veinte entradas por página, orden task_version DESC. Consultar 21 permite determinar continuación; versiones no necesitan ser consecutivas.

Cursor opaco: base64url canónica sin padding de JSON estricto con exactamente projectId, taskId y taskVersion entero positivo dentro de BIGINT. Vinculado a proyecto/tarea; continúa por versión menor que la última devuelta. No requiere fecha ni UUID de desempate. Rechaza claves ausentes/extra/duplicadas, tipos y límites inválidos, query repetida/desconocida o cursor de otra colección según s13; ejecuta cada variante allí indicada. Historia sin transiciones devuelve vacío confirmado; 503 nunca se convierte en vacío.

## Evento y publicación

Cada transición real produce TaskStatusChanged.v1 con exactamente nueve campos: eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, taskId, fromStatus y toStatus. aggregateId identifica el proyecto; taskId la tarea; schemaVersion es 1 y type TaskStatusChanged.v1. No incluye título, criterio, estimación ni tiempo trabajado. occurredAt coincide con historia y updatedAt.

Publicar en exchange organization.events, ruta task.status-changed.v1, cola quorum durable organization.task-status-changed.v1. Conservar JSON original, identidad, persistencia, mandatory, confirmación y entrega al menos una vez. Tipo/versión desconocidos se bloquean como UNSUPPORTED_EVENT; esquema/identidad/estados incompatibles como INVALID_EVENT, sin envío. s32 ejecuta ausencia de cada uno de los nueve campos. Los cinco contratos anteriores permanecen intactos. Broker detenido no impide HTTP 200 ni consultar historia; la fila pendiente conserva identidad al publicar tras recuperar RabbitMQ.

## Interfaz y límites

El detalle carga estado y revisión antes de habilitar Completar tarea o Reabrir tarea. Muestra fecha de finalización actual y permite consultar el historial incluso después de reabrir. El estado del detalle y su control permanecen alineados. Anuncia espera, impide doble envío y sólo aplica una confirmación válida con ETag correspondiente.

412, 503, fallo de red o HTTP 200 inválido no repiten PUT automáticamente: ofrecen consulta deliberada del estado vigente. Un fallo posterior de historial conserva la transición confirmada y presenta recuperación independiente; reintentar historia no reenvía la escritura. GET anterior no sustituye el PUT confirmado. Navegación/cierre de sesión descarta respuestas tardías de status, history y escritura; 401/404 retira detalle, estado e historial privados, también durante el reintento.

Verificación: teclado/foco visible, objetivos de al menos 44 × 44 CSS, matriz y breakpoints de docs/ux-requirements.md, zoom nativo al 200 % con ancho interior de 320, feedback medido inferior a 400 ms antes de liberar respuesta retenida y treinta principios UX con evidencia o limitaciones explícitas. No atribuir bloques con inicio/fin, sesiones de concentración o progreso medido a esta feature. No incluye edición de contenido, borrado, propagación de estados, suma automática de estimaciones ni planificación. La futura edición de tarea podrá compartir esta versión interna mediante contrato propio.