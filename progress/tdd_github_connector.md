# TDD del conector de GitHub (feature 27)

Rama `claude/github-connector` (worktree `C:/Users/vhurt/ow-worktrees/github-connector`),
reasentada sobre `origin/main` en `a6164e4`, que ya trae las features 24 y 26 y el contrato
enmendado. Contrato: `features/github_connector.feature`, 42 escenarios @s1–@s42.
Puerto E2E usado: 18094.

## Estado

Feature en curso: 27 — `github_connector`. Escenarios recorridos: @s1…@s42.
E2E de @s42 ejecutado en el puerto 18094: **12 de 12 en verde**.
No se marca `done`: falta `judge` y `mutation_tester`.

## Decisión de diseño para la feature 29

El listado de issues sale por el puerto `IssueSource` (`application/IssueSource.java`), que devuelve
`ExternalIssue` de dominio y `IssuePage`. GitLab (feature 29) implementa el mismo puerto sin tocar el
caso de uso: el caso de uso no conoce GitHub, sólo `IssueSource`, `ExternalIssue` y los códigos de
error de `IssueSourceException`.

## Ciclos rojo → verde → refactor

### Ciclo 1 — @s5 `owner/repo` recortado y validado

- ROJO `GithubRepositoryTest`: recorte Unicode White_Space, expresión del propietario (1–39, sin
  guion inicial ni final), del nombre (1–100), `REQUIRED` cuando falta.
- VERDE `domain/GithubRepository`: record con `Pattern` y `parse` que recorta.
- REFACTOR: la aserción del caso inválido comprueba `field` y `code` en lugar de reinyectar el
  mensaje real en el esperado (era una tautología).

### Ciclo 2 — @s6 el PAT nunca se imprime

- ROJO `PersonalAccessTokenTest`: 1–255 ASCII imprimibles sin espacios, `REQUIRED`, `TOO_LONG`,
  `INVALID_FORMAT`, y `toString` sin el valor.
- VERDE `domain/PersonalAccessToken`.
- REFACTOR: misma corrección de tautología en la aserción.

### Ciclo 3 — @s14 título de la issue recortado a 160 puntos de código

- ROJO `ExternalIssueTest` (no compilaba: `ExternalIssue` no existía).
- VERDE `domain/ExternalIssue.taskTitle()`: recorte Unicode y, si supera 160 puntos de código,
  159 puntos más `…`. Se cuenta por puntos de código, así que `🚀` no se parte.

### Ciclo 4 — @s15 criterio de completitud

- ROJO `ExternalIssueCriterionTest` (no compilaba: faltaba `taskCompletionCriterion`).
- VERDE `ExternalIssue.taskCompletionCriterion()`: URL sola cuando el cuerpo falta o es sólo
  Unicode White_Space; si no, URL, línea en blanco y las 20 primeras líneas del cuerpo con `\r\n`
  normalizado por `String.lines()`; recorte a 2000 puntos de código con `…`.
- REFACTOR: `cut` y `trim` compartidos con el título; `firstLines` con `Collectors.joining`.

### Ciclo 5 — reanudación: el resguardo `wip` era verde

Al retomar el carril, `compileTestJava` y las clases `adapter.connectors.*`, `GithubRepositoryTest`,
`ExternalIssue*Test` y `PersonalAccessTokenTest` pasaron sin tocar nada: el rojo de la validación de
la base de la API (`GithubApiBase`, @s35/B11) ya había encontrado su verde antes de aparcar.

### Ciclo 6 — @s1 @s3 @s5 @s6 @s7 @s8 @s9 @s21 @s22 @s28 conectar

- ROJO `ConnectGithubTest` (no compilaba: faltaban `ConnectGithub` y sus cinco puertos).
- VERDE: puertos `ConnectorConnectionStore`, `ImportReceiptStore`, `IssueSource`,
  `ImportedTaskCommit`; valores `StoredConnection`, `ConnectionView`, `IssuePage`,
  `RepositoryIdentity`, `domain/ImportReceipt`; errores `IssueSourceException` (con `Reason`),
  `GithubTokenRejectedException`, `GithubRepositoryUnavailableException`,
  `GithubUnavailableException`, `ConnectorRateLimitedException`, `ConnectionNotFoundException`,
  `ConnectionInvalidException`, `ImportInProgressException`, `ImportNotFoundException`; caso de uso
  `ConnectGithub`.
