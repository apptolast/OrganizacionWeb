# TDD del conector de GitHub (feature 27)

Rama `claude/github-connector` (worktree `C:/Users/vhurt/ow-worktrees/github-connector`), partiendo de
`codex/integration-api`. Contrato: `features/github_connector.feature`, 42 escenarios @s1–@s42.
Puerto E2E reservado: 18093.

## Estado

Feature en curso: 27 — `github_connector`. Escenarios a recorrer: @s1…@s42.
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
  4. El canal Bearer: ver el desvío de abajo.
- El recibo parcial (`importId`, `created`, `skipped`, `failed`) viaja dentro del problema para que
  la interfaz de @s39 no tenga que volver a preguntar.

### Desvío documentado respecto a @s31 (pendiente de decisión del coordinador)

@s31 espera **401 UNAUTHENTICATED** cuando se usa una credencial Bearer del canal de integraciones
contra el conector. El comportamiento real es **403 `API_SCOPE_DENIED`**: el
`ApiCredentialBearerFilter` de la feature 24 autentica la credencial y sólo después comprueba su
lista blanca de rutas, que —correctamente— no incluye ninguna ruta del conector.

- La propiedad de seguridad que @s31 persigue se cumple entera: la credencial no abre el conector,
  no se escribe nada y no se contacta con el servidor falso. La prueba lo comprueba así.
- **No he tocado la lista blanca ni el filtro**: son de la feature 24, de otro carril.
- Hace falta decidir: o el `.feature` acepta 403 `API_SCOPE_DENIED` para esta fila, o el carril de
  la feature 24 cambia el filtro para responder 401 en rutas fuera de su lista blanca.

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
