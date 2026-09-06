# API de creación de proyectos

Java 25, Spring Boot 3.5.11, Gradle Kotlin DSL 9.3.1. `./gradlew` en Linux/macOS y `gradlew.bat` en Windows.

| Comando | Resultado |
| --- | --- |
| `./gradlew test` | Dominio, aplicación, ArchUnit y contrato HTTP con PostgreSQL real de Testcontainers (Docker necesario) |
| `./gradlew spotlessCheck` | Formato Java |
| `./gradlew pitest` | Mutación de dominio, aplicación y adaptadores propios de sesión, umbral 80 % |
| `./gradlew bootJar` | JAR ejecutable en `build/libs/organization-api-0.1.0.jar` |
| `./gradlew bootRun` | API en puerto 8080 con variables de entorno configuradas |

## Configuración

| Variable | Valor esperado |
| --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL, p. ej. `jdbc:postgresql://postgres:5432/organization` |
| `DB_USERNAME` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |
| `APP_AUTH_USERNAME` | Usuario bootstrap; se convierte en propietario autenticado |
| `APP_AUTH_PASSWORD` | Contraseña bootstrap; sin valor predeterminado, vacío/espacios rechazados |
| `APP_PUBLIC_ORIGIN` | Origen exacto de la web, p. ej. `https://organizacion.example.com` (sin ruta ni barra final) |

Las solicitudes con `Origin` requieren coincidencia exacta con `APP_PUBLIC_ORIGIN`. Se admite HTTPS y HTTP loopback para desarrollo local. No se activa CORS ni HTTP Basic. El proxy debe proporcionar HTTPS en el servidor.

`GET /api/session` es público y devuelve `authenticated`, `username`, `csrfToken` y `csrfHeaderName`, con `Cache-Control: no-store`. `POST /api/session` recibe un formulario con `username` y `password`, más el token en `X-CSRF-TOKEN`; responde 204 y rota la sesión. Se consulta un token nuevo después del login. `POST /api/session/logout` requiere ese token y elimina la sesión.

Spring Session JDBC guarda la sesión durante 30 minutos de inactividad mediante V6. La cookie SESSION usa Path `/api`, HttpOnly y SameSite=Lax; Secure depende del origen HTTPS configurado. Un fallo al guardar o eliminar la sesión devuelve 503 `SESSION_UNAVAILABLE`. El logout no confirmado conserva la cookie anterior para permitir un reintento.

## Contrato

`POST /api/v1/projects` admite `name` y `description`. El nombre recorta únicamente Unicode White_Space exterior y valida 1–120 puntos de código; la descripción conserva el texto y admite hasta 4000. Ausencia/null de descripción se convierte en `""`. Se rechazan otros tipos y campos desconocidos. El nombre puede repetirse.

201 incluye `Location: /api/v1/projects/{id}` y `id`, `ownerId`, `name`, `description`, `status: idea`, `createdAt`, `updatedAt`. Los instantes UTC se truncan a microsegundos antes de crear proyecto/evento para conservar exactamente la precisión de PostgreSQL. La lectura del recurso y su colección se describe a continuación.

Los errores usan `application/problem+json` con `type`, `title`, `status`, `code`; validación añade `errors` (`field`, `code`, `message`) y error interno añade `correlationId`. Mensajes en español y códigos estables: `VALIDATION_ERROR` (400), `MALFORMED_JSON` (400), `UNAUTHENTICATED` (401), `UNTRUSTED_ORIGIN` (403), `UNSUPPORTED_MEDIA_TYPE` (415), `STORAGE_UNAVAILABLE` (503), `INTERNAL_ERROR` (500).

## Fronteras y persistencia

`adapter.http → application.CreateProjectUseCase ← application.CreateProject → application.ProjectCommit ← adapter.persistence.PostgresProjectCommit`. Dominio y aplicación no dependen de frameworks. Spring configura reloj, puertos y adaptadores; ArchUnit verifica las fronteras.

Flyway aplica `V1__projects_and_outbox.sql` y la migración aditiva `V2__outbox_publication.sql`. Una única transacción JDBC inserta `projects` y `outbox_events`; 201 sale después del commit. `ProjectCreated.v1` contiene identidad, propietario, instante y nombre normalizado, nunca descripción. Se almacena como `pending`. El publicador permanece deshabilitado por defecto y puede habilitarse con la configuración siguiente.

Los tests de atomicidad provocan fallos PostgreSQL reales mediante triggers temporales en ambas tablas y verifican cero escrituras. No sustituyen PostgreSQL por H2. La integración de la raíz verifica también recarga de página y reinicio del backend conservando los mismos registros.

