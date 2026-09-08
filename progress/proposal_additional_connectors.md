# Propuesta normativa — feature 29 `additional_connectors`

Redactada el 8 de septiembre de 2026 por el `spec_partner` bajo la autorización global del 5 de septiembre de 2026, sin humano disponible. Las propuestas de 25–28 no existían al redactar; se asumen las decisiones fijas de 27 comunicadas por el coordinador (PAT cifrado AES-GCM con `APP_CONNECTOR_KEY`, base de API configurable, importación bajo demanda mediante el caso de uso de crear tarea, tabla `task_external_links`, recibo `{created, skipped, failed, truncated}`, 200 issues por ejecución, errores `RATE_LIMITED`, `CONNECTION_INVALID`, `IMPORT_IN_PROGRESS`, `CONNECTORS_DISABLED`). Si 27 fija algo distinto, 27 prevalece y esta sección se ajusta antes de destilar Gherkin. Sección lista para pegar en `project-spec.md`; no edita nada más.

---

## Feature 29: additional_connectors — Catálogo de conectores y segundo gestor de issues (GitLab)

Propuesta pendiente de ratificación humana. No acredita implementación ni inicia TDD antes de revisar Gherkin.

### Propósito y priorización por uso

Entrega dos cosas verificables: un **catálogo** que muestra el estado real de cada conector del producto y un **segundo gestor de issues, GitLab**, construido sobre el mismo puerto que GitHub. El roadmap fija «adaptadores priorizados por uso» y advierte que un botón no constituye un conector funcional; por eso este corte entrega un solo proveedor completo con permisos, secretos, desconexión, mapeo, conflictos, idempotencia, límites y pruebas, en lugar de varios a medias.

GitLab se elige como segundo gestor porque comparte con GitHub el modelo exacto que 27 ya prueba: repositorio con issues abiertas, PAT personal, API REST paginada. Es el proveedor más usado como alternativa autoalojada a GitHub entre desarrolladores, perfil del propietario de esta web, y su instancia privada (`gitlab.example.com`) queda cubierta por la base de API configurable. Jira, Trello y Todoist se descartan en este corte: Jira exige autenticación distinta (correo + API token en Basic o OAuth 2.0 3LO), un modelo de proyectos/JQL sin equivalente directo y cuota por sitio; Trello usa clave + token y tarjetas en listas, no issues; Todoist es un gestor de tareas personal cuyo solape con esta web es la propia web. Ninguno valida el puerto compartido con una segunda implementación isomorfa; GitLab sí. No se atribuyen porcentajes de uso no medidos (matriz UX, fila Pareto): la prioridad se contrastará con uso real y cada proveedor descartado tendrá su feature propia si se pide.

El catálogo es un requisito de operabilidad: con seis integraciones (24–29) el usuario necesita un lugar que diga qué está conectado, cuándo actuó por última vez y qué falló, sin abrir cada pantalla. Sin catálogo, un conector con error silencioso no se distingue de uno sano.

### Modelo y persistencia

Migración reservada **`V27__additional_connectors.sql`**, sin cambios en tablas ajenas:

- `gitlab_connections(owner_id TEXT PRIMARY KEY, api_base TEXT NOT NULL, project_path TEXT NOT NULL, project_id BIGINT NOT NULL, token_ciphertext BYTEA NOT NULL, token_nonce BYTEA NOT NULL, token_hint TEXT NOT NULL, status TEXT NOT NULL CHECK (status IN ('connected','error')), last_activity_at TIMESTAMPTZ, last_error_code TEXT, last_error_at TIMESTAMPTZ, version BIGINT NOT NULL CHECK (version >= 0), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL)`. Una conexión por propietario, simétrica a la de GitHub de 27. `token_hint` son los últimos cuatro caracteres del PAT; nunca se guarda el token en claro ni se registra en logs.
- Se **reutiliza** `task_external_links` de 27 con `source = 'gitlab'`. `external_id = "<host de api_base>:<id global de la issue>"`; el id global es único por instancia y el prefijo de host evita colisiones si el propietario cambia de instancia. `url` es `web_url` de la issue. UNIQUE `(owner_id, source, external_id)` es la única garantía de idempotencia de importación: no se añade otra tabla de deduplicación.
- El guardián `IMPORT_IN_PROGRESS` de 27 se reutiliza tal cual y se aplica **por propietario**, no por proveedor: una importación de GitHub y otra de GitLab no corren a la vez. Si 27 lo definió por proveedor, se conserva esa decisión y se anota aquí.

