# Mutación backend19: revisión semanal

Gate global de mutación SUPERADO con límites: **178/190 KILLED estrictos = 93,6842105263%**, umbral80. XML original:178 KILLED,6 SURVIVED y6 NO_COVERAGE; cero TIMED_OUT, NON_VIABLE, MEMORY_ERROR, RUN_ERROR, NOT_STARTED o STARTED. No se reclasificó ningún estado ni se ejecutó replay. Este resultado no cierra por sí solo19 ni acredita UI/UX/despliegue.

Root autorizó tras init integrado EXIT0 d8ca05. El arnés `node .harness/harness.mjs mutate weekly_review-backend` terminó **EXIT0 18d5a2**,10m51s Gradle/10m45s PIT (cobertura2m59s, mutación7m44s). Log original weekly_review_pit.log y EXIT weekly_review_pit_exit.txt. Scope: siete patrones, nueve fuentes, candidatos JUnit completos, cuatro workers y umbral80, sin cambios de filtros/timeouts.366 clases candidatas,13 unidades; estos conteos no equivalen a190 mutantes ni a100 ejemplos Gherkin.

## Preservación

388 inputs iguales antes y después, comprobación ab83aa; sin nuevos inputs versionados. Ambos manifiestos SHA **1FA459E2E381F0C224623693849954399A49EB9B1CC49800CC94B9936603E67D**. XML/HTML original copiado íntegro a `progress/weekly_review_pit_final`; XML SHA **E4AF020394AF9D4518C7E5D0B301D48B8F2BFF0A2FF38B836FAC5C1752FB8DC1**. Inventario completo weekly_review_pit_inventory.json y12 residuos en weekly_review_pit_residuals.json, identificados por clase/método/descriptor/línea/mutador/índice/bloque, sin ordinales inestables. Artifact manifest enumera todos los archivos preservados con SHA.

## Resultado por clase

| Clase | KILLED | SURVIVED | NO_COVERAGE |
| --- | ---: | ---: | ---: |
| adapter.config.ApplicationConfiguration | 36 | 0 | 0 |
| adapter.http.WeeklyReviewController | 11 | 0 | 0 |
| adapter.http.WeeklyReviewController$DayResponse | 6 | 0 | 0 |
| adapter.http.WeeklyReviewController$Response | 11 | 0 | 0 |
| adapter.http.WeeklyReviewController$TotalsResponse | 3 | 0 | 0 |
| adapter.persistence.PostgresWeeklyReviewQueries | 38 | 0 | 0 |
| adapter.persistence.PostgresWeeklyReviewQueries$SessionWork | 10 | 0 | 0 |
| application.ReadWeeklyReview | 18 | 2 | 0 |
| domain.WeeklyReview | 11 | 0 | 0 |
| domain.WeeklyReview$Day | 6 | 0 | 0 |
| domain.WeeklyReview$Totals | 3 | 0 | 0 |
| domain.WeeklyReviewWindow | 20 | 4 | 6 |
| domain.WeeklyReviewWindow$Interval | 5 | 0 | 0 |

## Seis supervivientes diferenciables

Todos proceden de dos clases; **no se consideran equivalentes**. La lectura de fuentes y de los tests actuales identifica huecos acotados de frontera, no un defecto observado en la implementación original.

- `ReadWeeklyReview.get`, línea25, ConditionalsBoundaryMutator, índice16/bloque4: cambia el rechazo `date.year >9999` por `>=9999`. Línea26, mismo mutador, índice27/bloque9: hace lo mismo con el año del domingo. La fecha pública válida9999-12-20 debe producir semana20–26; el caso actual9999-12-31 sólo comprueba rechazo de semana incompleta y no diferencia esos mutantes.
- `WeeklyReviewWindow.summarize(List,List,long)`, línea31, ConditionalsBoundaryMutator, índice21/bloque5: el Clock local de año1 pasa a rechazarse (`<1`→`<=1`). El test PG de fecha año1 usa un Clock2026, por lo que no comprueba esta condición. Línea31, índice25/bloque7: `>9999`→`>=9999` para Clock local. Fecha y Clock válidos en9999 permiten distinguirlo.
- Misma clase/método, línea35, ConditionalsBoundaryMutator, índice62/bloque19: domingo con año9999 pasa a rechazarse. La última semana completa20–26 de diciembre es pública y representable en UTC; no es la fila imposible ya corregida en@s6.
- Misma clase/método, línea29, VoidMethodCallMutator, índice5/bloque0: elimina `requirePublicInstant(serverNow)`. El test actual Clock+10000 en UTC también falla por año local, ocultando la eliminación. Un Clock UTC `0000-12-31T23:30:00Z`, zona catalogada `Etc/GMT-14` (local0001-01-01) y fecha explícita segura2026-09-07 debe seguir dando409 por instante UTC público no representable. Sin esa guarda, la fecha local y la semana seleccionada son válidas y el resultado podría publicar un serverNow fuera del contrato. Es un oráculo de reloj permitido por la estrategia existente, no una fecha query inválida.

**Propuesta para revisión, no ejecutada:** máximo tres oráculos de aplicación, sin producción: (1) fecha y Clock válidos en0001 UTC, siete días y extremos exactos; (2) fecha y Clock9999-12-20 UTC, semana20–26 y extremo exclusivo27, cubre cuatro firmas superiores; (3) Clock UTC fuera de rango con zona que lo lleva al año local válido y fecha explícita segura, exige excepción409. Se escribirían individualmente, documentando inicialmenteGREEN si corresponde. Sólo después de autorización podría medirse un replay dirigido; no es necesario repetir la campaña global ni perseguir100%.

## Seis NO_COVERAGE

`WeeklyReviewWindow`, línea9, índice5/bloque0: accessors `availabilityZoneId`, `budgets`, `date`, `serverNow`, `zoneId` y `zoneSource`. Mutadores EmptyObjectReturnVals para strings/map y NullReturnVals para date/serverNow. Son accessors generados del record interno, no getters del DTO público WeeklyReview (estos están medidos y KILLED). El flujo real aplica la función de ventana, llama emptyReview/summarize y consume el DTO resultante; summarize usa los campos internos directamente. La búsqueda de consumidores y la lectura del adaptador no muestran invocaciones a esos accessors. No se propone prueba reflexiva ni acceso artificial para matar código sin consumidor. Se mantienen6NC en el denominador, sin declarar equivalencia general de la clase ni ocultar la carencia de cobertura.

No hay errores de ejecución que diagnosticar. No se modificó fuente, test ni configuración durante la campaña. Root decidirá sobre los tres refuerzos propuestos; el resultado original93,6842% y sus residuos quedan preservados.