PIT desactiva `FRECORD`, porque el filtro predeterminado excluye también la validación escrita en el constructor compacto del record. Solo se excluyen `equals`, `hashCode` y `toString` generados por el compilador. El informe predeterminado incluye dominio, aplicación y los cuatro adaptadores propios de sesión. `./gradlew -PmutationScope=authentication pitest` mide estos últimos en `build/reports/pitest-authentication`. La integración de Spring se comprueba mediante HTTP real, PostgreSQL y E2E.

## Publicador outbox

`OUTBOX_PUBLISHER_ENABLED=true` activa ciclos separados por un segundo, máximo20 registros distintos por ciclo. Variables de conexión: `RABBITMQ_HOST`, `RABBITMQ_PORT` (5672), `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` (/). Host, usuario y secreto no tienen credenciales predeterminadas; configuración incompleta/puerto inválido registra CONFIGURATION_ERROR y deja inactivo el worker manteniendo disponible la API.

`PublisherSchedule → PublishOutboxUseCase ← PublishOutbox` coordina los puertos `OutboxWork`, `BrokerPublisher` y `PublicationAudit`. Cada reclamación JDBC utiliza su propia transacción con FOR UPDATE SKIP LOCKED. Solo después de la confirmación Rabbit y del commit PostgreSQL se registra published. El mensaje JSON original conserva su identidad; la entrega es al menos una vez y los futuros consumidores deberán deduplicar por eventId.

Rabbit prepara exchange durable direct `organization.events`, cola durable quorum `organization.project-created.v1` y binding `project.created.v1`. Mensajes persistentes, content-type application/json, message-id igual al eventId, mandatory y confirmaciones del publicador. Una devolución prevalece sobre ACK. Una topología incompatible se conserva y registra TOPOLOGY_MISMATCH; no se borra ni sustituye.

Los fallos conservan pending y aplican intervalos1,2,4,8,16,32,60 segundos con tope60, calculados desde finalización. Eventos incompatibles quedan blocked sin incrementar intentos; ninguna fila se elimina. La API no espera a Rabbit. Transporte y cleanup tienen plazos finitos y cada intento usa conexión/canal nuevos; recuperación automática del cliente desactivada. Los logs del publicador solo contienen eventId/outcome/attempt/code, y worker_error con código estable.

## Consultar proyectos propios

`GET /api/v1/projects` devuelve `{items, nextCursor}` con un máximo de 20 resúmenes propios. Cada resumen contiene id, name, status, createdAt y updatedAt; orden createdAt DESC e id DESC. La continuación acepta `?cursor=...` opaco, sin parámetros adicionales. El cursor señala el último elemento visible y la consulta selecciona posiciones estrictamente más antiguas, siempre filtradas por el propietario autenticado. La migración aditiva V3 incorpora el índice por propietario, fecha e id.

`GET /api/v1/projects/{id}` devuelve la representación completa original. Un recurso ajeno o inexistente responde 404 PROJECT_NOT_FOUND con el mismo mensaje. Parámetros/UUID mal formados responden 400 VALIDATION_ERROR; fallos de almacenamiento, 503; errores inesperados, 500 con referencia segura. Todas las lecturas requieren la autenticación existente y llevan Cache-Control no-store, también en error. Las consultas no escriben proyectos/outbox ni publican eventos.

La frontera es `ProjectReadController → ReadProjectsUseCase ← ReadProjects → ProjectQueries ← PostgresProjectQueries`. Páginas inmutables y lógica de paginación permanecen en dominio/aplicación; JSON/cursor/HTTP y SQL permanecen en adaptadores.

## Editar proyectos propios

`GET /api/v1/projects/{id}` añade un ETag fuerte opaco, obtenido de la misma fila que el cuerpo. `PUT /api/v1/projects/{id}` exige ese valor exacto en `If-Match` y un objeto JSON con `name` y `description` de tipo string. La respuesta conserva los siete campos públicos, sin exponer una propiedad de versión. La migración V4 añade la versión interna, inicialmente cero.

`ProjectEditController → EditProjectUseCase ← EditProject → ProjectEditing ← PostgresProjectEditing` mantiene HTTP y SQL fuera del núcleo. La transacción bloquea la fila propia antes de comparar la revisión. Un cambio equivalente devuelve el snapshot original; una revisión antigua devuelve 412 PROJECT_CONFLICT, incluso si el cuerpo coincide. Una modificación actualiza la fila y añade exactamente un ProjectUpdated.v1; ambas escrituras deben afectar una fila y cualquier fallo revierte la transacción.

If-Match ausente devuelve 428 PRECONDITION_REQUIRED; formato inválido, campos inválidos o JSON ambiguo devuelven 400 VALIDATION_ERROR. La edición conserva la autenticación, protección Origin y respuestas privadas no-store. ProjectUpdated.v1 usa la misma estructura de siete campos que ProjectCreated.v1, sin descripción. El publicador selecciona de forma cerrada la cola quorum durable `organization.project-updated.v1` y la ruta `project.updated.v1`, conservando el exchange y las garantías de confirmación existentes.