Mapeo issue → tarea, aplicado por el caso de uso compartido y no por el adaptador: título recortado de espacios exteriores y truncado al límite vigente de nombre de tarea de la feature 7 por puntos de código; descripción = cuerpo de la issue truncado a 4000 puntos de código, o cadena vacía; la tarea nace en el estado inicial de la feature 7 y no hereda etiquetas, asignados, hitos ni fechas. Título vacío tras recorte cuenta como `failed` con motivo `EMPTY_TITLE`. La tarea queda enlazada en la misma transacción que la crea; sin enlace no hay tarea.

### Puerto compartido `IssueSource`

En `application/` se define el puerto de salida `IssueSource` con dos operaciones: `source()` devuelve la clave estable (`github`, `gitlab`) y `openIssues(connection, page)` devuelve `IssuePage(List<ExternalIssue> issues, boolean hasMore)` para páginas de hasta 100 elementos, donde `ExternalIssue(externalId, title, body, url)` es un valor sin dependencias de framework. `connection` transporta base de API, referencia de proyecto y el token ya descifrado, sólo en memoria y sólo durante la ejecución. El adaptador traduce respuestas del proveedor a `ExternalIssue` o a las excepciones de aplicación `RateLimitedException`, `ConnectionInvalidException` y `SourceUnavailableException`; no conoce tareas ni proyectos.

El caso de uso de importación de 27 pasa a ser `ImportIssues(source)`: resuelve el `IssueSource` por clave en un mapa cableado en `ApplicationConfiguration`, lee hasta dos páginas (200 issues), marca `truncated = true` si la segunda página indica más resultados, y por cada issue llama al caso de uso de crear tarea de la feature 7 en su propia transacción junto con el enlace. El adaptador GitHub de 27 pasa a implementar este puerto; ninguna llamada HTTP ocurre dentro de una transacción de base de datos. Es una interfaz con dos implementaciones reales exigida por la arquitectura hexagonal, no una abstracción especulativa.

Adaptador GitLab: `GET {api_base}/projects/{path codificado}` al conectar, para validar token y obtener `id` y `path_with_namespace`; `GET {api_base}/projects/{id}/issues?state=opened&per_page=100&page={n}` al importar, con cabecera `PRIVATE-TOKEN`. `hasMore` se deriva de la cabecera `X-Next-Page` no vacía. Se **excluyen** las issues cuyo `issue_type` sea distinto de `issue` (incidentes, test cases, tasks de GitLab) y las que `moved_to_id` no nulo; ambas cuentan como `skipped`. Las issues confidenciales accesibles con el token se importan como cualquier otra: el propietario ya tiene acceso y la tarea es privada. Respuestas HTTP: 401/403/404 → `ConnectionInvalidException`; 429 o `RateLimit-Remaining: 0` → `RateLimitedException` conservando `Retry-After`; 5xx, tiempo agotado, redirección o cuerpo no JSON → `SourceUnavailableException`. Propiedad `app.gitlab.api-base` con valor por defecto `https://gitlab.com/api/v4`, entorno `APP_GITLAB_API_BASE`; es configuración de servidor, no entrada del usuario.

### API

Todas las rutas son privadas, autenticadas por sesión, sin caché, con CSRF y comprobación de origen existentes; errores en `application/problem+json` con código estable. Los recursos nunca devuelven el token.

- `GET /api/v1/me/connectors` → 200 `{ connectors: [ { id, status, lastActivityAt, lastError } ] }` con exactamente seis filas en orden fijo: `api_credentials` (24), `webhooks` (25), `ics_calendar` (26), `github` (27), `external_calendar` (28), `gitlab` (29). `status` ∈ `connected | not_connected | disabled | error`; `lastActivityAt` es instante UTC o `null`; `lastError` es `null` o `{ code, at }` sin mensaje libre, sin URL ni secreto. Cada feature aporta su estado mediante el puerto `ConnectorStatusProvider(ownerId)`; si una feature aún no está desplegada o su clave de cifrado falta, su fila es `disabled`. El catálogo falla cerrado: un dato no confirmado se muestra `disabled` o `null`, nunca `connected` por defecto. `api_credentials` está `connected` si existe al menos una credencial válida de 24; `lastActivityAt` sólo si 24 registra último uso, si no `null`.
- `GET /api/v1/me/connectors/gitlab` → 200 `{ status, apiBase, projectPath, projectId, tokenHint, lastActivityAt, lastError, version }`; sin conexión devuelve `status: not_connected` y el resto `null`, no 404.
- `PUT /api/v1/me/connectors/gitlab` con `{ token, projectPath }` → valida (`token` string no vacío ≤ 200 caracteres sin espacios; `projectPath` cumple `^[\w.+-]+(/[\w.+-]+){1,4}$`, sin `..`, ≤ 255), consulta el proyecto en GitLab, cifra y hace upsert; 200 con la representación anterior. Campos desconocidos → 400 `VALIDATION_ERROR`. Reemplazar un token válido por otro no borra enlaces ni tareas.
- `DELETE /api/v1/me/connectors/gitlab` → 204 idempotente. Borra la fila de conexión y la clave en memoria; conserva tareas y `task_external_links` como trazabilidad histórica.
- `POST /api/v1/me/connectors/gitlab/imports` con `{ projectId }` (UUID de un proyecto propio destino) → 200 `{ source: "gitlab", projectId, startedAt, finishedAt, created, skipped, failed, truncated }`. Antes de contactar GitLab aplica las precondiciones del proyecto destino de la feature 7 (existencia, propiedad, estado que admite tareas) y devuelve sus mismos errores. Sin cuerpo idempotente: repetir la importación crea sólo las issues nuevas y cuenta las ya enlazadas como `skipped`.

