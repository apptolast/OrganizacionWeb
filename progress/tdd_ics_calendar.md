# TDD — Feature 26 `ics_calendar`

Worktree `C:/Users/vhurt/ow-worktrees/ics-calendar`, rama `claude/ics-calendar`. Contrato:
`features/ics_calendar.feature` (@s1–@s38). Ponytail full y Caveman lite aplicados.

Nota: `.memoria-cache/patterns/` no existe en este worktree (sin sincronización de memoria
organizacional); se trabaja con las plantillas del repositorio (feature 24, export_data, today).

## Verificación del commit de resguardo `937c967` (9 de septiembre de 2026, 05:29)

El carril se aparcó con un `wip` no verificado (15 archivos, 591 líneas). Antes de construir
nada encima se ha compilado y ejecutado ese estado tal cual:

```
backend\gradlew.bat -p backend compileJava compileTestJava --no-daemon
BUILD SUCCESSFUL in 11s   (compileJava, compileTestJava ejecutadas; sin errores)

backend\gradlew.bat -p backend test --no-daemon ^
  --tests "com.apptolast.organization.domain.*Calendar*" ^
  --tests "com.apptolast.organization.application.CalendarFeed*"
BUILD SUCCESSFUL in 11s
  IcsCalendarTest           tests=18 failures=0 errors=0 skipped=0
  CalendarFeedUseCasesTest  tests=15 failures=0 errors=0 skipped=0
  CalendarFeedSecretTest    tests=9  failures=0 errors=0 skipped=0
  Total: 42 pruebas, 0 fallos, 0 errores, 0 omitidas.
```

Las 42 pruebas que la sesión anterior declaró en verde quedan **confirmadas** sobre el estado
`937c967`. El `wip` deja de ser una incógnita: compila y su suite de dominio y aplicación pasa.
(`CustomFieldValuesTest.xml` aparece en `build/test-results` pero es salida rancia de una
ejecución anterior; no lo selecciona ninguno de los dos filtros y no se cuenta.)

Lo que ese estado sigue **sin** demostrar, y es el trabajo de esta sesión: migración `V24`,
adaptadores PostgreSQL, capa HTTP, `location /calendar/` en nginx, frontend y E2E.

## Plan de escenarios

Orden: escritor iCalendar puro (s12, s13, s14, s22, s23, s24, s25) → token y casos de uso
(s1–s5, s10, s26 umbral) → persistencia V24 (s1, s4, s5, s9, s18–s21, s27, s28, s29) → HTTP
(s1–s8, s10, s11, s15–s17, s26, s30) → frontend (s31–s37) → E2E (s29, s38, recorrido real)
→ docs y registro de mutación.

## Bitácora de ciclos

### Ciclo 1 — @s12 @s13 @s14 @s22 @s23 @s24 @s25 (escritor iCalendar puro)

Rojo: `IcsCalendarTest` (18 pruebas) contra `IcsCalendar.render`. Sesión anterior dejó el
escritor casi completo; al recuperarla quedaba en rojo
`s25_eventsAreOrderedByStartThenUidRegardlessOfInput`
(`org.opentest4j.AssertionFailedError` en IcsCalendarTest.java:259): `render` recorría
`snapshot.entries()` en el orden de entrada.

Verde: `IcsCalendar.ordered(...)` ordena por `startAt` y después por el UID
(`blockId().toString()`) antes de emitir los VEVENT. Cambio mínimo: una función privada y
una llamada.

Refactor: nada que extraer; `line`, `escapeText` y `event` ya son funciones cortas.

Comando: `backend\gradlew.bat test --no-daemon --tests 'com.apptolast.organization.domain.IcsCalendarTest'`
→ 18 pruebas, 0 fallos.

Límites fijados por este ciclo (contrato):
- Toda línea física mide como máximo **75 octetos** sin CRLF, incluido el espacio inicial
  de continuación (74 octetos de carga útil).