## Estados y capacidad activa

`PUT /api/v1/projects/{id}/status` recibe exclusivamente `{status}` y el ETag vigente en `If-Match`. Los estados son idea, active, paused y completed. Idea permite activar o terminar; Activo permite pausar o terminar; Pausado permite retomar o terminar; Terminado permite reabrir en pausa. Un estado equivalente conserva representación, fechas, versión y outbox. Edición de texto y cambios de estado comparten la misma versión, de modo que una revisión antigua produce 412 PROJECT_CONFLICT.

`APP_MAX_ACTIVE_PROJECTS` configura el máximo de proyectos activos de cada propietario: entero entre 1 y 10, valor predeterminado 3. Una configuración inválida impide arrancar la API. Todas las réplicas deben recibir el mismo valor. Reducir el límite no pausa proyectos automáticamente. Si no hay plaza, la activación devuelve 409 ACTIVE_PROJECT_LIMIT con activeCount y limit propios; una transición prohibida devuelve 409 INVALID_PROJECT_TRANSITION.

`ChangeProjectStatusUseCase` coordina el puerto `ProjectStatusEditing`. El adaptador PostgreSQL adquiere primero `pg_advisory_xact_lock(749189081)` y luego la fila propia; dentro de esa transacción cuenta los activos propios y persiste estado, versión y evento. El mecanismo requiere el aislamiento READ_COMMITTED predeterminado del despliegue: el conteo posterior al bloqueo observa el commit del anterior titular. La clave es común para cambios de estado y no debe reutilizarse para otra función. Simplificación Ponytail: se serializan brevemente cambios de todos los propietarios; separar por propietario solo si la contención medida lo justifica. No se espera al broker ni se bloquean lecturas.

V5 amplía de forma aditiva la restricción de estados, conservando las filas anteriores. ProjectStatusChanged.v1 contiene exactamente eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, fromStatus y toStatus. No transporta nombre ni descripción. El publicador valida la transición con la misma tabla del dominio y utiliza la ruta `project.status-changed.v1`, cola quorum durable `organization.project-status-changed.v1`, exchange `organization.events`. Created y Updated conservan sus rutas y garantías.

## Tareas del proyecto

`POST /api/v1/projects/{projectId}/tasks` crea una tarea hija y TaskCreated.v1 en una transacción. Admite title, completionCriterion y estimatedMinutes; título obligatorio de 1–160 puntos de código, criterio opcional de hasta 2000 y estimación null o entero de 1–1440. Sólo se recortan espacios Unicode exteriores del título. La estimación representa planificación, no tiempo realizado.

La respuesta 201 contiene ocho campos: id, projectId, title, completionCriterion, estimatedMinutes, status, createdAt y updatedAt. Estado inicial pending, criterio omitido/null convertido en cadena vacía y estimación omitida convertida en null. Location permite recuperar el mismo detalle mediante GET. La lista GET devuelve items y nextCursor, veinte tareas por página, orden descendente por createdAt/id y cursor opaco vinculado al proyecto. Las consultas no modifican el proyecto ni su ETag.

El proyecto es el límite de consistencia: aggregateId del evento sigue siendo el UUID del proyecto; taskId identifica la entidad hija. V7 conserva la clave externa de la outbox y añade tasks con índice de paginación. La creación bloquea la fila del proyecto bajo READ_COMMITTED, compartida con cambios de estado. Si completar confirma primero, crear devuelve 409 PROJECT_COMPLETED. Si crear confirma primero, completar puede conservar esa tarea pending. No se añade bloqueo asesor de capacidad.

Las rutas de tareas usan 404 RESOURCE_NOT_FOUND uniforme para recursos inexistentes, ajenos o bajo otro proyecto. Sesión, CSRF, origen y no-store conservan la política general. Ambas inserciones deben confirmar una fila; cualquier fallo revierte tarea y evento. El broker no interviene en la confirmación HTTP.

TaskCreated.v1 contiene eventId, aggregateId, ownerId, occurredAt, schemaVersion, type, taskId y title. No incluye criterio ni estimación. El publicador usa task.created.v1 y organization.task-created.v1 sobre el exchange existente. Mantiene confirmaciones, entrega obligatoria, reintentos y entrega al menos una vez; no promete orden entre eventos distintos del proyecto.

`./gradlew -PmutationScope=create_task pitest` selecciona la lógica de tareas, validación HTTP/cursor, adaptadores PostgreSQL y extensión del publicador. Su informe separado es `build/reports/pitest-create-task`; el alcance predeterminado también conserva esos adaptadores para la CI. El resultado observado se documenta en el informe de mutación de la feature.