| HTTP | Código | Cuándo |
| --- | --- | --- |
| 400 | VALIDATION_ERROR | Cuerpo inválido, campos desconocidos, `projectPath` o `token` fuera de forma |
| 404 | PROJECT_NOT_FOUND | Proyecto destino inexistente o ajeno (respuesta genérica) |
| 409 | IMPORT_IN_PROGRESS | Otra importación del propietario en curso; también bloquea PUT y DELETE |
| 409 | CONNECTION_MISSING | Importar sin conexión GitLab |
| 422 | CONNECTION_INVALID | GitLab rechaza el token o no encuentra el proyecto; la conexión pasa a `error` |
| 429 | RATE_LIMITED | GitLab limita; se reenvía `Retry-After` si existe |
| 502 | SOURCE_UNAVAILABLE | GitLab no responde, 5xx, redirección o cuerpo inesperado |
| 503 | CONNECTORS_DISABLED | `APP_CONNECTOR_KEY` ausente o inválida |
| 503 | STORAGE_UNAVAILABLE | Fallo de persistencia reconocido |

**PREGUNTA ABIERTA**: si 27 nombra de otro modo el fallo de red del proveedor, 29 adopta ese código en lugar de `SOURCE_UNAVAILABLE`.

### Seguridad

Cifrado AES-256-GCM con la clave `APP_CONNECTOR_KEY` compartida con 27, nonce aleatorio de 12 bytes por escritura y datos autenticados adicionales `owner_id + ":gitlab"`, para que un cifrado no sea reutilizable en otra fila. Sin clave configurada, los conectores 27 y 29 responden `CONNECTORS_DISABLED` y su fila del catálogo es `disabled`; el catálogo sigue disponible. El token viaja sólo en la cabecera `PRIVATE-TOKEN`, nunca en la URL, en logs ni en el problema JSON; los logs registran código de error y correlación. Salida HTTPS obligatoria salvo hosts de loopback, permitidos únicamente para pruebas; sin seguir redirecciones; tiempo de conexión 5 s y lectura 10 s; cuerpo por página limitado a 5 MiB. La base de API no es editable por el usuario, lo que cierra la superficie SSRF; `projectPath` se codifica con `URLEncoder` y se valida antes de componer la URL. Se recomienda en la interfaz un PAT con alcance `read_api`; el servidor no puede verificar el alcance sin un endpoint adicional y no lo afirma.

### Concurrencia y recuperación

Una importación por propietario a la vez mediante el guardián de 27; una entrada `RUNNING` sin finalizar de más de 10 minutos se considera abandonada y no bloquea. Cada issue se confirma en su propia transacción; una caída a mitad deja tareas completas ya enlazadas y ninguna parcial, y la siguiente ejecución las cuenta como `skipped`. Un fallo de GitLab en la página 2 devuelve el recibo con lo creado hasta ese punto y `lastError` actualizado; no hay rollback de las tareas ya confirmadas y el recibo lo dice. Reemplazar o borrar la conexión durante una importación devuelve `IMPORT_IN_PROGRESS`. Respuesta de red incierta en el cliente: no se reintenta automáticamente; la pantalla ofrece «Actualizar estado», que relee `GET .../gitlab` y el catálogo. `version` de la conexión se incrementa en cada PUT y se expone para diagnóstico; el PUT no exige `If-Match` porque hay un único operador por cuenta.

### Interfaz

