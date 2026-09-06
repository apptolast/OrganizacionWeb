# Mutación frontend: completar y reabrir tareas

## Campaña original preservada

Autorizada por `progress/judge_complete_reopen_task_frontend.md`; fuente congelada y baseline normal **625/625** con lint/build verdes. Stryker terminó con **EXIT 0 en 13 min 45 s: 415 Killed, 1 Timeout, 49 Survived, 0 NoCoverage y 0 errores; 416/465 = 89,46 %**. El timeout cuenta como detectado según Stryker, pero no se presenta como una aserción Killed.

Perfil `frontend/stryker.complete-reopen-task.config.json`, concurrency 2 y umbral 80, sin exclusiones nuevas. Completos: `task-state.tsx`, `task-history.tsx`, `task-status-api.ts`. Rangos: `tasks-api.ts:87` y `:115`, `project-tasks.tsx:103–105`, `task-reader.tsx:107–111`. Se instrumentaron 6 archivos; el montaje JSX no generó mutantes. El global conserva historia y añade los tres módulos nuevos.

Original intacto: `frontend/reports/mutation-complete-reopen-task/mutation-original.json`; JSON/HTML de la campaña en el mismo directorio. El dry run informó **581 tests**, contador separado de los 625 casos normales. El sandbox contiene los 17 archivos; raíz verificó los 44 registros de `task-state.test.tsx` y su participación en `killedBy` de 220 mutantes. No se deduce equivalencia de los dos contadores.

## Revisión y refuerzos

Revisiones independientes: `progress/review_complete_reopen_task_api_mutants.md` (raíz) y `progress/review_complete_reopen_task_frontend_mutants.md` (integración). La decisión posterior de raíz considera los espacios 67/212 brechas de presentación, corrigiendo su propuesta inicial como variantes.

Inventario original: **25 brechas, 16 equivalentes, 8 variantes permitidas**, más el timeout 475 pendiente de replay separado. No hay NoCoverage que justificar.

| Clasificación | Identidades originales |
| --- | --- |
| Brechas de TaskHistory | 26,31,48,53,67,74,78,79,87 |
| Brechas de TaskState | 90,118,136,140,170,174,193,212 |
| Brechas de API | 296,303,304,305,411,437,440,441 |
| Equivalentes de TaskHistory | 20,51,54,83 |
| Equivalentes de TaskState | 106,112,115,116,131,137,155,171 |
| Equivalentes de API | 282,299,300,322 |
| Variantes permitidas de TaskHistory | 15,16,80 |
| Variantes permitidas de TaskState | 91,98,99,100,102 |

Los equivalentes se sustentan en montaje/ref disponible, valores ya retirados, guardas de interfaz, counters usados sólo como identidad o validación posterior ISO. Las variantes cambian tiempos permitidos de foco o disponibilidad de volver durante carga; no se afirma que produzcan DOM idéntico. Véanse los dictámenes para el razonamiento por identidad.

Refuerzos observables: segunda recuperación y segunda confirmación; carga sin entradas antiguas; ausencia de éxito inicial; foco perdido por disabled (simulación JSDOM ya usada en otras suites); StrictMode no cooperativo; denegación tardía sobre TaskReader aún vivo; validación de fechas, cursores y UUID JSON; separación legible de fecha. No se exponen hooks ni se crean controles de producto para activar carreras.

Único ajuste de presentación tras campaña: espacio JSX explícito antes de UTC en TaskState, respaldado por DOM real observado por integración. Se conserva el instante original. El espacio no cambia líneas de lógica ni se utiliza para inferir un nuevo resultado global. Build y lint focal verdes; capturas reconstruidas por integración.

## Replays separados

`stryker.complete-reopen-task.replay.config.json`: 89 mutantes de 3 archivos, sesión32983, para las 23 brechas de lógica. Se añadieron 11 casos UI y 10 API: ejecuciones focales **55 UI** y **110 API** verdes. Resultado: **83/89 = 93,26 %, EXIT 0, 3 min 7 s**, sin timeout ni errores. Las 23 identidades objetivo quedaron Killed por comparación exacta sensible a mayúsculas; los 6 restantes son equivalentes o variantes ya revisados.

Replay pequeño (`timeout-replay.json`): **9/10 = 90 %, EXIT 0, 1 min 3 s**. 67 y 212 quedaron Killed. El timeout original 475 reapareció como Survived: la guarda completa de estados podía desaparecer sin detección. Se añadió un único caso DTO8 con status active y error controlado, verde junto a las 6 pruebas de compatibilidad anteriores. Esto eleva las brechas reales totales a 26, contando el antiguo timeout; no cambia las 16 equivalencias y 8 variantes restantes.

Replay final de línea 87 (`status-replay.json`): **8/8 Killed, 100 %, EXIT 0 en 1 min 17 s**, sin timeout ni errores. El original475 corresponde al replay0 y queda Killed. No se suman puntuaciones ni se declara una nueva ejecución global.

Validación normal independiente final: **646/646 en 17 archivos**, más **7/7 focales de compatibilidad** tras añadir el único caso nuevo. No se presenta una corrida global de 647. Lint focal y formato verdes; build de la fuente final con espacio UTC verde.


### Mapeo exacto de objetivos

| Archivo | Original → replay principal (Killed) |
| --- | --- |
| task-history.tsx | 26→1,31→5,48→6,53→8,74→10,78→11,79→12,87→15 |
| task-state.tsx | 90→16,118→17,136→19,140→22,170→28,174→31,193→32 |
| task-status-api.ts | 296→33,303→40,304→41,305→42,411→58,437→64,440→67,441→68 |

Replay pequeño: original67→0 Killed;212→1 Killed;475→2 Survived. No confundir la numeración local de cada JSON. El match usa archivo, ubicación completa, mutador y replacement exactos; en regex distingue `\d` de `\D`.

Los 6 supervivientes del principal corresponden a originales54,80,137,171,299,300, sin huecos nuevos. La justificación independiente de los 24 restantes está enlazada arriba; no se duplica otra tabla exhaustiva.


## Liberación

Las 26 brechas reales (25 supervivientes y el timeout) tienen detección Killed focal confirmada. Permanecen 16 equivalencias y 8 variantes contractualmente permitidas, revisadas de forma independiente. Original y tres replays se conservan separados. No quedan procesos de mutación ni cambios de fuente pendientes; archivos liberados al coordinador para cierre, sin commits del autor.
