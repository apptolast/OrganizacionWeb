## Feature 27: github_connector — Importar issues de GitHub con trazabilidad

Propuesta normativa preparada el 8 de septiembre de 2026 bajo la autorización global del 5 de septiembre. Pendiente de revisión independiente antes de incorporarse a `project-spec.md`; no acredita implementación. Depende de la fusión de la feature 24 (`/integraciones`, canal Bearer) porque reutiliza su página y su exclusión de rutas.

### Propósito y frontera

Traer issues abiertas de un repositorio de GitHub como tareas de un proyecto propio, conservando de forma duradera qué tarea procede de qué issue. Es una importación bajo demanda y de un solo sentido: nada se escribe en GitHub, nada se sincroniza después y una tarea importada es una tarea normal del producto. Una conexión por propietario, un repositorio por conexión, sin GitHub App, OAuth ni webhooks.

### Modelo y persistencia

Migración reservada **V25__github_connector.sql** con tres tablas:

- `github_connections(owner_id TEXT PRIMARY KEY, repository TEXT NOT NULL, login TEXT NOT NULL, token_ciphertext BYTEA NOT NULL, status TEXT NOT NULL CHECK (status IN ('valid','invalid')), connected_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL)`. `repository` cumple `^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?/[A-Za-z0-9._-]{1,100}$` mediante CHECK. `token_ciphertext` contiene nonce de 12 bytes, texto cifrado y etiqueta de 16 bytes; CHECK `octet_length BETWEEN 29 AND 283`.
- `task_external_links(owner_id TEXT NOT NULL, task_id UUID NOT NULL UNIQUE REFERENCES tasks(id), source TEXT NOT NULL CHECK (source IN ('github')), external_id TEXT NOT NULL CHECK (char_length(external_id) BETWEEN 1 AND 200), url TEXT NOT NULL CHECK (url ~ '^https://' AND char_length(url) <= 2048), imported_at TIMESTAMPTZ NOT NULL, PRIMARY KEY (owner_id, source, external_id))`. Tabla genérica en forma; el enum de `source` es cerrado y cada conector futuro lo amplía por migración. Una tarea tiene como máximo un origen externo.
- `github_imports(id UUID PRIMARY KEY, owner_id TEXT NOT NULL, project_id UUID NOT NULL REFERENCES projects(id), repository TEXT NOT NULL, status TEXT NOT NULL CHECK (status IN ('running','completed','failed')), created INTEGER NOT NULL DEFAULT 0, skipped INTEGER NOT NULL DEFAULT 0, failed INTEGER NOT NULL DEFAULT 0, truncated BOOLEAN NOT NULL DEFAULT false, error_code TEXT, started_at TIMESTAMPTZ NOT NULL, finished_at TIMESTAMPTZ, CHECK ((status = 'running') = (finished_at IS NULL)))`, contadores con CHECK `>= 0`, índice único parcial `github_imports_one_running ON github_imports(owner_id) WHERE status = 'running'` e índice `(owner_id, started_at DESC, id DESC)`.

`external_id` es el `id` numérico global de la issue en GitHub, en decimal; `url` es su `html_url`. La conexión y los enlaces no forman parte del JSON v1 de exportación (22) ni de importación (23): añadir colecciones cambiaría esos contratos cerrados.

### API

Rutas privadas, sólo con sesión cookie (CSRF y OriginGuard vigentes), `Cache-Control: no-store`, JSON estricto sin campos desconocidos y sin query. El canal Bearer de la feature 24 no las incluye en su allowlist.