- Orden fijado por el contrato: clave del conector, formato del repositorio, formato del token y
  sólo entonces la red. La fila se escribe después de que el gestor confirme, así que @s7, @s8,
  @s21 y @s28 dejan la conexión previa byte a byte igual.
- REFACTOR: `ConnectorFailures` concentra la traducción de `IssueSourceException`, que difiere
  entre conectar (token rechazado ⇒ 409 `GITHUB_TOKEN_REJECTED`) e importar (⇒ `CONNECTION_INVALID`).
- `StoredConnection.toString` redacta el texto cifrado y `ConnectionView` no tiene hueco para el
  token: la ausencia es estructural, no una omisión al serializar.

### Ciclo 7 — @s10 @s11 @s30 @s33 consultar, desconectar y leer un recibo

- ROJO `GithubConnectorQueriesTest` (no compilaba: faltaban `ReadGithubConnection`,
  `DisconnectGithub` y `ReadIssueImport`).
- VERDE los tres casos de uso. `lastImport` es el recibo de mayor `startedAt` **del propietario**;
  desconectar borra sólo la fila de conexión y es idempotente; un recibo ajeno y un identificador
  inexistente comparten error para no delatar qué existe fuera de la cuenta.
- REFACTOR de nombres obligado por una colisión real: la feature de importación de datos ya tenía
  `application/ImportReceipt`, `ReadImportReceipt` y `ImportReceiptQueries`. Los tipos del conector
  pasan a `domain/IssueImportReceipt`, `IssueImportReceiptStore`, `ReadIssueImport`,
  `IssueImportNotFoundException` e `IssueImportInProgressException`. Ninguna clase existente se ha
  tocado.

### Ciclo 8 — @s12 @s16 @s17 @s18 @s19 @s20 @s22 @s24 @s26 @s27 @s28 @s29 importar

- ROJO `ImportGithubIssuesTest`, 34 pruebas (no compilaba: faltaban `ImportGithubIssues` y
  `IssueImportFailedException`).
- VERDE `ImportGithubIssues`. Decisiones que el contrato fija y que el código hace explícitas:
  - **Orden de precondiciones** (@s29): clave, conexión, validez de la conexión, proyecto, estado
    del proyecto, exclusión mutua. El recibo se crea el último, así que @s23, @s24 (primera fila),
    @s25 y @s29 no dejan recibo ni tocan la red.
  - **Paginación** (@s16): se pide la página siguiente porque la anterior trajo exactamente 100
    *elementos* —los descartados por no ser issues también cuentan—, no porque venga `Link
    rel="next"`. `truncated` sale de que el gestor anuncie más después de la última página leída,
    que es lo único que distingue 200 issues (false) de 201 (true).
  - **Fallo aislado frente a fallo que detiene** (@s18 vs @s19): un título vacío se cuenta como
    fallido sin llegar al almacén; un `StorageUnavailableException` aborta, porque a partir de ahí
    no se puede garantizar que tarea, evento y enlace vayan juntos.
  - **Progreso incremental** (@s27): `receipts.progress` tras cada issue, para que una muerte a
    mitad deje el recibo `running` con lo confirmado y ni una tarea más.
  - **401 durante la importación** (@s22) marca la conexión `invalid`; el resto de fallos del
    gestor la dejan `valid`.
- Comprobación de que las pruebas muerden: al subir `MAX_PAGES` a 3 y cambiar la condición de
  parada a "página vacía", 9 de las 34 fallan. Revertido.

### Ciclo 9 — @s1 @s3 @s5 @s6 @s10 @s11 @s12 @s20 @s23 @s25 @s29 @s30 @s31 @s32 la frontera HTTP

- ROJO `GithubConnectorApiTest`, 43 pruebas (no compilaba: faltaba `GithubConnectorController`).
- VERDE `adapter/http/GithubConnectorController`: cinco rutas bajo `/api/v1/me/connectors/github`,
  todas con `Cache-Control: no-store, private`, cuerpo estricto (campo desconocido, tipo, requerido)
  y problemas RFC 7807 con los códigos del contrato.