- Documento vacío: **158 octetos**, 7 líneas. Documento de B: **714 octetos**, 21 líneas.
- El plegado no parte ni un punto de código UTF-8 ni una secuencia de escape (`\`, `\;`,
  `\,`, `\n`).

### Ciclo 2 — @s1 @s2 @s3 @s4 @s5 @s10 @s15 @s17 @s18 @s26 @s27 @s30 (token y casos de uso)

Restricción de esta sesión: el coordinador prohíbe levantar contenedores Docker hasta tener
turno. Todo este ciclo es dominio y aplicación puros, sin Testcontainers.

Rojo: los dos ficheros de prueba que la sesión anterior dejó sin confirmar
(`CalendarFeedSecretTest`, 9 pruebas; `CalendarFeedUseCasesTest`, 15 pruebas) **no
compilaban**, que es la forma más barata de fallar (Ley 2):

```
backend\gradlew.bat -p backend compileJava compileTestJava --no-daemon
> Task :compileTestJava FAILED
CalendarFeedUseCasesTest.java:153: error: cannot find symbol   symbol: class RenderCalendar
CalendarFeedUseCasesTest.java:154: error: cannot find symbol   symbol: class RenderCalendar
2 errors — BUILD FAILED in 16s
```

Verde: `application/RenderCalendar.java` y su puerto `RenderCalendarUseCase.java`, mínimos
para lo que exigen las 15 pruebas y nada más:
- `forToken(candidate)` resuelve el propietario con `CalendarFeedSecret.fingerprintOf` (SHA-256
  completo como única clave, `@s15`) y lanza `CalendarNotFoundException` sin tocar el
  calendario si no hay fila.
- `forOwner(owner)` no lee ni escribe ningún token (`@s17`).
- Ventana semiabierta `[now − 30 d, now + 365 d)` con el reloj inyectado (`@s18`).
- Techo de 2000 eventos comprobado **antes** de construir el documento (`@s26`), en los dos
  caminos, para que no exista cuerpo parcial.

Refactor: en verde, `withinCeiling` extraída como función con nombre y las tres constantes
(`WINDOW_BACK`, `WINDOW_FORWARD`, `MAX_EVENTS`) con nombre en vez de números sueltos.
`spotlessApply` retiró el import de `Instant` que quedó sin uso.

Comandos con sus números:
```
backend\gradlew.bat -p backend test --no-daemon \
  --tests "com.apptolast.organization.domain.CalendarFeedSecretTest" \
  --tests "com.apptolast.organization.domain.IcsCalendarTest" \
  --tests "com.apptolast.organization.application.CalendarFeedUseCasesTest"
BUILD SUCCESSFUL in 16s
CalendarFeedUseCasesTest  tests=15 failures=0 errors=0 skipped=0
CalendarFeedSecretTest    tests=9  failures=0 errors=0 skipped=0
IcsCalendarTest           tests=18 failures=0 errors=0 skipped=0
Total: 42 pruebas, 0 fallos.

backend\gradlew.bat -p backend spotlessApply --no-daemon   → BUILD SUCCESSFUL in 10s
```

Lo que este ciclo **no** demuestra y queda para el turno de Docker: que `CalendarFeedTokens`
tenga una implementación PostgreSQL con `ON CONFLICT`, que `token_hash` mida 32 octetos en la
columna, que la lectura salga de un único snapshot `REPEATABLE_READ` y que el 404 sea
indistinguible sobre HTTP real. El `FakeTokens` de la prueba imita las reglas de unicidad de
la tabla, no la tabla.

### Ciclo 3 — @s1 @s4 @s5 @s10 @s14 @s15 @s18 @s19 @s20 @s21 @s27 @s28 @s29 (migración V24 y adaptador PostgreSQL)

Rojo: `CalendarPersistenceTest` no compilaba (`cannot find symbol: PostgresCalendarStore`, 4
errores). Es el fallo más barato posible (Ley 2).

Verde:
- `V24__calendar_feed_tokens.sql`: `owner_id TEXT PRIMARY KEY`, `token_hash BYTEA NOT NULL UNIQUE
  CHECK (octet_length(token_hash)=32)`, `created_at TIMESTAMPTZ NOT NULL`. Ninguna migración
  anterior se toca.
- `adapter/persistence/PostgresCalendarStore`: implementa `CalendarFeedTokens` y `CalendarQueries`.
  `replace` es un único `INSERT … ON CONFLICT (owner_id) DO UPDATE`; `revoke` un `DELETE`;
  `ownerOf` busca por el hash completo (`WHERE token_hash=?`), sin prefijo ni comparación parcial.
  `read` abre una transacción `read-only` `REPEATABLE_READ` que cubre la zona de disponibilidad y
  los bloques, con `LIMIT 2001` para que el techo de 2000 se decida sin cargar la ventana entera.
  Cualquier `DataAccessException` o `TransactionException` se traduce a `StorageUnavailableException`.

Primer rojo real (no de compilación): `s18_theWindowIsSemiOpenOnBothEnds` reventó con
`DataIntegrityViolationException` al insertar. Motivo legítimo del esquema, no del código: `V11`
obliga a `date_trunc('minute',start_local)=start_local`, así que un bloque que empiece en
`…T11:00:01Z` **no es representable**. Los instantes con segundos de @s18 (y la fila que abarca
catorce meses, imposible por `duration_minutes BETWEEN 1 AND 1440`) se cubren donde sí son
representables: el nuevo objeto de dominio `CalendarWindow`.

### Ciclo 4 — @s18 (la ventana como regla de dominio)

Rojo: `CalendarWindowTest` no compilaba (`cannot find symbol: CalendarWindow`). Seis filas
`@CsvSource` que son literalmente las seis filas del Examples de @s18, con sus segundos exactos.

Verde: `domain/CalendarWindow` — `around(now)` fija `[now − 30 d, now + 365 d)` y
`covers(startAt, endAt)` devuelve `endAt.isAfter(from) && startAt.isBefore(to)`.

Refactor: `RenderCalendar` deja de calcular la ventana con dos `Duration` sueltas y usa
`CalendarWindow.around(clock.instant())`; el SQL de `PostgresCalendarStore` es el espejo exacto de
`covers` (`end_at>? AND start_at<?`) y la prueba de persistencia lo comprueba en las fronteras
representables (fin justo en `from` fuera, un minuto dentro; inicio justo en `to` fuera, un minuto
dentro).

Comandos:
```
backend\gradlew.bat -p backend test --no-daemon --tests "…adapter.persistence.CalendarPersistenceTest"
BUILD SUCCESSFUL — 14 pruebas, 0 fallos
backend\gradlew.bat -p backend test --no-daemon --tests "…domain.CalendarWindowTest" \
  --tests "…domain.IcsCalendarTest" --tests "…domain.CalendarFeedSecretTest" \
  --tests "…application.CalendarFeedUseCasesTest"
BUILD SUCCESSFUL — 49 pruebas, 0 fallos
```

### Ciclo 5 — @s1–@s8, @s10, @s11, @s15–@s17, @s26, @s30 (capa HTTP)

Rojo: `adapter/CalendarApiTest` (27 pruebas, `@WebMvcTest` con `SecurityConfiguration`) no compilaba
(`cannot find symbol: CalendarFeedController, PublicCalendarController`).

Verde:
- `CalendarPaths`: las tres direcciones en un solo sitio, para que enrutado y seguridad no se
  separen.
- `PublicCalendarController` (`/calendar/**`, GET y HEAD): endurece la respuesta antes de resolver
  nada, exige un único segmento terminado en `.ics` y sin parámetros, y responde 405 con
  `Allow: GET, HEAD` para el resto de métodos **sin llegar a leer el candidato**, de modo que un
  token válido y uno inexistente producen respuestas idénticas (@s16).
- `CalendarFeedController`: estado, generación (cuerpo vacío obligatorio, 400 `VALIDATION_ERROR` en
  cuanto hay un solo octeto), revocación 204 y descarga con
  `Content-Disposition: attachment; filename="organizationweb-bloques.ics"`.
- `CalendarDocuments`: `text/calendar; charset=utf-8`, `Cache-Control: private, no-store`,
  `X-Content-Type-Options: nosniff`, `Content-Length` en octetos UTF-8 y cuerpo vacío en HEAD.
- `SecurityConfiguration`: nueva cadena `@Order(0)` para `/calendar/**` (permitAll, sin CSRF, sin
  sesión, `STATELESS`), y la cadena Bearer de la feature 24 deja de reclamar las rutas de
  calendario, para que sin sesión respondan `401 UNAUTHENTICATED` y no `API_UNAUTHENTICATED` (@s7).

Segundo rojo, este de comportamiento y muy instructivo: `@s15`, `@s26` y `@s30` fallaban con
`Content-Disposition: inline;filename=f.txt`. Es la protección contra *Reflected File Download* de
`AbstractMessageConverterMethodProcessor`, que añade esa cabecera a toda respuesta serializada por
un conversor cuya ruta termina en una extensión no segura — es decir, a todos nuestros `.ics`. Hacía
que el 404 de `/calendar/<token>.ics` y el de `/calendar/<token>` (sin extensión) **no** fueran
idénticos, justo lo que @s15 prohíbe.

Verde: `CalendarProblems` escribe los problemas de calendario directamente en la respuesta
(status, `application/problem+json`, `Content-Length`) sin pasar por el conversor; los manejadores
`@ExceptionHandler` locales de ambos controladores devuelven `void`. Se retiran de `ApiErrors` los
dos manejadores globales de calendario que habían quedado sin uso.

### Ciclo 6 — @s28 (una lectura, un snapshot) y cableado

Rojo: `SnapshotRenderCalendarTest` no compilaba; después `CalendarWiringTest` falló con
`NoSuchBeanDefinitionException: ManageCalendarFeedUseCase`.

Verde: `adapter/persistence/SnapshotRenderCalendar` decora `RenderCalendarUseCase` con una
`TransactionTemplate` read-only `REPEATABLE_READ`; como las plantillas de `PostgresCalendarStore`
usan propagación `REQUIRED`, la resolución del token, la zona y los bloques quedan dentro de la
misma transacción. Beans `manageCalendarFeed` y `renderCalendar` en `ApplicationConfiguration`.

Comandos:
```
--tests "…adapter.CalendarApiTest"                       → 27 pruebas, 0 fallos
--tests "…adapter.persistence.SnapshotRenderCalendarTest" \
--tests "…adapter.config.CalendarWiringTest"             → BUILD SUCCESSFUL
--tests "…ArchitectureTest" --tests "…SecurityConfigurationTest" → BUILD SUCCESSFUL
```

### Ciclo 7 — @s9 (dos regeneraciones a la vez) y @s11 (silencio en los logs)

Rojo: `s9_twoConcurrentRegenerationsLeaveExactlyOneToken` y
`s11_s15_theRequestLeavesNoTokenAndNoCalendarPathInTheLogs` antes de existir el comportamiento.

Verde: no hizo falta código nuevo. El `INSERT … ON CONFLICT (owner_id) DO UPDATE` ya deja una sola
fila cuando dos hilos escriben a la vez y sólo uno de los dos hashes resuelve después; y ninguna
ruta de calendario registra nada, porque los 404 y 413 se resuelven con manejadores propios que no
tocan el `logger` de `ApiErrors`. Las dos pruebas confirman propiedades que el diseño ya garantiza
y las dejan protegidas frente a regresiones (si alguien cambiara `ON CONFLICT` por dos sentencias,
o devolviera el 404 por el manejador genérico que sí escribe una línea de log, se pondrían rojas).

### Ciclo 8 — @s31–@s37 (módulo de cliente y vista /calendario)

Rojo 1: `calendar-feed-api.test.ts` (13 pruebas) no resolvía el módulo
(`Failed to resolve import "./calendar-feed-api"`).

Verde: `calendar-feed-api.ts` con `readCalendarFeed`, `createCalendarFeed`, `revokeCalendarFeed` y
`readCalendarFile`. Cada uno valida la forma exacta de la respuesta con los ayudantes `exact` e
`instant` que ya usa el resto del cliente: el estado debe traer exactamente `active` y `createdAt`
y ser coherente (activo con instante, inactivo con `null`); la creación debe traer exactamente
`url` y `createdAt` y la url debe ser la dirección pública del feed; la descarga exige
`text/calendar; charset=utf-8`, `Content-Length` que coincida con los octetos recibidos y un cuerpo
que empiece por `BEGIN:VCALENDAR\r\n` y acabe en `END:VCALENDAR\r\n` antes de devolver nada. El 413
se propaga como `Response` para que la vista explique el límite.

Un rojo intermedio útil: `toEqual` sobre dos `Uint8Array` fallaba con «Compared values have no
visual difference» en jsdom; se compara con `Array.from` en ambos lados.

Rojo 2: `calendar.test.tsx` (25 pruebas) no resolvía `./calendar`.

Verde: `calendar.tsx`. Un único `AbortController` por vista (`run(...)` centraliza ocupado, fallo,
descarte de respuestas tardías y aborto al desmontar), confirmación inline con `role="group"` que
recibe el foco, portapapeles nativo sin fingir éxito, descarga validada antes de crear el `Blob` y
foco al h1 sólo si el control iniciador desapareció y la persona no movió el foco. `key={owner}` en
`Calendar` hace que cambiar de identidad reinicie la vista entera (@s37).

Rojo 3: la entrada de navegación. `Unable to find an accessible element with the role "link" and
name "Calendario"`.

Verde: ruta `/calendario` y sección «Calendario» en `App.tsx`, entrada `RouteLink` en
`workspace.tsx` justo después de «Exportación», y estilos `.calendar-feed` en `styles.scss`
(objetivos de 44 px, campo de url a ancho completo sin desbordar, tokens de tema).

Efecto colateral honesto: `export-data.test.tsx` y `appearance.test.tsx` afirmaban la posición de
«Exportación» y «Apariencia» contando desde el final de la navegación. Insertar «Calendario» las
desplaza una posición; se han corregido los índices y se ha añadido la afirmación de que la nueva
entrada está justo después de «Exportación», que es lo que exige @s31.

Nota de honestidad sobre este ciclo: las 25 pruebas de la vista se escribieron juntas y pasaron en
la primera ejecución tras implementar el componente. El rojo fue de resolución de módulo, no de
comportamiento prueba a prueba; es un ciclo más grueso que los del backend. Lo compensa la
mutación de Stryker, que dirá si alguna de esas 25 no está realmente sujetando nada.

Comandos:
```
pnpm vitest run src/calendar-feed-api.test.ts   → 13 pruebas
pnpm vitest run src/calendar.test.tsx           → 26 pruebas
pnpm vitest run src/export-data.test.tsx src/appearance.test.tsx → 72 pruebas
pnpm exec eslint src/ ; pnpm exec prettier --check src/ ; pnpm exec tsc --noEmit → limpio
```

### Ciclo 9 — infraestructura de cierre (nginx, mutación, E2E, documentación)

- `deploy/nginx.conf`: `location /calendar/` enruta al backend con `access_log off`. El token viaja
  en la ruta, así que no puede aparecer en un log de acceso; además, sin este bloque el `try_files`
  del SPA devolvería `index.html` en vez del calendario. Esto cierra la «pregunta abierta» que la
  propuesta de la feature 26 dejó pendiente de infraestructura.
- `backend/build.gradle.kts`: alcance PIT `ics_calendar` (dominio, casos de uso, los dos
  controladores, `CalendarDocuments`, `CalendarProblems`, `CalendarPaths`, `PostgresCalendarStore`,
  `SnapshotRenderCalendar` y `ApplicationConfiguration`), con su `reportDir` propio y añadido a la
  unión del perfil por defecto.
- `frontend/stryker.ics-calendar.config.json` y los destinos `ics_calendar-backend` y
  `ics_calendar-frontend` en `scripts/project.mjs`, con tres pruebas nuevas en
  `scripts/project.test.mjs`. `node --test scripts/project.test.mjs` deja los mismos 3 fallos que
  ya traía la rama antes de tocar nada (dos de `integration_api` y uno de `appearance`); ninguno es
  de este carril.
- `docs/ics-calendar.md`.

## Mapa de trazabilidad @s a prueba

Backend, 96 pruebas en 8 clases. Frontend, 40 pruebas en 2 ficheros. (Corregido tras el dictamen:
la cifra que dio esta bitacora al cerrar el ciclo 9, 95 y 39, estaba mal contada; el juez las
ejecuto y conto 96 y 40, y son las buenas. Con la fila OPTIONS del ciclo 15 el backend pasa a 97.)

| @s | Prueba |
| --- | --- |
| @s1 | `CalendarFeedUseCasesTest.s1_generateReturnsTheUrlOnceAndStoresOnlyTheFingerprint`, `CalendarPersistenceTest.s1_s29_onlyTheThirtyTwoOctetFingerprintIsStoredAndOutlivesTheStore`, `CalendarApiTest.s1_creatingTheLinkAnswersOnlyUrlAndCreatedAt`, `CalendarWiringTest` |
| @s2 | `CalendarFeedUseCasesTest.s2_statusWithoutTokenIsInactiveAndWritesNothing`, `CalendarApiTest.s2_statusWithoutLinkIsInactive` |
| @s3 | `CalendarFeedUseCasesTest.s3_statusWithTokenExposesOnlyTheCreationInstant`, `CalendarApiTest.s3_statusWithLinkNeverRevealsTheTokenOrItsDigest` |
| @s4 | `CalendarFeedUseCasesTest.s4_regeneratingKeepsOneRowAndInvalidatesThePreviousToken`, `CalendarPersistenceTest.s4_regeneratingLeavesOneRowAndTheOldFingerprintResolvesToNobody`, `e2e/ics-calendar.spec.mjs` (regeneración) |
| @s5 | `CalendarFeedUseCasesTest.s5_revokeIsIdempotentAndLeavesNoRow`, `CalendarPersistenceTest.s5_revokeDeletesTheRowAndRepeatingItChangesNothing`, `CalendarApiTest.s5_revokingAnswersTwoHundredFourWithoutBody` |
| @s6 | `CalendarApiTest.s6_aNonEmptyGenerationBodyIsRejectedWithoutTouchingTheToken` (5 filas) |
| @s7 | `CalendarApiTest.s7_withoutSessionNoManagementRouteAnswersData` (4 rutas por con y sin Bearer) |
| @s8 | `CalendarApiTest.s8_generatingAndRevokingKeepCsrfAndOrigin`, `CalendarApiTest.s8_s11_thePublicFeedNeedsNoCredentialsAndCarriesTheExactHeaders` |
| @s9 | `CalendarPersistenceTest.s9_twoConcurrentRegenerationsLeaveExactlyOneToken` |
| @s10 | `CalendarFeedUseCasesTest.s10_aFailedWriteIsAttemptedOnceAndNeverRetried`, `CalendarPersistenceTest.s10_aColludingFingerprintFailsOnceAndKeepsThePreviousToken`, `CalendarApiTest.s10_aFailedGenerationAnswersFiveHundredThreeWithoutUrl` |
| @s11 | `CalendarApiTest.s8_s11_...`, `CalendarApiTest.s11_headAnswersTheSameHeadersWithAnEmptyBody`, `CalendarApiTest.s11_s15_theRequestLeavesNoTokenAndNoCalendarPathInTheLogs` |
| @s12 | `IcsCalendarTest` (documento de 714 octetos byte a byte), `CalendarPersistenceTest.s12_aPlannedBlockWithoutProjectionCarriesVersionOneAndItsCreationStamp`, `e2e/ics-calendar.spec.mjs` |
| @s13 | `IcsCalendarTest` (158 octetos, 7 líneas, sin VEVENT ni X-WR-TIMEZONE) |
| @s14 | `IcsCalendarTest` (las tres filas de disponibilidad), `CalendarPersistenceTest.s14_theZoneIsTheAvailabilityZoneAndIsAbsentWithoutAvailability` |
| @s15 | `CalendarFeedUseCasesTest.s15_anUnknownCandidateIsNotFoundAndNeverReachesTheCalendar`, `CalendarPersistenceTest.s15_anUnknownFingerprintResolvesToNobodyWithoutWriting`, `CalendarApiTest.s15_everyUnresolvableAddressAnswersTheSameNotFound` (8 direcciones), `SnapshotRenderCalendarTest.s15_s30_aFailureRollsBackAndTravelsUnchanged` |
| @s16 | `CalendarApiTest.s16_thePublicResourceOnlyAcceptsGetAndHead` (4 métodos, token conocido y desconocido) |
| @s17 | `CalendarFeedUseCasesTest.s17_theDownloadRendersTheOwnerFeedWithoutTouchingAnyToken`, `CalendarApiTest.s17_theSessionDownloadIsTheSameDocumentAsAnAttachment` (con y sin token) |
| @s18 | `CalendarWindowTest` (las seis filas exactas del Examples), `CalendarFeedUseCasesTest.s18_theWindowIsThirtyDaysBackAndThreeHundredSixtyFiveDaysForward`, `CalendarPersistenceTest.s18_theWindowIsSemiOpenOnBothEnds` |
| @s19 | `CalendarPersistenceTest.s19_aCancelledBlockLeavesTheFeedButNotTheDatabase` |
| @s20 | `CalendarPersistenceTest.s20_aMovedBlockPublishesTheProjectionIntervalVersionAndStamp` |
| @s21 | `CalendarPersistenceTest.s21_completedOrPausedOwnersStillPublishTheirBlocksWithTheTaskTitle` |
| @s22 | `IcsCalendarTest` (los dos lados del cambio de hora en UTC, sin TZID) |
| @s23 | `IcsCalendarTest` (escapes y viaje de ida y vuelta, incluidos 500 puntos de código) |
| @s24 | `IcsCalendarTest` (las cinco fronteras de plegado) |
| @s25 | `IcsCalendarTest.s25_eventsAreOrderedByStartThenUidRegardlessOfInput` |
| @s26 | `CalendarFeedUseCasesTest.s26_twoThousandEventsStillRender` y `s26_moreThanTwoThousandEventsAreRefusedBeforeAnyDocumentExists`, `CalendarApiTest.s26_tooManyEventsAnswerThirteenWithoutPartialCalendar` |
| @s27 | `CalendarFeedUseCasesTest.s27_atokenOnlyResolvesTheFeedOfItsOwner`, `CalendarPersistenceTest.s27_theFeedOfAnOwnerNeverContainsBlocksOfAnother` |
| @s28 | `CalendarPersistenceTest.s28_zoneAndBlocksComeFromASingleRepeatableReadSnapshot`, `SnapshotRenderCalendarTest.s28_bothReadsRunInsideOneReadOnlyRepeatableReadTransaction` |
| @s29 | `CalendarPersistenceTest.s1_s29_...` (una tienda nueva lee el token persistido), `e2e/ics-calendar.spec.mjs` (reinicio real del backend) |
| @s30 | `CalendarFeedUseCasesTest.s30_anUnavailableStoreNeverProducesAPartialDocument`, `CalendarPersistenceTest.s30_anUnreachableDatabaseIsReportedAsStorageUnavailable`, `CalendarApiTest.s30_anUnavailableStoreAnswersFiveHundredThreeOnEveryRoute` (las tres rutas) |
| @s31 | `calendar-feed-api.test.ts` (3 pruebas de estado), `calendar.test.tsx` (carga, sin enlace, enlace activo, fallo con reintento, entrada de navegación) |
| @s32 | `calendar-feed-api.test.ts` (2 pruebas de creación), `calendar.test.tsx` (doble clic con un solo POST, anuncio, olvido tras recargar) |
| @s33 | `calendar.test.tsx` (portapapeles disponible, que rechaza y ausente) |
| @s34 | `calendar.test.tsx` (confirmación de regeneración y de revocación, con foco y sin petición) |
| @s35 | `calendar-feed-api.test.ts` (DELETE sólo acepta 204), `calendar.test.tsx` (cancelar, confirmar regeneración, confirmar revocación, foco al h1, fallo sin reintento automático) |
| @s36 | `calendar-feed-api.test.ts` (6 pruebas de validación y 413), `calendar.test.tsx` (preparación válida, tres respuestas inválidas, límite) |
| @s37 | `calendar-feed-api.test.ts` (señal ya abortada), `calendar.test.tsx` (aborto al salir, 401 tardío descartado, nada en almacenamiento ni consola) |
| @s38 | `e2e/ics-calendar-ux.spec.mjs`: los **siete** estados a 320/768/1280, texto al 200 %,
zoom nativo al 200 %, tema claro/oscuro, `forced-colors`, movimiento reducido, recorrido de teclado
y seleccion del campo; matriz de los treinta principios en `progress/ux_ics_calendar.md`. Ver el
ciclo 16. (Antes decia «cinco estados»: eran **cuatro**, y el juez lo conto bien.) |

## Lo que este carril NO ha ejecutado

Por la disciplina de recursos de la sesión (cinco carriles compartiendo la máquina) no se han
lanzado ni la suite completa del backend, ni la suite completa de Vitest, ni Playwright, ni PIT, ni
Stryker. En consecuencia:

- (Superado en el ciclo 14, que lo ejecuto, y en el 16, que lo amplia.) `e2e/ics-calendar.spec.mjs`
  está escrito y pasa `node --check`, pero nunca se ha ejecutado. @s38 y
  la parte E2E de @s29 siguen sin evidencia. Es lo primero que debe correr quien integre.
- Los rangos linea:columna de `stryker.ics-calendar.config.json` sobre `App.tsx` y `workspace.tsx`
  se han calculado leyendo el fichero, no ejecutando Stryker; si alguien reformatea esos ficheros
  habrá que recalcularlos.
- La mutación (PIT y Stryker) queda para el `mutation_tester`.
- El estado de la feature 26 en `feature_list.json` esta en **`spec_ready`**, no en `in_progress`
  como afirmo esta bitacora: el commit que lo cambiaba se descarto en el reasentamiento del ciclo
  11. Lo corrige el coordinador; a este agente no le corresponde ni eso ni marcar `done`.

### Ciclo 10 — regresión encontrada en `ApplicationWiringTest`

Al cerrar el carril se ejecutó `ApplicationWiringTest` (que no es de este carril) y sus 24 pruebas
estaban en rojo por mi culpa:

```
NoSuchBeanDefinitionException: No qualifying bean of type
'com.apptolast.organization.application.CalendarFeedTokens'
  ... creating bean 'manageCalendarFeed' defined in ApplicationConfiguration
```

Causa: `PostgresCalendarStore` se había anotado `@Component`, pero `ApplicationWiringTest` levanta
un contexto estrecho con `withUserConfiguration(ApplicationConfiguration.class)` y **sin escaneo de
componentes**. Todo adaptador que un bean de `ApplicationConfiguration` necesite tiene que estar
declarado allí como `@Bean`, igual que `exportDataQueries`, `historyQueries` o
`apiCredentialStore`.

Verde: se retira `@Component` de `PostgresCalendarStore` y se declara el bean `calendarStore(jdbc,
manager)` en `ApplicationConfiguration`. Un único bean sirve a los dos puertos (`CalendarFeedTokens`
y `CalendarQueries`), que es justo lo que hace falta para que la lectura del feed salga de un solo
snapshot.

Comprobado después del cambio: `ApplicationWiringTest` (24), `CalendarWiringTest`,
`CalendarPersistenceTest`, `CalendarApiTest`, `ArchitectureTest` y `SecurityConfigurationTest`, más
`ApiCredentialApiTest`, `ApiCredentialBearerTest`, `ApiCredentialBusinessCompatibilityTest` y
`ExportDataApiTest` para descartar regresiones en los carriles vecinos. Todo verde.

### Ciclo 11 — reasentamiento sobre `origin/main` (7ea682d) y lo que salió a la luz

`main` se había reescrito (la feature 24 entró como squash `0277c50`), así que un
`git rebase origin/main` intentaba reproducir 108 commits ya presentes aguas arriba. Se abortó y se
reasentaron sólo los ocho commits propios:

```
git rebase --onto origin/main 4c58a5a claude/ics-calendar
```

Se descarta `4c58a5a` (contrato Gherkin): `features/ics_calendar.feature` y
`progress/gherkin_ics_calendar.md` ya están en `main` byte a byte; lo único que aportaba era el
cambio de estado en `feature_list.json`, que no me corresponde tocar.

Dos conflictos, ambos resueltos conservando las dos partes:

1. `SecurityConfiguration.java`. La feature 24 (hallazgo A8) añadió `securityHeaders(...)` con el
   literal de CSP y `Referrer-Policy: same-origin` en sus dos cadenas; yo había cambiado el
   `securityMatcher` de la cadena Bearer para dejar fuera las rutas de calendario. La resolución
   mantiene **las dos**: el matcher excluye el calendario y la cadena sigue llamando a
   `.headers(SecurityConfiguration::securityHeaders)`.
2. `scripts/project.mjs`. La lista blanca de destinos de mutación: se conservan los tres de
   `integration_api` y se añaden los dos de `ics_calendar`.

`deploy/nginx.conf` no dio conflicto y el literal de CSP que la 24 dejó atado sigue intacto; lo
único que añado es el bloque `location /calendar/` con `access_log off`.

### Ciclo 12 — @s11 @s15: la cadena del feed también emite las cabeceras de la 24

El reasentamiento destapó un hueco real que no estaba tapado por ninguna prueba: mi cadena
`@Order(0)` para `/calendar/**` **no** emitía `Content-Security-Policy` ni `Referrer-Policy`. La
postura de la 24 dice que la emiten todas las cadenas, no sólo dos; con mi cambio había tres.

Rojo: `CalendarApiTest.s11_s15_thePublicChainAlsoEmitsTheSecurityHeadersOfTheWholeApi`, que pide
las dos cabeceras sobre el feed servido y sobre su 404.

```
CalendarApiTest > s11_s15_thePublicChainAlsoEmitsTheSecurityHeadersOfTheWholeApi() FAILED
29 tests completed, 1 failed
```

Verde: `.headers(SecurityConfiguration::securityHeaders)` en `publicCalendarSecurity`. No rompe
nada del contrato: @s11 sólo prohíbe `Content-Disposition`, `ETag`, `Set-Cookie` y un
`Content-Encoding` distinto de identity, y @s15 exige que el conjunto de cabeceras sea idéntico
entre filas, cosa que se mantiene porque todas las respuestas de la cadena las reciben igual.

Evidencia pedida por el coordinador, ejecutada tras la resolución:
```
--tests "…adapter.SecurityHeadersTest"        BUILD SUCCESSFUL
--tests "…adapter.ApiCredentialBearer*"       BUILD SUCCESSFUL
--tests "…adapter.CalendarApiTest"            BUILD SUCCESSFUL (29 pruebas)
--tests "…ApplicationWiringTest"              BUILD SUCCESSFUL
--tests "…CalendarWiringTest"                 BUILD SUCCESSFUL
```

### Ciclo 13 — rectificación: el fallo de `project.test.mjs` sí era mío

Antes del reasentamiento informé de «3 fallos preexistentes» en `node --test
scripts/project.test.mjs` y dije que ninguno era de este carril. **Me equivoqué en uno.** Dos eran
de `integration_api` y `main` ya los ha arreglado, pero
`appearance Stryker preserves all candidates and reviewed integration nodes` fallaba por mi culpa:
esa prueba comprueba que los rangos `línea:columna` de `stryker.appearance.config.json` sobre
`App.tsx` y `workspace.tsx` siguen apuntando al código de apariencia, y mi entrada de navegación y
mi rama de ruta desplazaron esas líneas. La comprobación de base que hice con `git stash` no lo
detectó porque esas ediciones ya estaban **commiteadas**, así que el «antes» también las tenía.

Verde: recalculados los cuatro rangos de apariencia (`App.tsx:33:8-33:44`, `53:18-65:34`,
`84:10-125:7`, `workspace.tsx:78:10-83:22`) en el config y en la expectativa de la prueba,
verificando con la misma extracción que hace la prueba que cada uno empieza y contiene lo que debe.

Además se recalcularon mis propios rangos, que se habían calculado antes del reasentamiento y
habían quedado desplazados, y se añade `ics calendar Stryker selects its own nodes of the shared
files`: la misma guarda que tiene apariencia, para que un desplazamiento futuro de `App.tsx` o
`workspace.tsx` salga en rojo en vez de mutar código ajeno en silencio.

`node --test scripts/project.test.mjs` → 76 pruebas, 0 fallos.

Frontend tras el reasentamiento (modo oscuro incluido): `theme-tokens` 14, `theme-color` 4,
`appearance` 50, `export-data` 22, `calendar` 26, `calendar-feed-api` 13, `App` 16; `eslint`,
`prettier --check` y `tsc --noEmit` limpios. El bloque `.calendar-feed` de `styles.scss` pasa la
guarda global de colores fijos del modo oscuro porque sólo usa tokens (`$ink`, `var(--panel)`,
`var(--editable)`, `var(--control-border)`, `var(--accent)`, `var(--line)`, `var(--warning)`,
`var(--error)`).

### Ciclo 14 — el E2E ejecutado por fin, y los tres defectos que destapó

`E2E_WEB_PORT=18092 pnpm test:e2e e2e/ics-calendar.spec.mjs`, una sola pila, bajada al terminar.
Primera ejecución: **3 de 4 en rojo**. Ninguno era ruido; los tres apuntaban a defectos reales.

**Defecto 1 (producto, frontend).** El POST de creación fallaba y la vista mostraba «No se pudo
completar la operación». Causa: `calendar-feed-api.ts` validaba la url devuelta con
`/^https:\/\/…/`, con el esquema **fijo a https**. La pila E2E sirve en
`http://127.0.0.1:18092`, así que el cliente rechazaba una respuesta perfectamente válida. Habría
roto también cualquier despliegue local o interno sin TLS. Rojo primero en
`calendar-feed-api.test.ts` («@s32 accepts the address of a deployment served over plain http»),
verde relajando el esquema a `https?` y manteniendo el resto de la forma que fija el contrato.

**Defecto 2 (mi propio E2E).** Las cuatro pruebas compartían el token del propietario: el arnés
`authenticated` limpia customización, no `calendar_feed_tokens`, así que la segunda prueba abría la
vista ya en estado «enlace activo» y no encontraba «Crear enlace de suscripción». Corregido con
`withoutFeedToken()` al principio de cada preparación, en vez de heredar el estado de la anterior.
Se corrigió además una lectura sin espera (`inputValue()` justo tras el clic) por la aserción
web-first `await expect(field).not.toHaveValue(first)`.

**Defecto 3 (despliegue, real).** El feed respondía `X-Content-Type-Options: nosniff, nosniff`.
`deploy/nginx.conf` añade la postura de seguridad con `add_header` a nivel de `server`, y desde el
hallazgo A8 el backend emite las mismas cabeceras; `add_header` **suma**, no sustituye. Corregido en
mi `location /calendar/` con `proxy_hide_header` de las tres (`X-Content-Type-Options`,
`Content-Security-Policy`, `Referrer-Policy`), de modo que llega exactamente una copia de cada una
venga o no de un proxy. El E2E ahora lo comprueba explícitamente.

**Desviación registrada, no tapada.** El contrato de @s11 cita
`Content-Type: text/calendar; charset=utf-8` y dice que las cabeceras se comparan exactas. Sobre
Tomcat la respuesta real es `text/calendar;charset=utf-8`, **sin el espacio**: `Response.setContentType`
parsea el tipo y lo reserializa, y ninguna capa de la aplicación puede evitarlo
(`setHeader("Content-Type", …)` desemboca en el mismo `setContentType`). El espacio es OWS opcional
según RFC 9110 y el tipo es semánticamente idéntico, así que se deja constancia en vez de
maquillarlo: la prueba MockMvc sigue fijando lo que el controlador pide y el E2E afirma lo que el
contenedor entrega, con el porqué escrito al lado. **Queda a criterio del juez** si esto exige una
enmienda al contrato.

**Hallazgo fuera de mi alcance, no tocado.** El mismo `add_header` duplicado afecta a `location
/api/`: desde A8 el backend emite CSP y `Referrer-Policy` y nginx las vuelve a añadir, así que las
rutas `/api/` deberían estar devolviendo esas tres cabeceras por duplicado. No lo he tocado para no
alterar el comportamiento que el juez de la feature 24 ya revisó; lo reporto al coordinador.

Resultado final, con la pila levantada y bajada:
```
✓ ics: an anonymous client reads the feed … @s11 @s12 @s15 @s16 @s27 @s32 @s35   (2,3 s)
✓ ics: regenerating invalidates the previous address and a restart keeps … @s4 @s29 @s35 (9,5 s)
✓ ics: the session download offers the same document as an attachment @s17 @s36  (2,2 s)
✓ ics: the view is operable and free of axe violations in every state and width @s38 (6,8 s)
4 passed (22,4 s)
```

Con esto **@s38 y la parte de reinicio de @s29 dejan de estar sin demostrar**: axe sin violaciones
en cuatro estados a 320, 768 y 1280 px (los siete llegan en el ciclo 16), sin desbordamiento
horizontal, y el feed devuelve los mismos
octetos después de reiniciar el proceso del backend contra la misma base de datos.

Sigue pendiente, y no me corresponde: la mutación (PIT `ics_calendar` y Stryker
`ics_calendar-frontend`) y el cambio de estado de la feature 26.

---

# Respuesta al dictamen REJECTED (`progress/judge_ics_calendar.md`)

El juez acepta el carril entero salvo **@s38**, y tiene razón: lo que había auditaba **cuatro** de
los siete estados, nunca ejecutaba texto al 200 % ni zoom nativo ni los modos de presentación, el
oráculo de teclado era un `Tab` suelto con `toBeTruthy()`, y el artefacto de los treinta principios
no existía. Se cierra de verdad.

### Ciclo 15 — el mapeo de OPTIONS sin oráculo (hallazgo menor del dictamen)

`PublicCalendarController` declaraba `RequestMethod.OPTIONS` en el mapeo del 405 y ninguna prueba lo
pedía. Se añade la fila `OPTIONS` al `@ValueSource` de
`CalendarApiTest.s16_thePublicResourceOnlyAcceptsGetAndHead`, con un comentario que distingue las
cuatro filas del Examples de la quinta.

**Honestidad sobre este ciclo: la fila no nació roja.** La producción ya existía y ya se comportaba
así; lo que se cierra es una línea sin oráculo, que es exactamente lo que pedía el punto 4 del
dictamen. Se deja dicho en vez de presentarlo como un rojo→verde que no fue.
`CalendarApiTest` pasa de 29 a 30 pruebas; el backend del carril, de 96 a 97.

### Ciclo 16 — @s38 cerrado: los siete estados y las cuatro modalidades

**Rojo de producto encontrado por el propio escenario.** El campo de la url era un `input` con
`text-overflow: ellipsis`. Eso es literalmente «recortar la url», que es lo que la cláusula
«ningún ancho recorta la url» prohíbe, y el juez ya lo había señalado como fragilidad. Se sustituye
por un `textarea` de solo lectura que envuelve (`overflow-wrap: anywhere`, `white-space: pre-wrap`):
a 320 px y con el texto al 200 % la url de 43 caracteres se lee entera. El oráculo nuevo es
`textarea.scrollWidth > clientWidth`, medido en las 42 combinaciones y bajo zoom nativo. Las 40
pruebas de Vitest siguen verdes sin tocarlas: `getByRole("textbox")` y `toHaveValue` valen igual
para un `textarea`.

**Spec nueva `e2e/ics-calendar-ux.spec.mjs`**, y el antiguo `@s38` de cuatro estados se retira de
`ics-calendar.spec.mjs` para que haya un único oráculo autoritativo:

1. *Los siete estados a 320, 768 y 1280 px* — 21 medidas. `cargando` y `fallo` se provocan
   interceptando `GET /api/v1/me/calendar-feed`, la única petición de apertura; `con enlace activo`
   se alcanza recargando tras crear, que es el estado sin campo de url y distinto del recién creado.
   Cada medida: axe (5 etiquetas), desbordamiento, controles fuera del viewport, objetivos de 44 px
   medidos con `getBoundingClientRect` (sin delegar en `target-size` de axe) y recorte de la url.
2. *Los siete estados con el texto al 200 %* — otras 21 medidas, duplicando el tamaño de letra
   calculado y verificando elemento a elemento que el factor es exactamente 2, como
   `reschedule-text` y `appearance-ux-audit`. Con captura por estado.
3. *Tema claro, tema oscuro, `forced-colors` y movimiento reducido* — 12 medidas.
4. *Recorrido de teclado real* — orden de tabulación igual al orden del DOM, nombre accesible y
   foco visible en cada parada, salida de `main` hacia delante y hacia atrás, y selección íntegra
   del campo con `Ctrl/Cmd+A`.
5. *Zoom nativo de Chromium al 200 %* — `chrome.tabs.setZoom(tab, 2)` real sobre contexto
   persistente con extensión, como `reschedule-native-zoom`; no viewport emulado ni zoom CSS.

**Tres rojos propios durante el ciclo, los tres arreglados en la causa:**

- *Fuga de estado entre medidas.* Las dos pruebas grandes agotaron los 180 s: `plannedBlock` se
  llamaba una vez por estado y no por medida, así que el token creado en el primer ancho sobrevivía
  al segundo y ya no había botón «Crear enlace de suscripción». Ahora cada medida arranca con
  `withoutFeedToken()`. Las dos pruebas pasan de agotar el tiempo a 22 s y 26 s.
- *Fuga entre modos de presentación.* `page.emulateMedia` **conserva** lo que no se le nombra, así
  que `forced-colors` seguía activo durante la pasada de movimiento reducido: la evidencia mostraba
  tinta blanca donde debía haber tinta verde oscura. Cada modo fija ahora las tres preferencias.
  Sin el arreglo, la prueba habría pasado igual midiendo el modo equivocado — el peor tipo de verde.
- *Oráculo de trampa de foco al revés.* Comprobaba que tras `Shift+Tab` el foco seguía en `main`,
  que es lo normal y no demuestra nada. Ahora afirma que tabulando hacia delante se sale de `main`
  tras el último control **y** que desde el primero `Shift+Tab` también sale.

**Resultado, una sola pila con `E2E_WEB_PORT=18092`, bajada al terminar:**

```
✓ ics ux: los siete estados se sostienen a 320, 768 y 1280 px @s38            (22,0 s)
✓ ics ux: los siete estados se sostienen con el texto al 200 % @s38           (26,2 s)
✓ ics ux: tema claro, tema oscuro, forced-colors y movimiento reducido @s38   (12,4 s)
✓ ics ux: el recorrido de teclado alcanza todo en orden, con foco visible …   ( 2,8 s)
✓ ics ux: zoom nativo de Chromium al 200 % con 320 px CSS @s38                ( 3,7 s)
5 passed
```

Y las dos specs juntas en una sola pila: **8 passed (1,3 m)**.

Medidas que quedan escritas: sin desbordamiento ni recorte en las 42 combinaciones; control más
ancho con texto al 200 %, 889,8 px a 1280; claro `rgb(35,57,47)` sobre `rgb(248,249,245)`, oscuro
`rgb(232,238,233)` sobre `rgb(17,24,39)`; todas las transiciones ≤ 0,01 s con movimiento reducido;
zoom nativo con `devicePixelRatio` duplicado y 373 px CSS de ancho útil.

### Ciclo 17 — `progress/ux_ics_calendar.md`

Escrito siguiendo `progress/ux_integration_api.md`: qué se ejecutó, las medidas concretas, la matriz
de los treinta principios de `docs/ux-requirements.md` y —lo que pedía `AGENTS.md:51`— una sección
de **límites explícitos**: sin pruebas con personas, axe como condición necesaria y no suficiente,
contraste forzado como revisión visual y no medida, zoom nativo sólo en Chromium, un solo motor, sin
cronometrar Doherty, sin lector de pantalla real y sin los anchos 1440/1920.

### Correcciones de bitácora exigidas por el dictamen

- «95 pruebas» → **96** y «39 del frontend» → **40**, que son las que el juez ejecutó y contó.
  Con la fila `OPTIONS` del ciclo 15 el backend queda en **97**.
- «cinco estados» → **cuatro**, que es lo que auditaba la spec vieja contando las llamadas a
  `audit(...)`. Los siete llegan con el ciclo 16.
- «la feature 26 sigue en `in_progress`» → está en **`spec_ready`**; el commit que la ponía
  `in_progress` se descartó en el reasentamiento del ciclo 11. Lo corrige el coordinador.
- La nota del ciclo 9 que decía que el E2E nunca se había ejecutado queda marcada como superada.

### Lo que sigue sin ser mío

La desviación del `Content-Type` (el juez la acepta y la enmienda del `.feature` va por la puerta
del propietario), el `location /api/` que duplica tres cabeceras (deuda de la feature 24, carril
propio), el estado en `feature_list.json`, la mutación y `bin/harness init`.

---

# Respuesta a la campaña de mutación frontend (69,97 % con umbral 80 %)

`progress/mutation_ics_calendar_frontend.md` enumera 115 mutantes no muertos: 5 equivalentes
argumentados por el `mutation_tester` y **110 huecos reales**. El informe es preciso y se ha
trabajado con la lista delante, entrada por entrada.

Las pruebas nuevas **no cambian ni una línea de producción**: `git diff --stat` sólo toca los dos
ficheros de prueba. (Matiz añadido después: eso es cierto de **este** commit, pero el anterior del
carril, `a158cf2`, sí cambió producción — el `<input>` del enlace pasó a `<textarea>` — y por eso el
denominador de la campaña sube de 383 a 384. Ver la corrección al pie.) Era de esperar y conviene decirlo — un mutante que sobrevive no es un defecto,
es una conducta correcta sin oráculo. Por eso **ninguna de estas pruebas nació roja contra el código
actual**, y en vez de fiarme de eso he verificado que discriminan de la única forma honesta
disponible: mutando a mano las líneas que deben proteger.

### Ciclo 18 — el cliente (`calendar-feed-api.ts`, 32 entradas)

Las 32 quedan cubiertas. Lo importante no es la cifra sino la número 20, que **ya me había mordido**:
todas las pruebas usaban `text/calendar; charset=utf-8` **con espacio** y el ciclo 14 documenta que
Tomcat entrega la forma **sin espacio**. Ninguna prueba cubría la forma que produce el servidor real.
Ahora hay una fila explícita para ella, y otras cuatro que fijan que el tipo se compara entero
(otro tipo delante, parámetros detrás, subtipo pegado, otra codificación).

Además: `Content-Length` ausente, vacío, con basura delante o detrás, cero y con cero a la izquierda;
longitudes de dos y de tres cifras que **sí** deben aceptarse; UTF-8 malformado rechazado (lo que
`fatal: true` protege) y BOM rechazado (lo que `ignoreBOM: true` protege); `Accept: text/calendar` y
la señal de cancelación afirmados en la llamada; abortar tras la respuesta en las **cuatro**
operaciones; 500 en el estado y 200 en la creación entregados como `Response`; cuerpo que no es JSON;
`active` no booleano con el resto bien formado; url con basura antes del esquema o con camino
colgando detrás; url que no es una cadena pero se coacciona a la correcta; y el mensaje del error.

Verificación por mutación manual (mutar, ejecutar, restaurar; el fichero queda idéntico):

```
fatal:false                     -> MUERTO
ignoreBOM:false                 -> MUERTO
content-type exige un espacio   -> MUERTO
content-length dos cifras       -> MUERTO
sin ancla ^ en la direccion     -> MUERTO
```

### Ciclo 19 — la vista (`calendar.tsx`)

**B8, las tres ramas de reintento** (13 mutantes, doce sin cobertura). Tres pruebas: fallar la
creación y reintentar exige un segundo POST y **cero** DELETE, cero descargas y ningún GET de estado
de más; lo mismo para la revocación y para la descarga. Era, como decía el coordinador, el camino que
la persona recorre justo cuando algo ha fallado.

**B7, el 413 no es un fallo cualquiera** (8). Dos pruebas: un 503 en la descarga da el mensaje
genérico **y** el botón «Reintentar»; un 413 da el mensaje del límite **y no** ofrece reintentar,
porque repetir no lo resuelve.

**B1 y B2, el contrato de foco** (17). Cuatro pruebas. jsdom no imita al navegador aquí: cuando React
deshabilita el control que tiene el foco, un navegador real lo devuelve al `body` y jsdom lo deja
pegado a un botón deshabilitado, que además ya no se puede desenfocar. Se modela explícitamente
—foco en el `body`, `fireEvent` que no mueve el foco, y `focusin` emitidos a mano— para ejercer justo
el predicado que decide si la persona se movió. Quedan sujetos: vuelve al control si no se movió, no
vuelve si se movió, y que el foco entre en el **propio** control iniciador no cuenta como moverse.
Más una prueba de que al desmontar no queda ninguna escucha `focusin` colgando.

**B3** (4). El `h1` recibe el foco al abrir; la vista lee el estado **una sola vez** aunque vuelva a
renderizar.

**B5 y B6** (19). Anuncios «Revocando enlace…» y «Preparando archivo…» en vuelo; tras revocar no
queda enlace, ni aviso de copia, ni confirmación, ni fecha de creación; al regenerar se retira el
aviso de copia del enlace anterior; al empezar un reintento desaparece el `role="alert"` previo; y el
Blob que se ofrece a descargar es `text/calendar;charset=utf-8`.

**B9** (5). Al desmontar se revoca la url del archivo preparado, y una segunda descarga revoca la de
la primera — nadie descargaba dos veces en toda la suite.

**B10** (4). `tabindex="-1"` en el `h1` y en el grupo de confirmación (un `tabIndex` positivo es un
defecto de accesibilidad real); al enfocar el campo su contenido queda seleccionado entero; y al
confirmar desaparece la confirmación.

Verificación por mutación manual, veinte líneas de `calendar.tsx`: **19 MUERTOS, 1 SOBREVIVE**. El
superviviente es el de B4 y no lo mato: lo argumento abajo.

### Equivalentes que declaro, con argumento (12)

No los mato. Escribir una prueba que matase a estos sería relleno: no describiría ninguna conducta
que le importe a nadie.

**B4, las ocho guardas de carrera de `run` (mutantes 22–29). Equivalentes bajo dos invariantes.**
La primera: **ningún control puede iniciar una operación mientras otra está en vuelo**, porque los
que crean, regeneran, revocan y descargan llevan `disabled={Boolean(busy)}` y los de confirmar y
reintentar se desmontan al empezar. Luego `pending.current` nunca llega ocupado a `run` y
`pending.current !== controller` sólo puede ser cierto tras desmontar. La segunda:
`calendar-feed-api` llama a `signal.throwIfAborted()` después de cada `await`, así que un aborto
**siempre** sale por excepción y la guarda del camino feliz no se alcanza jamás. Y tras desmontar,
React 19 ignora las actualizaciones de estado, de modo que las guardas del `catch` y del `finally` no
tienen efecto observable. Comprobado a mano: forzar la guarda del camino feliz a `false` no cambia
nada observable, ni siquiera la creación de la object URL.

Como esa equivalencia **depende de una invariante**, la invariante queda pinchada con dos pruebas
propias («mientras una operación está en vuelo ningún control puede iniciar otra» y su gemela sin
enlace). Si alguien retira un `disabled`, se ponen rojas y este argumento caduca en voz alta.
Verificado: quitar el `disabled` del botón de descarga mata esas pruebas.

**Mutante 20**, `useState<Busy | null>("loading")` a `""`. El efecto de montaje llama a `run` y fija
`busy` antes de que nada sea observable desde una prueba de unidad. Caveat honesto: en un navegador
real hay **un fotograma** pintado sin el anuncio, porque el efecto es `useEffect` y no
`useLayoutEffect`; ninguna prueba de unidad puede verlo. Equivalente para esta suite, no en absoluto.

**Mutante 31**, `setLink(null)` dentro de `load`. `load` sólo corre al montar —donde `link` ya es
`null`— y al reintentar un fallo **de estado**, que sólo puede existir si nunca llegó a mostrarse un
enlace. Inalcanzable con efecto.

**Mutante 33**, `setConfirming(null)` dentro de `generate`. El botón de confirmar ya hace
`setConfirming(null)` antes de llamar a `generate()`. Redundante por construcción.

**Mutante 36**, `setStatus({ active: false, createdAt: null })` a `setStatus({})`. Se renderiza
idéntico: `showCreate` mira `!status.active` (`!undefined` y `!false` son ambos `true`), `showManage`
mira `status.active` (ambos falsy) y `created` es `status?.createdAt ?? null`, que da `null` en los
dos casos. Ninguna diferencia observable en el DOM.

### Lo que no es ni hueco ni equivalente (3)

Los mutantes 46, 47 y 48 (`Intl.DateTimeFormat("es", …)` y sus dos opciones) son los que hacen que la
construcción del formateador lance `RangeError` **al evaluar el módulo**. El propio informe da la
hipótesis: un mutante que revienta la importación deja el fichero de pruebas sin ejecutar y Stryker
lo anota «Survived». Las pruebas que deberían matarlos **existen** —`readable()` compara la fecha
formateada con la misma configuración— y siguen ahí. No es un hueco de las pruebas ni una
equivalencia: es un punto ciego del corredor, y no lo maquillo.

### Cuadre y lo que no puedo afirmar

De los 110 huecos reales: **95 cerrados con pruebas** (32 del cliente y 63 de la vista), **12
declarados equivalentes con argumento** y **3 atribuidos al punto ciego del corredor**. 95 + 12 + 3 =
110.

> **CORREGIDO tras la campaña final: eran 71 cerrados, no 95.** 24 de los que aquí doy por cerrados
> seguían vivos. Ver la sección «Corrección tras la campaña final» al pie de este documento, con los
> 24 enumerados uno a uno.

`src/calendar.test.tsx` pasa de 26 a 51 pruebas y `src/calendar-feed-api.test.ts` de 14 a 45: **96 en
total**, todas verdes, con `eslint`, `prettier --check` y `tsc --noEmit` limpios.

**No puedo dar el score nuevo.** Lanzar Stryker es la puerta del `mutation_tester` y esta sesión lo
tiene prohibido; lo relanza el coordinador. Lo que sí acredito es que **24 de los 25 mutantes que he
reproducido a mano mueren**, y que el que sobrevive es el que declaro equivalente con su argumento.

### Backend

La campaña de backend no llegó a arrancar: PIT aborta en cobertura porque la suite de `main` está
rota por las fixtures que hacen `TRUNCATE` enumerando tablas a mano sin las de la feature 27. No es
de este carril y no se toca; hay otro arreglándolo. La calidad de las 97 pruebas del backend de esta
feature sigue **sin medir**.

---

# Corrección tras la campaña final (340/384 = 88,54 %, salida 0)

`progress/mutation_ics_calendar_frontend_final.md` mide sobre `8527823`. La puerta se pasa, pero el
informe deja tres correcciones que **son mías** y que rectifico aquí, porque una bitácora con cifras
que no cuadran con lo medido deja de servir de mapa.

## 1. Dije 95 huecos cerrados. Son 71.

Mi cuadre era «110 = 95 cerrados + 12 equivalentes + 3 corredor». La medición dice que **de los 95
que declaré cerrados, 24 seguían vivos**. El error no fue de aritmética sino de método: di por
cerrado un hueco por el hecho de haber escrito una prueba que lo mencionaba, en vez de comprobar que
esa prueba lo mataba. Verifiqué a mano 25 mutantes y extrapolé al resto; los 25 que elegí murieron y
los que no elegí, no. Extrapolar era exactamente lo que no debía hacer.

El patrón dominante en los que fallé es el **enmascaramiento**: la prueba existe, se ejecuta y pasa,
pero afirma «esto falla» sin fijar **qué** comprobación lo rechazó, y otra comprobación posterior
atrapa el mismo caso. El ejemplo que más me duele es la tabla de `Content-Length`: seis filas que
cité como «puntos 22 a 27» y que en realidad pasan todas por la línea 91
(`bytes.byteLength !== Number(declared)`), de modo que la guarda de la línea 88 se puede borrar
entera sin que ninguna fila proteste. La prueba describía «falla», no describía la conducta.

**Cuadre corregido de los 110: 71 cerrados con pruebas, 12 equivalentes declarados por mí (los 12
confirmados por el `mutation_tester`, uno de ellos matizado como inmatable y no como equivalencia),
3 del punto ciego del corredor y 24 que seguían vivos.**

### Los 24 que seguían vivos, enumerados

En `src/calendar-feed-api.ts` (10):

| Mutante | Por qué sobrevivió |
| --- | --- |
| `36:55` ArrowFunction | Con `undefined`, `typeof status.active` lanza `TypeError`: sigue rechazando. Falta fijar el error, no sólo que lo haya. |
| `54:53` ArrowFunction | Lo mismo en la creación. |
| `79:3` CallExpression | Enmascarado por el `throwIfAborted` siguiente. |
| `90:3` CallExpression | Enmascarado por el anterior. Falta abortar **durante** `arrayBuffer()`. |
| `83:47` StringLiteral | **Equivalente**, y lo declara el propio `mutation_tester` corrigiendo su campaña previa: `""` y cualquier otra cadena fallan igual contra el patrón. |
| `88:7` ConditionalExpression | Los cinco del `Content-Length`: la línea 91 atrapa las seis filas de mi tabla por su cuenta, así que el `incompatible()` de la línea 88 **no se dispara en ninguna de las 98 pruebas**. |
| `88:7` LogicalOperator | Ídem. |
| `88:53` CallExpression | Ídem. |
| `88:21` Regex (ancla `^`) | Ídem. |
| `88:21` Regex (ancla `$`) | Ídem. |

En `src/calendar.tsx` (14):

| Mutante | Por qué sobrevivió |
| --- | --- |
| `55:5`, `67:21`, `77:12` OptionalChaining | **Equivalentes** bajo la invariante de renderizado. Además describí mal el mutador: `a?.b` pasa a `a.b`, no a «acceso sin llamada». |
| `56:6`, `64:6`, `88:5`, `128:6` ArrayDeclaration | Huecos reales: el efecto se remonta en cada render. El de `128:6` es el más instructivo — **escribí la prueba que debía matarlo** y no lo mata, porque la guarda `if (pending.current) return` tapa la relectura mientras la primera petición sigue en vuelo. Un enmascaramiento entre dos mutantes que yo había puesto en bandos distintos: uno como equivalente y otro como cerrado. |
| `75:20` ConditionalExpression | Hueco real: falta el caso en que el foco no está ni en el `body` ni en el iniciador. |
| `142:7` CallExpression | Hueco real enmascarado por el desmontaje de la sección al revocar. |
| `143:7`, `300:15` CallExpression | **Equivalentes** por el mismo argumento que usé para `135:7`; los declaré cerrados en vez de equivalentes, que es una incoherencia mía. |
| `149:7` StringLiteral | **Equivalente**: `download` siempre pasa `onFailure`, así que su `failureKind` es código muerto. |
| `187:21` ConditionalExpression | Hueco real **con conducta rota visible**. Cerrado en el ciclo 20, abajo. |
| `192:9` ConditionalExpression | **Equivalente**: última de cuatro guardas, alcanzable sólo con `failure === "download"`. |

Tras el ciclo 20 quedan **23** de esos 24 vivos, de los cuales el `mutation_tester` declara
equivalentes 7 (`83:47`, `55:5`, `67:21`, `77:12`, `143:7`, `149:7`, `192:9`): **16 huecos reales
abiertos**, ninguno bloqueante y todos enumerados arriba para quien los quiera cerrar.

## 2. Sí toqué producción, y lo dije de forma que inducía a error

Escribí que las pruebas nuevas «no cambian ni una línea de producción». Eso es cierto de `8527823`
—su diff son dos ficheros de prueba y la bitácora— pero **el marco daba a entender que la subida del
score se explicaba sólo por oráculos nuevos, y no es exacto**: el commit anterior de este mismo
carril, `a158cf2`, sí cambió producción. Sustituyó el `<input>` del enlace por un
`<textarea readOnly spellCheck={false}>` que envuelve, porque el `text-overflow: ellipsis` del
`input` recortaba la url y @s38 prohíbe literalmente que ningún ancho la recorte. El cambio está
justificado y medido (el E2E comprueba `scrollWidth <= clientWidth` en 42 combinaciones y bajo zoom
nativo), pero tiene dos consecuencias que debí anotar y no anoté: **el denominador pasa de 383 a
384** y aparece un mutante nuevo, `249:25` (`spellCheck={false}` a `true`), que es mío y sigue vivo
porque ninguna prueba afirma que el campo de la url no lleve corrección ortográfica.

## 3. Los tres de `Intl` seguían vivos; ahí sí acerté el diagnóstico

`testsCompleted=0` y `static=true` confirman que son punto ciego del corredor, no un hueco de las
pruebas. Acerté la causa, pero los conté como «atribuidos», no como cerrados, y así siguen.

### Ciclo 20 — @s31 @s35 @s36: no se ofrece reintentar lo que ha ido bien

El superviviente `187:21` fuerza el operando izquierdo de
`retriable = failure !== null && failure !== "limit"` y deja `retriable = failure !== "limit"`. Con
`failure === null` eso es `true`, y como el `{retriable && …}` no está anidado bajo ningún
`{failure && …}`, **el botón «Reintentar» aparece en pantalla sin que nada haya fallado**. De mis 96
pruebas, la única que afirmaba su ausencia lo hacía con `failure === "limit"`, donde mutante y
original coinciden: por eso no lo notó nadie.

Rojo primero, y aquí el rojo sólo puede producirse contra el mutante, porque la expresión enviada es
correcta. Aplicado a mano `const retriable = true && failure !== "limit";`:

```
× @s31 @s35 @s36 no ofrece reintentar mientras nada ha fallado          (22 ms)
× @s35 reintentar aparece con el fallo y se retira cuando el paso sale bien (136 ms)
Tests  2 failed | 51 passed (53)
```

Fallan **exactamente las dos nuevas** y ninguna otra, que es la señal de que discriminan el cambio y
nada más. Restaurada la línea original, las 98 pasan.

Las dos pruebas: la primera recorre cuatro estados de camino feliz —recién cargada, con el enlace
recién creado, con el archivo preparado y tras revocar— y exige que «Reintentar» no exista en
ninguno; la segunda sujeta la cara complementaria, que tras un fallo recuperable sí aparece y **se
retira** cuando el paso vuelve a salir bien. Sin la segunda, la primera se podría satisfacer
borrando el botón.

`src/calendar.test.tsx` pasa de 51 a 53 pruebas: **98 en total** con el cliente, todas verdes, con
`eslint`, `prettier --check` y `tsc --noEmit` limpios. `git diff --stat` de este ciclo toca **sólo**
`src/calendar.test.tsx`; `src/calendar.tsx` queda byte a byte como estaba.

No relanzo la mutación: es la puerta del `mutation_tester` y la lanza el coordinador.
