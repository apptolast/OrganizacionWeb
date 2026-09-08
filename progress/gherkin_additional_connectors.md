# Destilación de additional_connectors (29) — borrador revisable

Fuente: `progress/proposal_additional_connectors.md` (normativa) y `progress/proposal_github_connector.md` (27), aplicando la regla de la propia propuesta 29: donde 27 fija algo distinto, 27 prevalece. Rol gherkin_author: no producción, no tests, no ejecución de suites. Ponytail full / Caveman lite. Este documento no aprueba TDD ni cambia `feature_list.json` ni `project-spec.md`.

Archivo: `features/additional_connectors.feature`. **38 escenarios estables @s1–@s38**, 16 Scenario Outline. No lleva `@approved`.

## Mapa @s → sección de la propuesta

| Sección de la propuesta 29 (y 27 donde prevalece) | Escenarios |
| --- | --- |
| API · catálogo: seis filas cerradas, orden fijo, campos cerrados | @s1 |
| API · catálogo: estado derivado por fuente (24 credenciales, 25 endpoints, 26 token de feed, 27 conexión, 28 suscripción, 29 conexión GitLab) | @s2 |
| Seguridad · `APP_CONNECTOR_KEY` ausente: filas `disabled`, catálogo disponible | @s3, @s29 |
| API · catálogo falla cerrado, aislamiento por propietario, `lastError` sin secretos | @s4, @s5, @s7 |
| Propósito · el catálogo no sincroniza ni llama a terceros | @s6 |
| API · `GET /connectors/gitlab` sin conexión devuelve `not_connected` | @s8 |
| API · `PUT` valida, consulta el proyecto, cifra y hace upsert; `tokenHint`; `version` | @s9, @s13 |
| API · validación de `token` y `projectPath`; `apiBase` no editable (campo desconocido) | @s10 |
| Adaptador GitLab · `projectPath` codificado, `app.gitlab.api-base` de servidor | @s11 |
| Adaptador GitLab · 401/403/404, 429, 5xx, redirección, cuerpo no JSON, tiempo agotado al conectar | @s12 |
| API · `DELETE` idempotente que conserva tareas y enlaces | @s14 |
| Importación · recibo, cabecera `PRIVATE-TOKEN`, `state=opened&per_page=100`, enlaces `host:id`, `lastActivityAt` | @s15 |
| Adaptador GitLab · `X-Next-Page`, dos páginas, 200 issues, `truncated` | @s16 |
| Adaptador GitLab · exclusión de `issue_type != issue` y `moved_to_id` como `skipped` | @s17 |
| Modelo · UNIQUE `(owner_id, source, external_id)` como única idempotencia; mismo `external_id` en github y gitlab | @s18, @s19 |
| Puerto `IssueSource` · un solo caso de uso de importación para ambos proveedores | @s20 |
| Modelo · mapeo issue → tarea idéntico al de 27 (159 + `…`, 20 líneas, 2000 puntos, título vacío `failed`) | @s21 |
| API · precondiciones del proyecto destino (feature 7) y de la conexión antes de contactar GitLab | @s22 |
| Errores · `CONNECTION_INVALID` durante la importación marca `error` y actualiza catálogo | @s23 |
| Errores · `RATE_LIMITED` con `Retry-After` / `RateLimit-Remaining: 0` | @s24 |
| Errores · proveedor indisponible sin seguir redirecciones, límite de 5 MiB, tiempo agotado | @s25 |
| Concurrencia y recuperación · fallo en página 2, transacción por issue, sin rollback | @s26 |
| Concurrencia · guardián `IMPORT_IN_PROGRESS` por propietario compartido entre proveedores; bloquea PUT y DELETE | @s27 |
| Concurrencia · recibo `running` abandonado (15 min, `INTERRUPTED`) | @s28 |
| API · recibo recuperable por id, ajeno = inexistente | @s30 |
| Seguridad · sesión, Bearer de 24 excluido, CSRF, origen | @s31 |
| Seguridad · token sólo en `PRIVATE-TOKEN`; ausente en respuestas, logs y navegador | @s32 |
| Interfaz · `/conectores`: navegación, h1, seis filas, estado accesible, enlaces por fila, «No disponible» | @s33 |
| Interfaz · `/conectores/gitlab` sin conexión: formulario, estados guardando/guardado/fallo | @s34 |
| Interfaz · con conexión: importar, selector nativo, recibo de cuatro cifras, desconexión con confirmación | @s35 |
| Interfaz · errores recuperables y «Actualizar estado» | @s36 |
| Interfaz · cancelación, logout, sesión expirada | @s37 |
| Interfaz · 320/768/1280, texto 200 %, zoom 200 %, 44 px, foco, axe | @s38 |

## Dependencias con 27 (qué debe existir antes de iniciar TDD de 29)

