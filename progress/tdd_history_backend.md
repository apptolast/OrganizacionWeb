# TDD backend18 — historial propio

## 1. Nominal compilable @s1

ReadHistoryTest.s1_returnsTheOriginalSessionStartWithCurrentContextLabels exige consulta por owner, filtros y cursor completos, con SessionStart original conservado y etiquetas actuales. RED de compilación real ff11af por tipos/puertos ausentes; GREEN1f83d9 con caso de uso y frontera real. No PG/HTTP aún; mock sólo del puerto de lectura. HistoryEntry<D> conserva el tipo del detalle existente al construir cada entrada y la página heterogénea usa HistoryEntry<?>; no DTO HTTP en dominio, ObjectMapper ni dependencia de infraestructura.

Formato: 2e010d ignoró glob relativo;2a400e con rutas absolutas sólo informó IS DIRTY por el hook IDE, no aplicó. SpotlessJavaApply real84183d aplicó formato y foco ReadHistoryTest **1/1 GREEN93bad2** después. No se presenta el hook anterior como formato aplicado. No test global/init repetido.

Primer bundle compilable congelado en history_first_bundle.json: ocho fuentes application y un test. ReadHistoryUseCase.list(owner,HistoryFilters,HistoryCursor) devuelve HistoryPage; Queries.list misma entrada devuelve List<HistoryEntry<?>>. Filters(category,projectId,taskId,from,to); Cursor(owner,filters,upper,after); Position(occurredAt,type,id). El HTTP de C valida sintaxis; propiedad y luego semántica del cursor se completarán en adaptador PG. Primer caso terminal únicamente: paginación21 y cincofuentes/validación/RR/errores pendientes, no feature completa ni stubs PG.

Fuentes del bundle quietas para revisión/commit selectivo root. No Git mutado por autor, migración ni consumer. C avisado de firmas reales; no debe copiar archivos en vuelo.

## 2–5. Nominal PG, owner y lookahead

- @s1 PG HistoryQueriesPersistenceTest.s1_readsTheOriginalStartWithCurrentLabelsFromPostgres: RED853637 adapter ausente; GREEN7e7475 con consulta real PostgreSQL y comparación de fila original/outbox vacío. Primer adapter sólo sesión; no afirma cincofuentes ni filtros ya implementados.
- @s3 PG.s3_anotherOwnerCannotReadASessionStart: REDdc49fc devolvía fila ajena; GREEN4ee979 añade owner tanto en sesión como en proyecto y join task/project coherente.
- @s6 ReadHistoryTest.s6_lookaheadCreatesACursorFromTheLastDeliveredFact: incidente de captura genérica en fixture2a5d96 corregido antes de RED funcional4011bc (devolvía21); GREENaab706 limita20 y crea cursor último entregado/primero inicial.
- @s14 ReadHistoryTest.s14_continuationKeepsTheInitialUpperInsteadOfTheCurrentFirstRow: RED2199d4 reemplazaba upper por primera fila actual; GREEN8598dd conserva upper recibido.

Root aprobó y versionó primer bundle2e60131, liberado. Sin cambios de firmas. V14 mostraba M pero git diff específico ab0c7e no contiene cambios; no fue editada por este autor.

## 6–9. Fuentes durables restantes

- @s1 transición de tarea: RED b924a7 por ausencia de fuente; GREEN 42bc95 (tres PG). Incidente de compilación db25aa por inferencia de Class en el mapper corregido separadamente.
- @s1 pausa durable junto a inicio: incidente genérico f09744 del fixture corregido; RED af5dbd devolvía sólo inicio. GREEN verificado en XML 495baf, un caso sin fallos, tras incorporar receipt durable sin derivarlo de la proyección.
- @s1 planificación con destino futuro: RED 91d67a devolvía vacío; GREEN 6a441e devuelve createdAt y PlannedBlock original, sin usar startAt como fecha del hecho.
- @s1 cancelación: RED 6542b0 faltaba recibo; GREEN 49622d conserva ambas entradas y lee BlockChangeReceipt original. Cinco fuentes nominales disponibles; filtros, orden SQL y transacción aún pendientes. No se atribuye cobertura completa de las variantes de @s1.

## 10–14. Orden, lookahead SQL y categorías

