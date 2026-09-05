# Propuesta acotada — project_states (feature 5)

Sólo diseño para decisión del coordinador; no inicia esta feature ni modifica producción o metadata. Parte de los cuatro estados del spec y reutiliza la versión interna/ETag de edit_project. No incorpora tareas, borrado, estados arbitrarios, automatizaciones ni personalización general.

## Estados y operación

API: `idea`, `active`, `paused`, `completed`; etiquetas: Idea, Activo, Pausado, Terminado. Crear sigue produciendo Idea. Propuesta: `PUT /api/v1/projects/{id}/status` con JSON exacto `{status}` y `If-Match` obligatorio. Devuelve 200, los siete campos del detalle y el ETag vigente. Cambio real incrementa la misma versión de proyecto usada por edición y actualiza updatedAt; preserva nombre, descripción, id, propietario y createdAt.

| Origen | Destinos permitidos | Acciones de interfaz |
| --- | --- | --- |
| Idea | Activo, Terminado | Activar; Marcar terminado |
| Activo | Pausado, Terminado | Pausar; Marcar terminado |
| Pausado | Activo, Terminado | Retomar; Marcar terminado |
| Terminado | Pausado | Reabrir en pausa |

Marcar una idea como terminada permite registrar trabajo realizado fuera de la aplicación sin ocupar artificialmente una plaza activa. Reabrir deja el proyecto pausado: no inicia trabajo ni ocupa capacidad por sorpresa; Retomar es deliberado. No hay retorno a Idea después de empezar. No se infieren tareas terminadas, sesiones ni tiempo trabajado a partir del estado.

Destino igual al actual con ETag vigente es no-op: 200 sin cambio de versión/updatedAt/evento. Toda otra transición entre valores válidos produce 409 INVALID_PROJECT_TRANSITION. Valor desconocido, null o JSON inválido se rechaza siguiendo las convenciones de validación/JSON del API. Autenticación 401, privacidad 404 uniforme, ausencia de precondición 428 y ETag obsoleto 412 se conservan. Una edición de texto concurrente y un cambio de estado compiten sobre la misma versión, evitando que cualquiera sobrescriba una representación obsoleta.

## Límite activo inicial

Propuesta MVP: máximo 3 proyectos activos por propietario, configurable en despliegue mediante `APP_MAX_ACTIVE_PROJECTS`, rango entero 1–10; ausencia usa 3 y configuración inválida impide arrancar la API. Tres es una decisión inicial revisable para favorecer foco, no una cifra psicológica demostrada. Todas las réplicas del despliegue deben recibir el mismo valor desde su configuración común. No se añade una pantalla de preferencias ni endpoint de configuración en este corte; preferencia individual queda para la feature correspondiente.

El límite sólo se comprueba al entrar en Activo. Si se reduce la configuración por debajo de la cantidad actual, no se pausa ni termina nada automáticamente: se bloquean nuevas activaciones hasta disponer de una plaza. Una activación rechazada devuelve 409 ACTIVE_PROJECT_LIMIT con `activeCount` y `limit` del propio usuario, sin nombres ajenos. Pausar y terminar siempre permiten liberar capacidad con ETag vigente. UI explica el bloqueo y ofrece volver a proyectos para elegir qué pausar; nunca pausa otro proyecto por el usuario.

## Exclusión concurrente en PostgreSQL

Usar un único `pg_advisory_xact_lock` con clave reservada para cambios de estado, sin añadir una tabla de bloqueos. Dentro de la misma transacción: adquirir ese bloqueo, cargar el proyecto por propietario/id, verificar ETag y transición, contar los activos propios y aplicar el cambio condicionado por versión junto con el outbox. Todos los cambios de estado adquieren primero el bloqueo asesor y después el del proyecto; edición de texto sólo bloquea proyecto. El bloqueo termina automáticamente con commit o rollback, conforme a la [documentación de PostgreSQL](https://www.postgresql.org/docs/17/explicit-locking.html#ADVISORY-LOCKS). No se usa un contador de memoria ni un COUNT fuera de la transacción.

Dos activaciones simultáneas para la última plaza se serializan: una confirma y la otra observa el nuevo conteo y recibe 409, sin superar el límite. No se promete qué petición gana. El no-op no altera proyecto ni outbox; conflicto, privacidad y fallos revierten la transacción. Ningún evento se produce para un cambio rechazado.

Simplificación Ponytail: este bloqueo serializa brevemente los cambios de estado de todos los propietarios. Es suficiente para la web personal; no bloquea lecturas ni espera al broker. Pasar a bloqueos por propietario sólo si la contención medida lo justifica. Mantener este límite explícito en un comentario `ponytail:` al implementar.

## Evento y publicación

Cambio y outbox `ProjectStatusChanged.v1` se confirman atómicamente. Payload exacto de ocho campos: eventId nuevo, aggregateId, ownerId, occurredAt igual a updatedAt, schemaVersion 1, type, fromStatus y toStatus. No transporta nombre ni descripción. El publicador valida el esquema según tipo, conserva JSON original y añade únicamente la ruta cerrada `project.status-changed.v1` → cola quorum durable `organization.project-status-changed.v1` en exchange `organization.events`.

Mantiene compatibilidad de ProjectCreated.v1 y ProjectUpdated.v1, mandatory/confirms, reintentos y entrega al menos una vez; no añade consumidor ni promete orden global. Broker caído no impide confirmar el cambio local. Un fallo de UPDATE o INSERT outbox revierte estado, versión y fechas.

## Comprobaciones mínimas siguientes

Tabla completa de transiciones y no-op; límites inicial/configuración inválida/reducción sin pausa automática; dos activaciones reales concurrentes sobre la última plaza y liberación por pausa; aislamiento entre propietarios; ETag compartido con edición; rollback proyecto/outbox; publicación real del nuevo esquema y regresión de rutas anteriores. En lista/detalle, sustituir las etiquetas Idea actualmente fijas y ampliar validadores DTO a los cuatro valores. Controles por transición con estado pendiente, error recuperable y resultado confirmado, sin métricas de productividad inventadas. Aplicar teclado, táctil y matriz UX al nuevo control sin repetir pruebas ajenas que no cambien.

