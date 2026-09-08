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