- @s6 PG.s6_databaseReturnsOnlyTwentyOneFactsInDescendingTime: RED 82d533, GREEN bad18f. Veintidós transiciones coherentes; SQL entrega sólo21 en orden temporal inverso.
- @s13 y @s4 PG.s13_equalTimesUseSourceRankThenUnsignedUuidDescending: RED 1a53d5, GREEN aabda4. Empates reales, UUID extremos y misma UUID en familias distintas; orden nativo PostgreSQL, sin comparación UUID firmada en Java.
- Refactor de variable SQL en verde: regresión de los ocho PG y tres application GREEN d6a170.
- @s7 categoría sessions: RED 25f5fc, GREEN 60bfb7; planning: RED 1a8a08, GREEN d61cd0; task-status: RED 48162b, GREEN 4fba3a. Cada caso conserva entrada positiva y excluye otra familia.

## 15–23. Contexto, vínculo del cursor y fechas

- @s9 proyecto desconocido: RED 450811, GREEN e09e1b; tarea de otro proyecto propio: RED a34bbb/60864a, GREEN c30a46. No se oculta 404 como lista vacía.
- @s8 proyecto idea propio vacío frente a hechos de otro: RED 11d813, GREEN 8aff65; tarea completed vacía frente a hermana con hechos: RED c7ec08, GREEN 61a896. SQL filtra contexto después de confirmar propiedad.
- @s17 owner del cursor: RED 79d6fb, GREEN c2d99b, con field cursor / INVALID_VALUE. @s16 propiedad antes de vínculo: inicialmente GREEN 385ffc, sin cambio de producción ni RED inventado.
- @s17 filtros del cursor: RED c53964, GREEN 11701d; upper inferior a after en empate de instante: RED e06ca0, GREEN 11b539. Comparación de posiciones por instante, rank y UUID textual canónica, consistente con orden PostgreSQL.
- @s14 continuación estricta por familia/UUID: RED aae4dd, GREEN 9c5ab4. SQL aplica ambas fronteras; no necesita consultar existencia de la posición.
- @s10 día UTC inclusivo: RED 20cec1, GREEN 343024. Se incluyen medianoche y último microsegundo; se excluyen días adyacentes. Comparación de fecha UTC explícita sin construir año10000.

Este corte todavía no incorpora transacción RR/read-only, traducción de errores/cierre, validación semántica de recibo seleccionado, commits tardíos ni wiring. Tampoco afirma que las variantes heredadas y los cinco owners estén acreditados por un único ejemplo.

Formato real y regresión focal conjunta GREEN 01ce2f: 21 PG + 3 application, cero fallos/errores/omitidos. XML preservados en history_queries_checkpoint y cuatro archivos propios congelados en history_queries_checkpoint.json para revisión selectiva. Fuente/test frontend en WIP pertenecen a B y no forman parte del paquete.

## 24–27. Transacción y conexión real de beans

Root aprobó el checkpoint de consultas en06bd830, liberando las cuatro fuentes/tests.

- @s22 HistoryReadTransactionTest.s22_pageRunsInsideReadOnlyRepeatableRead: RED 0a6047 observó readOnly off, GREEN 0f8bd9 observa on/repeatable read dentro de la consulta PostgreSQL. Constructor del adapter pasa a (JdbcTemplate, PlatformTransactionManager, ObjectMapper); puerto application intacto, consumidores PG ajustados mecánicamente.
- @s23 cierre de transacción: RED d761a9, GREEN c0f448; manager delegado completa doCommit y lanza TransactionSystemException, capturada fuera de execute. No página vacía ante fallo.
- @s23 SQL real: RED 270040, GREEN 4da4d0; tabla de este contenedor renombrada/restaurada en finally y DataAccessException traducida. No test global ni cambio de otra fuente.
- @s1 ApplicationWiringTest.history_s1_readBeanUsesTheRealQueries: RED 1758e2 por bean ausente, GREEN 73e1f2 con ReadHistory y PostgresHistoryQueries reales. JdbcTemplate y manager simulados en este contexto: alcanza el adapter y su rechazo de contexto; no se atribuye PostgreSQL ni HTTP reales a este caso.

Cierre de este checkpoint: formato y regresión GREEN4382ad; los cinco XML preservados en history_wiring_checkpoint suman **56** casos (21 PG consultas +3 PG transacción +3 application +22 wiring +7 configuración), todos sin fallos/errores/omitidos. El mensaje de entrega indicó57 por error de suma; los XML y conteos por suite siempre fueron56. Manifiesto de cinco archivos history_wiring_checkpoint.json, SHA D433DFF3ABDA3091643F763450AEA1BF04950DBCA9C90C6CE3A43302B0920BA5. Se conserva el límite de la prueba wiring: infraestructura simulada, adaptador real.
