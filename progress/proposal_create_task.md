# Propuesta inicial — create_task (feature 7)

Preparación de producto, sin contrato aprobado ni producción. Authentication sigue siendo la única feature activa. El objetivo siguiente es guardar una unidad pequeña de trabajo dentro de un proyecto y recuperarla después de recargar la web.

## Decisiones propuestas

Una tarea pertenece a un proyecto propio. Pedir título y permitir un criterio de finalización y una estimación en minutos, ambos opcionales. El título expresa el resultado; el criterio permite decidir cuándo se ha conseguido. La estimación nunca cuenta como trabajo realizado. Crear no activa el proyecto, programa bloques ni inicia sesiones.

Propuesta de límites: título de 1 a 160 puntos de código tras recortar espacios; criterio de finalización hasta 2000 puntos de código conservando el texto; estimación null o entero de 1 a 1440 minutos. Los límites evitan datos accidentales sin imponer una duración recomendada. UI con minutos y ayuda breve; sin selector de fechas hasta el contrato de planificación.

Identidad UUID generada en servidor, propietario derivado del Principal, referencia al proyecto, estado inicial pendiente e instantes de creación/actualización. Completar y reabrir corresponden a feature 9; subtareas a feature 8. No añadir campos de progreso, prioridad, etiquetas o fechas que todavía no tengan comportamiento acordado.

Persistir tarea y TaskCreated.v1 atómicamente mediante el patrón outbox existente. Reutilizar publicación, confirmaciones y recuperación para una ruta cerrada nueva. Mantener privacidad de criterio de finalización en el evento; fijar el esquema mínimo exacto al destilar el contrato. Fallos de almacenamiento no producen una tarea parcial ni un falso éxito. Sin reintento automático de creación cuando la red deje un resultado incierto.

El roadmap no contiene una feature independiente para consultar tareas. Este corte debe incluir la lectura necesaria para ver las tareas guardadas dentro del proyecto tras recargar, con límite y paginación explícitos, sin implementar filtros o vistas avanzadas. La creación aislada con una confirmación que desaparece al recargar no cubre el objetivo del usuario.

Antes del contrato hay que decidir: política de creación dentro de un proyecto terminado; relación entre terminar un proyecto y sus tareas pendientes; campos públicos y evento exactos; orden y tamaño de página; rutas; tratamiento de concurrencia con un cambio de estado del proyecto. Se propone exigir reapertura en pausa para añadir trabajo a un proyecto terminado, sin completar tareas automáticamente ni modificar los proyectos al consultar.

Revisión del esquema existente: V1 restringe outbox_events.aggregate_id mediante una clave externa a projects.id. Antes de añadir TaskCreated hay que fijar el límite de agregado: un evento del agregado proyecto con taskId separado, o una tarea como agregado propio y la migración correspondiente de esa restricción. No introducir un UUID de tarea en la outbox actual ni retirar la clave externa sin declarar y probar esa decisión. La creación y su evento conservarán una única transacción en ambos diseños.

## Verificación prevista

Propiedad y privacidad; límites Unicode/estimación y JSON estricto; lectura persistente; atomicidad de tarea/evento; carreras pertinentes con el estado del proyecto; publicación real del nuevo tipo; CSRF y sesión ya establecidos por feature 6. Formulario y lista con SCSS existente, teclado/foco, errores conservando borrador, matriz de treinta principios y pruebas responsive/zoom. No declarar evidencia antes de ejecutar ni iniciar este corte durante autenticación.

## Revisión backend del agregado y la concurrencia

Ponytail full y Caveman lite activos. Revisión documental del esquema V1, PostgresProjectStatusEditing, OutboxMessage y RabbitBrokerPublisher; sin producción ni pruebas nuevas.

Se recomienda mantener el proyecto como límite de consistencia en este corte y tratar la tarea como entidad hija con UUID propio. TaskCreated.v1 conservaría aggregateId igual al UUID del proyecto y añadiría taskId. Así se mantiene la clave externa actual de la outbox y no se introduce todavía una infraestructura de agregados polimórficos. Una tabla tasks con referencia al proyecto permite consultar las tareas sin cargar una colección completa dentro del objeto Project. El dominio de tarea puede ser puro y validarse por separado; la aplicación coordina mediante un puerto de persistencia transaccional.