- Cuatro rojos legítimos que corrigieron el código o la prueba:
  1. `@JsonInclude(NON_NULL)` **fuera**: el contrato pide once campos del recibo y cinco de la
     conexión *siempre presentes*, con `errorCode`, `finishedAt` y `lastImport` a `null` cuando
     toca. "Sin importaciones todavía" es información, no un campo ausente. Las pruebas comparan
     ahora el conjunto exacto de claves serializadas, que es lo que el contrato dice.
  2. `DELETE` devolvía 200: pasa a `ResponseEntity.noContent()`.
  3. Mi propio fixture de @s23 era inservible: `11111111-2222-3333-4444-555555555555` en mayúsculas
     es idéntico a sí mismo. Cambiado por uno con letras hexadecimales, que es lo que la prueba
     pretendía medir.
  4. El canal Bearer: ver la sección de @s31, más abajo.
- El recibo parcial (`importId`, `created`, `skipped`, `failed`) viaja dentro del problema para que
  la interfaz de @s39 no tenga que volver a preguntar.

### @s31: el contrato se enmendó y ya no hay desvío

Lo que este carril levantó como desvío quedó resuelto el 9 de septiembre de 2026: el propietario
ratificó que **403 `API_SCOPE_DENIED` es lo correcto** y el `.feature` se enmendó en `a6164e4`. El
razonamiento, que ahora vive en el propio contrato: una credencial Bearer válida **sí** está
autenticada, así que responder «no sé quién eres» a quien sí se ha identificado es falso; 403 dice
la verdad, «sé quién eres y esto no es para ti».

- La implementación **no cambió**: era la correcta desde el principio, y el filtro de la feature 24
  sigue sin tocarse.
- `GithubConnectorApiTest` afirma ahora las seis filas tal y como las nombra el contrato: 401
  `UNAUTHENTICATED` en las cinco sin credencial y 403 `API_SCOPE_DENIED` en la del canal Bearer,
  comprobando el **código** del problema y no sólo el estado HTTP.

### Ciclo 10 — @s1 @s13 @s16 @s20 @s28 @s34 @s35 el adaptador HTTP de GitHub

- ROJO `HttpGithubIssueSourceTest`, 32 pruebas contra `FakeGithub`, un servidor del JDK en loopback
  y puerto efímero (sin Testcontainers, sin red saliente).
- VERDE `adapter/connectors/HttpGithubIssueSource`:
  - `followRedirects(NEVER)`: seguir un 302 llevaría el PAT del usuario a un destino que elige el
    otro extremo. Un 302 es `UNAVAILABLE` y la ruta del `Location` no recibe ninguna petición.
  - Plazos explícitos: 2 s de conexión y 4 s de petición, por debajo de los 2,9 s y 4,9 s que pide
    @s28. Las pruebas los miden con reloj de pared.
  - Clasificación: 401 token rechazado; 404 repositorio no disponible; 429 cuota; 403 **es** cuota
    si GitHub lo dice (`x-ratelimit-remaining: 0` o `Retry-After`) y si no, repositorio no
    disponible, que es justo lo que distingue @s8 de @s20; el resto, caída.
  - Segundos de espera: `Retry-After` manda sobre `x-ratelimit-reset`, un reinicio ya pasado pide 1
    segundo y sin ninguna de las dos cabeceras se piden 60.
  - `more` sale del `Link` con `rel="next"`; los elementos con `pull_request` se descartan pero
    cuentan para decidir si se pide la página siguiente.
  - Ningún mensaje de error incorpora el token ni el cuerpo de la respuesta.
- Un rojo legítimo, y era la prueba: registraba la ruta en minúsculas y pedía el repositorio tal y
  como se tecleó. El adaptador pide exactamente lo que recibe; el nombre canónico viene del
  `full_name` que responde GitHub, no de normalizar por nuestra cuenta.

### Ciclo 11 — @s1 @s9 @s11 @s12 @s17 @s19 @s23 @s24 @s25 @s26 @s27 @s33 la persistencia

- ROJO `GithubConnectorPersistenceTest`, 25 pruebas, **un solo contenedor PostgreSQL** para las tres
  tablas y los tres adaptadores.
