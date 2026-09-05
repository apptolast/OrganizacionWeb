# API de creación de proyectos

Java 25, Spring Boot 3.5.11, Gradle Kotlin DSL 9.3.1. `./gradlew` en Linux/macOS y `gradlew.bat` en Windows.

| Comando | Resultado |
| --- | --- |
| `./gradlew test` | Dominio, aplicación, ArchUnit y contrato HTTP con PostgreSQL real de Testcontainers (Docker necesario) |
| `./gradlew spotlessCheck` | Formato Java |
| `./gradlew pitest` | Mutación de dominio/aplicación, umbral 80 % |
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

Las solicitudes de navegador con `Origin` requieren coincidencia exacta con `APP_PUBLIC_ORIGIN`; sin configurar se rechazan. Los clientes API sin `Origin` siguen necesitando HTTP Basic válido. No se activa CORS. El adaptador solo acepta JSON, evitando escrituras mediante formularios de otro origen. En servidor, el proxy debe proporcionar HTTPS y no publicar directamente el puerto interno del backend.

`GET /api/session` devuelve 204 con credenciales verificadas y 401 con desafío HTTP Basic en otro caso. Sirve para que el proxy solicite autenticación antes de mostrar la web; no representa una sesión persistente ni una funcionalidad de inicio/cierre de sesión completa.

## Contrato

`POST /api/v1/projects` admite `name` y `description`. El nombre recorta únicamente Unicode White_Space exterior y valida 1–120 puntos de código; la descripción conserva el texto y admite hasta 4000. Ausencia/null de descripción se convierte en `""`. Se rechazan otros tipos y campos desconocidos. El nombre puede repetirse.

201 incluye `Location: /api/v1/projects/{id}` y `id`, `ownerId`, `name`, `description`, `status: idea`, `createdAt`, `updatedAt`. Los instantes UTC se truncan a microsegundos antes de crear proyecto/evento para conservar exactamente la precisión de PostgreSQL. No hay GET de ese recurso en este corte.

Los errores usan `application/problem+json` con `type`, `title`, `status`, `code`; validación añade `errors` (`field`, `code`, `message`) y error interno añade `correlationId`. Mensajes en español y códigos estables: `VALIDATION_ERROR` (400), `MALFORMED_JSON` (400), `UNAUTHENTICATED` (401), `UNTRUSTED_ORIGIN` (403), `UNSUPPORTED_MEDIA_TYPE` (415), `STORAGE_UNAVAILABLE` (503), `INTERNAL_ERROR` (500).

## Fronteras y persistencia

`adapter.http → application.CreateProjectUseCase ← application.CreateProject → application.ProjectCommit ← adapter.persistence.PostgresProjectCommit`. Dominio y aplicación no dependen de frameworks. Spring configura reloj, puertos y adaptadores; ArchUnit verifica las fronteras.

Flyway aplica `V1__projects_and_outbox.sql` y la migración aditiva `V2__outbox_publication.sql`. Una única transacción JDBC inserta `projects` y `outbox_events`; 201 sale después del commit. `ProjectCreated.v1` contiene identidad, propietario, instante y nombre normalizado, nunca descripción. Se almacena como `pending`. El publicador permanece deshabilitado por defecto y puede habilitarse con la configuración siguiente.

Los tests de atomicidad provocan fallos PostgreSQL reales mediante triggers temporales en ambas tablas y verifican cero escrituras. No sustituyen PostgreSQL por H2. La integración de la raíz verifica también recarga de página y reinicio del backend conservando los mismos registros.

PIT desactiva `FRECORD`, porque el filtro predeterminado excluye también la validación escrita en el constructor compacto del record. Solo se excluyen `equals`, `hashCode` y `toString` generados por el compilador. El informe deja explícito el alcance dominio/aplicación; los adaptadores se validan mediante integración HTTP/PostgreSQL y E2E.

## Publicador outbox

`OUTBOX_PUBLISHER_ENABLED=true` activa ciclos separados por un segundo, máximo20 registros distintos por ciclo. Variables de conexión: `RABBITMQ_HOST`, `RABBITMQ_PORT` (5672), `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` (/). Host, usuario y secreto no tienen credenciales predeterminadas; configuración incompleta/puerto inválido registra CONFIGURATION_ERROR y deja inactivo el worker manteniendo disponible la API.

`PublisherSchedule → PublishOutboxUseCase ← PublishOutbox` coordina los puertos `OutboxWork`, `BrokerPublisher` y `PublicationAudit`. Cada reclamación JDBC utiliza su propia transacción con FOR UPDATE SKIP LOCKED. Solo después de la confirmación Rabbit y del commit PostgreSQL se registra published. El mensaje JSON original conserva su identidad; la entrega es al menos una vez y los futuros consumidores deberán deduplicar por eventId.

Rabbit prepara exchange durable direct `organization.events`, cola durable quorum `organization.project-created.v1` y binding `project.created.v1`. Mensajes persistentes, content-type application/json, message-id igual al eventId, mandatory y confirmaciones del publicador. Una devolución prevalece sobre ACK. Una topología incompatible se conserva y registra TOPOLOGY_MISMATCH; no se borra ni sustituye.

Los fallos conservan pending y aplican intervalos1,2,4,8,16,32,60 segundos con tope60, calculados desde finalización. Eventos incompatibles quedan blocked sin incrementar intentos; ninguna fila se elimina. La API no espera a Rabbit. Transporte y cleanup tienen plazos finitos y cada intento usa conexión/canal nuevos; recuperación automática del cliente desactivada. Los logs del publicador solo contienen eventId/outcome/attempt/code, y worker_error con código estable.
