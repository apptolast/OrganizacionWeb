# Destilación del conector de GitHub (feature 27)

Contrato: `features/github_connector.feature`, 42 escenarios con tags estables @s1–@s42. Fuente de verdad: `progress/proposal_github_connector.md` (propuesta normativa del 8 de septiembre de 2026). Estilo tomado de `features/import_data.feature` y `features/create_task.feature`. Estado del contrato: escrito, pendiente de puerta humana. No se han modificado `project-spec.md`, `feature_list.json`, `src/` ni tests; el cambio de estado en `feature_list.json` lo aplica el coordinador.

## Trazabilidad @s → sección de la propuesta

| @s | Comportamiento | Sección de la propuesta |
| --- | --- | --- |
| @s1 | PUT válido, DTO cerrado, cifrado AES-GCM con AAD owner_id, cabeceras hacia GitHub, token ausente de la respuesta | API (PUT), Seguridad y secretos |
| @s2 | Nonce nuevo por escritura; AAD y clave incorrectas fallan | Seguridad y secretos |
| @s3 | Sin `APP_CONNECTOR_KEY` las cinco rutas responden 503 CONNECTORS_DISABLED sin tocar GitHub | Seguridad y secretos, tabla de errores |
| @s4 | Clave no decodificable a 32 bytes impide arrancar | Seguridad y secretos |
| @s5 | Recorte y expresión regular de `repository`; sin llamadas cuando falla | API (PUT), Modelo (CHECK) |
| @s6 | Validación de `token`, cuerpo estricto, query prohibida, MALFORMED_JSON y 415 heredados | API (PUT), tabla de errores |
| @s7 | 401 de GitHub al conectar → 409 GITHUB_TOKEN_REJECTED sin fila | Tabla de errores |
| @s8 | 404 / 403 sin cuota al conectar o importar → 409 GITHUB_REPOSITORY_UNAVAILABLE, conexión intacta | Tabla de errores |
| @s9 | Reconectar sustituye repositorio y token; enlaces y recibos se conservan | API (PUT) |
| @s10 | GET de la conexión: 404 o DTO cerrado con `lastImport` | API (GET) |
| @s11 | DELETE idempotente 204; tareas, enlaces y recibos permanecen | API (DELETE), Decisiones |
| @s12 | Importación básica: tarea + `TaskCreated.v1` + enlace por issue; PR descartadas sin contar | Importación |
| @s13 | Query fija hacia GitHub y no seguir redirecciones | Seguridad y secretos, Importación |
| @s14 | Título: 159/160/161, fuera del BMP, recorte White_Space, vacío = failed | Importación (mapeo), Verificación prevista |
| @s15 | `completionCriterion`: URL, línea en blanco, 20 líneas, `\r\n`, 2000 con `…` | Importación (mapeo) |
| @s16 | Dos páginas máximo, per_page=100, `truncated` por `Link rel="next"` | Importación |
| @s17 | Idempotencia por enlace UNIQUE; repetición no crea tareas ni eventos | Importación, Modelo |
| @s18 | Una issue `failed` no detiene el resto; suma de contadores | Importación |
| @s19 | Atomicidad tarea + evento + enlace con fallo inyectado; repetición retoma | Importación, Verificación prevista |
| @s20 | 429 / 403 con cuota → 503 RATE_LIMITED, cálculo de `retryAfterSeconds` | Tabla de errores, `retryAfterSeconds` |
| @s21 | Cuota al conectar no guarda fila | Tabla de errores |
| @s22 | 401 durante la importación marca `invalid`; reconectar restaura | Tabla de errores (CONNECTION_INVALID) |
| @s23 | Proyecto inexistente / ajeno indistinguibles; `projectId` inválido | Tabla de errores (RESOURCE_NOT_FOUND, VALIDATION_ERROR) |
| @s24 | PROJECT_COMPLETED al empezar y a mitad; cuota de activos intacta | Tabla de errores, Importación |
| @s25 | Dos POST concurrentes reales: un 201 y un 409 | Concurrencia y recuperación |
| @s26 | Recibo `running` huérfano: 15 minutos, INTERRUPTED | Concurrencia y recuperación |
| @s27 | Reinicio a mitad: sólo lo confirmado, `lastImport` running, sin relanzar | Concurrencia y recuperación |
| @s28 | 5xx, timeouts 2 s / 4 s, JSON inesperado → 503 GITHUB_UNAVAILABLE | Tabla de errores, Seguridad (timeouts) |
| @s29 | Orden fijo de precondiciones | Importación (precondiciones en orden) |
| @s30 | GET del recibo: propio, persistente tras reinicio, ajeno = ausente | API (GET imports/{id}) |
| @s31 | Sólo sesión cookie; Bearer de la feature 24 no autentica | API (rutas privadas) |
| @s32 | CSRF y Origin en PUT / DELETE / POST | API (rutas privadas) |
| @s33 | Aislamiento por propietario; PK del enlace incluye `owner_id` | Modelo, API |
| @s34 | El token no sale por respuestas, problemas, logs, exportación, navegador ni auditoría | Seguridad y secretos |
| @s35 | `app.github.api-base` es configuración del servidor; `http` sólo loopback | Seguridad y secretos |
| @s36 | Siete estados excluyentes de `/integraciones/github` | Interfaz |
| @s37 | Campo token `password`, `autocomplete="off"`, vaciado tras enviar, error junto al campo | Interfaz, Seguridad |
| @s38 | Selector de proyectos no terminados, botón deshabilitado, `aria-live` honesto, resumen | Interfaz |
| @s39 | Errores accionables: cuota con segundos, reconectar, consultar estado | Interfaz |
| @s40 | Desconectar con confirmación; cancelar no envía DELETE | Interfaz |
| @s41 | Borradores, logout, respuestas tardías, sesión vencida | Interfaz (borradores salvo pérdida de sesión) |
| @s42 | Teclado, foco en h1, 44×44, 320/768/1440, zoom 200 %, axe, enlace desde `/integraciones` | Interfaz, Límites |

