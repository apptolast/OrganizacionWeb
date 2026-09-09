# TDD — Feature 26 `ics_calendar`

Worktree `C:/Users/vhurt/ow-worktrees/ics-calendar`, rama `claude/ics-calendar`. Contrato:
`features/ics_calendar.feature` (@s1–@s38). Ponytail full y Caveman lite aplicados.

Nota: `.memoria-cache/patterns/` no existe en este worktree (sin sincronización de memoria
organizacional); se trabaja con las plantillas del repositorio (feature 24, export_data, today).

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