- VERDE migración `V25__github_connector.sql` (la reservada; no se ha tocado ninguna anterior) y
  `PostgresConnectorConnectionStore`, `PostgresIssueImportReceiptStore`,
  `PostgresImportedTaskCommit`.
- Invariantes que viven en el esquema, no en el código, y que las pruebas comprueban rompiéndolos:
  - `CREATE UNIQUE INDEX ... ON issue_import_receipts (owner_id) WHERE status = 'running'`: la
    exclusión mutua de @s25 la arbitra PostgreSQL. `begin` traduce la violación a
    `IssueImportInProgressException`.
  - `PRIMARY KEY (owner_id, source, external_id)` en `task_external_links`: reimportar no puede
    duplicar (@s17), y dos propietarios sí pueden enlazar el mismo id externo (@s33).
  - `CHECK ((status = 'running') = (finished_at IS NULL))` y hermanos: no cabe un recibo en curso
    con final, ni uno cerrado sin él, ni uno en curso con código de error.
  - `octet_length(token_ciphertext) BETWEEN 30 AND 284`: descarta que nadie guarde texto en claro.
- `PostgresImportedTaskCommit` bloquea la fila del proyecto (`FOR UPDATE`) igual que la creación
  normal de tareas, y **repropaga** `ResourceNotFoundException`, `ProjectCompletedException` y
  `ValidationException` en lugar de disfrazarlas de fallo del almacén: la transacción revierte
  igual, pero el motivo no es el almacén y el recibo no debe decir que sí.
- Un rojo legítimo y era la prueba: construía un `ObjectMapper` pelado, incapaz de serializar
  `Instant`. Ahora usa el mismo Jackson que configura la aplicación.

### Ciclo 12 — @s3 @s4 @s35 el cableado y la configuración

- ROJO `GithubConnectorWiringTest` (no compilaba: faltaba `ConnectorConfiguration`).
- VERDE `adapter/config/ConnectorConfiguration` y tres propiedades nuevas en
  `application.properties`: `app.connectors.key`, `app.connectors.key-previous` y
  `app.github.api-base`.
- Las dos políticas de configuración, distintas a propósito y probadas por separado:
  - **Clave ausente** (variable sin definir, que llega como cadena vacía) ⇒ conector deshabilitado,
    503 `CONNECTORS_DISABLED`. Un despliegue que aún no usa conectores no debe dejar de arrancar.
  - **Clave presente y mal formada** ⇒ la aplicación no arranca, nombrando la propiedad y **sin**
    incluir el valor en el mensaje. Arrancar con una clave rota significaría escribir secretos que
    luego no se pueden leer.
- La base de la API se valida al construir el bean, no al usarla: es del servidor y nunca puede
  venir de nada con alcance de petición.
- `ArchitectureTest` (ArchUnit) sigue verde: dominio y aplicación no han ganado dependencias.

### Ciclo 13 — @s10 @s12 @s20 @s21 @s30 @s37 @s39 @s41 el cliente del navegador

- ROJO `github-connector-client.test.ts`, 23 pruebas (el módulo no existía).
- VERDE `github-connector-client.ts`: cinco llamadas sobre el transporte de cookie y CSRF que ya
  existe, y `ConnectorError` con `code`, `retryAfterSeconds`, `importId` y los contadores
  parciales, que es justo lo que @s39 necesita para ofrecer la acción que resuelve cada error.
- El decodificador comprueba la forma acordada y **rechaza** lo que no encaja en lugar de
  confiar: campos exactos, estados de la lista, contadores no negativos, y la regla de que sólo un
  recibo en curso carece de final y de error. Sin esto, un backend equivocado se vería como datos.
- Un rojo legítimo y era la prueba: `apiRequest` normaliza las cabeceras a `Headers`, así que
  `options.headers["X-CSRF-TOKEN"]` era `undefined`. Se comprueba con `.get(...)`.

### Ciclo 14 — @s36 @s37 @s38 @s39 @s40 @s41 la pantalla del conector