El evento mínimo propuesto tiene ocho campos exactos: eventId, aggregateId, ownerId, occurredAt, schemaVersion, type y taskId, más title. El título normalizado admite el límite específico de tarea (160 puntos de código), sin reutilizar accidentalmente el límite 120 del nombre de proyecto. El criterio de finalización y la estimación permanecen fuera del evento. La validación del publicador debe añadir una rama cerrada para este esquema; tipos o versiones desconocidos conservan UNSUPPORTED_EVENT y un payload incompatible conserva INVALID_EVENT. La nueva ruta sería task.created.v1 con cola organization.task-created.v1, sobre el exchange existente. El switch actual construye siempre rutas project.*, por lo que debe incorporar esa ruta explícita sin alterar las tres existentes.

Para la carrera con terminar un proyecto, la creación debe leer el proyecto propio con SELECT FOR UPDATE dentro de la misma transacción que inserta tarea y evento. El cambio de estado ya bloquea esa misma fila. Con READ_COMMITTED, si terminar confirma primero, crear observa completed y se rechaza sin escrituras; si crear confirma primero, terminar puede completarse después y la tarea permanece pendiente. Esta última consecuencia debe quedar explícita: completar el proyecto no completa sus tareas ni exige que no existan pendientes en este corte. Cambiar esa regla sería una decisión de producto adicional.

La creación no necesita el bloqueo asesor global de capacidad porque no cambia el estado ni consume plazas activas. No debe adquirirlo después de bloquear la fila: el cambio de estado sigue el orden asesor y después fila, y el orden inverso podría provocar un interbloqueo. Mantener creación con una sola fila bloqueada evita ampliar la contención global. No mantener ese bloqueo durante publicación RabbitMQ; sólo durante la transacción local breve.

Se recomienda que crear una tarea no cambie el ETag de los siete campos actuales del proyecto: la representación del detalle no contiene tareas y su versión protege edición y estado. La lista de tareas es un recurso separado. La comprobación del estado del proyecto se hace bajo el bloqueo transaccional, no con una versión leída previamente. Si el futuro contrato incorpora tareas dentro de la representación del proyecto, deberá revisar esta semántica de versión de forma explícita.

La atomicidad requiere comprobar exactamente una fila tanto en INSERT tasks como en INSERT outbox; un trigger que omita cualquiera no debe producir 201. La publicación mantiene entrega al menos una vez y deduplicación futura por eventId. Compartir aggregateId no promete orden de entrega entre TaskCreated y ProjectStatusChanged: el publicador existente puede procesar filas concurrentes y reintentos. Este corte no incorpora consumidores ni garantías nuevas de orden.

## Decisiones de producto para destilar el contrato

El coordinador acepta la recomendación de entidad hija del proyecto para este corte. La API de proyecto conserva su representación y ETag; las tareas se consultan por un recurso separado. No se elimina la clave externa de outbox. Terminar un proyecto conserva sus tareas pendientes; añadir más requiere reabrirlo explícitamente en pausa. La interfaz debe explicar esa consecuencia al crear, sin completar tareas o programar tiempo de forma implícita.

Propuesta de recurso: POST y GET /api/v1/projects/{projectId}/tasks, y GET /api/v1/projects/{projectId}/tasks/{taskId} para la Location de una creación confirmada. La representación de tarea tendrá ocho campos: id, projectId, title, completionCriterion, estimatedMinutes, status, createdAt y updatedAt. Criterio omitido se normaliza a cadena vacía; estimación omitida a null. El estado inicial será pending. Nunca se aceptan id, propietario, estado o fechas suministrados por el cliente.

La lista devolverá items y nextCursor; veinte tareas por página, orden descendente estable por createdAt e id y cursor estricto vinculado al proyecto. Un proyecto propio sin tareas devuelve lista vacía. Un proyecto inexistente o ajeno y una tarea ajena o de otro proyecto devuelven el mismo 404 público; no se filtran por una lista de tareas ya cargada. Toda lectura privada lleva no-store. La consulta de tareas no modifica el proyecto, su capacidad ni el outbox.

En la web, el detalle del proyecto incorporará una sección Tareas con formulario y lista propios. Sólo título obligatorio; criterio y estimación opcionales con ayuda breve. Mostrar la estimación como planificación, nunca como tiempo realizado. La creación confirmada presenta la tarea guardada; ante error o resultado incierto conserva el borrador y ofrece una acción deliberada, sin reenvío automático. El detalle y las acciones actuales del proyecto siguen disponibles aunque falle la lista de tareas. Completar, subtareas, etiquetas y calendario mantienen sus contratos posteriores.

Estas decisiones siguen en preparación documental mientras authentication termina su validación. Antes de implementar se destilarán límites, estados HTTP, cursor, atomicidad, concurrencia, publicación y UX en escenarios independientes.