- `GET /api/v1/me/connectors/github` devuelve 200 con exactamente `{repository, login, status, connectedAt, lastImport}`; `status` es `valid` o `invalid`; `lastImport` es `null` o el DTO de recibo. Sin conexión, 404 `CONNECTION_NOT_FOUND`.
- `PUT /api/v1/me/connectors/github` recibe exactamente `{repository, token}`. `repository` se recorta con Unicode White_Space y debe cumplir la expresión anterior; `token` es una cadena de 1 a 255 caracteres ASCII imprimibles sin espacios. El servidor llama a `GET /repos/{owner}/{repo}` y a `GET /user` con el token; si ambos responden 200 guarda `repository` con el `full_name` devuelto, `login` de `/user`, el token cifrado y `status: valid`, y responde 200 con el DTO de conexión. Repetir el PUT sustituye repositorio y token; los enlaces existentes se conservan. Nunca devuelve el token.
- `DELETE /api/v1/me/connectors/github` borra la fila completa, incluido el texto cifrado, y responde 204 aunque no exista conexión. Tareas, recibos y `task_external_links` permanecen.
- `POST /api/v1/me/connectors/github/imports` recibe exactamente `{projectId}` (UUID canónico) y ejecuta la importación de forma síncrona. Éxito: 201, `Location: /api/v1/me/connectors/github/imports/{id}` y el recibo con `status: completed`.
- `GET /api/v1/me/connectors/github/imports/{id}` devuelve el recibo propio o 404 `IMPORT_NOT_FOUND`; el ajeno es igual al ausente.

Recibo cerrado: `{id, projectId, repository, status, created, skipped, failed, truncated, errorCode, startedAt, finishedAt}`; `errorCode` y `finishedAt` son `null` mientras corre. Errores en `application/problem+json` con el formato de `ApiErrors`:

| HTTP | Código | Cuándo |
| --- | --- | --- |
| 400 | VALIDATION_ERROR | Cuerpo, campos o `projectId` inválidos; errores por campo REQUIRED, INVALID_TYPE, INVALID_FORMAT, TOO_LONG, UNKNOWN_FIELD |
| 404 | CONNECTION_NOT_FOUND / IMPORT_NOT_FOUND / RESOURCE_NOT_FOUND | Sin conexión; recibo ausente; proyecto inexistente o ajeno |
| 409 | GITHUB_TOKEN_REJECTED | GitHub responde 401 al conectar; no se guarda nada |
| 409 | GITHUB_REPOSITORY_UNAVAILABLE | GitHub responde 404 o 403 sin cabeceras de cuota al conectar o al importar; la conexión no cambia |
| 409 | CONNECTION_INVALID | Conexión con `status: invalid`, o GitHub respondió 401 durante la importación (se marca `invalid` en ese momento) |
| 409 | IMPORT_IN_PROGRESS | Ya existe un recibo `running` del propietario |
| 409 | PROJECT_COMPLETED | El proyecto destino está `completed` al empezar o durante la importación |
| 503 | CONNECTORS_DISABLED | `APP_CONNECTOR_KEY` ausente; ninguna ruta del conector escribe ni lee GitHub |
| 503 | RATE_LIMITED | GitHub 429, o 403 con `x-ratelimit-remaining: 0` o `Retry-After`; incluye `retryAfterSeconds` y cabecera `Retry-After` |
| 503 | GITHUB_UNAVAILABLE | Timeout, red, TLS, 5xx o JSON inesperado de GitHub |
| 503 | STORAGE_UNAVAILABLE | Persistencia fallida, según el contrato existente |

`retryAfterSeconds` toma `Retry-After` si existe, si no `x-ratelimit-reset` menos el reloj inyectado, mínimo 1 y por defecto 60. Cuando un POST de importación falla después de crear el recibo, el problema añade el miembro `importId` para consultar los contadores parciales.

### Seguridad y secretos

El token es un PAT de grano fino que el usuario pega; necesita permiso de lectura de issues y metadatos del repositorio. Se cifra con AES-256-GCM, nonce aleatorio de 12 bytes por escritura, etiqueta de 128 bits y `owner_id` como dato adicional autenticado, usando la clave de `app.connectors.key` (`APP_CONNECTOR_KEY`, base64 de 32 bytes). Clave ausente: el conector responde 503 `CONNECTORS_DISABLED` y el resto de la aplicación arranca. Clave presente pero no decodificable a 32 bytes: la aplicación no arranca. Rotar la clave exige volver a conectar; no hay identificador de clave.