- ROJO `github-connector.test.tsx`, 25 pruebas (el componente no existía).
- VERDE `github-connector.tsx`. Un solo estado visible a la vez: deshabilitado, sin conexión,
  conectada, importando, resultado, error recuperable e inválida.
  - **@s37** El token es `type="password"` con `autocomplete="off"` y se vacía tras enviar salga
    bien o mal (está en el `finally`); el repositorio sólo se conserva cuando el envío falló. El
    mensaje "GitHub rechazó el token" cuelga del campo por `aria-describedby` y el foco va allí.
  - **@s38** El selector ofrece sólo los proyectos no terminados, cargados con la API de proyectos
    que ya existe. Durante la petición el botón está deshabilitado y una región `aria-live="polite"`
    dice "Importando issues…" **sin porcentaje**, porque no hay progreso real que contar.
  - **@s39** Cada error ofrece la acción que lo resuelve: reintento con segundos para la cuota,
    "Reconectar", "Consultar estado", el selector otra vez, o los contadores parciales con enlace
    al proyecto. Ninguna petición se reintenta sola: las pruebas cuentan las llamadas.
  - **@s40** La confirmación es un grupo con dos botones, no `window.confirm`: cancelar no envía
    ningún DELETE y devuelve el foco a "Desconectar"; confirmar envía uno solo y lleva el foco al h1.
  - **@s41** `key={owner}` remonta la pantalla al cambiar de persona, y `mounted`/`AbortController`
    hacen que una respuesta tardía no toque el estado de la sesión nueva.
- Comprobación de que las pruebas muerden: cuatro mutaciones (no filtrar los proyectos terminados,
  no vaciar el token, mostrar siempre el aviso de truncado, no devolver el foco al cancelar) hacen
  fallar cuatro pruebas distintas. Revertidas.

### Ciclo 15 — @s42 (parte estructural) las rutas

- ROJO `github-connector-routing.test.tsx`.
- VERDE `App.tsx` gana `/integraciones/github` y `/integraciones`, y aparece `integrations-index.tsx`.
- **El menú no gana ninguna entrada**: ambas rutas pasan `section = null`, así que `workspace.tsx`
  no se toca. Era además el fichero más disputado con el carril de la feature 24.
- El índice `/integraciones` enlaza "Conector de GitHub" y "API para integraciones"; la prueba
  busca dentro de `main` para no confundirse con el enlace que el menú ya tenía.

### Ciclo 16 — @s25 @s34 los huecos que quedaban

Auditoría de trazabilidad @s → prueba: tres escenarios no estaban cubiertos de verdad.

1. **@s25 concurrencia real.** Había prueba del índice único, pero no de dos peticiones a la vez.
   Nueva prueba en `GithubConnectorPersistenceTest`: ocho hilos con `CyclicBarrier` llaman a
   `begin` sobre el mismo propietario. Exactamente uno empieza, siete reciben
   `IssueImportInProgressException` y queda un solo recibo `running`. Verde.
2. **@s34 la exportación.** `ConnectorExportExposureTest` fija que la exportación sigue teniendo
   catorce colecciones y que ninguna se llama como algo del conector. Si alguien añadiera las
   tablas nuevas al volcado, el texto cifrado del token acabaría en un fichero descargable.
3. **@s34 la bitácora.** No existía. ROJO `ConnectorAuditTest`, VERDE puerto
   `application/ConnectorAudit` y adaptador `adapter/logging/Slf4jConnectorAudit`, cableados en
   `ConnectGithub` y en `ImportGithubIssues`.
   - La bitácora registra lo que el contrato pide: propietario, repositorio, **código HTTP de
     GitHub** y contadores. Para eso `IssueSourceException` gana `githubStatus`.
   - **Ningún método del puerto admite el token.** Que no se pueda registrar es una propiedad del
     tipo, no una disciplina de quien escribe la línea; una prueba recorre los métodos por
     reflexión para que nadie añada después uno que lo acepte.

### Ciclo 17 — la puerta de mutación y el recorrido accesible

- Alcance de PIT `github_connector` en `backend/build.gradle.kts` (dominio, casos de uso,
  adaptadores del conector, controlador, persistencia, bitácora y el cableado), añadido también a
  la unión del perfil por defecto para que CI no pierda las clases nuevas.
- `frontend/stryker.github-connector.config.json` sobre los tres módulos propios de la pantalla.
- Objetivos `github_connector-backend` y `github_connector-frontend` en `scripts/project.mjs`, con
  sus tres pruebas en `scripts/project.test.mjs`.
