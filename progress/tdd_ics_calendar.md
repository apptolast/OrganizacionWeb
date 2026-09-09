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

Backend, 95 pruebas en 8 clases. Frontend, 39 pruebas en 2 ficheros.

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
| @s38 | `e2e/ics-calendar.spec.mjs` (axe y barrido 320/768/1280 en cinco estados) — PENDIENTE DE EJECUTAR |

## Lo que este carril NO ha ejecutado

Por la disciplina de recursos de la sesión (cinco carriles compartiendo la máquina) no se han
lanzado ni la suite completa del backend, ni la suite completa de Vitest, ni Playwright, ni PIT, ni
Stryker. En consecuencia:

- `e2e/ics-calendar.spec.mjs` está escrito y pasa `node --check`, pero nunca se ha ejecutado. @s38 y
  la parte E2E de @s29 siguen sin evidencia. Es lo primero que debe correr quien integre.
- Los rangos linea:columna de `stryker.ics-calendar.config.json` sobre `App.tsx` y `workspace.tsx`
  se han calculado leyendo el fichero, no ejecutando Stryker; si alguien reformatea esos ficheros
  habrá que recalcularlos.
- La mutación (PIT y Stryker) queda para el `mutation_tester`.
- El estado de la feature 26 en `feature_list.json` sigue en `in_progress`: no le corresponde a
  este agente marcarlo `done`.

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