El token no aparece en respuestas, logs, auditoría, mensajes de error, exportación, sesión ni almacenamiento del navegador; el campo del formulario se vacía tras enviar. Sólo viaja a `app.github.api-base` (por defecto `https://api.github.com`; `http` sólo para loopback, como `app.public-origin`), con `Authorization: Bearer`, `Accept: application/vnd.github+json`, `X-GitHub-Api-Version: 2022-11-28` y `User-Agent: OrganizationWeb`, sin seguir redirecciones. Timeouts: 2 s de conexión y 4 s de lectura por llamada. Los logs registran propietario, repositorio, código HTTP de GitHub y contadores; nunca cuerpos de issues.

### Importación

Precondiciones en orden: clave configurada, cuerpo válido, conexión existente y `valid`, proyecto propio y no `completed`, inserción del recibo `running` (la violación del índice parcial produce 409 `IMPORT_IN_PROGRESS`). Después se piden como máximo dos páginas de `GET /repos/{owner}/{repo}/issues?state=open&per_page=100&sort=created&direction=desc&page=n`; la segunda sólo si la primera trae 100 elementos. `truncated` es verdadero si tras la última página leída la cabecera `Link` contiene `rel="next"`. Los elementos con campo `pull_request` se descartan sin contar en ningún contador.

Mapeo por issue: el título se recorta con Unicode White_Space; si supera 160 puntos de código se conservan 159 y se añade `…`; si queda vacío la issue cuenta como `failed`. `completionCriterion` es `html_url`, una línea en blanco y las primeras 20 líneas del `body` (`\r\n` normalizado a `\n`; `body` nulo deja sólo la URL), recortado a 2000 puntos de código con `…` final. `estimatedMinutes` es `null`. `external_id` es el `id` de GitHub; existe enlace `(owner_id, 'github', external_id)` implica `skipped`, sin llamadas ni escrituras.

Cada issue restante se crea con el caso de uso `CreateTaskUseCase` existente dentro de una transacción abierta por un puerto nuevo `TaskLinkCommit`: el adaptador ejecuta el callback de creación (que a su vez usa `TaskCommit`, cuya transacción se une a la exterior) e inserta el enlace; tarea, `TaskCreated.v1` y enlace confirman o revierten juntos. No se define un evento nuevo. Una `ValidationException` de una issue suma `failed` y continúa. `StorageUnavailableException`, `ProjectCompletedException`, 401, 403/404 o cuota de GitHub abortan la ejecución: el recibo pasa a `failed` con `errorCode`, los contadores reflejan lo confirmado y la respuesta es el error correspondiente. Las tareas ya creadas no se revierten; repetir la importación las salta.

La atomicidad es por tarea y no por lote porque el lote exigiría mantener una transacción abierta durante las llamadas de red y el bloqueo del proyecto, y porque un único título inválido descartaría las 199 restantes. La idempotencia por enlace hace que la repetición sea segura y los contadores describen exactamente lo persistido. `created + skipped + failed` es el número de issues consideradas; el máximo de candidatas por ejecución es 200.

### Concurrencia y recuperación

El índice parcial es el bloqueo por propietario: dos POST simultáneos obtienen un 201 y un 409. Si el proceso muere con un recibo `running`, la siguiente petición de importación del mismo propietario marca `failed` con `errorCode: INTERRUPTED` cualquier recibo `running` con `started_at` anterior a 15 minutos según el reloj inyectado, y continúa; antes de ese plazo responde 409. Un enlace duplicado por carrera residual produce `skipped`, no error. Si el cliente pierde la respuesta del POST, no reintenta automáticamente: consulta `GET .../connectors/github` y muestra `lastImport`, que puede seguir `running`.

### Interfaz

Ruta `/integraciones/github`, sección de navegación `Integraciones` (sin entrada nueva en el menú); la página `/integraciones` de la feature 24 enlaza «Conector de GitHub». Estados excluyentes: conectores deshabilitados (explica que falta configuración del servidor, sin formulario); sin conexión (formulario con `repository` y `token` de tipo password, `autocomplete="off"`, ayuda sobre el permiso necesario, errores junto al campo); conectada (repositorio, login, estado, última importación con contadores y enlace al proyecto, selector de proyecto propio no terminado cargado con la API de proyectos existente, botón «Importar issues abiertas» y botón «Desconectar» con confirmación); importando (botón deshabilitado, aviso `aria-live` de progreso honesto sin porcentaje); resultado (contadores, aviso de truncado y de `failed`, enlace al proyecto); errores recuperables (cuota con segundos, conexión inválida que ofrece reconectar, importación en curso con opción de consultar estado). Se conservan borradores salvo pérdida de sesión, foco en `h1` al cambiar de estado, controles de 44 por 44, 320/768/1440 px, zoom 200 % y matriz UX de 30 filas con evidencia.