Ruta `/conectores`, encabezado «Conectores», entrada de navegación después de Importación sin desplazar Hoy. Lista de seis filas con nombre, estado en texto y marcador no dependiente del color, última actividad en la zona del usuario, último error como código traducido y un enlace por fila: `/integraciones` (24), y las rutas propias de 25–28 cuando existan; mientras no existan, la fila muestra «No disponible» sin enlace. Ruta `/conectores/gitlab`: formulario con `token` (`type="password"`, `autocomplete="off"`, nunca precargado), `projectPath` con ayuda «grupo/proyecto», botón «Conectar»; con conexión existente muestra `tokenHint`, proyecto y botones «Importar issues» con selector nativo de proyecto destino, «Actualizar token» y «Desconectar» con confirmación explícita que aclara que las tareas importadas se conservan. El recibo se presenta como cuatro cifras etiquetadas y aviso claro si `truncated`. Estados guardando, guardado y fallo diferenciados; ningún éxito antes de confirmación; controles de al menos 44 px, etiquetas asociadas, foco visible y anuncios `aria-live`. Reutiliza `apiRequest`, SCSS existente y controles nativos; sin dependencias nuevas ni almacenamiento del token en el navegador.

### Límites explícitos

Sin OAuth: el PAT basta para un único operador y OAuth exigiría registrar una aplicación por instancia GitLab. Sin sincronización bidireccional ni cierre de tareas al cerrar issues: la web no escribe en GitLab y no vigila cambios; repetir la importación sólo añade issues nuevas. Sin merge requests, comentarios, etiquetas, asignados ni hitos. Sin webhooks entrantes de GitLab: 25 es saliente. Sin Jira, Trello ni Todoist por las razones de la primera subsección; cada uno requerirá su feature con autenticación y mapeo propios. Sin edición de la base de API por el usuario. La exportación de 22 no incluye conexiones ni tokens; `task_external_links` sigue la decisión que 27 tome sobre exportación.

### Decisiones y alternativas descartadas

| Decisión | Alternativa descartada | Motivo |
| --- | --- | --- |
| GitLab como segundo gestor | Jira, Trello o Todoist | Único proveedor isomorfo a GitHub; valida el puerto con dos implementaciones reales y cubre autoalojado |
| Puerto `IssueSource` y `ImportIssues(source)` compartido | Caso de uso GitLab copiado del de GitHub | Un solo lugar para mapeo, límites y recibo; la mutación muerde una lógica, no dos |
| Reutilizar `task_external_links` con `source = gitlab` | Tabla `gitlab_links` propia | Misma forma, misma unicidad; una tabla menos que migrar y exportar |
| `external_id = host:id global` | `iid` del proyecto | El `iid` colisiona entre proyectos e instancias; el id global con host no |
| Base de API en propiedad de servidor | Campo por conexión | Un operador, una instancia; evita validar URLs arbitrarias (SSRF) |
| Guardián de importación por propietario | Por proveedor | Evita dos importaciones concurrentes contra el mismo proyecto destino |
| Transacción por issue | Una transacción por importación | Sin HTTP dentro de transacciones; caída deja estado consistente y reanudable |
| Catálogo con seis filas fijas y `disabled` para lo no desplegado | Sólo conectores existentes | El usuario ve el mapa completo; falla cerrado sin inventar `connected` |
| GET GitLab devuelve `not_connected` | 404 sin conexión | Una sola forma de respuesta simplifica interfaz y pruebas |
| Excluir `issue_type != issue` y movidas | Importar todo `state=opened` | Incidentes y test cases no son trabajo planificable; las movidas duplican |

### Verificación prevista

Unitarias de `ImportIssues` con `IssueSource` falso: mapeo, truncado por puntos de código, `EMPTY_TITLE`, 200 issues y `truncated`, `skipped` por enlace existente, fallo en página 2 con recibo parcial. Persistencia con PostgreSQL Testcontainers: cifrado/descifrado con AAD, UNIQUE del enlace, enlace y tarea en la misma transacción, guardián con entrada abandonada. Adaptador GitLab contra servidor falso `com.sun.net.httpserver`: codificación del `projectPath`, cabecera `PRIVATE-TOKEN`, `X-Next-Page`, filtros de tipo, 401/404/429/5xx, redirección rechazada, cuerpo sobredimensionado, tiempo agotado. MockMvc para las cinco rutas, errores de la tabla, `CONNECTORS_DISABLED` y ausencia del token en toda respuesta. Vitest para catálogo y pantalla GitLab: estados, recibo, confirmación de desconexión, foco y anuncios. E2E con pila real y servidor GitLab falso publicado en la red de Compose: conectar, importar dos veces, desconectar, catálogo coherente; axe y anchos 320/768/1440 con zoom 200 %. Mutación: PIT `-PmutationScope=additional_connectors` sobre dominio, `ImportIssues`, adaptador GitLab, controladores y `ApplicationConfiguration`; Stryker `frontend/stryker.additional-connectors.config.json` sobre `connectors.tsx`, `gitlab-connector.tsx`, sus módulos `*-api.ts` y los rangos de `App.tsx`/`workspace.tsx` de la feature. Umbral 80 %, sin rebajarlo para cerrar.