- `e2e/github-connector.spec.mjs` para @s42: axe en los estados, recorrido con teclado, anchos 320,
  768 y 1440, área mínima de 44 × 44 y ausencia del token en `localStorage`, `sessionStorage` y
  cookies. **Escrito y comprobado sintácticamente, no ejecutado**: el carril tiene prohibido
  levantar la pila de e2e mientras haya otros carriles compartiendo la máquina.

#### Una regresión propia, encontrada y corregida

Añadir dos rutas a `App.tsx` desplazó sus líneas, y varios `stryker.*.config.json` fijan trozos de
ese fichero por `línea:columna`. `scripts/project.test.mjs` tiene un guardián que comprueba que
esos rangos siguen apuntando al código que decían apuntar, y **falló**. Recalculados los tres
rangos de `stryker.appearance.config.json` (`34:8-34:44`, `53:16-65:32`, `86:10-127:7`) y
verificados con la misma extracción que usa el guardián.

**Aviso para el coordinador**: otros diez `stryker.*.config.json` fijan rangos de `App.tsx` y
`workspace.tsx` que ya estaban desincronizados entre sí antes de este carril, y ninguno tiene
guardián. No los he tocado: cambiarlos invalidaría la evidencia de mutación congelada de otros
carriles. Es una decisión de coordinación.

**Dos fallos preexistentes en `scripts/project.test.mjs`**, ajenos a este carril y comprobados
contra el árbol anterior a mis cambios: `integration backend targets invoke only their fixed PIT
scopes` e `integration frontend invokes only its fixed Stryker configuration`. Las pruebas de la
feature 24 existen pero sus objetivos no están registrados en `scripts/project.mjs`. Es de su
carril.

## Trazabilidad @s → prueba

| @s | Dónde se comprueba |
| --- | --- |
| @s1 | `ConnectGithubTest`, `GithubConnectorApiTest`, `HttpGithubIssueSourceTest`, `GithubConnectorPersistenceTest`, `GithubConnectorWiringTest` |
| @s2 | `AesGcmSecretCipherTest` |
| @s3 | `ConnectGithubTest`, `GithubConnectorQueriesTest`, `ImportGithubIssuesTest`, `GithubConnectorApiTest`, `GithubConnectorWiringTest` |
| @s4 | `GithubConnectorWiringTest` |
| @s5 | `GithubRepositoryTest`, `ConnectGithubTest`, `GithubConnectorApiTest` |
| @s6 | `PersonalAccessTokenTest`, `ConnectGithubTest`, `GithubConnectorApiTest` |
| @s7 | `ConnectGithubTest`, `HttpGithubIssueSourceTest`, `GithubConnectorApiTest` |
| @s8 | `ConnectGithubTest`, `ImportGithubIssuesTest`, `HttpGithubIssueSourceTest`, `GithubConnectorApiTest` |
| @s9 | `ConnectGithubTest`, `GithubConnectorPersistenceTest` |
| @s10 | `GithubConnectorQueriesTest`, `GithubConnectorApiTest`, `github-connector-client.test.ts` |
| @s11 | `GithubConnectorQueriesTest`, `GithubConnectorApiTest`, `GithubConnectorPersistenceTest`, `github-connector-client.test.ts` |
| @s12 | `ImportGithubIssuesTest`, `HttpGithubIssueSourceTest`, `GithubConnectorPersistenceTest`, `GithubConnectorApiTest` |
| @s13 | `HttpGithubIssueSourceTest`, `GithubConnectorApiTest` |
| @s14 | `ExternalIssueTest`, `ImportGithubIssuesTest` |
| @s15 | `ExternalIssueCriterionTest` |
| @s16 | `ImportGithubIssuesTest` (las seis filas), `HttpGithubIssueSourceTest` |
| @s17 | `ImportGithubIssuesTest`, `GithubConnectorPersistenceTest` |
| @s18 | `ImportGithubIssuesTest` |
| @s19 | `ImportGithubIssuesTest`, `GithubConnectorPersistenceTest`, `GithubConnectorApiTest` |
| @s20 | `HttpGithubIssueSourceTest` (las seis filas), `ImportGithubIssuesTest`, `GithubConnectorApiTest` |
| @s21 | `ConnectGithubTest`, `GithubConnectorApiTest`, `github-connector-client.test.ts` |
| @s22 | `ImportGithubIssuesTest`, `ConnectGithubTest`, `GithubConnectorPersistenceTest` |
| @s23 | `ImportGithubIssuesTest`, `GithubConnectorApiTest` |
| @s24 | `ImportGithubIssuesTest`, `GithubConnectorPersistenceTest`, `GithubConnectorApiTest` |
| @s25 | `GithubConnectorPersistenceTest` (ocho hilos reales), `ImportGithubIssuesTest`, `GithubConnectorApiTest` |
| @s26 | `ImportGithubIssuesTest`, `GithubConnectorPersistenceTest` |
| @s27 | `ImportGithubIssuesTest`, `GithubConnectorPersistenceTest` |
| @s28 | `HttpGithubIssueSourceTest`, `ImportGithubIssuesTest`, `ConnectGithubTest` |
| @s29 | `ImportGithubIssuesTest`, `GithubConnectorApiTest` |
| @s30 | `GithubConnectorQueriesTest`, `GithubConnectorApiTest`, `github-connector-client.test.ts` |
| @s31 | `GithubConnectorApiTest` — las seis filas, con el 403 de la fila Bearer ya ratificado |
| @s32 | `GithubConnectorApiTest` |
| @s33 | `GithubConnectorQueriesTest`, `GithubConnectorPersistenceTest` |
| @s34 | `ConnectorAuditTest`, `ConnectorExportExposureTest`, `GithubConnectorApiTest`, `HttpGithubIssueSourceTest`, `ConnectGithubTest`, `ImportGithubIssuesTest`, `github-connector.test.tsx`, `e2e/github-connector.spec.mjs` (no ejecutado) |
| @s35 | `GithubApiBaseTest`, `HttpGithubIssueSourceTest`, `GithubConnectorApiTest`, `GithubConnectorWiringTest` |
| @s36 | `github-connector.test.tsx` (los seis estados), `github-connector-routing.test.tsx` |
| @s37 | `github-connector.test.tsx` |
| @s38 | `github-connector.test.tsx` |
| @s39 | `github-connector.test.tsx` (las cinco filas) |
| @s40 | `github-connector.test.tsx` |
| @s41 | `github-connector.test.tsx`, `github-connector-client.test.ts` |
| @s42 | `github-connector-routing.test.tsx` (la parte estructural) y `e2e/github-connector.spec.mjs` — **el resto queda pendiente de ejecutar** |