### Límites explícitos

No se importan pull requests, issues cerradas, etiquetas, asignados, hitos, comentarios ni adjuntos. No se actualizan tareas ya importadas cuando la issue cambia ni se cierran al cerrarse la issue. No hay varios repositorios, organizaciones, filtros ni programación periódica. Repositorios con más de 200 issues abiertas importan las 200 más recientes por ejecución. Sin acceso Bearer, sin webhooks, sin escritura en GitHub. PREGUNTA ABIERTA: si una versión 2 de exportación incluirá `task_external_links`; hasta entonces restaurar una copia y reimportar puede duplicar tareas, y la interfaz no lo avisa.

### Decisiones y alternativas descartadas

- PAT pegado frente a OAuth o GitHub App: cero registro de aplicación y cero redirecciones; OAuth queda para cuando exista más de un usuario.
- Cifrado AES-GCM con clave de entorno frente a guardar el token en claro o sólo un hash: el hash no permite reutilizarlo y el claro expone secretos de terceros ante una fuga de la base.
- `id` global de GitHub como `external_id` frente a `owner/repo#number`: sobrevive a renombrados y transferencias del repositorio.
- Importación síncrona frente a trabajo en segundo plano con sondeo: dos llamadas acotadas y 200 inserciones caben en el timeout del proxy; el recibo persistido ya permite recuperar una respuesta perdida. Si repositorios reales superan ese tiempo, la ampliación es 202 con el mismo recibo.
- Índice único parcial como bloqueo frente a bloqueo asesor de PostgreSQL: el bloqueo no puede abarcar las llamadas de red y el índice sobrevive a reinicios con recuperación explícita.
- Reutilizar `TaskCreated.v1` frente a un evento `GithubIssuesImported.v1`: no hay consumidor y la lista blanca de `OutboxMessage` no cambia.
- Dos páginas fijas frente a paginar hasta reunir 200 issues no enlazadas: acota tiempo y cuota; el usuario ve `truncated`.
- DELETE idempotente 204 frente a 404 sin conexión: desconectar dos veces no es un error para quien quiere que el token desaparezca.

### Verificación prevista

Unitarias: mapeo de título y descripción en 159/160/161 puntos de código y con caracteres fuera del BMP, exclusión de `pull_request`, cálculo de `retryAfterSeconds`, ida y vuelta AES-GCM y rechazo de claves de longitud incorrecta. PostgreSQL con Testcontainers: unicidad de enlaces, atomicidad tarea + evento + enlace con fallo inyectado en el enlace, índice parcial de `running`, recuperación por antigüedad, desconexión que conserva enlaces y recibos. MockMvc: DTO cerrados, cada código de error, ausencia del token en toda respuesta, 503 sin clave, CSRF y origen. Servidor GitHub falso con `com.sun.net.httpserver` en tests: paginación con `Link`, elementos PR, 401, 403 con y sin cuota, 429, 404, 5xx, respuesta lenta y ausencia de redirecciones seguidas. Vitest: los siete estados, vaciado del campo token, foco y `aria-live`. E2E Playwright con un servicio falso de GitHub en la pila de compose de e2e (`APP_GITHUB_API_BASE` apuntando a él): conectar, importar, reimportar con `skipped`, desconectar y estado deshabilitado, con axe y barrido responsive. Mutación: PIT scope `github_connector` sobre dominio, casos de uso, cliente HTTP, controlador, adaptador de persistencia y wiring; `frontend/stryker.github-connector.config.json` y destino `github-connector-frontend` en `scripts/project.mjs`; umbral 80 %, sin timeouts enmascarando supervivientes.