## Huecos detectados en la propuesta y cómo se resolvieron

1. **Segunda página por conteo, no por `Link`.** La propuesta pide la página 2 «sólo si la primera trae 100 elementos» y define `truncated` por `Link rel="next"`. Con exactamente 100 issues eso implica dos llamadas y `truncated false`. @s16 lo fija explícitamente (fila 100 → 2 llamadas) para que el TDD no lo resuelva por `Link`.
2. **`body` vacío frente a `body` nulo.** La propuesta sólo trata `body` nulo. @s15 iguala cadena vacía o sólo White_Space a nulo (sólo la URL) para que no quede un criterio con línea en blanco final.
3. **Orden de precondiciones cuando varias fallan a la vez.** La propuesta enumera el orden pero ningún caso lo combina. @s29 cruza condiciones simultáneas para que cada rama tenga un mutante que la mate (por ejemplo, proyecto ajeno con recibo `running` debe dar 404 y no 409).
4. **Aislamiento del enlace por propietario.** La PK `(owner_id, source, external_id)` permite que dos personas importen la misma issue. Sin escenario, un test sólo con UNIQUE global pasaría igual. @s33 obliga a que B cree su propia tarea con `external_id "101"` mientras A conserva la suya.
5. **`connectedAt` al reconectar.** La propuesta no dice si el PUT repetido conserva `connected_at`. @s9 fija `connectedAt` igual al instante del nuevo PUT (una sustitución es una conexión nueva). Si el coordinador prefiere conservarlo, basta cambiar esa línea.
6. **Límite «de tareas» pedido en la delegación.** `project-spec.md` no define un máximo de tareas por proyecto; el único límite relacionado es `APP_MAX_ACTIVE_PROJECTS`, que la importación no toca. @s24 cubre PROJECT_COMPLETED y afirma que la cuota de activos no cambia. No se inventó un límite nuevo.

## Dudas para el coordinador

- **Dependencia de la feature 24.** `integration_api` sigue `pending` y `project-spec.md` sólo tiene su fila de índice. @s31 (Bearer no autentica) y @s42 (enlace desde `/integraciones`) presuponen esa página y ese canal. Si la 27 se implementa antes que la 24, esas dos afirmaciones deben aplazarse o la página `/integraciones/github` debe montarse sin el enlace padre.
- **Anchos responsive.** La delegación pedía 320/768/1280; la propuesta y `project-spec.md` (línea 65) fijan 320/768/1440. El contrato usa 1440 por fidelidad a la fuente. Confirmar.
- **Textos de interfaz.** El h1 «Conector de GitHub» y los mensajes de @s36–@s39 («GitHub rechazó el token», «Hay una importación en curso», «Reintenta en N segundos») no están en la propuesta; son propuestas del contrato para que el `Then` sea medible. Cambiarlos antes de la aprobación es una edición de texto.
- **Pregunta abierta heredada.** La propuesta deja abierta la exportación v2 con `task_external_links`; el contrato no la cubre y @s34 afirma que la exportación v1 sigue con catorce colecciones. Restaurar una copia y reimportar puede duplicar tareas; la interfaz no avisa, como dice la propuesta.
- **UUID canónico.** @s23 rechaza `projectId` en mayúsculas con 400 por la palabra «canónico» de la propuesta. Si otras rutas aceptan mayúsculas, alinear.

## Comprobaciones documentales

42 tags consecutivos @s1–@s42, un `When` por escenario (los que encadenan acciones en un solo `When` siguen el precedente de `import_data @s36` y `create_task`), cada `Then` afirma código HTTP, campo, conteo, bytes o elemento visible. Las filas de `Examples` son casos para ciclos TDD individuales, no evidencia ejecutada. No se ha ejecutado `init` ni ninguna prueba en esta fase documental.

## Decisiones del coordinador (8 de septiembre de 2026, 21:00)

- La feature 24 SÍ existe en la rama de trabajo (`claude/github-connector` parte de `codex/integration-api`): @s31 y @s42 se implementan tal cual.
- Anchos 320/768/1440 y texto 200 % se conservan.
- Textos de interfaz propuestos se aceptan; el tdd_craftsman puede ajustar redacción sin cambiar semántica, anotándolo.
- Exportación v1 no cambia; `task_external_links` queda fuera de export/import (límite documentado).
- @s23 UUID: alinear con el comportamiento real de las rutas existentes (si aceptan mayúsculas, 27 también); el tdd_craftsman lo verifica y anota.