## Lo que falta para cerrar

1. `judge` y `mutation_tester`. **No marco la feature como `done`.**

Cerrado desde entonces: el E2E de @s42 se ejecutó en el puerto 18094 con **12 de 12 en verde**, y
el desvío de @s31 dejó de serlo al enmendarse el contrato.

### Ciclo 18 — el E2E ejecutado y los dos defectos que sólo él destapó

`E2E_WEB_PORT=18094 pnpm test:e2e e2e/github-connector.spec.mjs`. Primera ejecución: **3 pasan, 9
fallan**. Las dos causas eran reales, no del guion:

1. **La pila de e2e nunca definía `APP_CONNECTOR_KEY`**, así que todas las rutas del conector
   respondían 503 y la pantalla se quedaba en el estado deshabilitado. `docker-compose.yml` acepta
   ahora `APP_CONNECTOR_KEY`, `APP_CONNECTOR_KEY_PREVIOUS` y `APP_GITHUB_API_BASE` como opcionales
   —sin clave el conector sigue deshabilitado, que es lo correcto para un despliegue que no lo
   usa— y `scripts/e2e.mjs` fija una clave de pruebas de 32 bytes y apunta la base de la API a
   `http://127.0.0.1:9`, un puerto de descarte dentro del contenedor: ninguna salida llega a
   api.github.com y el intento de conectar muere en el acto.
2. **El selector de proyecto medía menos de 44 × 44.** Vive fuera de un formulario, así que no le
   alcanzaban las reglas de campo del área de trabajo. Bloque `.github-connector` en `styles.scss`
   con tokens del tema y ningún color fijo, así que las guardas de modo oscuro de `main` siguen
   verdes.