1. **Puerto `IssueSource` y caso de uso de importación por origen.** 27 debe entregar el puerto con `source()` y `openIssues(connection, page)` y su adaptador GitHub implementándolo; 29 sólo añade la segunda implementación. Si 27 se fusiona con un caso de uso acoplado a GitHub, @s20 y @s21 no son alcanzables sin refactorizar 27 primero.
2. **`task_external_links` con `source` ampliable.** 27 crea la tabla con `CHECK (source IN ('github'))`; la migración `V27__additional_connectors.sql` de 29 debe ampliar el CHECK a `('github','gitlab')`. @s15, @s18 y @s19 dependen de ello.
3. **Guardián `IMPORT_IN_PROGRESS` por propietario, no por tabla de proveedor.** 27 lo implementa con el índice parcial `github_imports_one_running`. Para que @s27 (una importación GitHub bloquea una de GitLab y viceversa) y @s28 se cumplan, el recibo debe vivir en una tabla común por propietario con columna `source`, o el índice parcial debe generalizarse. Decisión de implementación abierta al `tdd_craftsman`, pero el comportamiento queda fijado aquí.
4. **Recibo persistido con la forma de 27.** `{id, source, projectId, projectPath|repository, status, created, skipped, failed, truncated, errorCode, startedAt, finishedAt}`, `201 + Location` y `GET .../imports/{id}`. 29 proponía `200` sin `id`; se adopta 27 porque el caso de uso es compartido y la recuperación de una respuesta perdida exige un recibo consultable (@s15, @s23, @s26, @s30).
5. **Cifrado y `APP_CONNECTOR_KEY`.** Misma clave, AES-256-GCM, AAD `owner_id + ":gitlab"`; el comportamiento «clave ausente → 503 `CONNECTORS_DISABLED`, aplicación arranca» es el de 27 (@s3, @s29).
6. **Página `/integraciones` (24) y `/integraciones/github` (27)** para los enlaces del catálogo (@s33). Las rutas de 25, 26 y 28 se enlazan sólo si están desplegadas; si no, «No disponible».
7. **Catálogo: `ConnectorStatusProvider` por feature.** 24–28 aún no exponen ese puerto. El TDD de 29 debe implementarlo leyendo directamente sus tablas o filas existentes (fuente declarada en @s2) y devolviendo `disabled` para features no desplegadas; no se exige tocar el código de 24–28.

## Huecos detectados y resueltos

1. **Códigos y estados HTTP divergentes entre 27 y 29 para los errores compartidos.** 29 proponía `CONNECTION_INVALID` 422, `RATE_LIMITED` 429, `CONNECTION_MISSING` 409, `SOURCE_UNAVAILABLE` 502, `PROJECT_NOT_FOUND` 404. El caso de uso es compartido y 27 ya fija `CONNECTION_INVALID` 409, `RATE_LIMITED` 503 con `retryAfterSeconds` (por defecto 60), `CONNECTION_NOT_FOUND` 404 y `RESOURCE_NOT_FOUND` 404 (heredado de la feature 7). Resuelto adoptando 27 en @s12, @s22–@s25. El fallo de red se nombra `GITLAB_UNAVAILABLE` 503, simétrico a `GITHUB_UNAVAILABLE` (la pregunta abierta de 29 pedía adoptar el nombre de 27).
2. **Mapeo issue → tarea con dos versiones.** 29 decía «truncado al límite de la feature 7» y «descripción de 4000 puntos»; 27 fija 159 + `…`, `completionCriterion` = URL + línea en blanco + 20 líneas, 2000 puntos. 29 también exige «mapeo idéntico al de 27». Resuelto con el de 27 en @s21; el motivo `EMPTY_TITLE` de 29 no se expone porque el recibo sólo tiene contadores.
3. **Antigüedad del recibo abandonado.** 29 decía 10 minutos, 27 fija 15 minutos y `errorCode: INTERRUPTED`. Resuelto con 15 minutos en @s28 (guardián compartido, un solo umbral).
4. **«Mismo external_id en github y gitlab» no es alcanzable por la API.** El prefijo `host:` de GitLab hace que un `external_id` de gitlab nunca coincida textualmente con el decimal de GitHub. La propiedad que importa es la de la restricción: UNIQUE por `(owner_id, source, external_id)`. Se destila como escenario de persistencia (@s19) con el mismo texto en ambos orígenes, y @s18 cubre la idempotencia real vía API.
5. **Catálogo sin fuente para `lastActivityAt` en 24 y 26.** 26 declara que no guarda fecha de último uso; 24 no tiene propuesta escrita. Resuelto fallando cerrado en @s2: `null` salvo que la feature registre último uso. La tabla de @s2 fija también qué cuenta como `error` en 25 (`DELIVERY_EXHAUSTED`) y 28 (`lastStatus FAILED`), que la propuesta dejaba implícito.

## Dudas para el humano o el coordinador

- **Anchos responsive.** El coordinador pidió 320/768/1280; la propuesta 29, la 27 y 11 contratos previos usan 1440 (uno usa 1280). @s38 sigue la instrucción del coordinador; si se prefiere la convención del repo, cambiar 1280 por 1440 en una línea.
- **`GET /connectors/gitlab` devuelve 200 `not_connected` (29) mientras `GET /connectors/github` devuelve 404 (27).** Se mantiene 29 en @s8 porque es un recurso propio y su justificación (una sola forma de respuesta) es explícita; la asimetría queda visible para que el humano decida si 27 debería alinearse.
- **`skipped` con dos significados en GitLab.** 29 cuenta incidentes/test cases/tasks/movidas como `skipped`; en 27 los pull requests no cuentan y `skipped` significa «ya enlazada». @s17 sigue 29 porque la instrucción lo pedía si la propuesta lo fijaba. Si se quiere una semántica única, cambiar @s17 a «no cuentan en ningún contador».
- **Qué cifra 24.** @s3 asume que `api_credentials` guarda hashes (no depende de `APP_CONNECTOR_KEY`) y por eso no pasa a `disabled`. Si 24 cifra con la misma clave, la fila debe añadirse a la lista de `disabled` en @s3.
- **Recuperación de respuesta perdida en la interfaz.** «Actualizar estado» relee `GET .../gitlab` y el catálogo (@s36); no relee el recibo porque el cliente no conoce su id si perdió la respuesta. Si se quiere mostrar el último recibo, `GET .../gitlab` necesitaría `lastImport` como en 27.
- **Bearer de 24 fuera de la allowlist.** @s31 asume que 24 responde 401 `UNAUTHENTICATED` a rutas fuera de su allowlist, como en 27; confirmar cuando exista el contrato de 24.
