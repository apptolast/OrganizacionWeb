# Propuesta acotada — edit_project (feature 4)

Documento para decisión del coordinador; no implementa ni modifica el contrato activo read_projects. El roadmap aprobado incluye nombre, descripción y concurrencia.

## Operación y límites

`PUT /api/v1/projects/{id}` sustituye únicamente los dos campos editables mediante JSON exacto `{name, description}`. Ambos campos se envían siempre; description acepta string vacío, no null. Reutiliza las reglas de creación: nombre obligatorio normalizado con el trim acordado, máximo120 puntos de código; descripción preservada, máximo4000. Rechaza campos desconocidos y JSON ambiguo/malformado. No cambia ownerId, id, createdAt ni status. No añade tareas, estados, borrado, autosave ni edición masiva.

Respuesta200 con los siete campos actuales del detalle y Location opcional innecesario. updatedAt toma reloj servidor en UTC a precisión microsegundo sólo cuando cambia contenido. Un PUT que ya coincide con los valores almacenados devuelve representación actual y no escribe ni emite evento; sigue validando la precondición para no ocultar una edición concurrente.

## Concurrencia recomendada: versión interna y ETag

Añadir columna `version BIGINT NOT NULL DEFAULT 0` al proyecto; no añadirla a los cuerpos públicos de creación/consulta (conservan siete campos). GET detalle y PUT incluyen un ETag fuerte opaco derivado de id+version. Formulario obtiene ese ETag al abrir y lo envía en `If-Match`.

PUT sin If-Match:428 PRECONDITION_REQUIRED; mal formado, repetido o comodín no admitido:400 VALIDATION_ERROR; versión antigua de un proyecto propio:412 PROJECT_CONFLICT. Update atómico condicionado por id, owner y version; incrementa version junto con el cambio y el evento. No usar sólo updatedAt como versión: dos cambios con el mismo instante podrían perder una edición. La columna y una comparación SQL son una complejidad pequeña y explícita para resolver el riesgo real de dos pestañas; no se propone bloqueo distribuido, historial de versiones ni merge automático.

Privacidad antes de resolver conflictos: propietario inexistente/ajeno siempre404 PROJECT_NOT_FOUND con el mismo mensaje público, incluso si aporta un ETag válido de otro propietario. Sin credenciales401; id inválido400; almacenamiento503 e interno500 con correlationId y sin SQL/stacktrace/secretos. Respuestas privadas no-store. Mantener guard de Origin y JSON del API existente.

## Evento y efecto obligatorio sobre el publicador

Una única transacción escribe el proyecto editado y un nuevo outbox `ProjectUpdated.v1`. Payload mínimo de siete campos: eventId nuevo, aggregateId existente, ownerId autenticado, occurredAt=updatedAt confirmado, schemaVersion1, name normalizado y type ProjectUpdated.v1. No transportar descripción. Un fallo de cualquiera de ambas escrituras revierte todo. Broker caído no impide200 ni pierde el evento.

El publicador actual sólo admite ProjectCreated.v1 y usa ruta fija. Por tanto edit_project debe ampliar explícitamente su allowlist a Created y Updated con validación equivalente y routing cerrado:

| Tipo | Exchange | Routing key | Cola durable quorum |
| --- | --- | --- | --- |
| ProjectCreated.v1 | organization.events | project.created.v1 | organization.project-created.v1 |
| ProjectUpdated.v1 | organization.events | project.updated.v1 | organization.project-updated.v1 |

Conservar JSON original, mandatory, confirms, cinco códigos de fallo, política de reintento y entrega al menos una vez. No derivar rutas de texto arbitrario; otros tipos permanecen UNSUPPORTED_EVENT. INVALID_EVENT sigue reservado al payload incompatible, conservando el contrato de publish_outbox. No crear consumer de producto. Probar regresión de Created y entrega real de Updated con su metadata, así como rollback del update/outbox; no repetir toda la matriz de crash si la frontera transaccional/confirm no cambia. No confundir orden de llegada al broker con historial ordenado de versiones: este corte no expone consumidores ni promete orden global.

## Formulario y recuperación

Ruta `/proyectos/{id}/editar`, enlace Editar proyecto en detalle. Precarga real del detalle+ETag, campos equivalentes a creación, botón Guardar cambios, Cancelar vuelve al detalle. Estado Guardando cambios accesible, doble submit bloqueado. Tras200 mostrar Proyecto actualizado y representación confirmada; mantener edición recuperable en errores sin persistir contenido en localStorage.

Ante412 mantener el borrador, explicar que existe una edición más reciente y ofrecer Recargar versión guardada como acción deliberada que sustituye el borrador; no sobrescribir automáticamente ni reintentar a ciegas. Error401 elimina datos anteriores según la política privada acordada. Error404 ofrece volver a proyectos. Sin cambios locales, Guardar cambios puede quedar desactivado con explicación accesible; el servidor sigue resolviendo PUT equivalente sin evento.

## Núcleo del contrato siguiente

Éxito propio y preservación de campos inmutables; validaciones compartidas; Unicode/HTML literal; noop sin evento; autenticación y no-store; privacidad404; dos pestañas: primera200 y segunda412 sin pérdida; precondición ausente/malformada; rollback proyecto+outbox; broker caído200; entrega real Updated y regresión Created; load/error/retry/success del formulario; borrador conservado en400/412/503; foco, teclado/táctil, matriz responsive y zoom aplicables.

Alternativa conscientemente descartada: last-write-wins silencioso. La propuesta no solicita una nueva puerta humana: se encuadra en el roadmap aprobado; el coordinador concreta el contrato antes de producción.