Dos aserciones mías eran además demasiado estrictas y se corrigieron con su razón escrita: los
enlaces en línea dentro de un párrafo están exentos del tamaño mínimo por la excepción de
WCAG 2.2 §2.5.8, y el selector necesita un proyecto para tener geometría real.

Segunda ejecución: **12 de 12 en verde**, incluidos axe en tres estados, el recorrido con teclado,
los anchos 320, 768 y 1440, el área mínima y la ausencia del token en `localStorage`,
`sessionStorage` y cookies.

### Ciclo 19 — reasentamiento sobre `a6164e4` (features 24 y 26)

- Primero sobre `7ea682d`: la rama arrastraba la historia sin aplastar de la feature 24, que `main`
  ya tenía aplastada en `0277c50`. Reasentar los 117 commits habría sido absurdo, así que se
  reasentaron **sólo los del carril** con `--onto`. El commit del contrato se descarta porque
  `main` ya tenía el `.feature` byte a byte; con él se perdía el paso a `in_progress`, restaurado
  aparte.
- Después sobre `a6164e4`, que trae la feature 26. Cuatro conflictos, todos aditivos y resueltos
  conservando **los dos carriles**: `build.gradle.kts` (alcances `ics_calendar` y
  `github_connector`, y ambos en la unión del perfil por defecto), `scripts/project.mjs` (los
  cuatro objetivos), y los rangos de Stryker.
- `App.tsx` y `workspace.tsx` no dieron conflicto: la ruta y la entrada de menú del calendario
  conviven con las dos rutas del conector, que siguen sin añadir ninguna entrada al menú.
- **Los rangos `línea:columna` de Stryker**: tras la fusión ni los de `main` ni los míos servían,
  porque `App.tsx` tiene ahora las dos cosas. Recalculados y verificados con la misma extracción
  que usan los guardianes, los tres de `stryker.appearance.config.json` y los tres de
  `stryker.ics-calendar.config.json` —estos últimos los desplazaron mis rutas, así que me tocaba
  arreglarlos—. `scripts/project.test.mjs`: 79 de 79 en verde.
- La cuenta fija de entradas del menú en `github-connector-routing.test.tsx` era frágil y se rompió
  con la entrada del calendario. Ahora la prueba compara el menú de una ruta ajena con el de las
  rutas del conector: lo que el contrato exige es que el conector **no añada ninguna**, no que el
  menú tenga un tamaño concreto. Así no volverá a romperse con webhooks ni automatizaciones.

### Ciclo 20 — @s31 deja de ser un desvío

El propietario ratificó el 403 y el contrato se enmendó en `a6164e4`. La implementación no cambió.
`GithubConnectorApiTest` afirma ahora las seis filas como las nombra el contrato, comprobando el
**código** del problema y no sólo el estado: 401 `UNAUTHENTICATED` en las cinco sin credencial y
403 `API_SCOPE_DENIED` en la del canal Bearer. La bitácora ya no lo lista como desvío.

## Enmiendas al contrato aprobadas por el coordinador (9 de septiembre de 2026)

Origen: `progress/security_review_connectors.md` (rama `main`). El coordinador actualiza
`project-spec.md` y el `.feature` en `main`; aquí se implementa ya el comportamiento corregido.

- **B11** — `app.github.api-base` se valida al arrancar contra una lista fija: exactamente
  `https://api.github.com`, o un host de loopback (`127.0.0.1`, `::1`, `localhost`) para pruebas.
  Cualquier otro valor impide arrancar. El valor nunca se toma de nada con alcance de petición.
  Refuerza @s35, que ya exigía fallo de arranque con `http://api.github.com`.
- **B5** — El texto cifrado lleva delante un byte de versión de clave y se leen
  `APP_CONNECTOR_KEY` y `APP_CONNECTOR_KEY_PREVIOUS`, de modo que rotar la clave siga
  descifrando lo guardado. Política unificada: clave mal formada impide arrancar (@s4), clave
  ausente degrada a 503 `CONNECTORS_DISABLED` (@s3). El AAD ata el texto cifrado al propietario.
  **Desvío medible respecto a @s1**: `octet_length` pasa de 12 + 14 + 16 = 42 a
  1 + 12 + 14 + 16 = 43 bytes, y el `CHECK` de la migración pasa a `BETWEEN 30 AND 284`.
