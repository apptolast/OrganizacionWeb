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
